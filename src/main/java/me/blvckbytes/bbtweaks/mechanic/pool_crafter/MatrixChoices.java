package me.blvckbytes.bbtweaks.mechanic.pool_crafter;

import me.blvckbytes.bbtweaks.mechanic.auto_crafter.MatrixContent;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class MatrixChoices implements MatrixContent {

  private static final String ITEM_NAME_MODE_ANY = "any";
  private static final String ITEM_NAME_MODE_SIMILAR = "similar";
  private static final String ITEM_NAME_MODE_EXACT = "exact";

  public final @Nullable Material matrixMaterial;
  private final @Nullable List<Material> choices;
  private final boolean wildcardMode;
  public final boolean exact;

  private MatrixChoices(
    @Nullable Material matrixMaterial,
    @Nullable List<Material> choices,
    boolean wildcardMode,
    boolean exact
  ) {
    this.matrixMaterial = matrixMaterial;
    this.choices = choices;
    this.wildcardMode = wildcardMode;
    this.exact = exact;
  }

  public static MatrixChoices[] map(ItemStack[] matrixContents, SimilarMaterialsResolver similarMaterialsResolver) {
    var mappedContents = new MatrixChoices[matrixContents.length];

    for (var index = 0; index < mappedContents.length; ++index) {
      var matrixItem = matrixContents[index];

      if (!ItemUtil.isStackValid(matrixItem)) {
        mappedContents[index] = new MatrixChoices(null, null, false, false);
        continue;
      }

      var itemMaterial = matrixItem.getType();
      var itemMeta = matrixItem.getItemMeta();
      var exactMode = false;

      if (itemMeta != null && itemMeta.hasDisplayName()) {
        var nameText = ComponentUtil.asTrimmedText(itemMeta.displayName());

        if (ITEM_NAME_MODE_ANY.equalsIgnoreCase(nameText)) {
          mappedContents[index] = new MatrixChoices(itemMaterial, null, true, false);
          continue;
        }

        if (ITEM_NAME_MODE_SIMILAR.equalsIgnoreCase(nameText)) {
          mappedContents[index] = new MatrixChoices(
            itemMaterial,
            similarMaterialsResolver.resolveSimilarMaterials(itemMaterial),
            false,
            false
          );

          continue;
        }

        if (ITEM_NAME_MODE_EXACT.equalsIgnoreCase(nameText))
          exactMode = true;
      }

      mappedContents[index] = new MatrixChoices(itemMaterial, Collections.singletonList(itemMaterial), false, exactMode);
    }

    return mappedContents;
  }

  @Override
  public boolean isPresent() {
    return choices != null || wildcardMode;
  }

  @Override
  public boolean test(RecipeChoice.MaterialChoice materialChoice) {
    if (wildcardMode)
      return true;

    if (this.choices == null)
      return false;

    return doMaterialsIntersect(choices, materialChoice.getChoices());
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
