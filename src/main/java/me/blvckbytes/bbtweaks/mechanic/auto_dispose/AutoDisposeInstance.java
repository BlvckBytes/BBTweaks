package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

public class AutoDisposeInstance extends SISOInstance {

  private final ConfigKeeper<MainSection> config;
  private final @Nullable ItemPredicate predicate;

  public AutoDisposeInstance(
    Sign sign,
    @Nullable ItemPredicate predicate,
    ConfigKeeper<MainSection> config
  ) {
    super(sign);

    this.predicate = predicate;
    this.config = config;
  }

  @Override
  public boolean tick(int time) {
    if (time % config.rootSection.mechanic.autoDispose.clearIntervalTicks != 0)
      return true;

    var inputPower = tryReadInputPower();

    if (inputPower == null || inputPower > 0)
      return true;

    if (!(mountBlock.getState() instanceof Container container))
      return false;

    var inventory = container.getInventory();

    if (predicate == null) {
      inventory.clear();
      return true;
    }

    var size = inventory.getSize();

    for (var slotIndex = 0; slotIndex < size; ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir())
        continue;

      if (!predicate.test(currentItem))
        continue;

      inventory.setItem(slotIndex, null);
    }

    return true;
  }
}
