package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;

public interface PredicateErrorHandler {

  void handle(String predicate, String language, ItemPredicateParseException exception);

}
