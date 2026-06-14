package me.blvckbytes.bbtweaks.auto_pickup_container.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class AutoPickupContainerCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "autopickupcontainer";

  public ComponentMarkup playersOnly;
  public ComponentMarkup commandUsage;
  public ComponentMarkup functionalityNowEnabled;
  public ComponentMarkup functionalityAlreadyEnabled;
  public ComponentMarkup functionalityNowDisabled;
  public ComponentMarkup functionalityAlreadyDisabled;

  public AutoPickupContainerCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
