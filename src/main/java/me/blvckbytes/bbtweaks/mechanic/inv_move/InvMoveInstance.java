package me.blvckbytes.bbtweaks.mechanic.inv_move;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class InvMoveInstance extends SISOInstance {

  public final EnumSet<InvMoveFlag> flags;
  public final @Nullable ItemPredicate predicate;

  public InvMoveInstance(Sign sign, EnumSet<InvMoveFlag> flags, @Nullable ItemPredicate predicate) {
    super(sign);

    this.flags = flags;
    this.predicate = predicate;
  }

  @Override
  public boolean tick(int time) {
    if (time % 5 != 0)
      return true;

    return mountBlock.getState() instanceof Container;
  }
}
