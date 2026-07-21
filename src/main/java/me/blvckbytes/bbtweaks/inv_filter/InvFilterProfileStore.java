package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvFilterProfileStore implements Listener {

  private static final int FILTER_SLOT_COUNT = 9;

  private final IPPIntegration ippIntegration;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, InvFilterProfile> profileByPlayerId;

  private final NamespacedKey[] keysFilterPredicate, keysFilterLanguage;
  private final NamespacedKey keySelectedSlotIndex, keyEnabled;

  public InvFilterProfileStore(
    IPPIntegration ippIntegration,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.ippIntegration = ippIntegration;
    this.config = config;

    this.profileByPlayerId = new HashMap<>();

    this.keysFilterPredicate = new NamespacedKey[FILTER_SLOT_COUNT];
    this.keysFilterLanguage = new NamespacedKey[FILTER_SLOT_COUNT];

    this.keySelectedSlotIndex = new NamespacedKey(plugin, "invfilter-selected-slot-index");
    this.keyEnabled = new NamespacedKey(plugin, "invfilter-enabled");

    for (var slotIndex = 0; slotIndex < FILTER_SLOT_COUNT; ++slotIndex) {
      var baseKey = "invfilter";

      // Keep index zero suffixless, for backwards compatibility.
      if (slotIndex > 0)
        baseKey += "-" + slotIndex;

      keysFilterPredicate[slotIndex] = new NamespacedKey(plugin, baseKey + "-predicate");
      keysFilterLanguage[slotIndex] = new NamespacedKey(plugin, baseKey + "-language");
    }
  }

  public InvFilterProfile access(Player player) {
    return profileByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var profile = profileByPlayerId.remove(event.getPlayer().getUniqueId());

    if (profile != null)
      save(profile);
  }

  private InvFilterProfile load(Player player) {
    var pdc = player.getPersistentDataContainer();
    var filterBySlotIndex = new ArrayList<@Nullable PredicateAndLanguage>();

    for (var slotIndex = 0; slotIndex < FILTER_SLOT_COUNT; ++slotIndex)
      filterBySlotIndex.add(loadFilterAtIndex(pdc, slotIndex));

    var profile = new InvFilterProfile(player, config, ippIntegration, filterBySlotIndex);

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      profile.enabled = enabledValue;

    var slotIndexValue = pdc.get(keySelectedSlotIndex, PersistentDataType.INTEGER);

    if (slotIndexValue != null)
      profile.setSelectedSlotIndex(slotIndexValue);

    return profile;
  }

  private void save(InvFilterProfile profile) {
    var pdc = profile.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, profile.isEnabled());
    pdc.set(keySelectedSlotIndex, PersistentDataType.INTEGER, profile.getSelectedSlotIndex());

    for (var slotIndex = 0; slotIndex < profile.getSlotCount(); ++slotIndex)
      saveFilterAtIndex(pdc, slotIndex, profile.getFilter(slotIndex));
  }

  private void saveFilterAtIndex(PersistentDataContainer pdc, int slotIndex, @Nullable PredicateAndLanguage filter) {
    if (filter == null) {
      pdc.remove(keysFilterPredicate[slotIndex]);
      pdc.remove(keysFilterLanguage[slotIndex]);
      return;
    }

    pdc.set(keysFilterPredicate[slotIndex], PersistentDataType.STRING, filter.getTokenPredicateString());
    pdc.set(keysFilterLanguage[slotIndex], PersistentDataType.STRING, filter.language.name());
  }

  private @Nullable PredicateAndLanguage loadFilterAtIndex(PersistentDataContainer pdc, int slotIndex) {
    var filterLanguage = pdc.get(keysFilterLanguage[slotIndex], PersistentDataType.STRING);

    TranslationLanguage language;

    try {
      language = TranslationLanguage.valueOf(filterLanguage);
    } catch (Throwable _) {
      return null;
    }

    var filterPredicate = pdc.get(keysFilterPredicate[slotIndex], PersistentDataType.STRING);

    ItemPredicate predicate;

    try {
      var tokens = ippIntegration.predicateHelper.parseTokens(filterPredicate);
      predicate = ippIntegration.predicateHelper.parsePredicate(language, tokens);
    } catch (ItemPredicateParseException e) {
      return null;
    }

    if (predicate == null)
      return null;

    return new PredicateAndLanguage(predicate, language);
  }
}
