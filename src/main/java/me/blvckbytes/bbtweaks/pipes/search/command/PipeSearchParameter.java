package me.blvckbytes.bbtweaks.pipes.search.command;

import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PipeSearchParameter {

  public final @Nullable PredicateAndLanguage predicateAndLanguage;
  public final EnumSet<CommandFlag> flags;

  public PipeSearchParameter(@Nullable PredicateAndLanguage predicateAndLanguage, EnumSet<CommandFlag> flags) {
    this.predicateAndLanguage = predicateAndLanguage;
    this.flags = flags;
  }
}
