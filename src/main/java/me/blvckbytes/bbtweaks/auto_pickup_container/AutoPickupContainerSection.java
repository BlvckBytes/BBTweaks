package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XSound;
import me.blvckbytes.bbtweaks.auto_pickup_container.command.AutoPickupContainerCommandSection;

import java.lang.reflect.Field;
import java.util.List;

public class AutoPickupContainerSection extends ConfigSection {

  public @CSAlways AutoPickupContainerCommandSection command;

  public ComponentMarkup loreToSetOnUpdate;
  public ComponentMarkup filterErrorNotification;
  public ComponentMarkup overviewNoContainers;
  public ComponentMarkup overviewAllContainersEmpty;
  public ComponentMarkup overviewScreen;

  public ComponentMarkup capacityWarningTitle;
  public ComponentMarkup capacityWarningSubtitle;
  public ComponentMarkup capacityWarningChat;
  public String capacityWarningSound;
  public @CSIgnore XSound _capacityWarningSound;
  public int capacityWarningTitleStayMs;

  public AutoPickupContainerSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _capacityWarningSound = XSound.of(capacityWarningSound).orElseThrow(
      () -> new MappingError("Could not correspond property \"capacityWarningSound\"-value of \"" + capacityWarningSound + "\" to a valid XSound")
    );

    if (capacityWarningTitleStayMs <= 0)
      throw new MappingError("Property \"capacityWarningTitleStayMs\" cannot be less than or equal to zero");
  }
}
