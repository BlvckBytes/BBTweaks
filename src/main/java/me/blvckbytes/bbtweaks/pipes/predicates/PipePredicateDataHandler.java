package me.blvckbytes.bbtweaks.pipes.predicates;

import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class PipePredicateDataHandler {

  // There are countless PDC-entries on the server already, so we cannot afford to break backwards-compatibility.
  private static final String KEY_NAMESPACE = "craftbookpipepredicates";

  private final PredicateHelper predicateHelper;

  private final NamespacedKey tokensPredicateKey, expandedPredicateKey, predicateLanguageKey;
  private final NamespacedKey signLine1Key, signLine3Key, signLine4Key;

  public PipePredicateDataHandler(
    IPPIntegration ippIntegration
  ) {
    this.predicateHelper = ippIntegration.predicateHelper;

    this.tokensPredicateKey = new NamespacedKey(KEY_NAMESPACE, "tokens-predicate");
    this.expandedPredicateKey = new NamespacedKey(KEY_NAMESPACE, "expanded-predicate");
    this.predicateLanguageKey = new NamespacedKey(KEY_NAMESPACE, "predicate-language");

    this.signLine1Key = new NamespacedKey(KEY_NAMESPACE, "sign-line-1");
    this.signLine3Key = new NamespacedKey(KEY_NAMESPACE, "sign-line-3");
    this.signLine4Key = new NamespacedKey(KEY_NAMESPACE, "sign-line-4");
  }

  public void store(PredicateData data, Sign sign) {
    var container = sign.getPersistentDataContainer();

    if (data.parsedPredicate() != null)
      container.set(expandedPredicateKey, PersistentDataType.STRING, PlainStringifier.stringify(data.parsedPredicate(), false));
    else
      container.remove(expandedPredicateKey);

    container.set(tokensPredicateKey, PersistentDataType.STRING, data.tokensPredicate());
    container.set(predicateLanguageKey, PersistentDataType.STRING, data.predicateLanguage().name());
    container.set(signLine1Key, PersistentDataType.STRING, data.signLine1());
    container.set(signLine3Key, PersistentDataType.STRING, data.signLine3());
    container.set(signLine4Key, PersistentDataType.STRING, data.signLine4());

    sign.update(true, false);
  }

  public @Nullable PredicateData remove(Sign sign) {
    var predicateData = access(sign);

    if (predicateData == null)
      return null;

    var container = sign.getPersistentDataContainer();

    container.remove(expandedPredicateKey);
    container.remove(tokensPredicateKey);
    container.remove(predicateLanguageKey);
    container.remove(signLine1Key);
    container.remove(signLine3Key);
    container.remove(signLine4Key);

    sign.update(true, false);

    return predicateData;
  }

  public @Nullable PredicateData access(Sign sign) {
    var container = sign.getPersistentDataContainer();
    var expandedPredicate = container.get(expandedPredicateKey, PersistentDataType.STRING);

    if (expandedPredicate == null)
      return null;

    var tokensPredicate = container.get(tokensPredicateKey, PersistentDataType.STRING);
    var predicateLanguageName = container.get(predicateLanguageKey, PersistentDataType.STRING);
    var signLine1 = container.get(signLine1Key, PersistentDataType.STRING);
    var signLine3 = container.get(signLine3Key, PersistentDataType.STRING);
    var signLine4 = container.get(signLine4Key, PersistentDataType.STRING);

    TranslationLanguage predicateLanguage;

    try {
      predicateLanguage = TranslationLanguage.valueOf(predicateLanguageName);
    } catch (Exception e) {
      return null;
    }

    ItemPredicate predicate = null;
    ItemPredicateParseException exception = null;

    try {
      var tokens = predicateHelper.parseTokens(expandedPredicate);
      predicate = predicateHelper.parsePredicate(predicateLanguage, tokens);
    } catch (ItemPredicateParseException e) {
      exception = e;
    }

    var result = new PredicateData(
      tokensPredicate == null ? "" : tokensPredicate,
      expandedPredicate,
      predicateLanguage,
      signLine1 == null ? "" : signLine1,
      signLine3 == null ? "" : signLine3,
      signLine4 == null ? "" : signLine4,
      predicate, exception
    );

    updateSignErrorMode(sign, result);

    return result;
  }

  private void updateSignErrorMode(Sign pistonSign, PredicateData predicateData) {
    var signSide = pistonSign.getSide(Side.FRONT);

    var lineComponent = signSide.line(0);

    if (predicateData.parsedPredicate() == null) {
      var newComponent = Component.text(PipePredicateMarkerConstants.PREDICATE_MARKER).color(PipePredicateMarkerConstants.PREDICATE_ERROR_COLOR);

      if (!lineComponent.equals(newComponent)) {
        signSide.line(0, newComponent);
        pistonSign.update(true, false);
      }

      return;
    }

    var newComponent = Component.text(PipePredicateMarkerConstants.PREDICATE_MARKER).color(PipePredicateMarkerConstants.PREDICATE_OK_COLOR);

    if (!lineComponent.equals(newComponent)) {
      signSide.line(0, newComponent);
      pistonSign.update(true, false);
    }
  }
}
