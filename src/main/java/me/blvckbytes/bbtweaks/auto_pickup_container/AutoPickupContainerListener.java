package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.color.PackedColor;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.persistence.PersistentDataContainerView;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.settings.AutoPickupContainerSettingsStore;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.inv_magnet.PreAttractItemEvent;
import me.blvckbytes.bbtweaks.shulker_accessor.PostShulkerAccessorWriteEvent;
import me.blvckbytes.bbtweaks.shulker_accessor.ShulkerAccessorListener;
import me.blvckbytes.bbtweaks.shulker_accessor.PreShulkerAccessorWriteEvent;
import me.blvckbytes.item_predicate_parser.event.*;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class AutoPickupContainerListener implements Listener, Tickable, FilterPredicateAccessor {

  private static final int FILTER_PREDICATE_CACHE_CLEAR_PERIOD_T = 20 * 60 * 5;
  private static final int USAGE_INFO_MAX_UPDATE_AGE_T = 10;

  private record ShulkerCapture(Block block, ShulkerBox state) {}

  private static final List<TransmuteRecipe> shulkerRecolorRecipes;
  private static final Map<Material, DyeColor> dyeColorByShulkerMaterial;

  static {
    shulkerRecolorRecipes = new ArrayList<>();

    var dyeMaterials = Tag.ITEMS_DYES.getValues();

    recipeLoop:
    for (var iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
      var recipe = iterator.next();

      if (!(recipe instanceof TransmuteRecipe transmuteRecipe))
        continue;

      if (!(Tag.SHULKER_BOXES.isTagged(recipe.getResult().getType())))
        continue;

      if (!(transmuteRecipe.getMaterial() instanceof RecipeChoice.ItemTypeChoice typeChoice))
        continue;

      //noinspection UnstableApiUsage
      for (var itemType : typeChoice.itemTypes()) {
        if (dyeMaterials.stream().noneMatch(it -> itemType.key().equals(it.key())))
          continue recipeLoop;
      }

      shulkerRecolorRecipes.add(transmuteRecipe);
    }

    dyeColorByShulkerMaterial = new HashMap<>();

    for (var shulkerMaterial : Tag.SHULKER_BOXES.getValues()) {
      var enumName = shulkerMaterial.name();
      var suffixIndex = enumName.indexOf("_SHULKER_BOX");

      if (suffixIndex <= 0)
        continue;

      var colorName = enumName.substring(0, suffixIndex);

      DyeColor dyeColor;

      try {
        dyeColor = DyeColor.valueOf(colorName);
      } catch (Throwable e) {
        throw new IllegalStateException("Could not resolve a dye-color named \"" + colorName + "\"");
      }

      dyeColorByShulkerMaterial.put(shulkerMaterial, dyeColor);
    }

    if (dyeColorByShulkerMaterial.isEmpty())
      throw new IllegalStateException("Could not locate any dye-colors based on shulker-materials");
  }

  private final Plugin plugin;
  private final AutoPickupContainerSettingsStore settingsStore;
  private final ShulkerAccessorListener shulkerAccessor;
  private final IPPIntegration ippIntegration;
  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey containerMarkerKey;
  private final NamespacedKey filterPredicateKey;
  private final NamespacedKey filterPredicateLanguageKey;

  private final List<ShulkerCapture> markedShulkerCaptures;
  private final Map<String, PredicateAndLanguage> filterPredicateAccessorCache;
  private final Map<UUID, UsageInfo> usageInfoByPlayerId;
  private final Map<UUID, SlotChanges> slotChangesByPlayerId;
  private final Map<UUID, AddToContainerSession> perTickAddSessionByPlayerId;

  private long relativeTime;

  public AutoPickupContainerListener(
    Plugin plugin,
    AutoPickupContainerSettingsStore settingsStore,
    ShulkerAccessorListener shulkerAccessor,
    IPPIntegration ippIntegration,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.settingsStore = settingsStore;
    this.shulkerAccessor = shulkerAccessor;
    this.ippIntegration = ippIntegration;
    this.config = config;

    this.containerMarkerKey = new NamespacedKey(plugin, "auto-pickup-container");
    this.filterPredicateKey = new NamespacedKey(plugin, "auto-pickup-container-predicate");
    this.filterPredicateLanguageKey = new NamespacedKey(plugin, "auto-pickup-container-predicate-language");

    this.markedShulkerCaptures = new ArrayList<>();
    this.filterPredicateAccessorCache = new HashMap<>();
    this.usageInfoByPlayerId = new HashMap<>();
    this.slotChangesByPlayerId = new HashMap<>();
    this.perTickAddSessionByPlayerId = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;

    if (relativeTime % FILTER_PREDICATE_CACHE_CLEAR_PERIOD_T == 0)
      filterPredicateAccessorCache.clear();

    for (var usageInfo : usageInfoByPlayerId.values()) {
      if (!usageInfo.possiblyChanged)
        continue;

      var updateAge = relativeTime - usageInfo.lastUpdateTime;

      if (updateAge < USAGE_INFO_MAX_UPDATE_AGE_T)
        continue;

      usageInfo.lastKnownCounts = makePickupSession(usageInfo.player).calculateMarkedUsageCounts();
      usageInfo.lastUpdateTime = relativeTime;
      usageInfo.possiblyChanged = false;
    }

    for (var slotChanges : slotChangesByPlayerId.values()) {
      var playerInventory = slotChanges.player.getInventory();

      slotChanges.forEachChangedSlotAndUnmark(relativeTime, slot -> {
        var item = playerInventory.getItem(slot);

        if (item == null || item.getType().isAir())
          return;

        if (!doesContainMarker(item.getPersistentDataContainer()))
          return;

        // Important! We must not interfere with the shulker-accessor, as to avoid it getting
        // out-of-sync, which is why its writeback-events handle updating separately.
        if (shulkerAccessor.doesAnyAccessorHolderMatch(it -> it.isShulkerItemContainedByInventoryAtSlot(playerInventory, slot)))
          return;

        var meta = item.getItemMeta();

        var shulkerInventory = LazyContainer.tryAccessInventory(item);
        var counts = shulkerInventory == null ? MaterialCounts.EMPTY : MaterialCounts.fromInventory(shulkerInventory);

        updateLore(meta, item.getType(), counts);

        item.setItemMeta(meta);

        updateData(item);
      });
    }
  }

  @Override
  public @Nullable ItemPredicate accessFilterPredicate(Player player, PersistentDataContainer pdc) {
    var predicateAndLanguage = loadFilterFromPdc(pdc, filterPredicateAccessorCache, (predicate, language, exception) -> {
      config.rootSection.autoPickupContainer.filterErrorNotification.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("predicate", predicate)
          .withVariable("language", language)
          .withVariable("error", ippIntegration.predicateHelper.createExceptionMessage(exception))
      );
    });

    if (predicateAndLanguage == null)
      return null;

    return predicateAndLanguage.predicate;
  }

  public UsageCounts getLastKnownUsageCounts(Player player) {
    var usageInfo = usageInfoByPlayerId.get(player.getUniqueId());

    if (usageInfo == null)
      return UsageCounts.EMPTY;

    return usageInfo.lastKnownCounts;
  }

  public @Nullable MarkerModifyError modifyItemToBecomeAutoPickupContainer(ItemStack item) {
    if (!Tag.SHULKER_BOXES.isTagged(item.getType()))
      return MarkerModifyError.WRONG_ITEM_TYPE;

    var meta = Objects.requireNonNull(item.getItemMeta());
    var pdc = meta.getPersistentDataContainer();

    var existingValue = pdc.get(containerMarkerKey, PersistentDataType.BOOLEAN);

    if (existingValue != null && existingValue)
      return MarkerModifyError.ALREADY_MARKED;

    pdc.set(containerMarkerKey, PersistentDataType.BOOLEAN, true);

    var inventory = LazyContainer.tryAccessInventory(item);
    var counts = inventory == null ? MaterialCounts.EMPTY : MaterialCounts.fromInventory(inventory);

    updateLore(meta, item.getType(), counts);

    item.setItemMeta(meta);

    updateData(item);

    return null;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();

    usageInfoByPlayerId.remove(playerId);
    slotChangesByPlayerId.remove(playerId);
    perTickAddSessionByPlayerId.remove(playerId);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    var usageCounts = makePickupSession(player).calculateMarkedUsageCounts();

    usageInfoByPlayerId.put(player.getUniqueId(), new UsageInfo(player, usageCounts, relativeTime));
  }

  @EventHandler
  public void onSlotChanged(PlayerInventorySlotChangeEvent event) {
    var player = event.getPlayer();

    var newItem = event.getNewItemStack();
    var oldItem = event.getOldItemStack();

    var doesNewContainMarker = doesContainMarker(newItem.getPersistentDataContainer());

    if (doesContainMarker(oldItem.getPersistentDataContainer()) || doesNewContainMarker) {
      // For some odd reason, there are many calls with old being AIR, even when I just open containers
      // and do not change any of the actual slots in my inventory. Avoid updating in those cases.
      if (doesNewContainMarker && newItem.getType() == oldItem.getType())
        markSlotRequiringUpdate(player, event.getSlot());

      markUsageInfoAsPossiblyChanged(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onItemSpawn(ItemSpawnEvent event) {
    var item = event.getEntity();
    var itemStack = item.getItemStack();

    if (!Tag.SHULKER_BOXES.isTagged(itemStack.getType()))
      return;

    var location = event.getLocation();

    for (var captureIndex = 0; captureIndex < markedShulkerCaptures.size(); ++captureIndex) {
      var shulkerCapture = markedShulkerCaptures.get(captureIndex);
      var block = shulkerCapture.block;

      if (block.getWorld() != location.getWorld())
        continue;

      if (block.getX() != location.getBlockX() || block.getY() != location.getBlockY() || block.getZ() != location.getBlockZ())
        continue;

      markedShulkerCaptures.remove(captureIndex);

      markAndCopyFilterAndUpdateLore(shulkerCapture.state.getPersistentDataContainer(), itemStack);
      item.setItemStack(itemStack);

      return;
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPistonExtend(BlockPistonExtendEvent event) {
    for (var movedBlock : event.getBlocks())
      captureShulkerIfApplicable(movedBlock);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBlockExplode(EntityExplodeEvent event) {
    for (var explodedBlock : event.blockList())
      captureShulkerIfApplicable(explodedBlock);
  }

  private void captureShulkerIfApplicable(Block block) {
    if (!Tag.SHULKER_BOXES.isTagged(block.getType()))
      return;

    if (!(block.getState() instanceof ShulkerBox shulkerBox))
      return;

    if (!doesContainMarker(shulkerBox.getPersistentDataContainer()))
      return;

    var capture = new ShulkerCapture(block, shulkerBox);

    markedShulkerCaptures.add(capture);

    Bukkit.getScheduler().runTaskLater(plugin, () -> markedShulkerCaptures.removeIf(it -> it == capture), 1);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPlace(BlockPlaceEvent event) {
    if (!(event.getBlockPlaced().getState() instanceof ShulkerBox shulkerBox))
      return;

    var placedItem = event.getItemInHand();

    if (!Tag.SHULKER_BOXES.isTagged(placedItem.getType()))
      return;

    if (!doesContainMarker(placedItem.getPersistentDataContainer()))
      return;

    var blockPdc = shulkerBox.getPersistentDataContainer();

    blockPdc.set(containerMarkerKey, PersistentDataType.BOOLEAN, true);

    var itemPdc = placedItem.getPersistentDataContainer();

    copyOverFilter(itemPdc, blockPdc);

    shulkerBox.update(true, false);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockDropItem(BlockDropItemEvent event) {
    if (!(event.getBlockState() instanceof ShulkerBox shulkerBox))
      return;

    var blockPdc = shulkerBox.getPersistentDataContainer();

    if (!doesContainMarker(blockPdc))
      return;

    Item droppedItem = null;
    ItemStack droppedStack = null;

    for (var item : event.getItems()) {
      droppedStack = item.getItemStack();

      if (!Tag.SHULKER_BOXES.isTagged(droppedStack.getType()))
        continue;

      if (droppedItem != null)
        return;

      droppedItem = item;
    }

    if (droppedItem == null)
      return;

    markAndCopyFilterAndUpdateLore(blockPdc, droppedStack);

    droppedItem.setItemStack(droppedStack);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPrepareCraft(PrepareItemCraftEvent event) {
    var recipe = event.getRecipe();

    if (recipe != null)
      handleCraftEvent(recipe, event.getInventory());
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onCraft(CraftItemEvent event) {
    handleCraftEvent(event.getRecipe(), event.getInventory());
  }

  private void handleCraftEvent(Recipe recipe, CraftingInventory inventory) {
    if (!(recipe instanceof TransmuteRecipe transmuteRecipe))
      return;

    if (shulkerRecolorRecipes.stream().noneMatch(it -> it.getKey().equals(transmuteRecipe.getKey())))
      return;

    var result = inventory.getResult();

    if (result == null)
      return;

    if (updateLoreAfterColorChangeIfMarked(result))
      inventory.setResult(result);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var block = event.getClickedBlock();

    if (block == null || block.getType() != Material.WATER_CAULDRON)
      return;

    var playerInventory = event.getPlayer().getInventory();
    var itemBefore = playerInventory.getItemInMainHand();

    if (!Tag.SHULKER_BOXES.isTagged(itemBefore.getType()))
      return;

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var itemAfter = playerInventory.getItemInMainHand();

      if (!Tag.SHULKER_BOXES.isTagged(itemAfter.getType()))
        return;

      if (itemBefore.getType() == itemAfter.getType())
        return;

      if (updateLoreAfterColorChangeIfMarked(itemAfter))
        playerInventory.setItemInMainHand(itemAfter);
    }, 1L);
  }

  private boolean updateLoreAfterColorChangeIfMarked(ItemStack item) {
    if (!doesContainMarker(item.getPersistentDataContainer()))
      return false;

    var meta = item.getItemMeta();

    if (meta == null)
      return false;

    var inventory = LazyContainer.tryAccessInventory(item);
    var counts = inventory == null ? MaterialCounts.EMPTY : MaterialCounts.fromInventory(inventory);

    updateLore(meta, item.getType(), counts);

    item.setItemMeta(meta);

    updateData(item);

    return true;
  }

  @SuppressWarnings("UnstableApiUsage")
  private void updateData(ItemStack shulkerItem) {
    var builder = TooltipDisplay.tooltipDisplay();

    var current = shulkerItem.getData(DataComponentTypes.TOOLTIP_DISPLAY);

    if (current != null) {
      builder.hiddenComponents(current.hiddenComponents());
      builder.hideTooltip(current.hideTooltip());
    }

    builder.addHiddenComponents(DataComponentTypes.CONTAINER);

    shulkerItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, builder.build());
  }

  private void updateLore(ItemMeta shulkerMeta, Material shulkerType, MaterialCounts counts) {
    var dyeColor = dyeColorByShulkerMaterial.getOrDefault(shulkerType, DyeColor.WHITE);
    var bukkitColor = dyeColor.getColor();
    var hexColor = PackedColor.asNonAlphaHex(PackedColor.of(bukkitColor.getRed(), bukkitColor.getGreen(), bukkitColor.getBlue(), 255));

    var predicateString = shulkerMeta.getPersistentDataContainer().get(filterPredicateKey, PersistentDataType.STRING);

    var environment = new InterpretationEnvironment()
      .withVariable("shulker_color", hexColor)
      .withVariable("filter_predicate", predicateString)
      .withVariable("item_counts", counts.asSortedTranslatedCountList(ippIntegration));

    shulkerMeta.lore(config.rootSection.autoPickupContainer.loreToSetOnUpdate.interpret(SlotType.ITEM_LORE, environment));
  }

  // Important! Checking shulker-contents is rather expensive, so only execute
  // this "dry-run" if every other handler was unsuccessful in finding space.
  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPreAttractItem(PreAttractItemEvent event) {
    if (event.canHoldSome())
      return;

    var settings = settingsStore.accessSettings(event.getPlayer());

    if (!settings.enabled)
      return;

    var attractedItem = event.getAttractedItem();

    if (settings.didFailAttemptRecently(attractedItem, relativeTime))
      return;

    // This does not necessarily run in one burst with the pickups, but since we're only executing
    // a dry-run, it's fine to also allow reuse here, as no harm's done and the chance of somebody
    // sliding in an item in the very same tick that the pickup occurs is so slim that I'm rather
    // taking the benefit of the optimization, which is making a large difference.
    var session = makePickupSession(event.getPlayer(), true);

    if (session.tryAddItemToContainersAndGetAddedAmount(attractedItem, AddFlag.DRY_RUN) <= 0) {
      settings.submitFailedAttempt(attractedItem, relativeTime);
      return;
    }

    event.markCanHoldSome();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPickupAttempt(PlayerAttemptPickupItemEvent event) {
    var player = event.getPlayer();
    var settings = settingsStore.accessSettings(event.getPlayer());

    if (!settings.enabled)
      return;

    var itemEntity = event.getItem();
    var pickedUpStack = itemEntity.getItemStack();

    if (settings.didFailAttemptRecently(pickedUpStack, relativeTime))
      return;

    var session = makePickupSession(player, true);

    var availableAmount = pickedUpStack.getAmount();
    var addedAmount = session.tryAddItemToContainersAndGetAddedAmount(pickedUpStack);

    session.onCompletion();

    // Had no space to add any amount of the item to any of the containers carried by the player.
    if (addedAmount == 0) {
      settings.submitFailedAttempt(pickedUpStack, relativeTime);
      return;
    }

    // Cancelling prevents the default pickup-behavior from being executed, but sets the fly-at-player
    // flag to false, so we need to explicitly set it again; then, the pickup-animation will be played.
    event.setCancelled(true);
    event.setFlyAtPlayer(true);

    // We cannot immediately remove the entity, seeing how the pickup-animation packet is sent after this
    // event completes; if we remove the entity ahead of time, the client disposes of it before playing
    // said animation; therefore, let's just make it non-pickup-able in the meantime and remove it next tick.
    itemEntity.setPickupDelay(1024);
    Bukkit.getScheduler().runTaskLater(plugin, itemEntity::remove, 1L);

    if (addedAmount >= availableAmount)
      return;

    var remainderStack = new ItemStack(pickedUpStack);
    remainderStack.setAmount(availableAmount - addedAmount);

    var remainderItem = itemEntity.getWorld().spawn(itemEntity.getLocation(), Item.class);
    remainderItem.setItemStack(remainderStack);

    // Since we've only spliced-off a part of the stack, which itself was pickupable, the remainder
    // may also mimic that; also, as to not make it bounce around, let's clear its initial velocity.
    remainderItem.setPickupDelay(0);
    remainderItem.setVelocity(new Vector(0, 0, 0));
  }

  @EventHandler
  public void onPreShulkerAccessorWrite(PreShulkerAccessorWriteEvent event) {
    if (doesContainMarker(event.meta.getPersistentDataContainer()))
      updateLore(event.meta, event.type, MaterialCounts.fromInventory(event.inventory));
  }

  @EventHandler
  public void onPostShulkerAccessorWrite(PostShulkerAccessorWriteEvent event) {
    if (doesContainMarker(event.item.getPersistentDataContainer()))
      updateData(event.item);
  }

  @EventHandler
  public void onPredicateGet(PredicateGetEvent event) {
    tryAccessShulkerPdcAndAcknowledge(event, pdc -> {
      var predicate = loadFilterFromPdc(pdc, null, ((_, _, exception) -> event.setError(exception)));

      if (predicate != null)
        event.setResult(predicate);
    }, false);
  }

  @EventHandler
  public void onPredicateSet(PredicateSetEvent event) {
    tryAccessShulkerPdcAndAcknowledge(event, pdc -> writeFilterToPdc(pdc, event.getValue()), true);
  }

  @EventHandler
  public void onPredicateRemove(PredicateRemoveEvent event) {
    tryAccessShulkerPdcAndAcknowledge(event, pdc -> {
      var removedPredicate = loadFilterFromPdc(pdc, null, null);

      if (removedPredicate == null)
        return;

      removeFilterFromPdc(pdc);

      event.setRemovedPredicate(removedPredicate);
    }, true);
  }

  @EventHandler
  public void onPredicateHandGet(PredicateHandGetEvent event) {
    checkStackIsMarkedAndAcknowledge(event, targetItem -> {
      var pdc = targetItem.getPersistentDataContainer();
      var predicate = loadFilterFromPdc(pdc, null, ((_, _, exception) -> event.setError(exception)));

      if (predicate != null)
        event.setResult(predicate);
    });
  }

  @EventHandler
  public void onPredicateHandSet(PredicateHandSetEvent event) {
    checkStackIsMarkedAndAcknowledge(event, targetItem -> {
      event.setEncounteredStacks(1);

      tryUpdateFilterOnItem(targetItem, event.getValue());

      if (!event.isAll())
        return;

      forEachHandAllCandidate(event.getPlayer(), event.getHeldSlot(), item -> {
        if (tryUpdateFilterOnItem(item, event.getValue()))
          event.setEncounteredStacks(event.getEncounteredStacks() + 1);
      });
    });
  }

  @EventHandler
  public void onPredicateHandRemove(PredicateHandRemoveEvent event) {
    checkStackIsMarkedAndAcknowledge(event, targetItem -> {
      event.setEncounteredStacks(1);

      var removedPredicates = new ArrayList<PredicateAndLanguage>();
      event.setRemovedPredicates(removedPredicates);

      var removedTargetPredicate = loadFilterFromPdc(targetItem.getPersistentDataContainer(), null, null);

      if (removedTargetPredicate != null) {
        removedPredicates.add(removedTargetPredicate);
        tryUpdateFilterOnItem(targetItem, null);
      }

      if (!event.isAll())
        return;

      forEachHandAllCandidate(event.getPlayer(), event.getHeldSlot(), item -> {
        var removedPredicate = loadFilterFromPdc(item.getPersistentDataContainer(), null, null);

        if (tryUpdateFilterOnItem(item, null)) {
          if (removedPredicate != null)
            removedPredicates.add(removedPredicate);

          event.setEncounteredStacks(event.getEncounteredStacks() + 1);
        }
      });
    });
  }

  private void checkStackIsMarkedAndAcknowledge(PredicateHandEvent event, Consumer<ItemStack> handler) {
    var playerInventory = event.getPlayer().getInventory();
    var targetItem = playerInventory.getItem(event.getHeldSlot());

    if (targetItem == null)
      return;

    var targetPdc = targetItem.getPersistentDataContainer();

    if (!doesContainMarker(targetPdc))
      return;

    event.acknowledge();

    handler.accept(targetItem);
  }

  private boolean tryUpdateFilterOnItem(ItemStack item, @Nullable PredicateAndLanguage predicateAndLanguage) {
    if (!doesContainMarker(item.getPersistentDataContainer()))
      return false;

    var meta = item.getItemMeta();
    var pdc = meta.getPersistentDataContainer();

    if (predicateAndLanguage != null)
      writeFilterToPdc(pdc, predicateAndLanguage);
    else
      removeFilterFromPdc(pdc);

    var inventory = LazyContainer.tryAccessInventory(item);
    var counts = inventory == null ? MaterialCounts.EMPTY : MaterialCounts.fromInventory(inventory);

    updateLore(meta, item.getType(), counts);

    item.setItemMeta(meta);

    updateData(item);

    return true;
  }

  private void forEachHandAllCandidate(Player player, int heldSlot, Consumer<ItemStack> handler) {
    var storageContents = player.getInventory().getStorageContents();

    for (var slotIndex = 0; slotIndex < storageContents.length; ++slotIndex) {
      if (slotIndex == heldSlot)
        continue;

      var item = storageContents[slotIndex];

      if (item == null || item.getType().isAir())
        continue;

      handler.accept(item);
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean doesContainMarker(PersistentDataContainerView pdcView) {
    var markerFlag = pdcView.get(containerMarkerKey, PersistentDataType.BOOLEAN);
    return markerFlag != null && markerFlag;
  }

  private void tryAccessShulkerPdcAndAcknowledge(PredicateEvent event, Consumer<PersistentDataContainer> handler, boolean needsUpdate) {
    if (!(event.getBlock().getState() instanceof ShulkerBox shulkerBox))
      return;

    var pdc = shulkerBox.getPersistentDataContainer();

    if (!doesContainMarker(shulkerBox.getPersistentDataContainer()))
      return;

    event.acknowledge();
    handler.accept(pdc);

    if (needsUpdate)
      shulkerBox.update(true, false);
  }

  private void writeFilterToPdc(PersistentDataContainer pdc, PredicateAndLanguage predicateAndLanguage) {
    pdc.set(filterPredicateKey, PersistentDataType.STRING, predicateAndLanguage.getTokenPredicateString());
    pdc.set(filterPredicateLanguageKey, PersistentDataType.STRING, predicateAndLanguage.language.name());
  }

  private void removeFilterFromPdc(PersistentDataContainer pdc) {
    if (pdc.has(filterPredicateKey))
      pdc.remove(filterPredicateKey);

    if (pdc.has(filterPredicateLanguageKey))
      pdc.remove(filterPredicateLanguageKey);
  }

  private @Nullable PredicateAndLanguage loadFilterFromPdc(
    PersistentDataContainerView pdc,
    @Nullable Map<String, PredicateAndLanguage> cache,
    @Nullable PredicateErrorHandler errorHandler
  ) {
    var predicateString = pdc.get(filterPredicateKey, PersistentDataType.STRING);
    var languageString = pdc.get(filterPredicateLanguageKey, PersistentDataType.STRING);

    if (predicateString == null || predicateString.isBlank() || languageString == null || languageString.isBlank())
      return null;

    if (cache == null)
      return tryParsePredicateAndLanguage(predicateString, languageString, errorHandler);

    var identifier = languageString + ";" + predicateString;

    // Also cache null-values, as to not try to parse malformed predicates over and over again
    if (cache.containsKey(identifier))
      return cache.get(identifier);

    var result = tryParsePredicateAndLanguage(predicateString, languageString, errorHandler);

    cache.put(identifier, result);

    return result;
  }

  private @Nullable PredicateAndLanguage tryParsePredicateAndLanguage(
    String predicateString,
    String languageString,
    @Nullable PredicateErrorHandler errorHandler
  ) {
    TranslationLanguage language;

    try {
      language = TranslationLanguage.valueOf(languageString);
    } catch (Throwable e) {
      return null;
    }

    ItemPredicate predicate;

    try {
      var tokens = ippIntegration.predicateHelper.parseTokens(predicateString);
      predicate = ippIntegration.predicateHelper.parsePredicate(language, tokens);
    } catch (ItemPredicateParseException exception) {
      if (errorHandler != null)
        errorHandler.handle(predicateString, languageString, exception);

      return null;
    }

    if (predicate == null)
      return null;

    return new PredicateAndLanguage(predicate, language);
  }

  private void copyOverFilter(PersistentDataContainerView sourcePdc, PersistentDataContainer destinationPdc) {
    copyOverStringKey(sourcePdc, destinationPdc, filterPredicateKey);
    copyOverStringKey(sourcePdc, destinationPdc, filterPredicateLanguageKey);
  }

  private void copyOverStringKey(PersistentDataContainerView sourcePdc, PersistentDataContainer destinationPdc, NamespacedKey key) {
    if (!sourcePdc.has(key))
      return;

    var value = sourcePdc.get(key, PersistentDataType.STRING);

    if (value == null)
      return;

    destinationPdc.set(key, PersistentDataType.STRING, value);
  }

  private void markAndCopyFilterAndUpdateLore(PersistentDataContainer blockPdc, ItemStack droppedStack) {
    var droppedMeta = droppedStack.getItemMeta();

    var itemPdc = droppedMeta.getPersistentDataContainer();

    itemPdc.set(containerMarkerKey, PersistentDataType.BOOLEAN, true);

    copyOverFilter(blockPdc, itemPdc);

    var inventory = LazyContainer.tryAccessInventory(droppedStack);
    var counts = inventory == null ? MaterialCounts.EMPTY : MaterialCounts.fromInventory(inventory);

    updateLore(droppedMeta, droppedStack.getType(), counts);

    droppedStack.setItemMeta(droppedMeta);

    updateData(droppedStack);
  }

  public AddToContainerSession makePickupSession(Player player) {
    return makePickupSession(player, false);
  }

  private AddToContainerSession makePickupSession(Player player, boolean allowReuse) {
    if (allowReuse) {
      var existingSession = perTickAddSessionByPlayerId.get(player.getUniqueId());

      // This makes one crucial assumption - namely that all pickup-related events are fired in a burst within
      // the same tick; as long as that holds true, and nobody else will modify the shulkers during these events,
      // we can cache meta and state, as to avoid creating separate snapshots for each event individually. As for
      // our server, that should, all things considered, be a valid (albeit application-specific) optimization.
      if (existingSession != null && existingSession.createdAt == relativeTime)
        return existingSession;
    }

    var newSession = new AddToContainerSession(player, this, relativeTime, (inventory, slot, item) -> {
      var disableReasons = EnumSet.noneOf(DisableReason.class);

      if (!doesContainMarker(item.getPersistentDataContainer()))
        disableReasons.add(DisableReason.NOT_MARKED);

      // Disable auto-pickup for currently-viewed shulkers
      if (shulkerAccessor.doesAnyAccessorHolderMatch(holder -> holder.isShulkerItemContainedByInventoryAtSlot(inventory, slot)))
        disableReasons.add(DisableReason.CURRENTLY_VIEWED);

      return disableReasons;
    });

    if (allowReuse)
      perTickAddSessionByPlayerId.put(player.getUniqueId(), newSession);

    return newSession;
  }

  private void markSlotRequiringUpdate(Player player, int slot) {
    slotChangesByPlayerId
      .computeIfAbsent(player.getUniqueId(), _ -> new SlotChanges(player))
      .markRequiringUpdate(slot);
  }

  private void markUsageInfoAsPossiblyChanged(Player player) {
    var usageInfo = usageInfoByPlayerId.get(player.getUniqueId());

    if (usageInfo != null)
      usageInfo.possiblyChanged = true;
  }
}
