package me.blvckbytes.bbtweaks.pipes;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.pipes.notification.PipeNotificationsSection;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicatesSection;
import me.blvckbytes.bbtweaks.pipes.search.PipeSearchSection;

import java.lang.reflect.Field;
import java.util.List;

public class PipesSection extends ConfigSection {

  @CSAlways
  public PipeTimingsCommandSection pipeTimingsCommand;

  public ComponentMarkup signCreateNoPermission;
  public ComponentMarkup signCreateNoPistonFound;
  public ComponentMarkup unsupportedSignType;
  public ComponentMarkup signCreated;

  public ComponentMarkup wirelessSignCreateNoPermission;
  public ComponentMarkup wirelessSignMalformed;
  public ComponentMarkup wirelessSignNotOnGlassBlock;
  public ComponentMarkup wirelessSignCreated;
  public ComponentMarkup wirelessSignInformation;
  public ComponentMarkup wirelessSignMissingTeleportPermission;
  public ComponentMarkup wirelessSignTeleported;

  public ComponentMarkup enumerationExceededRetries;
  public ComponentMarkup enumerationNotAPipeBlock;
  public ComponentMarkup enumerationCannotBuildThere;
  public ComponentMarkup enumerationAlreadyInASession;

  @CSAlways
  public PipeNotificationsSection notifications;

  public boolean requireSign;
  public boolean dropNoSign;
  public int maxCacheLoadCount;
  public int chunkTicketDurationTicks;

  @CSAlways
  public PipePredicatesSection predicates;

  @CSAlways
  public PipeSearchSection search;

  public PipesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (maxCacheLoadCount <= 0)
      throw new MappingError("Property \"maxCacheLoadCount\" cannot be less than or equal to zero");

    if (chunkTicketDurationTicks <= 0)
      throw new MappingError("Property \"chunkTicketDurationTicks\" cannot be less than or equal to zero");
  }
}
