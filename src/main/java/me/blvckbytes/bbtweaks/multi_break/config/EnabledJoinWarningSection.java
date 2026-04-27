package me.blvckbytes.bbtweaks.multi_break.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class EnabledJoinWarningSection extends ConfigSection {

  public boolean enabled;
  public ComponentMarkup title;
  public ComponentMarkup subtitle;
  public int durationMillis;

  public EnabledJoinWarningSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
