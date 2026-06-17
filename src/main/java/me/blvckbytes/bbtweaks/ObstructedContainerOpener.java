package me.blvckbytes.bbtweaks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class ObstructedContainerOpener implements Listener {

  private static final Set<Material> SUPPORTED_CONTAINER_TYPES;

  static {
    SUPPORTED_CONTAINER_TYPES = new HashSet<>();
    SUPPORTED_CONTAINER_TYPES.add(Material.CHEST);
    SUPPORTED_CONTAINER_TYPES.add(Material.TRAPPED_CHEST);
    SUPPORTED_CONTAINER_TYPES.addAll(Tag.SHULKER_BOXES.getValues());
  }

  private final Plugin plugin;

  public ObstructedContainerOpener(Plugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInteract(PlayerInteractEvent event) {
    var clickedBlock = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null)
      return;

    if (event.getHand() != EquipmentSlot.HAND)
      return;

    var player = event.getPlayer();

    if (player.isSneaking())
      return;

    var blockType = clickedBlock.getType();

    if (!SUPPORTED_CONTAINER_TYPES.contains(blockType))
      return;

    if (!(clickedBlock.getState() instanceof Container container))
      return;

    var containerInventory = container.getInventory();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var topInventory = player.getOpenInventory().getTopInventory();

      // Assume that opening the container succeeded.
      if (topInventory.getHolder() instanceof Container)
        return;

      player.openInventory(containerInventory);
    }, 1);
  }
}
