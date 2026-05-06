package me.blvckbytes.bbtweaks.shulker_accessor.change_detection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InventoryChangeDetector implements Listener {

  private final List<ChangeDetectionHolder> observedHolders;

  public InventoryChangeDetector(Plugin plugin) {
    this.observedHolders = new ArrayList<>();

    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      for (var iterator = observedHolders.iterator(); iterator.hasNext();) {
        var observedHolder = iterator.next();

        if (!observedHolder.isValid()) {
          Bukkit.getPluginManager().callEvent(new InventoryInvalidatedEvent(observedHolder));
          iterator.remove();
          continue;
        }

        if (!observedHolder.isDirty())
          continue;

        observedHolder.clearDirty();

        Bukkit.getPluginManager().callEvent(new InventoryChangedEvent(observedHolder));
      }
    }, 0, 0);
  }

  public Collection<ChangeDetectionHolder> getObservedHolders() {
    return Collections.unmodifiableCollection(observedHolders);
  }

  @EventHandler
  public void onDisable(PluginDisableEvent event) {
    new ArrayList<>(observedHolders).forEach(ChangeDetectionHolder::closeAll);
    observedHolders.clear();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (!(event.getInventory().getHolder() instanceof ChangeDetectionHolder holder))
      return;

    holder.incrementViewCount();

    if (holder.getViewCount() > 1)
      return;

    if (observedHolders.stream().anyMatch(it -> it.getIdentifier() == holder.getIdentifier()))
      return;

    observedHolders.add(holder);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof ChangeDetectionHolder holder))
      return;

    holder.decrementViewCount();

    if (holder.getViewCount() > 0)
      return;

    observedHolders.removeIf(it -> it.getIdentifier() == holder.getIdentifier());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var inventory = player.getOpenInventory().getTopInventory();

    if (!(inventory.getHolder() instanceof ChangeDetectionHolder holder))
      return;

    var action = event.getAction();

    // Only mark as dirty if the action actually affected the top inventory, as to
    // save on needless write-backs in cases where no delta occurred.

    if (
      action == InventoryAction.MOVE_TO_OTHER_INVENTORY
        || action == InventoryAction.HOTBAR_SWAP
        || action == InventoryAction.COLLECT_TO_CURSOR
        || action == InventoryAction.UNKNOWN
    ) {
      holder.markDirty();
      return;
    }

    if (event.getRawSlot() >= inventory.getSize())
      return;

    holder.markDirty();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var inventory = player.getOpenInventory().getTopInventory();

    if (!(inventory.getHolder() instanceof ChangeDetectionHolder holder))
      return;

    // Let's rather not get too smart about change-detection on this one.

    holder.markDirty();
  }
}
