package me.blvckbytes.bbtweaks.inv_filter;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;

public record PredicateAndLanguage(
  ItemPredicate predicate,
  TranslationLanguage language
) {}
