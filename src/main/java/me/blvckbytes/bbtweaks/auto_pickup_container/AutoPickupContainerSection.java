package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.auto_pickup_container.command.AutoPickupContainerCommandSection;

public class AutoPickupContainerSection extends ConfigSection {

  public @CSAlways AutoPickupContainerCommandSection command;

  public ComponentMarkup loreToSetOnUpdate;
  public ComponentMarkup filterErrorNotification;
  public ComponentMarkup overviewNoContainers;
  public ComponentMarkup overviewAllContainersEmpty;
  public ComponentMarkup overviewScreen;

  public AutoPickupContainerSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
