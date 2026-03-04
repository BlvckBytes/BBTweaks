package me.blvckbytes.bbtweaks.auto_fly;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class AutoFlySection extends ConfigSection {

  public @CSAlways AutoFlyCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noFlyPermission;
  public ComponentMarkup currentMode;
  public ComponentMarkup commandUsage;
  public ComponentMarkup newModeOff;
  public ComponentMarkup newModeEnabled;
  public ComponentMarkup newModeEnabledSetFlying;

  public AutoFlySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
