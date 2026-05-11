package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface FilterPredicateAccessor {

  @Nullable ItemPredicate accessFilterPredicate(Player player, PersistentDataContainer pdc);

}
