package me.blvckbytes.bbtweaks.mechanic.common;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;

public interface OffsetSelectingSection {

  int maximumAxisOffset();
  int offsetSelectingTimeoutSeconds();
  ComponentMarkup axisOffsetLimitExceeded();
  ComponentMarkup triedBindingToSign();
  ComponentMarkup blockSelectionPrompt();
  ComponentMarkup blockSelectionTimeout();
  ComponentMarkup blockSelectionSuccess();

}
