package me.blvckbytes.bbtweaks.mechanic.planter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PlanterSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup missingRadius;
  public ComponentMarkup nonPositiveRadius;
  public ComponentMarkup creationSuccess;

  public PlanterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
