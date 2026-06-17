package me.blvckbytes.bbtweaks.integration.ipp;

import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.TranslationLanguageRegistry;

public class IPPIntegration {

  public final PredicateHelper predicateHelper;
  public final TranslationLanguageRegistry languageRegistry;

  public IPPIntegration(PredicateHelper predicateHelper, TranslationLanguageRegistry languageRegistry) {
    this.predicateHelper = predicateHelper;
    this.languageRegistry = languageRegistry;
  }
}
