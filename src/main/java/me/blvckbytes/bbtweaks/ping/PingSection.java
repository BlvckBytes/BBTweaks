package me.blvckbytes.bbtweaks.ping;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PingSection extends ConfigSection {

  public ComponentMarkup noOtherPermission;
  public ComponentMarkup noTargetConsoleSender;
  public ComponentMarkup targetNotOnline;
  public ComponentMarkup pingSelf;
  public ComponentMarkup pingOther;

  public PingSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
