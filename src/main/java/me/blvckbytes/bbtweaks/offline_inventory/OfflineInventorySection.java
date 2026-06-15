package me.blvckbytes.bbtweaks.offline_inventory;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class OfflineInventorySection extends ConfigSection {

  public @CSAlways OfflineInventoryCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup commandUsage;
  public ComponentMarkup invalidUsername;
  public ComponentMarkup hasNotPlayedBefore;
  public ComponentMarkup nbtApiNotAvailable;
  public ComponentMarkup failedToLoadFile;
  public ComponentMarkup openingInventoryView;
  public ComponentMarkup cannotModifyView;
  public ComponentMarkup playerInventoryTitle;
  public ComponentMarkup playerEnderChestTitle;

  public ItemStackSection airPlaceholderItem;

  public OfflineInventorySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
