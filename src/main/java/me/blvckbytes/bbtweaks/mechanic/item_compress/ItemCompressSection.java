package me.blvckbytes.bbtweaks.mechanic.item_compress;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ItemCompressSection extends ConfigSection {

  public int maxCompressedAmount;

  public ComponentMarkup noPermission;
  public ComponentMarkup noChest;
  public ComponentMarkup existingSign;
  public ComponentMarkup creationSuccess;

  public ComponentMarkup compressedStackLore;

  public ItemCompressSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
