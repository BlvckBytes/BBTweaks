package me.blvckbytes.bbtweaks.custom_commands;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CustomCommandsSection extends ConfigSection {

  public List<CustomCommandSection> commands = new ArrayList<>();

  public CustomCommandsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    var seenNamesAndAliases = new HashSet<>();

    for (var command : commands) {
      if (!(seenNamesAndAliases.add(command.evaluatedName)))
        throw new MappingError("Command-name \"" + command.evaluatedName + "\" is already used by another command or alias");

      for (var alias : command.evaluatedAliases) {
        if (!(seenNamesAndAliases.add(alias)))
          throw new MappingError("Command-alias \"" + alias + "\" is already used by another command or alias");
      }
    }
  }
}
