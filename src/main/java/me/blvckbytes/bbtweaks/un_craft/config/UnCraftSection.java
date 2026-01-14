package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class UnCraftSection extends ConfigSection {

  public ComponentMarkup inGameOnly;
  public ComponentMarkup missingPermission;
  public ComponentMarkup missingPermissionAllMode;
  public ComponentMarkup noItemInMainHand;
  public ComponentMarkup unsupportedItem;
  public ComponentMarkup choicesScreen;
  public ComponentMarkup invalidSelection;
  public ComponentMarkup unacceptedSubtraction;
  public ComponentMarkup notEnoughItems;
  public ComponentMarkup notEnoughItemsReduced;
  public ComponentMarkup droppedItems;
  public ComponentMarkup noMoreSpace;
  public ComponentMarkup notEnoughSpace;
  public ComponentMarkup resultOverview;
  public ComponentMarkup subtractionOverview;
  public ComponentMarkup successfulUnCraft;

  public @CSAlways AdditionalReasonsSection additionalReasons;

  public UnCraftSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
