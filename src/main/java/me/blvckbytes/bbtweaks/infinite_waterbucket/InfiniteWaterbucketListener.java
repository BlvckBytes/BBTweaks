package me.blvckbytes.bbtweaks.infinite_waterbucket;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import io.papermc.paper.persistence.PersistentDataContainerView;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.MarkerModifyError;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.durability_warnings.PlayerHand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class InfiniteWaterbucketListener implements Listener, Tickable {

  private record BucketSlot(int slot, long relativeTime) {}

  private final Plugin plugin;
  private final NamespacedKey bucketMarkerKey;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, BucketSlot> bucketSlotByPlayerId;

  private long relativeTime;

  public InfiniteWaterbucketListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.bucketMarkerKey = new NamespacedKey(plugin, "infinite-waterbucket");
    this.config = config;

    this.bucketSlotByPlayerId = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    var player = event.getPlayer();
    var playerInventory = player.getInventory();

    var heldItem = playerInventory.getItem(event.getHand());

    if (heldItem.getType() != Material.WATER_BUCKET || !doesContainMarker(heldItem.getPersistentDataContainer()))
      return;

    if (!player.hasPermission("bbtweaks.infinite-waterbucket")) {
      config.rootSection.infiniteWaterbucket.noPermission.sendMessage(player);
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

  public @Nullable MarkerModifyError modifyItemToBecomeInfiniteWaterBucket(ItemStack item) {
    if (item.getType() != Material.WATER_BUCKET)
      return MarkerModifyError.WRONG_ITEM_TYPE;

    var meta = Objects.requireNonNull(item.getItemMeta());
    var pdc = meta.getPersistentDataContainer();

    var existingValue = pdc.get(bucketMarkerKey, PersistentDataType.BOOLEAN);

    if (existingValue != null && existingValue)
      return MarkerModifyError.ALREADY_MARKED;

    pdc.set(bucketMarkerKey, PersistentDataType.BOOLEAN, true);

    meta.lore(config.rootSection.infiniteWaterbucket.lore.interpret(SlotType.ITEM_LORE, null));
    meta.displayName(config.rootSection.infiniteWaterbucket.name.interpret(SlotType.ITEM_NAME, null).getFirst());
    meta.setEnchantmentGlintOverride(config.rootSection.infiniteWaterbucket.glint);

    item.setItemMeta(meta);

    return null;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean doesContainMarker(PersistentDataContainerView pdcView) {
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

    var newBucketItem = new ItemStack(Material.WATER_BUCKET);
    modifyItemToBecomeInfiniteWaterBucket(newBucketItem);
    playerInventory.setItem(bucketSlot.slot, newBucketItem);
  }
}
