package me.blvckbytes.bbtweaks.pipes.search.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface SearchDisplayEntry extends Comparable<SearchDisplayEntry> {

  /**
   * @return null if the entry is no longer valid
   */
  @Nullable ItemStack makeRepresentative(InterpretationEnvironment baseEnvironment, ConfigKeeper<MainSection> config);

  void updateAmount();

}
