package me.blvckbytes.bbtweaks.rd_breaker;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.BlockTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class RDBreakerListener implements Listener {

  // The time it takes to break a block of reinforced deepslate with the RD-breaker.
  // 82.5s = 1650 ticks, we mine with 32x speed, so 51.5625 ticks = ~52.
  private static final int BREAKING_TIME_T = 52;

  private final NamespacedKey rdBreakerKey;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final FloodgateIntegration floodgateIntegration;

  private final Map<UUID, BukkitTask> blockBreakTaskByPlayerId;

  public RDBreakerListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration
  ) {
    this.rdBreakerKey = new NamespacedKey(plugin, "rd_breaker");

    this.plugin = plugin;
    this.config = config;
    this.floodgateIntegration = floodgateIntegration;

    this.blockBreakTaskByPlayerId = new HashMap<>();
  }

  @SuppressWarnings("UnstableApiUsage")
  public void modifyItemToBecomeRdBreaker(ItemStack item) {
    var meta = item.getItemMeta();

    meta.displayName(config.rootSection.rdBreaker.itemName.interpret(SlotType.ITEM_NAME, null).getFirst());
    meta.lore(config.rootSection.rdBreaker.itemLore.interpret(SlotType.ITEM_LORE, null));

    meta.removeEnchantments();
    meta.addEnchant(Enchantment.UNBREAKING, 3, false);
    meta.addEnchant(Enchantment.MENDING, 1, false);

    meta.getPersistentDataContainer().set(rdBreakerKey, PersistentDataType.BYTE, (byte) 1);

    item.setItemMeta(meta);

    var blocks = RegistrySet.keySet(
      RegistryKey.BLOCK,
      BlockTypeKeys.REINFORCED_DEEPSLATE
    );

    item.setData(
      DataComponentTypes.TOOL,
      Tool.tool()
        .addRule(Tool.rule(blocks, 32f, TriState.TRUE))
        .build()
    );
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockDamage(BlockDamageEvent event) {
    var player = event.getPlayer();

    // For some odd reason, the block never breaks when on a Bedrock-Client. But they also do
    // not abort block-damage, so if we just simulate the break manually after the required
    // damage-time elapsed, we can also fully support Bedrock, without much additional work.
    if (!floodgateIntegration.isFloodgatePlayer(player))
      return;

    cancelTaskIfAny(player);

    var block = event.getBlock();

    handleIfUsedRdBreakerOnBlock(player, block, () -> {
      var fakeBreakTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
        simulateBlockBreak(player, block);
      }, BREAKING_TIME_T);

      blockBreakTaskByPlayerId.put(player.getUniqueId(), fakeBreakTask);
    });
  }

  @EventHandler
  public void onBlockDamageAbort(BlockDamageAbortEvent event) {
    cancelTaskIfAny(event.getPlayer());
  }

  private void handleIfUsedRdBreakerOnBlock(Player player, Block block, Runnable handler) {
    if (player.getGameMode() != GameMode.SURVIVAL)
      return;

    if (block.getType() != Material.REINFORCED_DEEPSLATE)
      return;

    var toolUsed = player.getInventory().getItemInMainHand();

    if (toolUsed.getType().isAir())
      return;

    var toolPdc = toolUsed.getPersistentDataContainer();
    var rdMode = toolPdc.get(rdBreakerKey, PersistentDataType.BYTE);

    if (rdMode == null || rdMode <= 0)
      return;

    if (!block.getDrops().isEmpty() || !block.getDrops(toolUsed).isEmpty()) {
      plugin.getLogger().log(Level.WARNING, "Refrained from responding to the break-event of a reinforced deepslate, since the drop-list was not empty");
      return;
    }

    handler.run();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBlockBreak(BlockBreakEvent event) {
    var block = event.getBlock();

    cancelTaskIfAny(event.getPlayer());

    handleIfUsedRdBreakerOnBlock(event.getPlayer(), block, () -> dropItemNextTick(block));
  }

  private void dropItemNextTick(Block block) {
    // Spawn next tick, as to ensure that the block is gone and the item won't glitch around
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      block.getWorld().dropItemNaturally(
        block.getLocation().add(.5, .5, .5),
        new ItemStack(Material.REINFORCED_DEEPSLATE)
      );
    }, 1L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    cancelTaskIfAny(event.getPlayer());
  }

  private void cancelTaskIfAny(Player player) {
    var task = blockBreakTaskByPlayerId.remove(player.getUniqueId());

    if (task != null)
      task.cancel();
  }

  private void simulateBlockBreak(Player player, Block block) {
    var blockData = block.getBlockData();

    if (blockData.getMaterial() != Material.REINFORCED_DEEPSLATE)
      return;

    //noinspection UnstableApiUsage
    var breakEvent = new BlockBreakEvent(block, player);
    Bukkit.getPluginManager().callEvent(breakEvent);

    if (breakEvent.isCancelled())
      return;

    player.incrementStatistic(Statistic.MINE_BLOCK, blockData.getMaterial());

    var world = block.getWorld();

    // For some reason, the particle-effects always concentrate at the bottom
    // of the block, so let's bump it up by another half of a unit.
    var blockCenter = block.getLocation().add(.5, 1, .5);

    world.spawnParticle(
      Particle.BLOCK,
      blockCenter,
      55,
      .3, .3, .3,
      blockData
    );

    world.playSound(
      blockCenter,
      blockData.getSoundGroup().getBreakSound(),
      SoundCategory.BLOCKS,
      1.0f,
      1.0f
    );

    block.setType(Material.AIR);
  }
}
