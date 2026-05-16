package me.blvckbytes.bbtweaks.command_items;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandItemsSection extends ConfigSection {

  public long useCooldownMs;
  public List<CommandItemSection> items = new ArrayList<>();

  public @CSIgnore Map<String, CommandItemSection> commandItemByNameLower = new HashMap<>();

  public CommandItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var commandItem : items) {
      if (commandItemByNameLower.put(commandItem.itemName.toLowerCase(), commandItem) != null)
        throw new MappingError("Duplicate item-name: " + commandItem.itemName);
    }

    if (useCooldownMs <= 0)
      throw new MappingError("Property \"useCooldownMs\" cannot be less than or equal to zero");
  }
}
