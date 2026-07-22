package me.blvckbytes.bbtweaks.pipes.search.display;

import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SearchDisplayData(
  @Nullable PredicateAndLanguage predicate,
  List<? extends SearchDisplayEntry> entries,
  @Nullable PipeSearchDisplay backToDisplay
) {}