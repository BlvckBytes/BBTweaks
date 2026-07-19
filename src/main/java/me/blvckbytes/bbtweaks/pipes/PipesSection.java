package me.blvckbytes.bbtweaks.pipes;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.pipes.notification.PipeNotificationsSection;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicatesSection;

public class PipesSection extends ConfigSection {

  @CSAlways
  public PipeTimingsCommandSection pipeTimingsCommand;

  public ComponentMarkup signCreateNoPermission;
  public ComponentMarkup signCreateNoPistonFound;
  public ComponentMarkup unsupportedSignType;
  public ComponentMarkup signCreated;

  @CSAlways
  public PipeNotificationsSection notifications;

  public boolean requireSign;
  public boolean dropNoSign;
  public int maxCacheLoadCount;

  @CSAlways
  public PipePredicatesSection predicates;

  public PipesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
