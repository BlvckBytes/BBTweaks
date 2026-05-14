package me.blvckbytes.bbtweaks.mechanic.pipe_request;

import at.blvckbytes.component_markup.util.TriState;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PipeRequestInstance extends SISOInstance {

  private final @Nullable ItemPredicate predicate;
  private final int maxStackCount;
  private final EnumSet<PipeRequestFlag> flags;

  private TriState lastPowerState;

  public PipeRequestInstance(
    Sign sign,
    @Nullable ItemPredicate predicate,
    int maxStackCount,
    EnumSet<PipeRequestFlag> flags
  ) {
    super(sign);

    this.predicate = predicate;
    this.maxStackCount = maxStackCount;
    this.flags = flags;
  }

  public void request(@Nullable Player initiator) {
    Inventory targetInventory;

    if (mountBlock.getState() instanceof Container container)
      targetInventory = container.getInventory();

    else {
      // We cannot act based on redstone-signals if there's no container attached; requiring an initiator.
      if (initiator == null)
        return;

      targetInventory = initiator.getInventory();
    }
  }

  @Override
  public boolean tick(int time) {
    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    var currentPowerState = inputPower > 0 ? TriState.TRUE : TriState.FALSE;

    if (lastPowerState == TriState.FALSE && currentPowerState == TriState.TRUE)
      request(null);

    lastPowerState = currentPowerState;

    return true;
  }
}
