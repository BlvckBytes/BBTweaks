package me.blvckbytes.bbtweaks.mechanic.showcase;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ShowcaseDisplayData(
  @Nullable ShowcaseInstance instance,
  ItemStack frameItem
) {}
