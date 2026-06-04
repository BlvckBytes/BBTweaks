package me.blvckbytes.bbtweaks.sidebar.color_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;
import me.blvckbytes.bbtweaks.sidebar.preferences.ColorAndFormats;
import me.blvckbytes.bbtweaks.sidebar.preferences.Format;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SidebarColorDisplay extends Display<ColorDisplayData> {

  public final boolean isFloodgate;

  private boolean selectingLabelColor;

  private final Int2ObjectMap<NamedColor> colorBySlotIndex;

  private @Nullable NamedColor commonColor;
  private final boolean[] commonFormatStates;

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

    this.commonFormatStates = new boolean[Format.ALL_VALUES.size()];

    setSelectingLabelColor(true);

    show();
  }

  public void setSelectingLabelColor(boolean value) {
    this.selectingLabelColor = value;

    if (displayData.statistic() != null)
      return;

    NamedColor commonColor = null;
    var commonColorMismatched = false;

    Boolean[] commonFormatStates = new Boolean[Format.ALL_VALUES.size()];

    for (var styleEntry : getSelectedStyleMap().entrySet()) {
      if (styleEntry.getKey().isSpacer)
        continue;

      var style = styleEntry.getValue();

      if (commonColor == null)
        commonColor = style.color;
      else if (style.color != commonColor)
        commonColorMismatched = true;

      for (var format : Format.ALL_VALUES) {
        var commonValue = commonFormatStates[format.ordinal()];
        var formatValue = style.formats.contains(format);

        if (commonValue == null)
          commonFormatStates[format.ordinal()] = formatValue;
        else if (formatValue != commonValue)
          commonFormatStates[format.ordinal()] = false;
      }
    }

    this.commonColor = commonColorMismatched ? null : commonColor;

    for (var format : Format.ALL_VALUES) {
      var commonValue = commonFormatStates[format.ordinal()];
      this.commonFormatStates[format.ordinal()] = commonValue != null && commonValue;
    }
  }

  @Override
  protected void renderItems() {
    inventory.clear();

    var environment = makeEnvironment();

    config.rootSection.sidebar.colorDisplay.items.filler.renderInto(inventory, environment);
    config.rootSection.sidebar.colorDisplay.items.backButton.renderInto(inventory, environment);

    config.rootSection.sidebar.colorDisplay.items.labelColorMode.renderInto(inventory, environment);
    config.rootSection.sidebar.colorDisplay.items.valueColorMode.renderInto(inventory, environment);

    config.rootSection.sidebar.colorDisplay.items.toggleBold.renderInto(inventory, environment);
    config.rootSection.sidebar.colorDisplay.items.toggleUnderlined.renderInto(inventory, environment);
    config.rootSection.sidebar.colorDisplay.items.toggleItalic.renderInto(inventory, environment);

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

      var activeColor = commonColor;

      var statisticStyle = accessSelectedStatisticStyle();

      if (statisticStyle != null)
        activeColor = statisticStyle.color;

      environment
        .withVariable("color", color.hexColor())
        .withVariable("selected", color == activeColor)
        .withVariable("name", color.name())
        .withVariable("display_name", color.displayName())
        .withVariable("icon_type", color.iconType());

      inventory.setItem(index, config.rootSection.sidebar.colorDisplay.items.colorIcon.build(environment));
    }
  }

  public @Nullable NamedColor getColorBySlotIndex(int slotIndex) {
    return colorBySlotIndex.get(slotIndex);
  }

  public void toggleFormat(Format format) {
    var currentStyle = accessSelectedStatisticStyle();

    if (currentStyle == null) {
      var value = commonFormatStates[format.ordinal()] ^= true;

      for (var valueStyle : getSelectedStyleMap().values())
        valueStyle.setFormat(format, value);

      return;
    }

    currentStyle.toggleFormat(format);
  }

  public void onColorSelection(NamedColor color) {
    var currentStyle = accessSelectedStatisticStyle();

    if (currentStyle == null) {
      this.commonColor = color;

      for (var valueStyle : getSelectedStyleMap().values())
        valueStyle.color = color;

      return;
    }

    currentStyle.color = color;
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
    var statisticStyle = accessSelectedStatisticStyle();

    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("is_label_color", selectingLabelColor)
      .withVariable("statistic", displayData.statistic() == null ? null : displayData.statistic().iconData.name.markupNode)
      .withVariable("is_bold", statisticStyle == null ? commonFormatStates[Format.BOLD.ordinal()] : statisticStyle.formats.contains(Format.BOLD))
      .withVariable("is_underlined", statisticStyle == null ? commonFormatStates[Format.UNDERLINED.ordinal()] : statisticStyle.formats.contains(Format.UNDERLINED))
      .withVariable("is_italic", statisticStyle == null ? commonFormatStates[Format.ITALIC.ordinal()] : statisticStyle.formats.contains(Format.ITALIC));
  }

  private @Nullable ColorAndFormats accessSelectedStatisticStyle() {
    if (displayData.statistic() == null)
      return null;

    var statistic = displayData.statistic()._sidebarStatistic;

    return getSelectedStyleMap().get(statistic);
  }

  private Map<SidebarStatistic, ColorAndFormats> getSelectedStyleMap() {
    if (selectingLabelColor)
      return displayData.preferences().labelStyleByStatistic;

    return displayData.preferences().valueStyleByStatistic;
  }
}
