package me.blvckbytes.bbtweaks.durability_warnings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DurabilityWarningsListener implements Listener {

  private final WarningsProfileStore profileStore;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  private final Map<UUID, DurabilityTrackingSession> trackingSessionByPlayerId;

  public DurabilityWarningsListener(
    WarningsProfileStore profileStore,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.profileStore = profileStore;
    this.config = config;
    this.plugin = plugin;

    this.trackingSessionByPlayerId = new HashMap<>();
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    var playerHand = PlayerHand.getFromEquipmentSlot(event.getHand());

    if (playerHand != null)
      onPossibleItemUse(event.getPlayer(), playerHand);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    onPossibleItemUse(event.getPlayer(), PlayerHand.MAIN_HAND);
  }

  @EventHandler
  public void onDamageEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player)
      onPossibleItemUse(player, PlayerHand.MAIN_HAND);
  }

  @EventHandler
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    var playerHand = PlayerHand.getFromEquipmentSlot(event.getHand());

    if (playerHand != null)
      onPossibleItemUse(event.getPlayer(), playerHand);
  }

  @EventHandler
  public void onBowShoot(EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    var playerHand = PlayerHand.getFromEquipmentSlot(event.getHand());

    if (playerHand != null)
      onPossibleItemUse(player, playerHand);
  }

  @EventHandler
  public void onFish(PlayerFishEvent event) {
    var playerHand = PlayerHand.getFromEquipmentSlot(event.getHand());

    if (playerHand != null)
      onPossibleItemUse(event.getPlayer(), playerHand);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    trackingSessionByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onSlotChanged(PlayerInventorySlotChangeEvent event) {
    var trackingSession = trackingSessionByPlayerId.get(event.getPlayer().getUniqueId());

    if (trackingSession != null)
      trackingSession.submitSlotUpdate(event.getSlot(), event.getNewItemStack());
  }

  private void onPossibleItemUse(Player player, PlayerHand hand) {
    if (!player.hasPermission("bbtweaks.durabilitywarning"))
      return;

    var profile = profileStore.accessProfile(player);

    if (!profile.enabled)
      return;

    var inventory = player.getInventory();
    var targetItem = hand.accessItem(inventory);

    if (targetItem == null)
      return;

    if (targetItem.getType().getMaxDurability() <= 0)
      return;

    var applicativeWarnings = config.rootSection.durabilityWarnings.getApplicativeWarningsByItemType(targetItem.getType());

    if (applicativeWarnings.isEmpty())
      return;

    if (!(targetItem.getItemMeta() instanceof Damageable priorDamageable))
      return;

    var priorDamage = priorDamageable.getDamage();

    var slotIndex = hand.accessSlotIndex(inventory);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!(targetItem.getItemMeta() instanceof Damageable postDamageable))
        return;

      var postDamage = postDamageable.getDamage();
      var damageDelta = postDamage - priorDamage;

      if (damageDelta <= 0)
        return;

      trackingSessionByPlayerId
        .computeIfAbsent(player.getUniqueId(), k -> new DurabilityTrackingSession(profile, config))
        .submitDamageUpdate(slotIndex, targetItem, postDamage, applicativeWarnings);
    }, 1L);
  }
}
