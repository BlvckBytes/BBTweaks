package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.inventory.RecipeChoice;

import java.util.function.Predicate;

public interface MatrixContent extends Predicate<RecipeChoice.MaterialChoice> {

  boolean isPresent();

}
