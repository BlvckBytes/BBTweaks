package me.blvckbytes.bbtweaks.mechanic.sign_flipper;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SignFlipperSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup noAdjacentSign;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup currentState;

  public SignFlipperSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
