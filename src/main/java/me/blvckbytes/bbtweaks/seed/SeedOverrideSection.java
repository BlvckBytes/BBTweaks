package me.blvckbytes.bbtweaks.seed;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeedOverrideSection extends ConfigSection {

  public ComponentMarkup playersOnly;

  public Map<String, ComponentMarkup> worldSpecificMessages;

  @CSIgnore
  public Map<String, ComponentMarkup> _worldSpecificMessageByNameLower = new HashMap<>();

  public ComponentMarkup fallbackMessage;

  public SeedOverrideSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (worldSpecificMessages != null) {
      for (var entry : worldSpecificMessages.entrySet())
        _worldSpecificMessageByNameLower.put(entry.getKey().toLowerCase(), entry.getValue());
    }
  }
}
