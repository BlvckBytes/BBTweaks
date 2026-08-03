package me.blvckbytes.bbtweaks.inv_magnet;

import it.unimi.dsi.fastutil.ints.*;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Comparator;

public class InvMagnetListener implements Listener, Tickable {

  private record EntityAndDistance(Entity entity, double distanceSquared) {}

  private record AttractionInfo(long relativeTime, Player target) {}

  // Per minecraft-wiki, it's 1.425, but I'd rather remain on the low side of that.
  // I'm aware that it's not just a simple radius in vanilla, but rather a hitbox distance.
  private static final double VANILLA_PICKUP_RADIUS = 1.42;

  private static final int ATTEMPT_CLEANUP_PERIOD_T = 10;

  private static final int ATTRACTION_STAMP_MAX_AGE_T = 20;
  private static final int ATTRACTION_STAMP_CLEANUP_PERIOD_T = 2 * ATTRACTION_STAMP_MAX_AGE_T;

  private final InvMagnetParametersStore parametersStore;

  private final Int2ObjectMap<EntityAttractionSession> perTickAttractionSessionByEntityId;
  private final Int2ObjectMap<AttractionInfo> attractionInfoByEntityId;

  private long relativeTime;

  public InvMagnetListener(
    InvMagnetParametersStore parametersStore
  ) {
    this.parametersStore = parametersStore;

    this.perTickAttractionSessionByEntityId = new Int2ObjectArrayMap<>();
    this.attractionInfoByEntityId = new Int2ObjectArrayMap<>();
  }

  public boolean didAttractToRecently(Entity entity, Player target) {
    var attractionInfo = attractionInfoByEntityId.get(entity.getEntityId());

    if (attractionInfo == null || attractionInfo.target != target)
      return false;

    return relativeTime - attractionInfo.relativeTime <= ATTRACTION_STAMP_MAX_AGE_T;
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;

    if (relativeTime % ATTRACTION_STAMP_CLEANUP_PERIOD_T == 0)
      attractionInfoByEntityId.values().removeIf(info -> relativeTime - info.relativeTime > ATTRACTION_STAMP_MAX_AGE_T);

    attractNearbyItemsAndOrbs(relativeTime);
  }

  private void attractNearbyItemsAndOrbs(long relativeTime) {
    for (var world : Bukkit.getWorlds()) {
      perTickAttractionSessionByEntityId.clear();

      for (var player : world.getPlayers()) {
        if (player.getGameMode() != GameMode.SURVIVAL)
          continue;

        var parameters = parametersStore.accessParameters(player);

        if (relativeTime % ATTEMPT_CLEANUP_PERIOD_T == 0)
          parameters.cleanupExpiredAttempts(relativeTime);

        if (parameters.updateLimitsAndConstrain() == null)
          continue;

        double effectiveRadius = parameters.getRadius();
        var isMagnetDisabled = !parameters.isEnabled() || effectiveRadius <= 0;

        if (isMagnetDisabled)
          effectiveRadius = VANILLA_PICKUP_RADIUS;

        // Attract near their chest
        var playerLocation = player.getLocation().add(0, .75, 0);

        // Always attract the closest entities first, as to ensure consistent behavior.
        // We don't know what order the nearby lookup will hand us.
        var nearbyEntitiesInOrder = player
          .getNearbyEntities(effectiveRadius, effectiveRadius, effectiveRadius)
          .stream()
          .map(entity -> new EntityAndDistance(entity, entity.getLocation().distanceSquared(playerLocation)))
          .sorted(Comparator.comparing(EntityAndDistance::distanceSquared))
          .map(EntityAndDistance::entity)
          .toList();

        for (var nearbyEntity : nearbyEntitiesInOrder) {
          if (nearbyEntity.isDead() || !nearbyEntity.isValid())
            continue;

          if (nearbyEntity instanceof Item item) {
            // Important! We need some tolerance here, as we do not know when we execute; it could be that vanilla pickup
            // happens right after our attraction and then, if they decrement the delay by one, the stack within reach
            // will be picked up immediately, having had us attract a remote stack for nothing, which looks bad. We take
            // at least one tick to attract anyway, so this should be a solution with no downsides at all.
            if (item.getPickupDelay() > 1)
              continue;

            var itemStack = item.getItemStack();

            if (parameters.didFailAttemptAndNotSucceedOnceRecently(itemStack, relativeTime))
              continue;

            var attractEvent = new PreAttractItemEvent(player, itemStack, parameters);

            Bukkit.getPluginManager().callEvent(attractEvent);

            if (attractEvent.isCancelled() || !attractEvent.canHoldSome()) {
              parameters.submitFailedAttempt(itemStack, relativeTime);
              continue;
            }

            parameters.submitSuccessfulAttempt(itemStack, relativeTime);
          }

          else if (!(nearbyEntity instanceof ExperienceOrb))
            continue;

          var entityId = nearbyEntity.getEntityId();

          var didAttract = perTickAttractionSessionByEntityId
            .computeIfAbsent(entityId, _ -> new EntityAttractionSession(nearbyEntity))
            .attractOrClearIfClosest(
              nearbyEntity,
              playerLocation,
              // Do not actually attract if the current player has their magnet disabled,
              // but still cause the attraction of a further-away player to be cancelled,
              // such that they cannot "steal" the item the player is trying to pick up.
              isMagnetDisabled
            );

          if (didAttract)
            attractionInfoByEntityId.put(entityId, new AttractionInfo(relativeTime, player));
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
    var attractedItem = event.getAttractedItem();

    var simulatingInventory = event.getParameters().getSimulatingInventoryForCurrentTick(relativeTime);
    var addedAmount = simulatingInventory.addItemAndGetAddedAmount(attractedItem, attractedItem.getAmount());

    if (addedAmount > 0)
      event.markCanHoldSome();
  }
}
