package me.blvckbytes.bbtweaks.infinite_bucket;

import io.papermc.paper.persistence.PersistentDataContainerView;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.durability_warnings.PlayerHand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class InfiniteBucketListener implements Listener, Tickable {

  private record BucketSlot(int slot, long relativeTime) {}

  private final Material fullBucketType;
  private final NamespacedKey bucketMarkerKey;
  private final String usePermission;
  private final Supplier<InfiniteBucketSection> sectionSupplier;

  private final Plugin plugin;

  private final Map<UUID, BucketSlot> bucketSlotByPlayerId;

  private long relativeTime;

  public InfiniteBucketListener(
    Material fullBucketType,
    NamespacedKey bucketMarkerKey,
    String usePermission,
    Supplier<InfiniteBucketSection> sectionSupplier,
    Plugin plugin
  ) {
    this.fullBucketType = fullBucketType;
    this.bucketMarkerKey = bucketMarkerKey;
    this.usePermission = usePermission;
    this.sectionSupplier = sectionSupplier;

    this.plugin = plugin;

    this.bucketSlotByPlayerId = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockDispense(BlockDispenseEvent event) {
    if (doesContainMarker(event.getItem().getPersistentDataContainer()))
      event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    var player = event.getPlayer();
    var playerInventory = player.getInventory();

    var heldItem = playerInventory.getItem(event.getHand());

    if (heldItem.getType() != fullBucketType || !doesContainMarker(heldItem.getPersistentDataContainer()))
      return;

    if (!player.hasPermission(usePermission)) {
      sectionSupplier.get().noPermission.sendMessage(player);
      event.setCancelled(true);
      return;
    }

    var playerId = player.getUniqueId();
    var playerHand = PlayerHand.getFromEquipmentSlot(event.getHand());

    if (playerHand == null || bucketSlotByPlayerId.containsKey(playerId)) {
      event.setCancelled(true);
      return;
    }

    bucketSlotByPlayerId.put(playerId, new BucketSlot(playerHand.accessSlotIndex(playerInventory), relativeTime));

    Bukkit.getScheduler().runTaskLater(plugin, () -> tryRestoringBucket(player), 1);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBucketFill(PlayerBucketFillEvent event) {
    var player = event.getPlayer();

    var bucketSlot = bucketSlotByPlayerId.get(player.getUniqueId());

    if (bucketSlot == null)
      return;

    if (event.getHand() == EquipmentSlot.HAND) {
      if (player.getInventory().getHeldItemSlot() == bucketSlot.slot) {
        event.setCancelled(true);
        return;
      }
    }

    if (event.getHand() == EquipmentSlot.OFF_HAND) {
      if (PlayerHand.OFFHAND_SLOT_INDEX == bucketSlot.slot)
        event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onDropItem(PlayerDropItemEvent event) {
    var player = event.getPlayer();

    // Yes, this will also fire if they dropped by interacting with a slot via the click-event, but that's
    // essentially impossible to pull off as a non-hacking player, so I don't care if they lose their bucket
    // while trying to outsmart the system with a race-condition - not worth the additional complexity.
    removeBucketSlotIfMatches(player, player.getInventory().getHeldItemSlot());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var playerInventory = player.getInventory();

    if (event.getClickedInventory() != playerInventory)
      return;

    removeBucketSlotIfMatches(player, event.getSlot());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onSwapHands(PlayerSwapHandItemsEvent event) {
    var player = event.getPlayer();

    removeBucketSlotIfMatches(player, PlayerHand.OFFHAND_SLOT_INDEX);
    removeBucketSlotIfMatches(player, player.getInventory().getHeldItemSlot());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    tryRestoringBucket(event.getPlayer());
  }

  public @Nullable MarkerModifyError modifyItemToBecomeInfiniteBucket(ItemStack item) {
    if (item.getType() != fullBucketType)
      return MarkerModifyError.WRONG_ITEM_TYPE;

    var meta = Objects.requireNonNull(item.getItemMeta());
    var pdc = meta.getPersistentDataContainer();

    var existingValue = pdc.get(bucketMarkerKey, PersistentDataType.BOOLEAN);

    if (existingValue != null && existingValue)
      return MarkerModifyError.ALREADY_MARKED;

    pdc.set(bucketMarkerKey, PersistentDataType.BOOLEAN, true);

    sectionSupplier.get().applyToMeta(meta);

    item.setItemMeta(meta);

    return null;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean doesContainMarker(PersistentDataContainerView pdcView) {
    var markerFlag = pdcView.get(bucketMarkerKey, PersistentDataType.BOOLEAN);
    return markerFlag != null && markerFlag;
  }

  private void removeBucketSlotIfMatches(Player player, int slot) {
    var playerId = player.getUniqueId();

    var bucketSlot = bucketSlotByPlayerId.get(playerId);

    if (bucketSlot != null && bucketSlot.slot == slot)
      bucketSlotByPlayerId.remove(playerId);
  }

  private void tryRestoringBucket(Player player) {
    var bucketSlot = bucketSlotByPlayerId.remove(player.getUniqueId());

    if (bucketSlot == null || relativeTime - bucketSlot.relativeTime > 1)
      return;

    var playerInventory = player.getInventory();
    var bucketItem = playerInventory.getItem(bucketSlot.slot);

    if (bucketItem == null || bucketItem.getType() != Material.BUCKET)
      return;

    var newBucketItem = new ItemStack(fullBucketType);
    modifyItemToBecomeInfiniteBucket(newBucketItem);
    playerInventory.setItem(bucketSlot.slot, newBucketItem);
  }
}
