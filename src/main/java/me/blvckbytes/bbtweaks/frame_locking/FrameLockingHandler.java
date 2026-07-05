package me.blvckbytes.bbtweaks.frame_locking;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class FrameLockingHandler implements Listener {

  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey lockedFrameKey;

  public FrameLockingHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.config = config;

    this.lockedFrameKey = new NamespacedKey(plugin, "locked-frame");
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onInteractAtEntity(PlayerInteractEntityEvent event) {
    if (handleFrameLockToggle(event.getPlayer(), event.getRightClicked())) {
      event.setCancelled(true);
      return;
    }

    var frame = getFrameIfLocked(event.getRightClicked());

    if (frame == null)
      return;

    event.setCancelled(true);

    var frameFace = frame.getAttachedFace();
    var mountedOnBlock = frame.getLocation().getBlock().getRelative(frameFace);

    if (!(mountedOnBlock.getState() instanceof Container container))
      return;

    event.getPlayer().openInventory(container.getInventory());
  }

  @EventHandler
  public void onHangingBreak(HangingBreakEvent event) {
    var cause = event.getCause();

    // Let it still be destroyed by physics/obstruction, as to not end up
    // with forever-hanging-in-air item-frames.
    if (cause != HangingBreakEvent.RemoveCause.ENTITY && cause != HangingBreakEvent.RemoveCause.EXPLOSION)
      return;

    if (getFrameIfLocked(event.getEntity()) != null)
      event.setCancelled(true);
  }

  @EventHandler
  public void onDamageByEntity(EntityDamageByEntityEvent event) {
    if (getFrameIfLocked(event.getEntity()) != null)
      event.setCancelled(true);
  }

  @EventHandler
  public void onDamage(EntityDamageEvent event) {
    if (getFrameIfLocked(event.getEntity()) != null)
      event.setCancelled(true);
  }

  private @Nullable ItemFrame getFrameIfLocked(Entity entity) {
    if (!(entity instanceof ItemFrame frame))
      return null;

    var lockedValue = frame.getPersistentDataContainer().get(lockedFrameKey, PersistentDataType.BOOLEAN);

    if (lockedValue == null || !lockedValue)
      return null;

    return frame;
  }

  private boolean handleFrameLockToggle(Player player, Entity entity) {
    if (!(entity instanceof ItemFrame itemFrame))
      return false;

    if (!player.isSneaking())
      return false;

    var heldItem = player.getInventory().getItemInMainHand();

    if (heldItem.getType() != Material.TRIAL_KEY)
      return false;

    //noinspection UnstableApiUsage
    var fakeBreakEvent = new BlockBreakEvent(entity.getLocation().getBlock(), player);
    Bukkit.getPluginManager().callEvent(fakeBreakEvent);

    if (fakeBreakEvent.isCancelled())
      return false;

    var pdc = itemFrame.getPersistentDataContainer();

    var lockedValue = pdc.get(lockedFrameKey, PersistentDataType.BOOLEAN);

    var environment = new InterpretationEnvironment()
      .withVariable("x", (int) itemFrame.getX())
      .withVariable("y", (int) itemFrame.getY())
      .withVariable("z", (int) itemFrame.getZ());

    if (lockedValue != null && lockedValue) {
      pdc.set(lockedFrameKey, PersistentDataType.BOOLEAN, false);
      config.rootSection.frameLocking.nowUnlocked.sendMessage(player, environment);
      return true;
    }

    pdc.set(lockedFrameKey, PersistentDataType.BOOLEAN, true);
    config.rootSection.frameLocking.nowLocked.sendMessage(player, environment);
    return true;
  }
}
