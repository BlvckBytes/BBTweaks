package me.blvckbytes.bbtweaks.private_vaults;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PrivateVaultsSection extends ConfigSection {

  public ComponentMarkup inventoryTitle;

  public ComponentMarkup playersOnly;
  public ComponentMarkup handedOutExcessStacks;
  public ComponentMarkup vaultResizedToZero;
  public ComponentMarkup viewCommandNoPermission;
  public ComponentMarkup viewCommandUsage;
  public ComponentMarkup vaultNotExistingAndOwnerNotOnline;
  public ComponentMarkup vaultHasNoActiveRowsSelf;
  public ComponentMarkup vaultHasNoActiveRowsOther;

  public PrivateVaultsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
