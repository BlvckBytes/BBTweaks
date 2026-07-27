package me.blvckbytes.bbtweaks.entity_eggs;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;

public class EntityEggsListener implements Listener, Tickable {

  private static final long MAX_DOUBLE_CLICK_DELAY_MS = 800;
  private static final long LOG_FLUSH_PERIOD_T = 20;

  private record TimeAndEntityId(long time, long relativeTime, int entityId) {}

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final Map<UUID, TimeAndEntityId> lastInteractionByPlayerId;

  private final File captureLogFile;
  private final List<String> linesToLog = new ArrayList<>();

  private long relativeTime;

  public EntityEggsListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) throws Exception {
    this.plugin = plugin;
    this.config = config;
    this.lastInteractionByPlayerId = new HashMap<>();

    this.captureLogFile = new File(plugin.getDataFolder(), "entity-eggs-log.txt");

    if (!captureLogFile.exists()) {
      if (!captureLogFile.createNewFile())
        throw new IllegalArgumentException("Could not create " + captureLogFile);
    } else if (!captureLogFile.isFile())
      throw new IllegalArgumentException("Expected file at " + captureLogFile);
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;

    if (relativeTime % LOG_FLUSH_PERIOD_T != 0 || linesToLog.isEmpty())
      return;

    var textToAppend = String.join("\n", linesToLog) + "\n";
    linesToLog.clear();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        Files.write(captureLogFile.toPath(), textToAppend.getBytes(), StandardOpenOption.APPEND);
      } catch (Throwable e) {
        plugin.getLogger().log(Level.WARNING, "An error occurred while trying to append to the entity-eggs log", e);
      }
    });
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (handleRightClickInteraction(event.getPlayer(), event.getRightClicked()))
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteract(PlayerInteractEvent event) {
    if (!event.getAction().isRightClick())
      return;

    if (handleRightClickInteraction(event.getPlayer(), null))
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    var holder = event.getInventory().getHolder(false);

    if (holder instanceof Entity entity) {
      if (handleRightClickInteraction(player, entity))
        event.setCancelled(true);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lastInteractionByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private boolean handleRightClickInteraction(Player player, @Nullable Entity entity) {
    if (!player.isSneaking())
      return false;

    var heldItem = player.getInventory().getItemInMainHand();

    if (heldItem.getType() != Material.EGG || heldItem.getAmount() < 1)
      return false;

    if (!player.hasPermission("bbtweaks.entity-eggs"))
      return false;

    var lastInteraction = lastInteractionByPlayerId.get(player.getUniqueId());

    if (lastInteraction != null && relativeTime - lastInteraction.relativeTime <= 1)
      return true;

    if (entity == null) {
      var rayTraceResult = player.rayTraceEntities(config.rootSection.entityEggs.rayTraceDistance);

      if (rayTraceResult != null)
        entity = rayTraceResult.getHitEntity();
    }

    if (!(entity instanceof LivingEntity livingEntity))
      return false;

    if (!livingEntity.getType().isSpawnable())
      return false;

    var environment = new InterpretationEnvironment()
      .withVariable("time", System.currentTimeMillis())
      .withVariable("player", player.getName())
      .withVariable("x", (int) livingEntity.getX())
      .withVariable("y", (int) livingEntity.getY())
      .withVariable("z", (int) livingEntity.getZ())
      .withVariable("world", player.getWorld().getName())
      .withVariable("entity_key", livingEntity.getType().translationKey())
      .withVariable("entity_type", livingEntity.getType().name());

    if (!canBuildAt(player, livingEntity.getLocation().getBlock())) {
      config.rootSection.entityEggs.cannotBuildAtEntity.sendMessage(player, environment);
      return false;
    }

    lastInteractionByPlayerId.put(player.getUniqueId(), new TimeAndEntityId(System.currentTimeMillis(), relativeTime, livingEntity.getEntityId()));

    if (
      lastInteraction == null
        || lastInteraction.entityId != livingEntity.getEntityId()
        || System.currentTimeMillis() - lastInteraction.time > MAX_DOUBLE_CLICK_DELAY_MS
    ) {
      return true;
    }

    var spawnEgg = captureEntityToEgg(livingEntity, environment);

    if (spawnEgg == null) {
      config.rootSection.entityEggs.couldNotDetermineEggType.sendMessage(player, environment);
      return true;
    }

    if (!player.getInventory().addItem(spawnEgg).isEmpty()) {
      config.rootSection.entityEggs.noSpaceInInventory.sendMessage(player, environment);
      return true;
    }

    var newHeldAmount = heldItem.getAmount() - 1;

    heldItem.setAmount(newHeldAmount);

    if (newHeldAmount <= 0)
      player.getInventory().setItemInMainHand(null);

    config.rootSection.entityEggs.captureSuccess.sendMessage(player, environment);

    linesToLog.add(config.rootSection.entityEggs.captureSuccessLog.asPlainString(environment));

    livingEntity.remove();
    return true;
  }

  @SuppressWarnings("UnstableApiUsage")
  private @Nullable ItemStack captureEntityToEgg(LivingEntity livingEntity, InterpretationEnvironment environment) {
    var snapshot = livingEntity.createSnapshot();

    if (snapshot == null)
      return null;

    var eggMaterial = getSpawnEggMaterial(snapshot.getEntityType());

    if (eggMaterial == null)
      return null;

    var spawnEgg = new ItemStack(eggMaterial, 1);

    if (!(spawnEgg.getItemMeta() instanceof SpawnEggMeta eggMeta))
      return null;

    eggMeta.setSpawnedEntity(snapshot);

    eggMeta.lore(
      config.rootSection.entityEggs.spawnEggLore.interpret(
        SlotType.ITEM_LORE,
        environment
          .withVariable("details", EntityDetailType.captureDetails(livingEntity, config))
      )
    );

    if (livingEntity.customName() != null)
      eggMeta.displayName(livingEntity.customName());

    spawnEgg.setItemMeta(eggMeta);

    return spawnEgg;
  }

  private static Material getSpawnEggMaterial(EntityType type) {
    try {
      return Material.valueOf(type.name() + "_SPAWN_EGG");
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  private static boolean canBuildAt(Player player, Block block) {
    //noinspection UnstableApiUsage
    var fakePlaceEvent = new BlockPlaceEvent(block, block.getState(), block, new ItemStack(Material.DIRT), player, false, EquipmentSlot.HAND);
    Bukkit.getPluginManager().callEvent(fakePlaceEvent);
    return !fakePlaceEvent.isCancelled();
  }
}
