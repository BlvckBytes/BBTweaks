package me.blvckbytes.bbtweaks.mechanic.pipe_request;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PipeRequestSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup maxStackCountMalformedExpression;
  public ComponentMarkup maxStackCountNegativeOrZero;
  public ComponentMarkup unknownFlag;
  public ComponentMarkup handFlagOnlyWithShulkerFlag;
  public ComponentMarkup handFlagIncompatibleWithContainer;
  public ComponentMarkup creationSuccess;

  public PipeRequestSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
