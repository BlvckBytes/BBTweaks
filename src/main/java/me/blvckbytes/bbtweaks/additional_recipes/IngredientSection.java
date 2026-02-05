package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XMaterial;
import me.blvckbytes.bbtweaks.un_craft.ItemMaterialTagRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.RecipeChoice;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class IngredientSection extends ConfigSection {

  public List<ComponentMarkup> materials;
  public List<ComponentMarkup> tags;

  @CSIgnore
  public RecipeChoice _choice;

  public IngredientSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    var materials = new HashSet<Material>();

    if (this.materials != null) {
      for (var type : this.materials) {
        var materialString = type.asPlainString(null).toUpperCase().trim();
        var xMaterial = XMaterial.matchXMaterial(materialString);

        if (xMaterial.isEmpty())
          throw new MappingError("Could not correspond \"" + materialString + "\" to a valid XMaterial");

        materials.add(xMaterial.get().get());
      }
    }

    if (tags != null) {
      for (var tag : tags) {
        var tagString = tag.asPlainString(null).toUpperCase().trim();
        var materialTag = ItemMaterialTagRegistry.getByName(tagString);

        if (materialTag == null)
          throw new MappingError("Could not correspond \"" + tags + "\" to a valid material-tag");

        materials.addAll(materialTag.getValues());
      }
    }

    if (materials.isEmpty())
      throw new MappingError("Please define at least one material under \"materials\" or one tag under \"tags\"");

    _choice = new RecipeChoice.MaterialChoice(new ArrayList<>(materials));
  }
}
