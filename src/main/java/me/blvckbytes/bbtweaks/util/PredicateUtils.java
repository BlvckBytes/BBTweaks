package me.blvckbytes.bbtweaks.util;

import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.entity.Player;

import java.util.List;

public class PredicateUtils {

  public static List<String> tabCompletePredicate(
    Player player, String[] args, int firstArgIndex,
    IPPIntegration ippIntegration, boolean withLanguage
  ) {
    TranslationLanguage language;
    int argsOffset;

    if (withLanguage) {
      if (args.length == firstArgIndex + 1)
        return TranslationLanguage.matcher.createCompletions(args[firstArgIndex]);

      var matchedLanguage = TranslationLanguage.matcher.matchFirst(args[firstArgIndex]);

      if (matchedLanguage == null)
        return List.of();

      language = matchedLanguage.constant;
      argsOffset = firstArgIndex + 1;
    }

    else {
      language = ippIntegration.predicateHelper.getSelectedLanguage(player);
      argsOffset = firstArgIndex;
    }

    try {
      var tokens = ippIntegration.predicateHelper.parseTokens(args, argsOffset);
      var completions = ippIntegration.predicateHelper.createCompletion(language, tokens);

      if (completions.expandedPreviewOrError() != null)
        player.sendActionBar(completions.expandedPreviewOrError());

      return completions.suggestions();
    } catch (ItemPredicateParseException e) {
      player.sendActionBar(ippIntegration.predicateHelper.createExceptionMessage(e));
      return List.of();
    }
  }
}
