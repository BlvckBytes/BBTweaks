package me.blvckbytes.bbtweaks.pipes.predicates;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

public interface PipePredicateRegistry {

  @Nullable ItemPredicate getPredicateForPiston(Block block);

}
