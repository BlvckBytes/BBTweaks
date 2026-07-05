package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.mechanic.common.OffsetSelectingSection;

public class ShowcaseSection extends ConfigSection implements OffsetSelectingSection {

  public int maximumAxisOffset;
  public int offsetSelectingTimeoutSeconds;

  public ComponentMarkup noPermission;
  public ComponentMarkup malformedInventoryTitle;
  public ComponentMarkup malformedChatMessage;
  public ComponentMarkup cannotEditSign;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup axisOffsetLimitExceeded;
  public ComponentMarkup triedBindingToSign;
  public ComponentMarkup blockSelectionPrompt;
  public ComponentMarkup blockSelectionTimeout;
  public ComponentMarkup blockSelectionNoContainer;
  public ComponentMarkup blockSelectionSuccess;
  public ComponentMarkup defaultInventoryTitle;
  public ComponentMarkup cannotModifyInventory;

  public ShowcaseSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public int maximumAxisOffset() {
    return maximumAxisOffset;
  }

  @Override
  public int offsetSelectingTimeoutSeconds() {
    return offsetSelectingTimeoutSeconds;
  }

  @Override
  public ComponentMarkup axisOffsetLimitExceeded() {
    return axisOffsetLimitExceeded;
  }

  @Override
  public ComponentMarkup triedBindingToSign() {
    return triedBindingToSign;
  }

  @Override
  public ComponentMarkup blockSelectionPrompt() {
    return blockSelectionPrompt;
  }

  @Override
  public ComponentMarkup blockSelectionTimeout() {
    return blockSelectionTimeout;
  }

  @Override
  public ComponentMarkup blockSelectionSuccess() {
    return blockSelectionSuccess;
  }
}
