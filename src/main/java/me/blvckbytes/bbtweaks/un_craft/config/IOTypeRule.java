package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.un_craft.MaterialType;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.List;

public class IOTypeRule extends TypeRule {

  public ComponentExpression onUnCraftedItem;

  @CSIgnore
  public boolean _onUnCraftedItem;

  public ComponentExpression onUnCraftResult;

  @CSIgnore
  public boolean _onUnCraftResult;

  public IOTypeRule(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public boolean matches(Material material, MaterialType materialType) {
    if (materialType == MaterialType.UNCRAFTED_ITEM && !_onUnCraftedItem)
      return false;

    if (materialType == MaterialType.UNCRAFT_RESULT && !_onUnCraftResult)
      return false;

    return matches(material);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _onUnCraftResult = onUnCraftResult != null && baseEnvironment.getValueInterpreter().asBoolean(onUnCraftResult.interpret(null));
    _onUnCraftedItem = onUnCraftedItem != null && baseEnvironment.getValueInterpreter().asBoolean(onUnCraftedItem.interpret(null));
  }
}
