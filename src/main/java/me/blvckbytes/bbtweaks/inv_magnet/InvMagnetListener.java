package me.blvckbytes.bbtweaks.inv_magnet;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class InvMagnetListener implements Listener, Tickable {

  // Per minecraft-wiki, it's 1.425, but I'd rather remain on the low side of that.
  // I'm aware that it's not just a simple radius in vanilla, but rather a hitbox distance.
  private static final double VANILLA_PICKUP_RADIUS = 1.42;

  private final InvMagnetParametersStore parametersStore;

  private final Int2ObjectMap<EntityAttractionSession> perTickAttractionSessionByEntityId;

  public InvMagnetListener(
    InvMagnetParametersStore parametersStore
  ) {
    this.parametersStore = parametersStore;

    this.perTickAttractionSessionByEntityId = new Int2ObjectArrayMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    attractNearbyItemsAndOrbs(relativeTime);
  }

  private void attractNearbyItemsAndOrbs(long relativeTime) {
    for (var world : Bukkit.getWorlds()) {
      perTickAttractionSessionByEntityId.clear();

      for (var player : world.getPlayers()) {
        if (player.getGameMode() != GameMode.SURVIVAL)
          continue;

        var parameter = parametersStore.accessParameters(player);

        if (parameter.updateLimitsAndConstrain() == null)
          continue;

        double effectiveRadius = parameter.getRadius();
        var isMagnetDisabled = !parameter.isEnabled() || effectiveRadius <= 0;

        if (isMagnetDisabled)
          effectiveRadius = VANILLA_PICKUP_RADIUS;

        // Attract near their chest
        var playerLocation = player.getLocation().add(0, .75, 0);

        for (var nearbyEntity : player.getNearbyEntities(effectiveRadius, effectiveRadius, effectiveRadius)) {
          if (nearbyEntity.isDead() || !nearbyEntity.isValid())
            continue;

          if (nearbyEntity instanceof Item item) {
            if (item.getPickupDelay() > 0)
              continue;

            var itemStack = item.getItemStack();

            if (parameter.didFailAttemptRecently(itemStack, relativeTime))
              continue;

            var attractEvent = new PreAttractItemEvent(player, itemStack);

            Bukkit.getPluginManager().callEvent(attractEvent);

            if (attractEvent.isCancelled() || !attractEvent.canHoldSome()) {
              parameter.submitFailedAttempt(itemStack, relativeTime);
              continue;
            }
          }

          else if (!(nearbyEntity instanceof ExperienceOrb))
            continue;

          perTickAttractionSessionByEntityId
            .computeIfAbsent(nearbyEntity.getEntityId(), _ -> new EntityAttractionSession(nearbyEntity))
            .attractOrClearIfClosest(
              nearbyEntity,
              playerLocation,
              // Do not actually attract if the current player has their magnet disabled,
              // but still cause the attraction of a further-away player to be cancelled,
              // such that they cannot "steal" the item the player is trying to pick up.
              isMagnetDisabled
            );
        }
      }
    }
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    parametersStore.accessParameters(event.getPlayer()).updateLimitsAndConstrain();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
  public void onPreAttractItem(PreAttractItemEvent event) {
    var inventory = event.getPlayer().getInventory();
    var attractedItem = event.getAttractedItem();

    for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        event.markCanHoldSome();
        return;
      }

      if (!attractedItem.isSimilar(currentItem))
        continue;

      var remainingSpace = currentItem.getMaxStackSize() - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      event.markCanHoldSome();
      return;
    }
  }
}
