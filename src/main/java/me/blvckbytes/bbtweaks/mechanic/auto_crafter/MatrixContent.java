package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MatrixContent {

  private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

  private final @Nullable ItemStack itemStack;
  private final @Nullable RecipeChoice.MaterialChoice materialChoice;

  public MatrixContent(@Nullable ItemStack itemStack) {
    this.itemStack = itemStack;
    this.materialChoice = null;
  }

  public MatrixContent(@Nullable RecipeChoice.MaterialChoice materialChoice) {
    this.itemStack = null;
    this.materialChoice = materialChoice;
  }

  public boolean isValid() {
    if (this.materialChoice != null)
      return true;

    return ItemUtil.isStackValid(itemStack);
  }

  public boolean test(RecipeChoice.MaterialChoice recipeChoice) {
    if (materialChoice != null)
      return doMaterialsIntersect(materialChoice.getChoices(), recipeChoice.getChoices());

    return recipeChoice.test(itemStack == null ? AIR_STACK : itemStack);
  }

  private boolean doMaterialsIntersect(List<Material> listA, List<Material> listB) {
    if (listA.isEmpty() || listB.isEmpty())
      return false;

    for (var materialA : listA) {
      if (listB.contains(materialA))
        return true;
    }

    for (var materialB : listB) {
      if (listA.contains(materialB))
        return true;
    }

    return false;
  }
}
