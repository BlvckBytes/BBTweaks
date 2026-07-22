package me.blvckbytes.bbtweaks.pipes.search.command;

import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PipeFetchParameter extends PipeSearchParameter {

  public final FetchMode fetchMode;
  public final int maximumAmount;

  public PipeFetchParameter(
    @Nullable PredicateAndLanguage predicateAndLanguage,
    EnumSet<CommandFlag> flags,
    FetchMode fetchMode,
    int maximumAmount
  ) {
    super(predicateAndLanguage, flags);

    this.fetchMode = fetchMode;
    this.maximumAmount = maximumAmount;
  }
}
