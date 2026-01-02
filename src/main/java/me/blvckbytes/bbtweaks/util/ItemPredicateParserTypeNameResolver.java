package me.blvckbytes.bbtweaks.util;

import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.TranslationLanguageRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ItemPredicateParserTypeNameResolver implements TypeNameResolver {

  private final PredicateHelper predicateHelper;
  private final TranslationLanguageRegistry languageRegistry;

  public ItemPredicateParserTypeNameResolver() {
    var ippInstance = ItemPredicateParserPlugin.getInstance();

    if (ippInstance == null)
      throw new IllegalStateException("ItemPredicateParser is not enabled!");

    this.predicateHelper = ippInstance.getPredicateHelper();
    this.languageRegistry = ippInstance.getTranslationLanguageRegistry();
  }

  @Override
  public String resolve(Player player, Material type) {
    var language = predicateHelper.getSelectedLanguage(player);

    var translation = languageRegistry
      .getTranslationRegistry(language)
      .getTranslationBySingleton(type);

    return translation == null ? type.name() : translation;
  }
}
