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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiBreakParametersStore implements Listener {

  private final NamespacedKey keyEnabled, keySneakMode, keyExtents, keyFilterPredicate, keyFilterLanguage;

  private final Plugin plugin;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, MultiBreakParameters> parametersByPlayerId;

  public MultiBreakParametersStore(
    Plugin plugin,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
  ) {
    keyEnabled = new NamespacedKey(plugin, "multi-break-enabled");
    keySneakMode = new NamespacedKey(plugin, "multi-break-sneak-mode");
    keyExtents = new NamespacedKey(plugin, "multi-break-extents");
    keyFilterPredicate = new NamespacedKey(plugin, "multi-break-filter-predicate");
    keyFilterLanguage = new NamespacedKey(plugin, "multi-break-filter-language");
    this.predicateHelper = predicateHelper;

    this.plugin = plugin;
    this.config = config;
    this.parametersByPlayerId = new HashMap<>();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    var parameters = load(player);

    parametersByPlayerId.put(player.getUniqueId(), parameters);

    if (!parameters.enabled || !config.rootSection.multiBreak.enabledJoinWarning.enabled)
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
    var parameters = parametersByPlayerId.remove(player.getUniqueId());

    if (parameters != null)
      save(parameters);
  }

  public void onShutdown() {
    parametersByPlayerId.values().forEach(this::save);
    parametersByPlayerId.clear();
  }

  public MultiBreakParameters accessParameters(Player player) {
    // It shouldn't ever be unloaded, if called after the player joined, but better safe than sorry.
    return parametersByPlayerId.computeIfAbsent(player.getUniqueId(), k -> load(player));
  }

  private MultiBreakParameters load(Player player) {
    var result = new MultiBreakParameters(player, config);

    var pdc = player.getPersistentDataContainer();

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);
    result.enabled = enabledValue != null && enabledValue;

    var sneakModeOrdinal = pdc.get(keySneakMode, PersistentDataType.INTEGER);
    result.sneakMode = SneakMode.byOrdinalOrFirst(sneakModeOrdinal == null ? 0 : sneakModeOrdinal);

    var extentsArray = pdc.get(keyExtents, PersistentDataType.INTEGER_ARRAY);

    if (extentsArray != null) {
      for (var currentExtent : BreakExtent.values) {
        if (currentExtent.ordinal() >= extentsArray.length)
          break;

        result.setExtent(currentExtent, extentsArray[currentExtent.ordinal()], false);
      }
    }

    result.filter = tryLoadFilter(pdc);

    result.constrainAndSetFlags(false);

    return result;
  }

  private @Nullable PredicateAndLanguage tryLoadFilter(PersistentDataContainer pdc) {
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

  private void save(MultiBreakParameters parameters) {
    var pdc = parameters.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, parameters.enabled);
    pdc.set(keySneakMode, PersistentDataType.INTEGER, parameters.sneakMode.ordinal());
    pdc.set(keyExtents, PersistentDataType.INTEGER_ARRAY, parameters.extentByOrdinal);

    if (parameters.filter == null) {
      if (pdc.has(keyFilterPredicate))
        pdc.remove(keyFilterPredicate);

      if (pdc.has(keyFilterLanguage))
        pdc.remove(keyFilterLanguage);

      return;
    }

    pdc.set(keyFilterPredicate, PersistentDataType.STRING, parameters.filter.getTokenPredicateString());
    pdc.set(keyFilterLanguage, PersistentDataType.STRING, parameters.filter.language.name());
  }
}
