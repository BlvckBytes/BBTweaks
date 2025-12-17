package me.blvckbytes.bbtweaks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.BlockTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RDBreakTool implements Listener {

  private final NamespacedKey rdBreakerKey;
  private final Logger logger;
  private final Plugin plugin;

  public RDBreakTool(Plugin plugin) {
    this.rdBreakerKey = new NamespacedKey(plugin, "rd_breaker");
    this.logger = plugin.getLogger();
    this.plugin = plugin;
  }

  @SuppressWarnings("UnstableApiUsage")
  public void modifyItemToBecomeRdBreaker(ItemStack item) {
    var meta = item.getItemMeta();
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

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBlockBreak(BlockBreakEvent event) {
    var player = event.getPlayer();

    if (player.getGameMode() != GameMode.SURVIVAL)
      return;

    var block = event.getBlock();

    if (block.getType() != Material.REINFORCED_DEEPSLATE)
      return;

    var toolUsed = player.getInventory().getItemInMainHand();

    if (toolUsed.getType().isAir())
      return;

    var toolMeta = toolUsed.getItemMeta();

    if (toolMeta == null)
      return;

    var toolPdc = toolMeta.getPersistentDataContainer();
    var rdMode = toolPdc.get(rdBreakerKey, PersistentDataType.BYTE);

    if (rdMode == null || rdMode <= 0)
      return;

    if (!block.getDrops().isEmpty() || !block.getDrops(toolUsed).isEmpty()) {
      logger.log(Level.WARNING, "Refrained from responding to the break-event of a reinforced deepslate, since the drop-list was not empty");
      return;
    }

    // Spawn next tick, as to ensure that the block is gone and the item won't glitch around
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      block.getWorld().dropItemNaturally(
        block.getLocation().add(.5, .5, .5),
        new ItemStack(Material.REINFORCED_DEEPSLATE)
      );
    }, 1L);
  }
}
