package me.blvckbytes.bbtweaks.mechanic.wet_sponge;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class WetSpongeSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup noUsePermission;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup notHoldingSponges;
  public ComponentMarkup heldSpongesAlreadyWet;
  public ComponentMarkup heldSpongesHaveBeenWetted;
  public ComponentMarkup noSpongesInInventory;
  public ComponentMarkup allSpongesInInventoryAlreadyWet;
  public ComponentMarkup allSpongesInInventoryHaveBeenWetted;

  public WetSpongeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
