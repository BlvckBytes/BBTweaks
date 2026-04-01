package me.blvckbytes.bbtweaks.private_vaults;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.HashMap;
import java.util.Map;

public class PrivateVaultsSection extends ConfigSection {

  public Map<String, Integer> rowCounts = new HashMap<>();

  public ComponentMarkup inventoryTitle;

  public PrivateVaultsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
