package me.blvckbytes.bbtweaks.command_items;

import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class CommandItemSection extends ConfigSection {

  public String itemName;
  public @Nullable String itemType;
  public @CSIgnore @Nullable Material _itemType;

  public ComponentMarkup command;

  public CommandItemSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    itemName = itemName.trim();

    if (itemType != null && !itemType.isBlank()) {
      var matchedMaterial = MaterialMatcher.tryMatch(itemType.trim());

      if (matchedMaterial == null)
        throw new MappingError("Invalid \"itemType\" Material-value: " + itemType);

      _itemType = matchedMaterial;
    }
  }
}
