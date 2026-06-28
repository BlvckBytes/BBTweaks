package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class CachedShapedRecipe implements CachedRecipe {

  private final ItemStack result;

  public final int width, height;
  public final boolean horizontallyAsymmetrical;

  private final RecipeChoice[][] choiceMatrix;

  public CachedShapedRecipe(
    ItemStack result,
    int width,
    int height,
    boolean horizontallyAsymmetrical,
    RecipeChoice[][] choiceMatrix
  ) {
    this.result = result;
    this.width = width;
    this.height = height;
    this.horizontallyAsymmetrical = horizontallyAsymmetrical;
    this.choiceMatrix = choiceMatrix;
  }

  public static @Nullable CachedShapedRecipe createIfValid(ShapedRecipe recipe) {
    var shapeLines = recipe.getShape();

    var height = shapeLines.length;

    if (height == 0 || height > 3)
      return null;

    var width = Arrays.stream(shapeLines).mapToInt(String::length).max().orElse(0);

    if (width == 0 || width > 3)
      return null;

    var choiceMap = recipe.getChoiceMap();
    var choiceMatrix = new RecipeChoice[height][width];

    for (var rowIndex = 0; rowIndex < shapeLines.length; ++rowIndex) {
      var columnChars = shapeLines[rowIndex].toCharArray();

      for (var columnIndex = 0; columnIndex < columnChars.length; ++columnIndex) {
        var columnChar = columnChars[columnIndex];

        if (Character.isWhitespace(columnChar))
          continue;

        var choice = choiceMap.get(columnChar);

        // There may be letters used in the shape that have a null-value assigned,
        // for whatever odd internal reason of how they map them from minecraft.
        if (choice == null || choice == RecipeChoice.empty())
          continue;

        choiceMatrix[rowIndex][columnIndex] = choice;
      }
    }

    var horizontallyAsymmetrical = false;

    if (width != 1) {
      for (var rowContents : choiceMatrix) {
        if (!Objects.equals(rowContents[0], rowContents[width - 1])) {
          horizontallyAsymmetrical = true;
          break;
        }
      }
    }

    return new CachedShapedRecipe(recipe.getResult(), width, height, horizontallyAsymmetrical, choiceMatrix);
  }

  @Override
  public ItemStack getResultCopy() {
    return new ItemStack(result);
  }

  public @Nullable RecipeChoice getChoiceAt(int row, int column) {
    if (row < 0 || column < 0 || row >= choiceMatrix.length)
      return null;

    var choiceRow = choiceMatrix[row];

    if (column >= choiceRow.length)
      return null;

    return choiceRow[column];
  }
}
