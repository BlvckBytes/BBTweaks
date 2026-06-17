package me.blvckbytes.bbtweaks.list_chunk_tickets;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ListChunkTicketsSection extends ConfigSection {

  public @CSAlways ListChunkTicketsCommandSection command;

  public ComponentMarkup notAPlayer;
  public ComponentMarkup noPermission;
  public ComponentMarkup noActiveTickets;
  public ComponentMarkup ticketListScreen;

  public ListChunkTicketsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
