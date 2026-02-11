package me.blvckbytes.bbtweaks.markers_menu.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.inventory.ItemStack;

public interface MarkerDisplayItem {

  ItemStack makeRepresentative(InterpretationEnvironment baseEnvironment, ConfigKeeper<MainSection> config);

}
