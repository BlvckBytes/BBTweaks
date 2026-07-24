package me.blvckbytes.bbtweaks.pipes.search;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.pipes.search.display.SearchDisplayEntry;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemStackEntry implements SearchDisplayEntry {

  public final ItemAndSlot itemAndSlot;

  public ItemStackEntry(ItemAndSlot itemAndSlot) {
    this.itemAndSlot = itemAndSlot;
  }

  @Override
  public @Nullable ItemStack makeRepresentative(InterpretationEnvironment baseEnvironment, ConfigKeeper<MainSection> config) {
    if (!ItemUtil.isStackValid(itemAndSlot.item()))
      return null;

    var representativeItem = new ItemStack(itemAndSlot.item());

    config.rootSection.pipes.search.display.items.stackRepresentativePatch.patch(
      representativeItem,
      baseEnvironment.copy()
        .withVariable("container_x", itemAndSlot.block().getX())
        .withVariable("container_y", itemAndSlot.block().getY())
        .withVariable("container_z", itemAndSlot.block().getZ())
        .withVariable("slot", itemAndSlot.slot() + 1)
    );

    return representativeItem;
  }
}
