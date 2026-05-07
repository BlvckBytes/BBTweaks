package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.color.PackedColor;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.shulker_accessor.ShulkerAccessorListener;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AutoPickupContainerListener implements Listener {

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
  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey containerMarkerKey;
  private final NamespacedKey placedItemKey;

  private final Map<UUID, PickupTickWindow> pickupTickWindowByPlayerId;

  public AutoPickupContainerListener(
    Plugin plugin,
    ShulkerAccessorListener shulkerAccessor,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.shulkerAccessor = shulkerAccessor;
    this.config = config;

    this.containerMarkerKey = new NamespacedKey(plugin, "auto-pickup-container");
    this.placedItemKey = new NamespacedKey(plugin, "auto-pickup-placed-item");

    this.pickupTickWindowByPlayerId = new HashMap<>();

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      pickupTickWindowByPlayerId.values().forEach(this::processPickupTickWindow);
      pickupTickWindowByPlayerId.clear();
    }, 0L, 0L);
  }

  private void processPickupTickWindow(PickupTickWindow window) {
    if (!window.player.isOnline())
      return;

    var session = new InventoryManipulationSession(window.player, (inventory, slot, item) -> {
      if (!doesContainMarker(item))
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

    session.onCompletion();
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
    item.setItemMeta(meta);

    return null;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPlace(BlockPlaceEvent event) {
    if (!(event.getBlockPlaced().getState() instanceof ShulkerBox shulkerBox))
      return;

    var placedItem = event.getItemInHand();

    if (!Tag.SHULKER_BOXES.isTagged(placedItem.getType()))
      return;

    if (!doesContainMarker(placedItem))
      return;

    shulkerBox.getPersistentDataContainer().set(placedItemKey, PersistentDataType.BYTE_ARRAY, placedItem.serializeAsBytes());
    shulkerBox.update(true, false);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockDropItem(BlockDropItemEvent event) {
    if (!(event.getBlockState() instanceof ShulkerBox shulkerBox))
      return;

    var placedItemBytes = shulkerBox.getPersistentDataContainer().get(placedItemKey, PersistentDataType.BYTE_ARRAY);

    if (placedItemBytes == null)
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

    var placedItem = ItemStack.deserializeBytes(placedItemBytes);

    var placedMeta = placedItem.getItemMeta();
    var droppedMeta = droppedStack.getItemMeta();

    if (placedMeta == null || droppedMeta == null)
      return;

    droppedMeta.displayName(placedMeta.displayName());

    droppedMeta.getPersistentDataContainer().set(containerMarkerKey, PersistentDataType.BOOLEAN, true);

    droppedStack.setItemMeta(droppedMeta);

    updateLoreIfMarked(droppedStack);

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
    if (!doesContainMarker(item))
      return false;

    var meta = item.getItemMeta();

    if (meta == null)
      return false;

    var dyeColor = dyeColorByShulkerMaterial.getOrDefault(item.getType(), DyeColor.WHITE);
    var bukkitColor = dyeColor.getColor();
    var hexColor = PackedColor.asNonAlphaHex(PackedColor.of(bukkitColor.getRed(), bukkitColor.getGreen(), bukkitColor.getBlue(), 255));

    var environment = new InterpretationEnvironment()
      .withVariable("shulker_color", hexColor);

    meta.lore(config.rootSection.autoPickupContainer.loreToSetOnUpdate.interpret(SlotType.ITEM_LORE, environment));

    item.setItemMeta(meta);
    return true;
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

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean doesContainMarker(ItemStack item) {
    var markerFlag = item.getPersistentDataContainer().get(containerMarkerKey, PersistentDataType.BOOLEAN);
    return markerFlag != null && markerFlag;
  }
}
