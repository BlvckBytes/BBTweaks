package me.blvckbytes.bbtweaks.pipes;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.pipes.notification.PipeNotificationsSection;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicatesSection;
import me.blvckbytes.bbtweaks.pipes.search.PipeSearchSection;

public class PipesSection extends ConfigSection {

  @CSAlways
  public PipeTimingsCommandSection pipeTimingsCommand;

  public ComponentMarkup signCreateNoPermission;
  public ComponentMarkup signCreateNoPistonFound;
  public ComponentMarkup unsupportedSignType;
  public ComponentMarkup signCreated;

  public ComponentMarkup enumerationExceededRetries;
  public ComponentMarkup enumerationNotAPipeBlock;
  public ComponentMarkup enumerationCannotBuildThere;
  public ComponentMarkup enumerationAlreadyInASession;

  @CSAlways
  public PipeNotificationsSection notifications;

  public boolean requireSign;
  public boolean dropNoSign;
  public int maxCacheLoadCount;

  @CSAlways
  public PipePredicatesSection predicates;

  @CSAlways
  public PipeSearchSection search;

  public PipesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
