package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.color.PackedColor;
import io.papermc.paper.persistence.PersistentDataContainerView;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.shulker_accessor.ShulkerAccessorListener;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class AutoPickupContainerListener implements Listener, FilterPredicateAccessor {

  private record ShulkerCapture(Block block, ShulkerBox state, long time) {}

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
  private final ShulkerAccessorListener shulkerAccessor;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey containerMarkerKey;
  private final NamespacedKey filterPredicateKey;
  private final NamespacedKey filterPredicateLanguageKey;

  private final Map<UUID, PickupTickWindow> pickupTickWindowByPlayerId;
  private final List<ShulkerCapture> markedShulkerCaptures;
  private final Map<String, PredicateAndLanguage> filterPredicateAccessorCache;

  private long relativeTime;

  public AutoPickupContainerListener(
    Plugin plugin,
    ShulkerAccessorListener shulkerAccessor,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.shulkerAccessor = shulkerAccessor;
    this.predicateHelper = predicateHelper;
    this.config = config;

    this.containerMarkerKey = new NamespacedKey(plugin, "auto-pickup-container");
    this.filterPredicateKey = new NamespacedKey(plugin, "auto-pickup-container-predicate");
    this.filterPredicateLanguageKey = new NamespacedKey(plugin, "auto-pickup-container-predicate-language");

    this.pickupTickWindowByPlayerId = new HashMap<>();
    this.markedShulkerCaptures = new ArrayList<>();
    this.filterPredicateAccessorCache = new HashMap<>();

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      ++relativeTime;

      if (relativeTime % (20 * 60 * 5) == 0)
        filterPredicateAccessorCache.clear();

      pickupTickWindowByPlayerId.values().forEach(this::processPickupTickWindow);
      pickupTickWindowByPlayerId.clear();
    }, 0L, 0L);
  }

  @Override
  public @Nullable ItemPredicate accessFilterPredicate(Player player, PersistentDataContainer pdc) {
    var predicateAndLanguage = loadFilterFromPdc(pdc, filterPredicateAccessorCache, (predicate, language, exception) -> {
      config.rootSection.autoPickupContainer.filterErrorNotification.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("predicate", predicate)
          .withVariable("language", language)
          .withVariable("error", predicateHelper.createExceptionMessage(exception))
      );
    });

    if (predicateAndLanguage == null)
      return null;

    return predicateAndLanguage.predicate;
  }

  private void processPickupTickWindow(PickupTickWindow window) {
    if (!window.player.isOnline())
      return;

    var session = new InventoryManipulationSession(window.player, this, (inventory, slot, item) -> {
      if (!doesContainMarker(item.getPersistentDataContainer()))
        return false;

      // Disable auto-pickup for currently-viewed shulkers
      return !shulkerAccessor.doesAnyAccessorHolderMatch(holder -> holder.isShulkerItemContainedByInventoryAtSlot(inventory, slot));
    });

    for (var itemBucket : window.buckets) {
      var remainingAmountToReduce = session.tryAddItemToContainersAndGetAddedAmount(itemBucket.item, itemBucket.getTotalCount());

      for (var slotIndex = 0; slotIndex < ItemBucket.INVENTORY_SIZE; ++slotIndex) {
        if (remainingAmountToReduce <= 0)
          break;

        var pickedUpCount = itemBucket.getPickedUpCountForSlot(slotIndex);

        if (pickedUpCount <= 0)
          continue;

        var amountToReduce = Math.min(pickedUpCount, remainingAmountToReduce);

        session.reduceItemInPlayerInventoryBy(slotIndex, amountToReduce);

        remainingAmountToReduce -= amountToReduce;
      }
    }

    session.onCompletion((item, meta) -> updateLore(meta, item.getType()));
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

    updateLore(meta, item.getType());

    item.setItemMeta(meta);

    return null;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
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

    var capture = new ShulkerCapture(block, shulkerBox, relativeTime);

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

    if (updateLoreIfMarked(result))
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

      if (updateLoreIfMarked(itemAfter))
        playerInventory.setItemInMainHand(itemAfter);
    }, 1L);
  }

  private boolean updateLoreIfMarked(ItemStack item) {
    if (!doesContainMarker(item.getPersistentDataContainer()))
      return false;

    var meta = item.getItemMeta();

    if (meta == null)
      return false;

    updateLore(meta, item.getType());

    item.setItemMeta(meta);
    return true;
  }

  private void updateLore(ItemMeta shulkerMeta, Material shulkerType) {
    var dyeColor = dyeColorByShulkerMaterial.getOrDefault(shulkerType, DyeColor.WHITE);
    var bukkitColor = dyeColor.getColor();
    var hexColor = PackedColor.asNonAlphaHex(PackedColor.of(bukkitColor.getRed(), bukkitColor.getGreen(), bukkitColor.getBlue(), 255));

    var predicateString = shulkerMeta.getPersistentDataContainer().get(filterPredicateKey, PersistentDataType.STRING);

    var environment = new InterpretationEnvironment()
      .withVariable("shulker_color", hexColor)
      .withVariable("filter_predicate", predicateString);

    shulkerMeta.lore(config.rootSection.autoPickupContainer.loreToSetOnUpdate.interpret(SlotType.ITEM_LORE, environment));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    var pickedUpItem = event.getItem().getItemStack();

    if (Tag.SHULKER_BOXES.isTagged(pickedUpItem.getType()))
      return;

    pickupTickWindowByPlayerId
      .computeIfAbsent(player.getUniqueId(), k -> new PickupTickWindow(player))
      .accessOrCreateBucket(pickedUpItem)
      .analyzePickupSlots(pickedUpItem, player.getInventory());
  }

  @EventHandler
  public void onPredicateGet(PredicateGetEvent event) {
    tryAccessShulkerPdcAndAcknowledge(event, pdc -> {
      var predicate = loadFilterFromPdc(pdc, null, ((p, l, exception) -> event.setError(exception)));

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

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean doesContainMarker(PersistentDataContainerView pdcView) {
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
      var tokens = predicateHelper.parseTokens(predicateString);
      predicate = predicateHelper.parsePredicate(language, tokens);
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

    updateLore(droppedMeta, droppedStack.getType());

    droppedStack.setItemMeta(droppedMeta);
  }
}
