package me.blvckbytes.bbtweaks.world_players;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class WorldPlayersCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "worldplayers";

  public ComponentMarkup noPlayersOnline;
  public ComponentMarkup playerCountsOverview;

  public WorldPlayersCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}