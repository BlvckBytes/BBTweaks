package me.blvckbytes.bbtweaks.teleporter_sign;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class TeleporterSignSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup malformedCoordinates;
  public ComponentMarkup unknownFlag;
  public ComponentMarkup teleporterCreated;
  public ComponentMarkup teleported;

  public TeleporterSignSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
