package me.blvckbytes.bbtweaks.inv_filter;

import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import org.jetbrains.annotations.Nullable;

public class InventoryFilter {

  public final @Nullable PredicateAndLanguage predicateAndLanguage;
  public final @Nullable String predicateString;
  public final boolean enabled;

  public InventoryFilter(@Nullable PredicateAndLanguage predicateAndLanguage, boolean enabled) {
    this.predicateAndLanguage = predicateAndLanguage;
    this.predicateString = predicateAndLanguage == null ? null : PlainStringifier.stringify(predicateAndLanguage.predicate(), true);
    this.enabled = enabled;
  }
}
