package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class AutoPickupContainerSection extends ConfigSection {

  public ComponentMarkup loreToSetOnUpdate;
  public ComponentMarkup filterErrorNotification;

  public AutoPickupContainerSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
