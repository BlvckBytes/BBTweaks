package me.blvckbytes.bbtweaks.pipes.search;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.pipes.search.command.PipeSearchCommandSection;
import me.blvckbytes.bbtweaks.pipes.search.display.SearchDisplaySection;

public class PipeSearchSection extends ConfigSection {

  @CSAlways
  public PipeSearchCommandSection command;

  public ComponentMarkup getItemNoSpace;
  public ComponentMarkup getItemSuccess;
  public ComponentMarkup getItemContainerAbsent;
  public ComponentMarkup getItemContainerSizeChanged;
  public ComponentMarkup getItemMoved;
  public ComponentMarkup containerTeleportObstructed;
  public ComponentMarkup containerTeleported;
  public ComponentMarkup containerOpened;

  public ComponentMarkup searchNoResults;
  public ComponentMarkup searchNoContainers;
  public ComponentMarkup searchShowingResults;

  public ComponentMarkup predicateError;

  @CSAlways
  public SearchDisplaySection display;

  public PipeSearchSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
