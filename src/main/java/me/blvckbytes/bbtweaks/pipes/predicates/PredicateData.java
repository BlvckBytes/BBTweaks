package me.blvckbytes.bbtweaks.pipes.predicates;

import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.Nullable;

public record PredicateData(
  String tokensPredicate,
  String expandedPredicate,
  TranslationLanguage predicateLanguage,
  String signLine1,
  String signLine3,
  String signLine4,
  @Nullable ItemPredicate parsedPredicate,
  @Nullable ItemPredicateParseException parseException
) {

  public void restoreLines(Sign sign) {
    var signSide = sign.getSide(Side.FRONT);

    signSide.line(0, Component.text(signLine1));
    signSide.line(2, Component.text(signLine3));
    signSide.line(3, Component.text(signLine4));

    sign.update(true, false);
  }

  public static PredicateData makeInitial(ItemPredicate predicate, TranslationLanguage language, Sign sign) {
    var signSide = sign.getSide(Side.FRONT);

    return new PredicateData(
      PlainStringifier.stringify(predicate, true),
      PlainStringifier.stringify(predicate, false),
      language,
      ComponentUtil.asTrimmedText(signSide.line(0)),
      ComponentUtil.asTrimmedText(signSide.line(2)),
      ComponentUtil.asTrimmedText(signSide.line(3)),
      predicate, null
    );
  }

  public static PredicateData makeUpdate(ItemPredicate predicate, TranslationLanguage language, PredicateData previous) {
    return new PredicateData(
      PlainStringifier.stringify(predicate, true),
      PlainStringifier.stringify(predicate, false),
      language,
      previous.signLine1,
      previous.signLine3,
      previous.signLine4,
      predicate, null
    );
  }
}
