package me.blvckbytes.bbtweaks.item_piling.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.item_piling.ItemPilingEntityNamePatcher;
import me.blvckbytes.bbtweaks.item_piling.preferences.ItemPilingPreferences;
import me.blvckbytes.bbtweaks.item_piling.preferences.PreferenceFlag;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ItemPilingDisplayHandler extends DisplayHandler<ItemPilingDisplay, ItemPilingPreferences> {

  private static final double ITEMS_REFRESH_RADIUS = 50;

  private final ItemPilingEntityNamePatcher entityNamePatcher;
  private final FloodgateIntegration floodgateIntegration;

  public ItemPilingDisplayHandler(
    ItemPilingEntityNamePatcher entityNamePatcher,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin, ItemPilingDisplay.class);

    this.entityNamePatcher = entityNamePatcher;
    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected ItemPilingDisplay instantiateDisplay(Player player, ItemPilingPreferences displayData) {
    return new ItemPilingDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, ItemPilingDisplay display, ClickType clickType, int slot) {
    if (clickType != ClickType.LEFT)
      return;

    var flag = getPreferenceFlagFromSlot(slot);

    if (flag == null)
      return;

    display.displayData.toggleFlag(flag);
    display.updateItems();

    refreshNearbyItems(player);
  }

  private void refreshNearbyItems(Player player) {
    for (var entity : player.getWorld().getNearbyEntitiesByType(Item.class, player.getLocation(), ITEMS_REFRESH_RADIUS))
      entityNamePatcher.possiblyUpdateEntityIfHasMetadata(entity.getEntityId(), player);
  }

  private @Nullable PreferenceFlag getPreferenceFlagFromSlot(int slot) {
    if (config.rootSection.itemPiling.display.items.showItemCountForUnitStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.SHOW_ITEM_COUNT_FOR_UNIT_STACKS;

    if (config.rootSection.itemPiling.display.items.showItemCountForVanillaStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.SHOW_ITEM_COUNT_FOR_VANILLA_STACKS;

    if (config.rootSection.itemPiling.display.items.showItemCountForPiledStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.SHOW_ITEM_COUNT_FOR_PILED_STACKS;

    if (config.rootSection.itemPiling.display.items.showItemMaterialForUnitStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.SHOW_ITEM_MATERIAL_FOR_UNIT_STACKS;

    if (config.rootSection.itemPiling.display.items.showItemMaterialForVanillaStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.SHOW_ITEM_MATERIAL_FOR_VANILLA_STACKS;

    if (config.rootSection.itemPiling.display.items.showItemMaterialForPiledStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.SHOW_ITEM_MATERIAL_FOR_PILED_STACKS;

    if (config.rootSection.itemPiling.display.items.formatItemCountToStacks.getDisplaySlots().contains(slot))
      return PreferenceFlag.FORMAT_ITEM_COUNT_TO_STACKS;

    if (config.rootSection.itemPiling.display.items.immediatelyStackBlockBreakItems.getDisplaySlots().contains(slot))
      return PreferenceFlag.IMMEDIATELY_STACK_BLOCK_BREAK_ITEMS;

    return null;
  }
}
