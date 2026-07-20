package me.blvckbytes.bbtweaks.mechanic.pipe_fetch;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PipeFetchSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup notOnAPiston;
  public ComponentMarkup creationSuccess;

  public PipeFetchSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
