package me.blvckbytes.bbtweaks.mechanic.teleporter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class TeleporterSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup malformedCoordinates;
  public ComponentMarkup unknownFlag;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup teleported;

  public TeleporterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
