package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SingleBottleXpCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "singlebottlexp";

  public ComponentMarkup commandUsage;
  public ComponentMarkup experienceOverview;
  public ComponentMarkup cannotHoldBottle;
  public ComponentMarkup bottleLore;
  public ComponentMarkup afterBottling;

  public SingleBottleXpCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
