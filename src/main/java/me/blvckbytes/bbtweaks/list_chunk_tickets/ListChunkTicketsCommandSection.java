package me.blvckbytes.bbtweaks.list_chunk_tickets;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ListChunkTicketsCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "listchunktickets";

  public ListChunkTicketsCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
