package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class HomeCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "home";

  public ComponentMarkup homeList;
  public ComponentMarkup commandUsage;
  public ComponentMarkup homeDoesNotExist;
  public ComponentMarkup renameUsage;
  public ComponentMarkup newHomeNameAlreadyExists;
  public ComponentMarkup homeRenamed;
  public ComponentMarkup notMarkedAsFavorite;
  public ComponentMarkup removedFavoriteNumber;
  public ComponentMarkup markFavoriteUsage;
  public ComponentMarkup malformedFavoriteNumber;
  public ComponentMarkup setFavoriteNumber;
  public ComponentMarkup setIconUsage;
  public ComponentMarkup setIconMaterial;
  public ComponentMarkup noIconSet;
  public ComponentMarkup iconRemoved;

  public HomeCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
