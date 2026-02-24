package me.blvckbytes.bbtweaks.mechanic.hopper;

import org.bukkit.inventory.ItemStack;

public interface ItemCompatibilities {

  boolean isBrewingIngredient(ItemStack item);

  boolean isBrewingFuel(ItemStack item);

  boolean isPotion(ItemStack item);

  boolean isFurnaceIngredient(FurnaceType furnaceType, ItemStack item);

  boolean isFurnaceFuel(FurnaceType furnaceType, ItemStack item);

}
