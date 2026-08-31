package me.blvckbytes.bbtweaks.no_ai;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class NoAiSection extends ConfigSection {

  @CSAlways
  public NoAiCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup notLookingAtAnEntity;
  public ComponentMarkup cannotBuildHere;
  public ComponentMarkup notLookingAtSupportedEntity;
  public ComponentMarkup aiNowDisabled;
  public ComponentMarkup aiNowEnabled;
  public ComponentMarkup commandActionUsage;
  public ComponentMarkup entityIsNowLookingAtExecutor;
  public ComponentMarkup entityIsNotSimulated;
  public ComponentMarkup villagerStatusMessage;

  public NoAiSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
