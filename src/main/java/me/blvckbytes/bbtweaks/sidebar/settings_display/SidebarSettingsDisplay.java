package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.config.StatisticSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SidebarSettingsDisplay extends Display<SidebarPreferences> {

  private final Int2ObjectMap<StatisticSection> statisticBySlotIndex;

  public final boolean isFloodgate;

  public SidebarSettingsDisplay(
    Player player,
    SidebarPreferences displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    this.statisticBySlotIndex = new Int2ObjectOpenHashMap<>();

    show();
  }

  @Override
  protected void renderItems() {
    inventory.clear();

    var environment = makeEnvironment();

    config.rootSection.sidebar.settingsDisplay.items.filler.renderInto(inventory, environment);
    config.rootSection.sidebar.settingsDisplay.items.enabled.renderInto(inventory, environment);
    config.rootSection.sidebar.settingsDisplay.items.showTitle.renderInto(inventory, environment);
    config.rootSection.sidebar.settingsDisplay.items.valueColor.renderInto(inventory, environment);
    config.rootSection.sidebar.settingsDisplay.items.nextSneakMode.renderInto(inventory, environment);
    config.rootSection.sidebar.settingsDisplay.items.openSorting.renderInto(inventory, environment);
    config.rootSection.sidebar.settingsDisplay.items.resetToDefaults.renderInto(inventory, environment);

    var nextStatisticIndex = 0;

    for (var index = 0; index < inventory.getSize(); ++index) {
      var currentItem = inventory.getItem(index);

      if (currentItem != null && !currentItem.getType().isAir())
        continue;

      if (nextStatisticIndex == displayData.statisticsInOrder.size())
        break;

      var statistic = displayData.statisticsInOrder.get(nextStatisticIndex++);
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      statisticBySlotIndex.put(index, statisticSection);

      environment
        .withVariable("name", statisticSection.iconData.name.markupNode)
        .withVariable("description", statisticSection.iconData.description.markupNode)
        .withVariable("icon_type", statisticSection.iconData._iconType)
        .withVariable("label_color", displayData.labelColorByStatistic.get(statistic))
        .withVariable("enabled", displayData.enabledStatistics.contains(statistic));

      inventory.setItem(index, config.rootSection.sidebar.settingsDisplay.items.statisticIcon.build(environment));
    }
  }

  public @Nullable StatisticSection getStatisticBySlotIndex(int slotIndex) {
    return statisticBySlotIndex.get(slotIndex);
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.sidebar.settingsDisplay.createInventory(makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("value_color", displayData.valueColor)
      .withVariable("sidebar_enabled", displayData.enabled)
      .withVariable("show_title", displayData.showTitle)
      .withVariable("sneak_mode", displayData.sneakMode.name());
  }
}
