package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public record PredicateAndLanguage(ItemPredicate predicate, TranslationLanguage language) {

  private static final Component PREDICATE_MODE_LINE = Component.text("Predicate Mode").color(NamedTextColor.GREEN);

  public static @Nullable PredicateAndLanguage tryLoadFromSign(
    Sign sign,
    NamespacedKey filterPredicateKey,
    NamespacedKey filterLanguageKey
  ) {
    var pdc = sign.getPersistentDataContainer();

    var filterPredicate = pdc.get(filterPredicateKey, PersistentDataType.STRING);
    var filterLanguage = pdc.get(filterLanguageKey, PersistentDataType.STRING);

    if (filterPredicate == null || filterLanguage == null)
      return null;

    TranslationLanguage language;

    try {
      language = TranslationLanguage.valueOf(filterLanguage);
    } catch (Throwable e) {
      return null;
    }

    try {
      var ipp = ItemPredicateParserPlugin.getInstance();

      if (ipp == null)
        throw new IllegalStateException("Expected IPP to be loaded at this point");

      var predicateHelper = ipp.getPredicateHelper();
      var tokens = predicateHelper.parseTokens(filterPredicate);
      var predicate = predicateHelper.parsePredicate(language, tokens);
      return new PredicateAndLanguage(predicate, language);
    } catch (Throwable e) {
      return null;
    }
  }

  public static boolean updatePredicateMarkerAndGetIfMadeChanges(Sign sign, @Nullable PredicateAndLanguage predicateAndLanguage) {
    var side = sign.getSide(Side.FRONT);

    if (predicateAndLanguage == null) {
      if (side.line(0).equals(Component.empty()))
        return false;

      side.line(0, Component.empty());
      return true;
    }

    if (side.line(0).equals(PREDICATE_MODE_LINE))
      return false;

    side.line(0, PREDICATE_MODE_LINE);
    return true;
  }

  public static boolean writeToSignPdcAndGetIfMadeChanges(
    @Nullable PredicateAndLanguage predicateAndLanguage,
    Sign sign,
    NamespacedKey filterPredicateKey,
    NamespacedKey filterLanguageKey
  ) {
    var pdc = sign.getPersistentDataContainer();

    var existingFilterPredicate = pdc.get(filterPredicateKey, PersistentDataType.STRING);
    var existingFilterLanguage = pdc.get(filterLanguageKey, PersistentDataType.STRING);

    if (predicateAndLanguage != null) {
      var newFilterLanguage = predicateAndLanguage.language.name();
      var newFilterPredicate = PlainStringifier.stringify(predicateAndLanguage.predicate(), true);

      if (newFilterPredicate.equals(existingFilterPredicate) && newFilterLanguage.equals(existingFilterLanguage))
        return false;

      pdc.set(filterLanguageKey, PersistentDataType.STRING, newFilterLanguage);
      pdc.set(filterPredicateKey, PersistentDataType.STRING, newFilterPredicate);
      return true;
    }

    if (existingFilterPredicate == null && existingFilterLanguage == null)
      return false;

    pdc.remove(filterPredicateKey);
    pdc.remove(filterLanguageKey);
    return true;
  }
}
