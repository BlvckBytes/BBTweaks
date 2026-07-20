package me.blvckbytes.bbtweaks.integration.ipp;

import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.TranslationLanguageRegistry;
import org.bukkit.command.PluginCommand;

public class IPPIntegration {

  public final PredicateHelper predicateHelper;
  public final TranslationLanguageRegistry languageRegistry;
  public final PluginCommand mainCommand;

  public IPPIntegration(
    PredicateHelper predicateHelper,
    TranslationLanguageRegistry languageRegistry,
    PluginCommand mainCommand
  ) {
    this.predicateHelper = predicateHelper;
    this.languageRegistry = languageRegistry;
    this.mainCommand = mainCommand;
  }
}
