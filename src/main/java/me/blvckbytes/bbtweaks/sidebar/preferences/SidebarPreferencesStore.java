package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SidebarPreferencesStore implements Disableable, Listener {

  public static final int PREFERENCES_SLOTS_COUNT = 3;

  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey keyEnabled, keySelectedSlotIndex;

  private final NamespacedKey[] keysShowTitle, keysShowIcons, keysDoScroll, keysAutoSort, keysDelimitersMode, keysSneakMode,
    keysStatisticEnableModes, keysStatisticsOrder, keysStatisticsLabelStyles, keysStatisticsValueStyles;

  private final Map<UUID, SidebarPreferencesSlots> preferencesSlotsByPlayerId;

  public SidebarPreferencesStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.config = config;

    this.keyEnabled = new NamespacedKey(plugin, "sidebar-enabled");
    this.keySelectedSlotIndex = new NamespacedKey(plugin, "sidebar-selected-slot-index");

    this.keysShowTitle = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysShowIcons = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysDoScroll = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysAutoSort = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysDelimitersMode = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysSneakMode = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysStatisticEnableModes = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysStatisticsOrder = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysStatisticsLabelStyles = new NamespacedKey[PREFERENCES_SLOTS_COUNT];
    this.keysStatisticsValueStyles = new NamespacedKey[PREFERENCES_SLOTS_COUNT];

    for (var slotIndex = 0; slotIndex < PREFERENCES_SLOTS_COUNT; ++slotIndex) {
      var baseKey = "sidebar";

      if (slotIndex > 0)
        baseKey += "-" + slotIndex;

      this.keysShowTitle[slotIndex] = new NamespacedKey(plugin, baseKey + "-show-title");
      this.keysShowIcons[slotIndex] = new NamespacedKey(plugin, baseKey + "-show-icons");
      this.keysDoScroll[slotIndex] = new NamespacedKey(plugin, baseKey + "-do-scroll");
      this.keysAutoSort[slotIndex] = new NamespacedKey(plugin, baseKey + "-auto-sort");
      this.keysDelimitersMode[slotIndex] = new NamespacedKey(plugin, baseKey + "-delimiters-mode");
      this.keysSneakMode[slotIndex] = new NamespacedKey(plugin, baseKey + "-sneak-mode");
      this.keysStatisticEnableModes[slotIndex] = new NamespacedKey(plugin, baseKey + "-statistic-enable-modes");
      this.keysStatisticsOrder[slotIndex] = new NamespacedKey(plugin, baseKey + "-statistics-order");

      // Due to backwards compatibility, the naming will be a bit off...
      this.keysStatisticsLabelStyles[slotIndex] = new NamespacedKey(plugin, baseKey + "-statistics-colors");
      this.keysStatisticsValueStyles[slotIndex] = new NamespacedKey(plugin, baseKey + "-statistics-value-colors");
    }

    this.preferencesSlotsByPlayerId = new HashMap<>();
  }

  public SidebarPreferencesSlots accessPreferencesSlots(Player player) {
    return preferencesSlotsByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> loadPreferencesSlots(player));
  }

  @Override
  public void disable() {
    preferencesSlotsByPlayerId.values().forEach(this::savePreferencesSlots);
    preferencesSlotsByPlayerId.clear();
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper != config)
      return;

    for (var preferenceSlots : preferencesSlotsByPlayerId.values())
      preferenceSlots.onConfigReload();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var preferencesSlots = preferencesSlotsByPlayerId.remove(event.getPlayer().getUniqueId());

    if (preferencesSlots != null)
      savePreferencesSlots(preferencesSlots);
  }

  private SidebarPreferencesSlots loadPreferencesSlots(Player player) {
    var slotsList = new ArrayList<SidebarPreferences>();
    var slots = new SidebarPreferencesSlots(player, config, slotsList);

    for (var slotIndex = 0; slotIndex < PREFERENCES_SLOTS_COUNT; ++slotIndex)
      slotsList.add(loadPreferences(slots, slotIndex));

    var pdc = player.getPersistentDataContainer();

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      slots.enabled = enabledValue;

    var selectedSlotIndexValue = pdc.get(keySelectedSlotIndex, PersistentDataType.INTEGER);

    if (selectedSlotIndexValue != null)
      slots.setSelectedSlotIndex(selectedSlotIndexValue, false);

    return slots;
  }

  private SidebarPreferences loadPreferences(SidebarPreferencesSlots preferencesSlots, int slotIndex) {
    var result = new SidebarPreferences(preferencesSlots, slotIndex, config);

    var pdc = preferencesSlots.player.getPersistentDataContainer();

    var showTitleValue = pdc.get(keysShowTitle[slotIndex], PersistentDataType.BOOLEAN);

    if (showTitleValue != null)
      result.showTitle = showTitleValue;

    var showIconsValue = pdc.get(keysShowIcons[slotIndex], PersistentDataType.BOOLEAN);

    if (showIconsValue != null)
      result.showIcons = showIconsValue;

    var doScrollValue = pdc.get(keysDoScroll[slotIndex], PersistentDataType.BOOLEAN);

    if (doScrollValue != null)
      result.doScroll = doScrollValue;

    var defaults = config.rootSection.sidebar.getDefaultsForSlot(slotIndex);

    var delimitersModeValue = pdc.get(keysDelimitersMode[slotIndex], PersistentDataType.INTEGER);

    if (delimitersModeValue != null)
      result.delimitersMode = DelimitersMode.byOrdinalOrDefault(delimitersModeValue, defaults);

    var sneakModeValue = pdc.get(keysSneakMode[slotIndex], PersistentDataType.INTEGER);

    if (sneakModeValue != null)
      result.sneakMode = SneakMode.byOrdinalOrDefault(sneakModeValue, defaults);

    var autoSortValue = pdc.get(keysAutoSort[slotIndex], PersistentDataType.INTEGER);

    if (autoSortValue != null)
      result.autoSortMode = AutoSortMode.byOrdinalOrDefault(autoSortValue, defaults);

    var enableModesValue = pdc.get(keysStatisticEnableModes[slotIndex], PersistentDataType.INTEGER_ARRAY);

    if (enableModesValue != null) {
      for (var statistic : SidebarStatistic.ALL_VALUES) {
        var enableMode = defaults._enableModeByStatistic.get(statistic);

        if (statistic.ordinal() < enableModesValue.length) {
          var modeValue = StatisticEnableMode.byOrdinalOrNull(enableModesValue[statistic.ordinal()]);

          if (modeValue != null)
            enableMode = modeValue;
        }

        result.enableModeByStatistic.put(statistic, enableMode);
      }
    }

    var statisticsOrderValue = pdc.get(keysStatisticsOrder[slotIndex], PersistentDataType.INTEGER_ARRAY);

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

    loadStatisticStyles(pdc.get(keysStatisticsLabelStyles[slotIndex], PersistentDataType.STRING), result.labelStyleByStatistic::put);
    loadStatisticStyles(pdc.get(keysStatisticsValueStyles[slotIndex], PersistentDataType.STRING), result.valueStyleByStatistic::put);

    return result;
  }

  private void loadStatisticStyles(@Nullable String stylesValue, BiConsumer<SidebarStatistic, ColorAndFormats> setter) {
    if (stylesValue == null)
      return;

    var styleValues = stylesValue.split(";");

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      var ordinal = statistic.ordinal();

      if (ordinal >= styleValues.length)
        break;

      var styleParts = styleValues[ordinal].split("\\|");

      var color = config.rootSection.sidebar._colorByNameLower.get(styleParts[0].toLowerCase());

      if (color == null)
        continue;

      var formats = EnumSet.noneOf(Format.class);

      if (styleParts.length > 1) {
        var formatsMask = 0;

        try {
          formatsMask = Integer.parseInt(styleParts[1]);
        } catch (Throwable ignored) {}

        for (var format : Format.ALL_VALUES) {
          if ((formatsMask & (1 << format.ordinal())) != 0)
            formats.add(format);
        }
      }

      setter.accept(statistic, new ColorAndFormats(color, formats));
    }
  }

  private void savePreferencesSlots(SidebarPreferencesSlots preferencesSlots) {
    var pdc = preferencesSlots.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, preferencesSlots.enabled);
    pdc.set(keySelectedSlotIndex, PersistentDataType.INTEGER, preferencesSlots.getSelectedSlotIndex());

    preferencesSlots.preferencesBySlotIndex.forEach(this::savePreferences);
  }

  private void savePreferences(SidebarPreferences preferences) {
    var pdc = preferences.preferencesSlots.player.getPersistentDataContainer();

    pdc.set(keysShowTitle[preferences.slotIndex], PersistentDataType.BOOLEAN, preferences.showTitle);
    pdc.set(keysShowIcons[preferences.slotIndex], PersistentDataType.BOOLEAN, preferences.showIcons);
    pdc.set(keysDoScroll[preferences.slotIndex], PersistentDataType.BOOLEAN, preferences.doScroll);
    pdc.set(keysDelimitersMode[preferences.slotIndex], PersistentDataType.INTEGER, preferences.delimitersMode.ordinal());
    pdc.set(keysSneakMode[preferences.slotIndex], PersistentDataType.INTEGER, preferences.sneakMode.ordinal());
    pdc.set(keysAutoSort[preferences.slotIndex], PersistentDataType.INTEGER, preferences.autoSortMode.ordinal());

    var enableModes = new IntArrayList();

    for (var statistic : SidebarStatistic.ALL_VALUES)
      enableModes.add(preferences.enableModeByStatistic.get(statistic).ordinal());

    pdc.set(keysStatisticEnableModes[preferences.slotIndex], PersistentDataType.INTEGER_ARRAY, enableModes.toIntArray());

    pdc.set(keysStatisticsLabelStyles[preferences.slotIndex], PersistentDataType.STRING, serializeStatisticStyles(preferences.labelStyleByStatistic::get));
    pdc.set(keysStatisticsValueStyles[preferences.slotIndex], PersistentDataType.STRING, serializeStatisticStyles(preferences.valueStyleByStatistic::get));

    var statisticsOrder = new IntArrayList();

    for (var statistic : preferences.statisticsInOrder)
      statisticsOrder.add(statistic.ordinal());

    pdc.set(keysStatisticsOrder[preferences.slotIndex], PersistentDataType.INTEGER_ARRAY, statisticsOrder.toIntArray());
  }

  private String serializeStatisticStyles(Function<SidebarStatistic, ColorAndFormats> getter) {
    var colorsJoiner = new StringJoiner(";");

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      var style = getter.apply(statistic);

      var formatMask = 0;

      for (var format : Format.ALL_VALUES) {
        if (style.formats.contains(format))
          formatMask |= 1 << format.ordinal();
      }

      colorsJoiner.add(style.color.name() + "|" + formatMask);
    }

    return colorsJoiner.toString();
  }
}
