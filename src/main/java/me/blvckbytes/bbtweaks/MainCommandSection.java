package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MainCommandSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup commandUsage;
  public ComponentMarkup configReloadSuccess;
  public ComponentMarkup configReloadError;
  public ComponentMarkup setRdBreakerNoValidItem;
  public ComponentMarkup setRdBreakerMetadata;
  public ComponentMarkup setRdBreakerPlayersOnly;

  public MainCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
