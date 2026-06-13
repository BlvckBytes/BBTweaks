package me.blvckbytes.bbtweaks.rd_breaker;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.BlockTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import me.blvckbytes.bbtweaks.MainSection;
import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class RDBreakerListener implements Listener {

  private final NamespacedKey rdBreakerKey;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  public RDBreakerListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.rdBreakerKey = new NamespacedKey(plugin, "rd_breaker");

    this.plugin = plugin;
    this.config = config;
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
      plugin.getLogger().log(Level.WARNING, "Refrained from responding to the break-event of a reinforced deepslate, since the drop-list was not empty");
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
