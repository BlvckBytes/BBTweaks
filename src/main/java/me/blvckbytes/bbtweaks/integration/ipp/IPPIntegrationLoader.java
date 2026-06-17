package me.blvckbytes.bbtweaks.integration.ipp;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import org.bukkit.Bukkit;

public class PredicateHelperIntegrationLoader {

  @WrappedDependency
  public final PredicateHelper predicateHelper;

  public PredicateHelperIntegrationLoader() {
    ItemPredicateParserPlugin ipp;

    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ItemPredicateParser") || (ipp = ItemPredicateParserPlugin.getInstance()) == null)
      throw new IllegalStateException("Expected plugin ItemPredicateParser to have been loaded at this point");

    predicateHelper = ipp.getPredicateHelper();
  }
}
