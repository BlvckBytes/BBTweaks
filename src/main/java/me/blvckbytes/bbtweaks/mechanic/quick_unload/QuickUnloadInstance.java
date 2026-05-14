package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

public class QuickUnloadInstance extends SISOInstance {

  public final boolean silent;
  public final @Nullable ItemPredicate predicate;

  public QuickUnloadInstance(Sign sign, boolean silent, @Nullable ItemPredicate predicate) {
    super(sign);

    this.silent = silent;
    this.predicate = predicate;
  }

  @Override
  public boolean tick(int time) {
    if (time % 5 != 0)
      return true;

    return mountBlock.getState() instanceof Container;
  }
}
