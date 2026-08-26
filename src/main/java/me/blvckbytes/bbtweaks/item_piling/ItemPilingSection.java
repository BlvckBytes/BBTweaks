package me.blvckbytes.bbtweaks.item_piling;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.item_piling.command.ItemPilingCommandSection;
import me.blvckbytes.bbtweaks.item_piling.display.ItemPilingDisplaySection;

import java.lang.reflect.Field;
import java.util.List;

public class ItemPilingSection extends ConfigSection {

  public int periodTicks;
  public int blockRadius;
  public int minimumAgeTicks;
  public int minimumAgeTicksDroppedByPlayer;

  @CSAlways
  public ItemPilingCommandSection command;

  @CSAlways
  public ItemPilingDisplaySection display;

  public ComponentMarkup itemEntityName;

  public ItemPilingSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (periodTicks <= 0)
      throw new MappingError("Property \"periodTicks\" cannot be less than or equal to zero");

    if (blockRadius <= 0)
      throw new MappingError("Property \"blockRadius\" cannot be less than or equal to zero");

    if (minimumAgeTicks <= 0)
      throw new MappingError("Property \"minimumAgeTicks\" cannot be less than or equal to zero");

    if (minimumAgeTicksDroppedByPlayer <= 0)
      throw new MappingError("Property \"minimumAgeTicksDroppedByPlayer\" cannot be less than or equal to zero");
  }
}
