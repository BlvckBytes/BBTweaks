package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class QuickUnloadInstance extends SISOInstance {

  public final EnumSet<QuickUnloadFlag> flags;
  public final @Nullable ItemPredicate predicate;

  public QuickUnloadInstance(Sign sign, EnumSet<QuickUnloadFlag> flags, @Nullable ItemPredicate predicate) {
    super(sign);

    this.flags = flags;
    this.predicate = predicate;
  }

  @Override
  public boolean tick(long time) {
    if (time % 5 != 0)
      return true;

    return mountBlock.getState() instanceof Container;
  }
}
