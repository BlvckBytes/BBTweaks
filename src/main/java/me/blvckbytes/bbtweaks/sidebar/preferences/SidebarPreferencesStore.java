package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ReloadPriority;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SidebarPreferencesStore implements Listener {

  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey keyEnabled, keyShowTitle, keySneakMode, keyValueColor,
    keyEnabledStatistics, keyStatisticsOrder, keyStatisticsColors;

  private final Map<UUID, SidebarPreferences> preferencesByPlayerId;

  public SidebarPreferencesStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.config = config;

    this.keyEnabled = new NamespacedKey(plugin, "sidebar-enabled");
    this.keyShowTitle = new NamespacedKey(plugin, "sidebar-show-title");
    this.keySneakMode = new NamespacedKey(plugin, "sidebar-sneak-mode");
    this.keyValueColor= new NamespacedKey(plugin, "sidebar-value-color");
    this.keyEnabledStatistics = new NamespacedKey(plugin, "sidebar-enabled-statistics");
    this.keyStatisticsOrder = new NamespacedKey(plugin, "sidebar-statistics-order");
    this.keyStatisticsColors = new NamespacedKey(plugin, "sidebar-statistics-colors");

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

    var showTitleValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (showTitleValue != null)
      result.showTitle = showTitleValue;

    var sneakModeValue = pdc.get(keySneakMode, PersistentDataType.INTEGER);

    if (sneakModeValue != null)
      result.sneakMode = SneakMode.byOrdinalOrDefault(sneakModeValue);

    var valueColorValue = pdc.get(keyValueColor, PersistentDataType.STRING);

    if (valueColorValue != null) {
      var color = config.rootSection.sidebar._colorByNameLower.get(valueColorValue.toLowerCase());

      if (color != null)
        result.valueColor = color;
    }

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

    var statisticsColorsValue = pdc.get(keyStatisticsColors, PersistentDataType.STRING);

    if (statisticsColorsValue != null) {
      var colorNames = statisticsColorsValue.split(";");

      for (var statistic : SidebarStatistic.ALL_VALUES) {
        var ordinal = statistic.ordinal();

        if (ordinal >= colorNames.length)
          break;

        var color = config.rootSection.sidebar._colorByNameLower.get(colorNames[ordinal].toLowerCase());

        if (color == null)
          continue;

        result.labelColorByStatistic.put(statistic, color);
      }
    }

    return result;
  }

  private void savePreferences(SidebarPreferences preferences) {
    var pdc = preferences.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, preferences.enabled);
    pdc.set(keyShowTitle, PersistentDataType.BOOLEAN, preferences.showTitle);
    pdc.set(keySneakMode, PersistentDataType.INTEGER, preferences.sneakMode.ordinal());
    pdc.set(keyValueColor, PersistentDataType.STRING, preferences.valueColor.name());

    var enabledStatistics = new IntArrayList();
    var colorsJoiner = new StringJoiner(";");

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (preferences.enabledStatistics.contains(statistic))
        enabledStatistics.add(statistic.ordinal());

      var color = preferences.labelColorByStatistic.get(statistic);

      colorsJoiner.add(color.name());
    }

    pdc.set(keyEnabledStatistics, PersistentDataType.INTEGER_ARRAY, enabledStatistics.toIntArray());
    pdc.set(keyStatisticsColors, PersistentDataType.STRING, colorsJoiner.toString());

    var statisticsOrder = new IntArrayList();

    for (var statistic : preferences.statisticsInOrder)
      statisticsOrder.add(statistic.ordinal());

    pdc.set(keyStatisticsOrder, PersistentDataType.INTEGER_ARRAY, statisticsOrder.toIntArray());
  }
}
