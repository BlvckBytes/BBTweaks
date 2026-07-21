package me.blvckbytes.bbtweaks.util;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

public record DisplayInventoryParameters(
  @Nullable Component title,
  InventoryType inventoryType,
  int size
) {
  public Inventory makeInventory(Display<?> display) {
    if (inventoryType == InventoryType.CHEST) {
      if (title == null)
        return Bukkit.createInventory(display, size);

      return Bukkit.createInventory(display, size, title);
    }

    var usedInventoryType = inventoryType;

    // Sadly, a crafter-inventory with a non-null holder is currently completely
    // unusable on Paper, seeing how it does not show any items whatsoever.
    if (usedInventoryType == InventoryType.CRAFTER)
      usedInventoryType = InventoryType.DROPPER;

    if (title == null)
      return Bukkit.createInventory(display, usedInventoryType);

    return Bukkit.createInventory(display, usedInventoryType, title);
  }

  public static DisplayInventoryParameters fromSection(GuiSection<?> guiSection, InterpretationEnvironment environment) {
    var titleMarkup = guiSection.getTitle();

    return new DisplayInventoryParameters(
      titleMarkup == null ? null : titleMarkup.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst(),
      InventoryType.CHEST,
      guiSection.getRows() * 9
    );
  }
}
