package me.blvckbytes.bbtweaks.integration.arm;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ArmIntegrationSection extends ConfigSection {

  public ArmRegionSection shopRegion;
  public ArmRegionSection creativeRegion;

  public ArmIntegrationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
