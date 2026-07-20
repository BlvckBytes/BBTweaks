package me.blvckbytes.bbtweaks.pipes.search.display;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SearchDisplayData(
  @Nullable ItemPredicate predicate,
  List<? extends SearchDisplayEntry> entries,
  @Nullable PipeSearchDisplay backToDisplay
) {}