package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class HiddenSwitchSection extends ConfigSection {

  public int onTimeDurationTicks;
  public int maximumAxisOffset;
  public int offsetSelectingTimeoutSeconds;
  public ComponentMarkup noPermission;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup cannotEditSign;
  public ComponentMarkup anotherIsEditing;
  public ComponentMarkup keysInventoryOpening;
  public ComponentMarkup keysInventoryClosing;
  public ComponentMarkup destroyedWhileInInventory;
  public ComponentMarkup blockSelectionPrompt;
  public ComponentMarkup blockSelectionTimeout;
  public ComponentMarkup blockSelectionSuccess;
  public ComponentMarkup malformedGrantedMessage;
  public ComponentMarkup malformedDeniedMessage;
  public ComponentMarkup axisOffsetLimitExceeded;
  public ComponentMarkup triedBindingToSign;
  public ComponentMarkup keysInventoryTitle;
  public ComponentMarkup defaultGrantedMessage;
  public ComponentMarkup defaultDeniedMessage;

  public HiddenSwitchSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (onTimeDurationTicks <= 0)
      throw new MappingError("\"onTimeDurationTicks\" cannot be lass then or equal to zero");

    if (maximumAxisOffset <= 0)
      throw new MappingError("\"maximumAxisOffset\" cannot be lass then or equal to zero");

    if (offsetSelectingTimeoutSeconds <= 0)
      throw new MappingError("\"offsetSelectingTimeoutSeconds\" cannot be lass then or equal to zero");
  }
}
