package me.blvckbytes.bbtweaks.shulker_accessor.change_detection;

import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class InventoryChangeDetector implements Listener, Disableable, Tickable {

  private final List<ChangeDetectionHolder> observedHolders;

  public InventoryChangeDetector() {
    this.observedHolders = new ArrayList<>();
  }

  @Override
  public void tick(long relativeTime) {
    tickObservedHolders();
  }

  public void manuallyRegisterHolder(ChangeDetectionHolder holder) {
    if (observedHolders.stream().noneMatch(it -> it == holder))
      observedHolders.add(holder);
  }

  public Collection<ChangeDetectionHolder> getObservedHolders() {
    return Collections.unmodifiableCollection(observedHolders);
  }

  @Override
  public void disable() {
    new ArrayList<>(observedHolders).forEach(ChangeDetectionHolder::closeAll);
    observedHolders.clear();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryOpen(InventoryOpenEvent event) {
    tryAccessHolder(event, (player, holder) -> {
      holder.incrementViewCount();
      holder.onInventoryOpen(player);

      if (holder.getViewCount() > 1)
        return;

      manuallyRegisterHolder(holder);
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    tryAccessHolder(event, (player, holder) -> {
      holder.decrementViewCount();
      holder.onInventoryClose(player);

      if (holder.getViewCount() > 0)
        return;

      observedHolders.removeIf(it -> it == holder);
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onEarlyClick(InventoryClickEvent event) {
    tryAccessHolder(event, (player, holder) -> {
      if (holder.isLocked() || !holder.isValid())
        event.setCancelled(true);
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onClick(InventoryClickEvent event) {
    tryAccessHolder(event, (player, holder) -> {
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

      if (event.getRawSlot() < 0 || event.getRawSlot() >= holder.getInventory().getSize())
        return;

      holder.markDirty();
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onEarlyDrag(InventoryDragEvent event) {
    tryAccessHolder(event, (player, holder) -> {
      if (holder.isLocked() || !holder.isValid())
        event.setCancelled(true);
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onDrag(InventoryDragEvent event) {
    tryAccessHolder(event, (player, holder) -> {
      // Let's rather not get too smart about change-detection on this one.
      holder.markDirty();
    });
  }

  private void tryAccessHolder(InventoryEvent event, BiConsumer<Player, ChangeDetectionHolder> handler) {
    if (!(event.getView().getPlayer() instanceof Player player))
      return;

    var inventory = player.getOpenInventory().getTopInventory();

    if (!(inventory.getHolder() instanceof ChangeDetectionHolder holder))
      return;

    handler.accept(player, holder);
  }

  private void tickObservedHolders() {
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
  }
}
