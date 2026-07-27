package me.blvckbytes.bbtweaks.entity_eggs;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class EntityEggsSection extends ConfigSection {

  public int rayTraceDistance;

  public ComponentMarkup cannotBuildAtEntity;
  public ComponentMarkup couldNotDetermineEggType;
  public ComponentMarkup noSpaceInInventory;
  public ComponentMarkup captureSuccess;
  public ComponentMarkup captureSuccessLog;
  public ComponentMarkup detailLineRenderer;
  public ComponentMarkup spawnEggLore;

  public EntityEggsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (rayTraceDistance <= 0)
      throw new MappingError("The property \"rayTraceDistance\" must be greater than zero");
  }
}
