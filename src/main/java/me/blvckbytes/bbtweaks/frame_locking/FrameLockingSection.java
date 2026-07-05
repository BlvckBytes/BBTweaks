package me.blvckbytes.bbtweaks.frame_locking;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class FrameLockingSection extends ConfigSection {

  public ComponentMarkup nowLocked;
  public ComponentMarkup nowUnlocked;

  public FrameLockingSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
