package me.blvckbytes.bbtweaks.multi_break.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiBreakParametersStore implements Listener {

  public static final int PARAMETERS_SLOTS_COUNT = 5;

  private final NamespacedKey[] keysSneakMode, keysExtents, keysFilterPredicate, keysFilterLanguage;
  private final NamespacedKey keySelectedSlotIndex;
  private final NamespacedKey keyEnabled;

  private final Plugin plugin;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, MultiBreakParametersSlots> parametersSlotsByPlayerId;

  public MultiBreakParametersStore(
    Plugin plugin,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
  ) {
    this.keysSneakMode = new NamespacedKey[PARAMETERS_SLOTS_COUNT];
    this.keysExtents = new NamespacedKey[PARAMETERS_SLOTS_COUNT];
    this.keysFilterPredicate = new NamespacedKey[PARAMETERS_SLOTS_COUNT];
    this.keysFilterLanguage = new NamespacedKey[PARAMETERS_SLOTS_COUNT];

    this.keySelectedSlotIndex = new NamespacedKey(plugin, "multi-break-selected-slot-index");
    this.keyEnabled = new NamespacedKey(plugin, "multi-break-enabled");

    for (var slotIndex = 0; slotIndex < PARAMETERS_SLOTS_COUNT; ++slotIndex) {
      var baseKey = "multi-break";

      // Keep index zero suffixless, seeing how slots have been introduced at a point in time where players had
      // already accessed the multi-break feature; this way, we do not need to migrate and loose no data either.
      if (slotIndex > 0)
        baseKey += "-" + slotIndex;

      keysSneakMode[slotIndex] = new NamespacedKey(plugin, baseKey + "-sneak-mode");
      keysExtents[slotIndex] = new NamespacedKey(plugin, baseKey + "-extents");
      keysFilterPredicate[slotIndex] = new NamespacedKey(plugin, baseKey + "-filter-predicate");
      keysFilterLanguage[slotIndex] = new NamespacedKey(plugin, baseKey + "-filter-language");
    }

    this.predicateHelper = predicateHelper;
    this.plugin = plugin;
    this.config = config;

    this.parametersSlotsByPlayerId = new HashMap<>();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    possiblyWarnRegardingEnabledState(event.getPlayer());
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    possiblyWarnRegardingEnabledState(event.getPlayer());
  }

  private void possiblyWarnRegardingEnabledState(Player player) {
    var parametersSlots = accessParametersSlots(player);

    if (!parametersSlots.enabled || !config.rootSection.multiBreak.enabledJoinWarning.enabled)
      return;

    if (!config.rootSection.multiBreak.allowedWorlds.contains(player.getWorld().getName()))
      return;

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      player.sendTitlePart(
        TitlePart.TITLE,
        config.rootSection.multiBreak.enabledJoinWarning.title.interpret(SlotType.SINGLE_LINE_CHAT, new InterpretationEnvironment()).get(0)
      );

      player.sendTitlePart(
        TitlePart.SUBTITLE,
        config.rootSection.multiBreak.enabledJoinWarning.subtitle.interpret(SlotType.SINGLE_LINE_CHAT, new InterpretationEnvironment()).get(0)
      );

      player.sendTitlePart(
        TitlePart.TIMES,
        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(config.rootSection.multiBreak.enabledJoinWarning.durationMillis), Duration.ofMillis(100))
      );
    }, 10L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var player = event.getPlayer();
    var parametersSlots = parametersSlotsByPlayerId.remove(player.getUniqueId());

    if (parametersSlots != null)
      saveParametersSlots(parametersSlots);
  }

  public void onShutdown() {
    parametersSlotsByPlayerId.values().forEach(this::saveParametersSlots);
    parametersSlotsByPlayerId.clear();
  }

  public MultiBreakParametersSlots accessParametersSlots(Player player) {
    // It shouldn't ever be unloaded, if called after the player joined, but better safe than sorry.
    return parametersSlotsByPlayerId.computeIfAbsent(player.getUniqueId(), k -> loadParametersSlots(player));
  }

  private MultiBreakParametersSlots loadParametersSlots(Player player) {
    var slotsList = new ArrayList<MultiBreakParameters>();
    var slots = new MultiBreakParametersSlots(player, config, slotsList);

    for (var slotIndex = 0; slotIndex < PARAMETERS_SLOTS_COUNT; ++slotIndex)
      slotsList.add(loadParameters(slots, slotIndex));

    var pdc = player.getPersistentDataContainer();

    var selectedIndexValue = pdc.get(keySelectedSlotIndex, PersistentDataType.INTEGER);

    if (selectedIndexValue != null)
      slots.setSelectedSlotIndex(selectedIndexValue);

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      slots.enabled = enabledValue;

    return slots;
  }

  private MultiBreakParameters loadParameters(MultiBreakParametersSlots slots, int slotIndex) {
    var result = new MultiBreakParameters(slots, slotIndex);

    var pdc = slots.player.getPersistentDataContainer();

    var sneakModeOrdinal = pdc.get(keysSneakMode[slotIndex], PersistentDataType.INTEGER);
    result.sneakMode = SneakMode.byOrdinalOrFirst(sneakModeOrdinal == null ? 0 : sneakModeOrdinal);

    var extentsArray = pdc.get(keysExtents[slotIndex], PersistentDataType.INTEGER_ARRAY);

    if (extentsArray != null) {
      for (var currentExtent : BreakExtent.values) {
        if (currentExtent.ordinal() >= extentsArray.length)
          break;

        result.setExtent(currentExtent, extentsArray[currentExtent.ordinal()], false);
      }
    }

    result.filter = tryLoadFilter(pdc, keysFilterPredicate[slotIndex], keysFilterLanguage[slotIndex]);

    result.constrainAndSetFlags(false);

    return result;
  }

  private @Nullable PredicateAndLanguage tryLoadFilter(
    PersistentDataContainer pdc,
    NamespacedKey keyFilterPredicate,
    NamespacedKey keyFilterLanguage
  ) {
    if (!pdc.has(keyFilterPredicate, PersistentDataType.STRING) || !pdc.has(keyFilterLanguage, PersistentDataType.STRING))
      return null;

    TranslationLanguage language;

    try {
      language = TranslationLanguage.valueOf(pdc.get(keyFilterLanguage, PersistentDataType.STRING));
    } catch (Throwable e) {
      return null;
    }

    ItemPredicate predicate;

    try {
      var tokens = predicateHelper.parseTokens(pdc.get(keyFilterPredicate, PersistentDataType.STRING));
      predicate = predicateHelper.parsePredicate(language, tokens);
    } catch (Throwable e) {
      return null;
    }

    return new PredicateAndLanguage(predicate, language);
  }

  private void saveFilter(
    @Nullable PredicateAndLanguage filter,
    PersistentDataContainer pdc,
    NamespacedKey keyFilterPredicate,
    NamespacedKey keyFilterLanguage
  ) {
    if (filter == null) {
      if (pdc.has(keyFilterPredicate))
        pdc.remove(keyFilterPredicate);

      if (pdc.has(keyFilterLanguage))
        pdc.remove(keyFilterLanguage);

      return;
    }

    pdc.set(keyFilterPredicate, PersistentDataType.STRING, filter.getTokenPredicateString());
    pdc.set(keyFilterLanguage, PersistentDataType.STRING, filter.language.name());
  }

  private void saveParametersSlots(MultiBreakParametersSlots parametersSlots) {
    parametersSlots.parametersBySlotIndex.forEach(parameters -> saveParameters(parametersSlots.player, parameters));

    var pdc = parametersSlots.player.getPersistentDataContainer();

    pdc.set(keySelectedSlotIndex, PersistentDataType.INTEGER, parametersSlots.getSelectedSlotIndex());
    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, parametersSlots.enabled);
  }

  private void saveParameters(Player player, MultiBreakParameters parameters) {
    var pdc = player.getPersistentDataContainer();

    pdc.set(keysSneakMode[parameters.slotIndex], PersistentDataType.INTEGER, parameters.sneakMode.ordinal());
    pdc.set(keysExtents[parameters.slotIndex], PersistentDataType.INTEGER_ARRAY, parameters.extentByOrdinal);

    saveFilter(parameters.filter, pdc, keysFilterPredicate[parameters.slotIndex], keysFilterLanguage[parameters.slotIndex]);
  }
}
