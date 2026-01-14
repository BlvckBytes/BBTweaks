package me.blvckbytes.bbtweaks.furnace_level_display;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class FurnaceLevelSection extends ConfigSection {

  public ComponentMarkup noLevelsStored;
  public ComponentMarkup levelsStored;

  public FurnaceLevelSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
