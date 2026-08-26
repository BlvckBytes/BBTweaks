package me.blvckbytes.bbtweaks.item_piling.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.item_piling.preferences.ItemPilingPreferences;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ItemPilingDisplay extends Display<ItemPilingPreferences> {

  public ItemPilingDisplay(
    Player player,
    ItemPilingPreferences displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.itemPiling.display.items.itemCountIcon.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.itemMaterialIcon.renderInto(itemConsumer, environment);

    config.rootSection.itemPiling.display.items.unitStackIcon.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.vanillaStackIcon.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.piledStackIcon.renderInto(itemConsumer, environment);

    config.rootSection.itemPiling.display.items.showItemCountForUnitStacks.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.showItemCountForVanillaStacks.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.showItemCountForPiledStacks.renderInto(itemConsumer, environment);

    config.rootSection.itemPiling.display.items.showItemMaterialForUnitStacks.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.showItemMaterialForVanillaStacks.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.showItemMaterialForPiledStacks.renderInto(itemConsumer, environment);

    config.rootSection.itemPiling.display.items.formatItemCountToStacks.renderInto(itemConsumer, environment);
    config.rootSection.itemPiling.display.items.immediatelyStackBlockBreakItems.renderInto(itemConsumer, environment);
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.itemPiling.display.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.itemPiling.display, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return displayData.makeEnvironment()
      .withVariable("is_floodgate", isFloodgate);
  }
}
