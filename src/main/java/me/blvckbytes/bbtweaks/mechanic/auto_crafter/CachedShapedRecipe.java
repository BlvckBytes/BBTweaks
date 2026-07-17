package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CachedShapedRecipe implements CachedRecipe {

  private final ItemStack result;

  public final int width, height;
  public final boolean horizontallyAsymmetrical;

  private final RecipeChoice.MaterialChoice[][] choiceMatrix;
  private final List<RecipeChoice.MaterialChoice> allChoices;

  public CachedShapedRecipe(
    ItemStack result,
    int width,
    int height,
    boolean horizontallyAsymmetrical,
    RecipeChoice.MaterialChoice[][] choiceMatrix,
    List<RecipeChoice.MaterialChoice> allChoices
  ) {
    this.result = result;
    this.width = width;
    this.height = height;
    this.horizontallyAsymmetrical = horizontallyAsymmetrical;
    this.choiceMatrix = choiceMatrix;
    this.allChoices = allChoices;
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
    var choiceMatrix = new RecipeChoice.MaterialChoice[height][width];
    var allChoices = new ArrayList<RecipeChoice.MaterialChoice>();

    for (var rowIndex = 0; rowIndex < shapeLines.length; ++rowIndex) {
      var columnChars = shapeLines[rowIndex].toCharArray();

      for (var columnIndex = 0; columnIndex < columnChars.length; ++columnIndex) {
        var columnChar = columnChars[columnIndex];

        if (Character.isWhitespace(columnChar))
          continue;

        var choice = choiceMap.get(columnChar);

        // There may be letters used in the shape that have a null-value assigned,
        // for whatever odd internal reason of how they map them from minecraft.
        if (choice == null || RecipeChoice.empty().equals(choice))
          continue;

        if (!(choice instanceof RecipeChoice.MaterialChoice materialChoice))
          return null;

        choiceMatrix[rowIndex][columnIndex] = materialChoice;
        allChoices.add(materialChoice);
      }
    }

    if (allChoices.isEmpty())
      return null;

    var horizontallyAsymmetrical = false;

    if (width != 1) {
      for (var rowContents : choiceMatrix) {
        if (!Objects.equals(rowContents[0], rowContents[width - 1])) {
          horizontallyAsymmetrical = true;
          break;
        }
      }
    }

    return new CachedShapedRecipe(
      recipe.getResult(),
      width,
      height,
      horizontallyAsymmetrical,
      choiceMatrix,
      Collections.unmodifiableList(allChoices)
    );
  }

  @Override
  public ItemStack getResultCopy() {
    return new ItemStack(result);
  }

  @Override
  public boolean areMatrixContentsSatisfyingRecipe(MatrixContent[] matrixContents) {
    for (int rowOffset = 0; rowOffset <= 3 - height; ++rowOffset) {
      for (int columnOffset = 0; columnOffset <= 3 - width; ++columnOffset) {
        if (doesRecipeMatchAtOffset(rowOffset, columnOffset, false, matrixContents))
          return true;

        if (horizontallyAsymmetrical) {
          if (doesRecipeMatchAtOffset(rowOffset, columnOffset, true, matrixContents))
            return true;
        }
      }
    }

    return false;
  }

  @Override
  public List<RecipeChoice.MaterialChoice> getChoicesForAllSlots() {
    return allChoices;
  }

  private boolean doesRecipeMatchAtOffset(
    int rowOffset, int columnOffset, boolean mirrorHorizontally,
    MatrixContent[] matrixContents
  ) {
    for (int rowIndex = 0; rowIndex < 3; ++rowIndex) {
      for (int columnIndex = 0; columnIndex < 3; ++columnIndex) {
        var matrixContent = matrixContents[columnIndex + rowIndex * 3];

        // Recipes are always trimmed and aligned to the top left corner, i.e. (0, 0). If we now seek
        // to slide the window of the choices-matrix, all slots prior to the offset need to be vacant.
        if (rowIndex < rowOffset || columnIndex < columnOffset) {
          if (matrixContent.isPresent())
            return false;

          continue;
        }

        var targetColumn = columnIndex - columnOffset;

        if (mirrorHorizontally)
          targetColumn = (width - 1) - targetColumn;

        var choice = getChoiceAt(rowIndex - rowOffset, targetColumn);

        // The shaped recipe has a hole at this location, meaning we expect a vacant slot.
        if (choice == null) {
          if (matrixContent.isPresent())
            return false;

          continue;
        }

        if (!matrixContent.test(choice))
          return false;
      }
    }

    return true;
  }

  private @Nullable RecipeChoice.MaterialChoice getChoiceAt(int row, int column) {
    if (row < 0 || column < 0 || row >= choiceMatrix.length)
      return null;

    var choiceRow = choiceMatrix[row];

    if (column >= choiceRow.length)
      return null;

    return choiceRow[column];
  }
}
