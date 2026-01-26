package me.blvckbytes.bbtweaks.back;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class BackOverrideSection extends ConfigSection {

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup noLastLocation;
  public ComponentMarkup teleportedBack;

  public BackOverrideSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
