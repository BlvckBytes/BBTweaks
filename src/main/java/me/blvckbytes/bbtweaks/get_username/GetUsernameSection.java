package me.blvckbytes.bbtweaks.get_username;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class GetUsernameSection extends ConfigSection {

  public @CSAlways GetUsernameCommandSection command;

  public ComponentMarkup noPermission;
  public ComponentMarkup commandUsage;
  public ComponentMarkup malformedUuid;
  public ComponentMarkup unknownFloodgateId;
  public ComponentMarkup noMojangResult;
  public ComponentMarkup fetchErrorOccurred;
  public ComponentMarkup usernameResult;

  public GetUsernameSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
