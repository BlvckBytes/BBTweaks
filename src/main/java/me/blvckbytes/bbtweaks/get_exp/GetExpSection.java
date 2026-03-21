package me.blvckbytes.bbtweaks.get_exp;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class GetExpSection extends ConfigSection {

  public @CSAlways GetExpCommandSection command;

  public int experiencePerBottle;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup commandUsage;
  public ComponentMarkup notLookingAtAFurnace;
  public ComponentMarkup invalidPercentageProvided;
  public ComponentMarkup noStoredExperience;
  public ComponentMarkup limitTooLow;
  public ComponentMarkup handedOutExperience;

  public GetExpSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (experiencePerBottle <= 0)
      throw new MappingError("Property \"experiencePerBottle\" cannot be less than or equal to zero");
  }
}
