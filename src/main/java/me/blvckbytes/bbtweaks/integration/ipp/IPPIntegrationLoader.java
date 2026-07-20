package me.blvckbytes.bbtweaks.integration.ipp;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import org.bukkit.Bukkit;

public class IPPIntegrationLoader {

  @WrappedDependency
  public final IPPIntegration ippIntegration;

  public IPPIntegrationLoader() {
    ItemPredicateParserPlugin ipp;

    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ItemPredicateParser") || (ipp = ItemPredicateParserPlugin.getInstance()) == null)
      throw new IllegalStateException("Expected plugin ItemPredicateParser to have been loaded at this point");

    ippIntegration = new IPPIntegration(
      ipp.getPredicateHelper(),
      ipp.getTranslationLanguageRegistry(),
      ipp.getMainCommand()
    );
  }
}
