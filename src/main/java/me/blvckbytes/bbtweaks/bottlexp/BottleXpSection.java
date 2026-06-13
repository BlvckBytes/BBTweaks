package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class BottleXpSection extends ConfigSection {

  public @CSAlways BottleXpCommandSection command;

  public int experiencePerBottle;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup experienceOverview;
  public ComponentMarkup invalidMaximumValue;
  public ComponentMarkup maximumPercentageTooHigh;
  public ComponentMarkup maximumValueExceedsAvailable;
  public ComponentMarkup maximumValueBelowExpPerBottle;
  public ComponentMarkup hasNoExperienceToBottle;
  public ComponentMarkup cannotHoldAnyBottles;
  public ComponentMarkup afterBottling;

  public BottleXpSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (experiencePerBottle <= 0)
      throw new MappingError("The property \"experiencePerBottle\" cannot be less than or equal to zero");
  }
}
