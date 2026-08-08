package me.blvckbytes.bbtweaks.emotions.command;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class EmotionCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "emotion";

  public List<String> allSentinels = new ArrayList<>();
  public int paginationSize;

  public EmotionCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var index = 0; index < allSentinels.size(); ++index) {
      var allSentinel = allSentinels.get(index).strip();

      if (allSentinel.isEmpty())
        throw new MappingError("The encountered blank entry in \"allSentinels\" list");

      allSentinels.set(index, allSentinel);
    }

    if (allSentinels.isEmpty())
      throw new MappingError("The \"allSentinels\" list cannot be empty");

    if (paginationSize <= 0)
      throw new MappingError("The property \"paginationSize\" cannot be less than or equal to zero");
  }

  public String getMainAllSentinel() {
    return allSentinels.getFirst();
  }

  public boolean isAllSentinel(String input) {
    for (var sentinel : allSentinels) {
      if (StringUtils.equalsIgnoreCase(sentinel, input))
        return true;
    }

    return false;
  }

  public void addAllSentinelSuggestions(String input, List<String> output) {
    for (var sentinel : allSentinels) {
      if (StringUtils.startsWithIgnoreCase(sentinel, input))
        output.add(sentinel);
    }
  }
}
