package me.blvckbytes.bbtweaks.back;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

public class BackOverrideSection extends ConfigSection {

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup noLastLocation;
  public ComponentMarkup teleportedBack;
  public @CSAlways BacktrackCommandSection backtrackCommand;
  public ComponentMarkup backtrackAlreadyInASession;
  public ComponentMarkup backtrackCancelledDueToExternalTeleport;
  public @Nullable ComponentMarkup backtrackTitle;
  public @Nullable ComponentMarkup backtrackSubtitle;
  public @Nullable ComponentMarkup backtrackActionBar;

  public BackOverrideSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
