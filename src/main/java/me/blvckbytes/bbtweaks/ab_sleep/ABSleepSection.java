package me.blvckbytes.bbtweaks.ab_sleep;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ABSleepSection extends ConfigSection {

  public ComponentMarkup thresholdNotYetReached;
  public ComponentMarkup thresholdReached;

  public ABSleepSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
