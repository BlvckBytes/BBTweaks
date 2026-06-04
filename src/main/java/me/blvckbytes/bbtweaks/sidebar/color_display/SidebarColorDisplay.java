package me.blvckbytes.bbtweaks.sidebar.color_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SidebarColorDisplay extends Display<ColorDisplayData> {

  // TODO: Allow to select formats like bold, italic, etc. and display them in the statistic-lore also

  public final boolean isFloodgate;

  public boolean selectingLabelColor = true;

  private final Int2ObjectMap<NamedColor> colorBySlotIndex;

  public SidebarColorDisplay(
    Player player,
    ColorDisplayData displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    this.colorBySlotIndex = new Int2ObjectOpenHashMap<>();

    show();
  }

  @Override
  protected void renderItems() {
    inventory.clear();

    var environment = makeEnvironment();

    config.rootSection.sidebar.colorDisplay.items.filler.renderInto(inventory, environment);
    config.rootSection.sidebar.colorDisplay.items.backButton.renderInto(inventory, environment);

    if (displayData.statistic() != null) {
      config.rootSection.sidebar.colorDisplay.items.labelColorMode.renderInto(inventory, environment);
      config.rootSection.sidebar.colorDisplay.items.valueColorMode.renderInto(inventory, environment);
    }

    var colors = config.rootSection.sidebar._colors;
    var nextColorIndex = 0;

    for (var index = 0; index < inventory.getSize(); ++index) {
      var currentItem = inventory.getItem(index);

      if (currentItem != null && !currentItem.getType().isAir())
        continue;

      if (nextColorIndex == colors.size())
        break;

      var color = colors.get(nextColorIndex++);

      colorBySlotIndex.put(index, color);

      NamedColor selectedColor = null;

      if (displayData.statistic() != null) {
        var colorMap = selectingLabelColor ? displayData.preferences().labelColorByStatistic : displayData.preferences().valueColorByStatistic;
        selectedColor = colorMap.get(displayData.statistic()._sidebarStatistic);
      }

      environment
        .withVariable("color", color.hexColor())
        .withVariable("selected", color == selectedColor)
        .withVariable("name", color.name())
        .withVariable("display_name", color.displayName())
        .withVariable("icon_type", color.iconType());

      inventory.setItem(index, config.rootSection.sidebar.colorDisplay.items.colorIcon.build(environment));
    }
  }

  public @Nullable NamedColor getColorBySlotIndex(int slotIndex) {
    return colorBySlotIndex.get(slotIndex);
  }

  public void onColorSelection(NamedColor color) {
    if (displayData.statistic() == null) {
      for (var valueColorEntry : displayData.preferences().valueColorByStatistic.entrySet())
        valueColorEntry.setValue(color);

      return;
    }

    var colorMap = selectingLabelColor ? displayData.preferences().labelColorByStatistic : displayData.preferences().valueColorByStatistic;

    colorMap.put(displayData.statistic()._sidebarStatistic, color);

    renderItems();
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.sidebar.colorDisplay.createInventory(makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("is_label_color", selectingLabelColor)
      .withVariable("statistic", displayData.statistic() == null ? null : displayData.statistic().iconData.name.markupNode);
  }
}
