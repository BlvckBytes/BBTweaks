package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ReloadPriority;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SidebarPreferencesStore implements Listener {

  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey keyEnabled, keyShowTitle, keyDelimitersMode, keySneakMode,
    keyEnabledStatistics, keyStatisticsOrder, keyStatisticsLabelColors, keyStatisticsValueColors;

  private final Map<UUID, SidebarPreferences> preferencesByPlayerId;

  public SidebarPreferencesStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.config = config;

    this.keyEnabled = new NamespacedKey(plugin, "sidebar-enabled");
    this.keyShowTitle = new NamespacedKey(plugin, "sidebar-show-title");
    this.keyDelimitersMode = new NamespacedKey(plugin, "sidebar-delimiters-mode");
    this.keySneakMode = new NamespacedKey(plugin, "sidebar-sneak-mode");
    this.keyEnabledStatistics = new NamespacedKey(plugin, "sidebar-enabled-statistics");
    this.keyStatisticsOrder = new NamespacedKey(plugin, "sidebar-statistics-order");

    // Due to backwards compatibility, the first key has to stay that way...
    this.keyStatisticsLabelColors = new NamespacedKey(plugin, "sidebar-statistics-colors");
    this.keyStatisticsValueColors = new NamespacedKey(plugin, "sidebar-statistics-value-colors");

    this.preferencesByPlayerId = new HashMap<>();

    config.registerReloadListener(() -> {
      for (var preference : preferencesByPlayerId.values())
        preference.onConfigReload();
    }, ReloadPriority.LOW);
  }

  public SidebarPreferences accessPreferences(Player player) {
    return preferencesByPlayerId.computeIfAbsent(player.getUniqueId(), k -> loadPreferences(player));
  }

  public void onShutdown() {
    preferencesByPlayerId.values().forEach(this::savePreferences);
    preferencesByPlayerId.clear();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var preferences = preferencesByPlayerId.remove(event.getPlayer().getUniqueId());

    if (preferences != null)
      savePreferences(preferences);
  }

  private SidebarPreferences loadPreferences(Player player) {
    var result = new SidebarPreferences(player, config);

    var pdc = player.getPersistentDataContainer();

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      result.enabled = enabledValue;

    var showTitleValue = pdc.get(keyShowTitle, PersistentDataType.BOOLEAN);

    if (showTitleValue != null)
      result.showTitle = showTitleValue;

    var delimitersModeValue = pdc.get(keyDelimitersMode, PersistentDataType.INTEGER);

    if (delimitersModeValue != null)
      result.delimitersMode = DelimitersMode.byOrdinalOrDefault(delimitersModeValue);

    var sneakModeValue = pdc.get(keySneakMode, PersistentDataType.INTEGER);

    if (sneakModeValue != null)
      result.sneakMode = SneakMode.byOrdinalOrDefault(sneakModeValue);

    var enabledStatisticsValue = pdc.get(keyEnabledStatistics, PersistentDataType.INTEGER_ARRAY);

    if (enabledStatisticsValue != null) {
      result.enabledStatistics.clear();

      for (var ordinal : enabledStatisticsValue) {
        var statistic = SidebarStatistic.byOrdinalOrNull(ordinal);

        if (statistic != null)
          result.enabledStatistics.add(statistic);
      }
    }

    var statisticsOrderValue = pdc.get(keyStatisticsOrder, PersistentDataType.INTEGER_ARRAY);

    if (statisticsOrderValue != null) {
      result.statisticsInOrder.clear();

      for (var ordinal : statisticsOrderValue) {
        var statistic = SidebarStatistic.byOrdinalOrNull(ordinal);

        if (statistic != null && !result.statisticsInOrder.contains(statistic))
          result.statisticsInOrder.add(statistic);
      }

      for (var statistic : SidebarStatistic.ALL_VALUES) {
        if (!result.statisticsInOrder.contains(statistic))
          result.statisticsInOrder.add(statistic);
      }
    }

    loadStatisticColors(pdc.get(keyStatisticsLabelColors, PersistentDataType.STRING), result.labelColorByStatistic);
    loadStatisticColors(pdc.get(keyStatisticsValueColors, PersistentDataType.STRING), result.valueColorByStatistic);

    return result;
  }

  private void loadStatisticColors(@Nullable String colorsValue, Map<SidebarStatistic, NamedColor> output) {
    if (colorsValue == null)
      return;

    var colorNames = colorsValue.split(";");

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      var ordinal = statistic.ordinal();

      if (ordinal >= colorNames.length)
        break;

      var color = config.rootSection.sidebar._colorByNameLower.get(colorNames[ordinal].toLowerCase());

      if (color == null)
        continue;

      output.put(statistic, color);
    }
  }

  private void savePreferences(SidebarPreferences preferences) {
    var pdc = preferences.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, preferences.enabled);
    pdc.set(keyShowTitle, PersistentDataType.BOOLEAN, preferences.showTitle);
    pdc.set(keyDelimitersMode, PersistentDataType.INTEGER, preferences.delimitersMode.ordinal());
    pdc.set(keySneakMode, PersistentDataType.INTEGER, preferences.sneakMode.ordinal());

    var enabledStatistics = new IntArrayList();

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (preferences.enabledStatistics.contains(statistic))
        enabledStatistics.add(statistic.ordinal());
    }

    pdc.set(keyEnabledStatistics, PersistentDataType.INTEGER_ARRAY, enabledStatistics.toIntArray());

    pdc.set(keyStatisticsLabelColors, PersistentDataType.STRING, serializeStatisticColors(preferences.labelColorByStatistic));
    pdc.set(keyStatisticsValueColors, PersistentDataType.STRING, serializeStatisticColors(preferences.valueColorByStatistic));

    var statisticsOrder = new IntArrayList();

    for (var statistic : preferences.statisticsInOrder)
      statisticsOrder.add(statistic.ordinal());

    pdc.set(keyStatisticsOrder, PersistentDataType.INTEGER_ARRAY, statisticsOrder.toIntArray());
  }

  private String serializeStatisticColors(Map<SidebarStatistic, NamedColor> input) {
    var colorsJoiner = new StringJoiner(";");

    for (var statistic : SidebarStatistic.ALL_VALUES)
      colorsJoiner.add(input.get(statistic).name());

    return colorsJoiner.toString();
  }
}
