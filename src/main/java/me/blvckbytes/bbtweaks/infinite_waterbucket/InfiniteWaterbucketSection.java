package me.blvckbytes.bbtweaks.infinite_waterbucket;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class InfiniteWaterbucketSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup name;
  public ComponentMarkup lore;
  public boolean glint;

  public InfiniteWaterbucketSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
