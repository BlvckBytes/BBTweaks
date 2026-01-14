package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ShapedRecipeSection extends ConfigSection {

  public ComponentMarkup result;

  @CSIgnore
  public Material _result;

  public ComponentExpression amount;

  @CSIgnore
  public int _amount;

  public List<ComponentMarkup> shape;
  public Map<String, IngredientSection> ingredients;

  public ShapedRecipeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (result == null)
      throw new MappingError("Absent \"result\"-property");

    var resultString = result.asPlainString(null);
    var xMaterial = XMaterial.matchXMaterial(resultString);

    if (xMaterial.isEmpty())
      throw new MappingError("Unknown type: \"" + resultString + "\"");

    this._result = xMaterial.get().get();

    this._amount = amount == null ? 1 : ComponentExpression.asInt(amount, null);
  }
}
