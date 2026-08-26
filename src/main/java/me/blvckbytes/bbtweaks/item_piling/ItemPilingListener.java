package me.blvckbytes.bbtweaks.item_piling;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.item_piling.preferences.ItemPilingPreferencesStore;
import me.blvckbytes.bbtweaks.item_piling.preferences.PreferenceFlag;
import me.blvckbytes.bbtweaks.util.CompactId;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ItemPilingListener implements Listener, Tickable, PileEntityMetadataKeeper {

  private record ItemStackBucket(ItemStack stack, List<Item> items) {}

  private final ItemPilingPreferencesStore preferencesStore;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  private final Int2ObjectMap<AmountAndType> pileEntityMetadataByEntityId;
  private final IntSet playerDroppedItemEntityIds;

  public ItemPilingListener(
    ItemPilingPreferencesStore preferencesStore,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.preferencesStore = preferencesStore;
    this.config = config;
    this.plugin = plugin;

    this.pileEntityMetadataByEntityId = new Int2ObjectOpenHashMap<>();
    this.playerDroppedItemEntityIds = new IntArraySet();
  }

  @Override
  public void storePileMetadata(int entityId, AmountAndType data) {
    pileEntityMetadataByEntityId.put(entityId, data);
  }

  @Override
  public @Nullable AmountAndType getPileMetadata(int entityId) {
    return pileEntityMetadataByEntityId.get(entityId);
  }

  public ItemPile getPile(Item itemEntity) {
    return new ItemPile(itemEntity, this, config, plugin);
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % config.rootSection.itemPiling.periodTicks == 0) {
      for (var world : Bukkit.getWorlds())
        tryPileItems(world.getEntitiesByClass(Item.class));
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onItemDrop(PlayerDropItemEvent event) {
    playerDroppedItemEntityIds.add(event.getItemDrop().getEntityId());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onVehicleDestroy(VehicleDestroyEvent event) {
    var vehicle = event.getVehicle();

    if (!(vehicle instanceof InventoryHolder inventoryHolder))
      return;

    var vehicleInventory = inventoryHolder.getInventory();
    var vehicleWorld = vehicle.getWorld();
    var dropLocation = vehicle.getLocation().add(.5, .5, .5);

    var itemsToDrop = new ArrayList<Item>();

    for (var contentItem : vehicleInventory.getContents()) {
      if (!(ItemUtil.isStackValid(contentItem)))
        continue;

      var itemEntity = vehicleWorld.createEntity(dropLocation, Item.class);

      itemEntity.setItemStack(contentItem);
      itemEntity.setPickupDelay(10);

      itemsToDrop.add(itemEntity);
    }

    vehicleInventory.clear();

    pileAndPossiblyRemoveItems(itemsToDrop, false);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      for (var itemToDrop : itemsToDrop) {
        if (!itemToDrop.isInWorld())
          itemToDrop.getWorld().addEntity(itemToDrop);
      }
    }, 1L);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBlockDropItem(BlockDropItemEvent event) {
    var stackWithExisting = false;

    // Do not immediately stack the contents of containers, as that then
    // leaves out visual feedback, probably having players be confused.
    if (!(event.getBlockState() instanceof Container)) {
      var preferences = preferencesStore.accessPreferences(event.getPlayer());
      stackWithExisting = preferences.flags.contains(PreferenceFlag.IMMEDIATELY_STACK_BLOCK_BREAK_ITEMS);
    }

    pileAndPossiblyRemoveItems(event.getItems(), stackWithExisting);
  }

  private void pileAndPossiblyRemoveItems(List<Item> items, boolean stackWithExisting) {
    var buckets = new ArrayList<ItemStackBucket>();

    for (var currentItem : items) {
      var currentStack = currentItem.getItemStack();
      List<Item> targetList = null;

      for (var bucket : buckets) {
        if (bucket.stack.isSimilar(currentStack)) {
          targetList = bucket.items;
          break;
        }
      }

      if (targetList == null) {
        targetList = new ArrayList<>();
        buckets.add(new ItemStackBucket(currentStack, targetList));
      }

      targetList.add(currentItem);
    }

    var pilesToStackWithExisting = stackWithExisting ? new ArrayList<ItemPile>() : null;

    for (var bucket : buckets) {
      var firstItem = bucket.items.getFirst();
      var firstItemPile = getPile(firstItem);

      if (pilesToStackWithExisting != null)
        pilesToStackWithExisting.add(firstItemPile);

      for (var itemIndex = 1; itemIndex < bucket.items.size(); ++itemIndex) {
        var bucketItem = bucket.items.get(itemIndex);
        var bucketItemPile = getPile(bucketItem);

        firstItemPile.addTo(bucketItemPile.getAmountAndType().totalAmount());
        items.remove(bucketItem);
      }
    }

    if (pilesToStackWithExisting != null)
      tryAddNewPilesToExistingPiles(pilesToStackWithExisting, addedPile -> items.remove(addedPile.getItemEntity()));
  }

  private void tryAddNewPilesToExistingPiles(List<ItemPile> newPilesToAdd, Consumer<ItemPile> afterNewPileAddToExisting) {
    var blockRadius = config.rootSection.itemPiling.blockRadius;

    for (var newPile : newPilesToAdd) {
      var newEntity = newPile.getItemEntity();

      for (var existingItem : newEntity.getWorld().getNearbyEntitiesByType(Item.class, newEntity.getLocation(), blockRadius)) {
        var existingPile = getPile(existingItem);

        if (!existingPile.tryAddAndRemoveOther(newPile))
          continue;

        afterNewPileAddToExisting.accept(newPile);
        break;
      }
    }
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    for (var entity : event.getChunk().getEntities()) {
      if (entity instanceof Item item)
        getPile(item).updateItemName();
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPickupAttempt(PlayerAttemptPickupItemEvent event) {
    var itemPile = getPile(event.getItem());

    if (itemPile.getAdditionalAmount() <= 0)
      return;

    event.setCancelled(true);

    var playerInventory = event.getPlayer().getInventory();

    if (itemPile.reduceIntoInventoryAndGetIfAny(playerInventory, true))
      event.setFlyAtPlayer(true);
  }

  @EventHandler
  public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
    var entityId = event.getEntity().getEntityId();

    pileEntityMetadataByEntityId.remove(entityId);
    playerDroppedItemEntityIds.remove(entityId);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onEntityPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player)
      return;

    var itemPile = getPile(event.getItem());

    if (itemPile.getAdditionalAmount() <= 0)
      return;

    // There really is no reason for any entity to interact with a pile of items.
    // So for now, let's block it altogether, until an actual use-case arises.
    event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onInventoryPickup(InventoryPickupItemEvent event) {
    var itemPile = getPile(event.getItem());

    if (itemPile.getAdditionalAmount() <= 0)
      return;

    event.setCancelled(true);

    itemPile.reduceIntoInventoryAndGetIfAny(event.getInventory(), false);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onItemMerge(ItemMergeEvent event) {
    updateNameNextTickIfStillExists(event.getTarget());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onItemSpawn(ItemSpawnEvent event) {
    updateNameNextTickIfStillExists(event.getEntity());
  }

  private void updateNameNextTickIfStillExists(Item itemEntity) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (itemEntity.isDead() || !itemEntity.isValid())
        return;

      getPile(itemEntity).updateItemName();
    }, 1);
  }

  private void tryPileItems(Collection<Item> items) {
    var itemPiles = new ArrayList<ItemPile>(items.size());

    for (var item : items) {
      var itemEntityAge = item.getTicksLived();

      if (itemEntityAge < config.rootSection.itemPiling.minimumAgeTicks)
        continue;

      var entityId = item.getEntityId();

      if (playerDroppedItemEntityIds.contains(entityId)) {
        if (itemEntityAge < config.rootSection.itemPiling.minimumAgeTicksDroppedByPlayer)
          continue;

        // Once the minimum age elapsed, the item is no longer flagged as having been manually
        // dropped, as to allow successive pilings to happen without further unwanted delay.
        playerDroppedItemEntityIds.remove(entityId);
      }

      itemPiles.add(getPile(item));
    }

    var pileBucketByChunkId = new Long2ObjectOpenHashMap<List<ItemPile>>();

    for (var pile : itemPiles) {
      var itemEntity = pile.getItemEntity();
      var chunkId = CompactId.computeWorldlessBlockXYZChunkId((int) itemEntity.getX(), (int) itemEntity.getY(), (int) itemEntity.getZ());
      pileBucketByChunkId.computeIfAbsent(chunkId, _ -> new ArrayList<>()).add(pile);
    }

    var blockRadius = config.rootSection.itemPiling.blockRadius;
    var chunkRadius = (blockRadius + 15) / 16;

    var processedEntityIds = new IntOpenHashSet();

    for (var currentPile : itemPiles) {
      var currentEntity = currentPile.getItemEntity();

      if (!processedEntityIds.add(currentEntity.getEntityId()))
        continue;

      if (currentEntity.isDead() || !currentEntity.isValid())
        continue;

      var itemX = (int) currentEntity.getX();
      var itemY = (int) currentEntity.getY();
      var itemZ = (int) currentEntity.getZ();

      for (var deltaX = -chunkRadius; deltaX <= chunkRadius; ++deltaX) {
        for (var deltaY = -chunkRadius; deltaY <= chunkRadius; ++deltaY) {
          for (var deltaZ = -chunkRadius; deltaZ <= chunkRadius; ++deltaZ) {
            var chunkId = CompactId.computeWorldlessBlockXYZChunkId(itemX + deltaX, itemY + deltaY, itemZ + deltaZ);
            var neighboringPileBucket = pileBucketByChunkId.get(chunkId);

            if (neighboringPileBucket == null)
              continue;

            for (var otherPile : neighboringPileBucket) {
              if (otherPile == currentPile)
                continue;

              var otherEntity = otherPile.getItemEntity();
              var otherEntityId = otherEntity.getEntityId();

              if (processedEntityIds.contains(otherEntityId))
                continue;

              if (otherEntity.isDead() || !otherEntity.isValid())
                continue;

              if (!otherPile.isWithinDistance(currentPile, blockRadius))
                continue;

              if (currentPile.tryAddAndRemoveOther(otherPile))
                processedEntityIds.add(otherEntityId);
            }
          }
        }
      }
    }
  }
}
