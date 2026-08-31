package me.blvckbytes.bbtweaks.no_ai.simulator;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import io.papermc.paper.event.player.PlayerTradeEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.no_ai.TimeCache;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.MenuType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class VillagerBehaviorSimulator extends BehaviorSimulator<Villager> implements Listener {

  private static final int MAX_RESTOCKS_PER_DAY = 2;

  private static final long FIXED_RESTOCK_INTERVAL_TICKS = 20 * 60 * 2;
  private static final long RANDOM_RESTOCK_INTERVAL_TICKS = 20 * 30;
  private static final long MINIMUM_LAST_TRADE_AND_RESTOCK_DELAY_TICKS = 20 * 20;

  private static final Map<Villager.Profession, Sound> RESTOCK_SOUND_BY_PROFESSION;

  static {
    var soundMap = new HashMap<Villager.Profession, Sound>();

    soundMap.put(Villager.Profession.ARMORER, Sound.ENTITY_VILLAGER_WORK_ARMORER);
    soundMap.put(Villager.Profession.BUTCHER, Sound.ENTITY_VILLAGER_WORK_BUTCHER);
    soundMap.put(Villager.Profession.CARTOGRAPHER, Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER);
    soundMap.put(Villager.Profession.CLERIC, Sound.ENTITY_VILLAGER_WORK_CLERIC);
    soundMap.put(Villager.Profession.FARMER, Sound.ENTITY_VILLAGER_WORK_FARMER);
    soundMap.put(Villager.Profession.FISHERMAN, Sound.ENTITY_VILLAGER_WORK_FISHERMAN);
    soundMap.put(Villager.Profession.FLETCHER, Sound.ENTITY_VILLAGER_WORK_FLETCHER);
    soundMap.put(Villager.Profession.LEATHERWORKER, Sound.ENTITY_VILLAGER_WORK_LEATHERWORKER);
    soundMap.put(Villager.Profession.LIBRARIAN, Sound.ENTITY_VILLAGER_WORK_LIBRARIAN);
    soundMap.put(Villager.Profession.MASON, Sound.ENTITY_VILLAGER_WORK_MASON);
    soundMap.put(Villager.Profession.SHEPHERD, Sound.ENTITY_VILLAGER_WORK_SHEPHERD);
    soundMap.put(Villager.Profession.TOOLSMITH, Sound.ENTITY_VILLAGER_WORK_TOOLSMITH);
    soundMap.put(Villager.Profession.WEAPONSMITH, Sound.ENTITY_VILLAGER_WORK_WEAPONSMITH);

    RESTOCK_SOUND_BY_PROFESSION = Collections.unmodifiableMap(soundMap);
  }

  private final NamespacedKey
    keyLastRestocksTodayResetCheckWorldTime,
    keyNextPossibleRestockWorldTime,
    keyLastTradeWorldTime;

  public VillagerBehaviorSimulator(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config, Villager.class, EntityType.VILLAGER);

    this.keyLastRestocksTodayResetCheckWorldTime = new NamespacedKey(plugin, "last-restocks-today-reset-check-world-time");
    this.keyNextPossibleRestockWorldTime = new NamespacedKey(plugin, "next-possible-restock-world-time");
    this.keyLastTradeWorldTime = new NamespacedKey(plugin, "last-trade-world-time");
  }

  @Override
  public void sendStatusMessage(Player player, Entity entity) {
    ensureIsInstance(entity);

    var villager = mobType.cast(entity);
    var villagerWorld = villager.getWorld();
    var timeCache = TimeCache.captureCurrentTimes();

    var currentTime = timeCache.getFullTime(villagerWorld);

    var remainingRestockTime = 0L;

    var pdc = entity.getPersistentDataContainer();
    var lastTradeTime = pdc.get(keyLastTradeWorldTime, PersistentDataType.LONG);

    if (lastTradeTime != null)
      remainingRestockTime = Math.max(remainingRestockTime, MINIMUM_LAST_TRADE_AND_RESTOCK_DELAY_TICKS - (currentTime - lastTradeTime));

    var nextPossibleRestockTime = pdc.get(keyNextPossibleRestockWorldTime, PersistentDataType.LONG);

    if (nextPossibleRestockTime != null)
      remainingRestockTime = Math.max(remainingRestockTime, nextPossibleRestockTime - currentTime);

    config.rootSection.noAi.villagerStatusMessage.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("x", (int) villager.getX())
        .withVariable("y", (int) villager.getY())
        .withVariable("z", (int) villager.getZ())
        .withVariable("entity_key", villager.getType().translationKey())
        .withVariable("level", villager.getVillagerLevel())
        .withVariable("experience", villager.getVillagerExperience())
        .withVariable("within_working_window", timeCache.isNonNormalWorldOrDayTime(villager.getWorld()))
        .withVariable("restocks_today", villager.getRestocksToday())
        .withVariable("max_restocks_per_day", MAX_RESTOCKS_PER_DAY)
        .withVariable("remaining_restock_millis", (long) (remainingRestockTime / 20.0 * 1000.0))
        .withVariable("used_recipe_count", villager.getRecipes().stream().filter(recipe -> recipe.getUses() > 0).count())
        .withVariable("used_up_recipe_count", villager.getRecipes().stream().filter(recipe -> recipe.getUses() >= recipe.getMaxUses()).count())
        .withVariable("total_recipe_count", villager.getRecipes().size())
    );
  }

  @Override
  protected void simulate(Villager villager, TimeCache timeCache) {
    var actualLevel = villager.getVillagerLevel();
    var expectedLevel = getExpectedVillagerLevel(villager);
    var shouldLevelUp = actualLevel < expectedLevel;

    BukkitTask priorReopenTask = null;

    if (shouldLevelUp) {
      try {
        villager.increaseLevel(expectedLevel - actualLevel);
        villager.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false));
        priorReopenTask = reopenTradingWindowIfInUse(villager);
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "An error occurred while leveling up a villager at " + villager.getLocation(), e);
      }
    }

    var villagerWorld = villager.getWorld();

    if (!timeCache.isNonNormalWorldOrDayTime(villagerWorld))
      return;

    var currentTime = timeCache.getFullTime(villagerWorld);

    var pdc = villager.getPersistentDataContainer();

    var nextPossibleRestockTime = pdc.get(keyNextPossibleRestockWorldTime, PersistentDataType.LONG);
    if (nextPossibleRestockTime != null && currentTime < nextPossibleRestockTime)
      return;

    var lastTradeTime = pdc.get(keyLastTradeWorldTime, PersistentDataType.LONG);
    if (lastTradeTime != null && currentTime - lastTradeTime < MINIMUM_LAST_TRADE_AND_RESTOCK_DELAY_TICKS)
      return;

    resetRestocksTodayIfDayChanged(villager, timeCache);

    if (villager.getRestocksToday() >= MAX_RESTOCKS_PER_DAY)
      return;

    if (!hasAnyUsedRecipes(villager))
      return;

    var nextPossibleRestockIntervalTicks = FIXED_RESTOCK_INTERVAL_TICKS + (long) (Math.random() * RANDOM_RESTOCK_INTERVAL_TICKS);

    pdc.set(keyNextPossibleRestockWorldTime, PersistentDataType.LONG, currentTime + nextPossibleRestockIntervalTicks);

    var recipes = new ArrayList<>(villager.getRecipes());

    for (var recipe : recipes)
      recipe.setUses(0);

    villager.setRecipes(recipes);
    villager.setRestocksToday(villager.getRestocksToday() + 1);

    // Tell the villager to update the pricing of their trades.
    villager.updateDemand();

    var restockSound = RESTOCK_SOUND_BY_PROFESSION.get(villager.getProfession());

    if (restockSound != null)
      villager.getWorld().playSound(villager.getLocation(), restockSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);

    Particle.HAPPY_VILLAGER.builder()
      .location(villager.getEyeLocation())
      .offset(.4, .4, .4)
      .count(20)
      .spawn();

    if (reopenTradingWindowIfInUse(villager) != null && priorReopenTask != null)
      priorReopenTask.cancel();
  }

  private @Nullable BukkitTask reopenTradingWindowIfInUse(Villager villager) {
    var trader = villager.getTrader();

    if (trader instanceof Player player) {
      player.closeInventory();

      return Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!player.isOnline())
          return;

        openVillagerTradingMenu(player, villager);
      }, 1L);
    }

    return null;
  }

  @EventHandler
  public void onTrade(PlayerTradeEvent event) {
    if (!(event.getMerchant() instanceof Villager villager))
      return;

    if (!isEntityIdInSimulatedList(villager))
      return;

    var currentTime = villager.getWorld().getFullTime();

    villager.getPersistentDataContainer().set(keyLastTradeWorldTime, PersistentDataType.LONG, currentTime);
  }

  @SuppressWarnings("UnstableApiUsage")
  private void openVillagerTradingMenu(Player player, Villager villager) {
    var view = MenuType.MERCHANT.builder()
      .merchant(villager)
      .build(player);

    player.openInventory(view);
  }

  private void resetRestocksTodayIfDayChanged(Villager villager, TimeCache timeCache) {
    if (villager.getRestocksToday() <= 0)
      return;

    var currentTime = timeCache.getFullTime(villager.getWorld());

    var pdc = villager.getPersistentDataContainer();
    var lastTime = pdc.get(keyLastRestocksTodayResetCheckWorldTime, PersistentDataType.LONG);

    if (lastTime != null) {
      var lastDayIndex = lastTime / 24000L;
      var currentDayIndex = currentTime / 24000L;

      if (currentDayIndex > lastDayIndex)
        villager.setRestocksToday(0);
    }

    pdc.set(keyLastRestocksTodayResetCheckWorldTime, PersistentDataType.LONG, currentTime);
  }

  /**
   * Returns the villager level based on experience.
   * See <a href="https://minecraft.wiki/w/Trading#Level">Wiki</a>
   */
  private int getExpectedVillagerLevel(Villager villager) {
    var villagerExperience = villager.getVillagerExperience();

    // Master
    if (villagerExperience >= 250)
      return 5;

    // Expert
    if (villagerExperience >= 150)
      return 4;

    // Journeyman
    if (villagerExperience >= 70)
      return 3;

    // Apprentice
    if (villagerExperience >= 10)
      return 2;

    // Novice
    return 1;
  }

  private boolean hasAnyUsedRecipes(Villager villager) {
    for (var recipe : villager.getRecipes()) {
      if (recipe.getUses() > 0)
        return true;
    }

    return false;
  }
}
