package me.blvckbytes.bbtweaks.inv_filter;

import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import org.jetbrains.annotations.Nullable;

public class InventoryFilter {

  public final @Nullable PredicateAndLanguage predicateAndLanguage;
  public final @Nullable String predicateString;
  public final boolean enabled;

  public InventoryFilter(@Nullable PredicateAndLanguage predicateAndLanguage, boolean enabled) {
    this.predicateAndLanguage = predicateAndLanguage;
    this.predicateString = predicateAndLanguage == null ? null : predicateAndLanguage.getTokenPredicateString();
    this.enabled = enabled;
  }
}
