package me.blvckbytes.bbtweaks.mechanic.lever_array;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class LeverArraySection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup propagationSpeedEnableMalformedExpression;
  public ComponentMarkup propagationSpeedEnableNegative;
  public ComponentMarkup propagationSpeedDisableMalformedExpression;
  public ComponentMarkup propagationSpeedDisableNegative;
  public ComponentMarkup creationSuccess;

  public LeverArraySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
