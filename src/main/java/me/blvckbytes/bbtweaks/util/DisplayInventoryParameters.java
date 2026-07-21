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

    if (title == null)
      return Bukkit.createInventory(display, inventoryType);

    return Bukkit.createInventory(display, inventoryType, title);
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
