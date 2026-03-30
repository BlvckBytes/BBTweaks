package me.blvckbytes.bbtweaks.newbie_announce;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class NewbieAnnounceSection extends ConfigSection {

  public boolean enableInGame;
  public ComponentMarkup inGameMessage;

  public boolean enableDiscord;
  public ComponentMarkup discordMessage;

  public NewbieAnnounceSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
