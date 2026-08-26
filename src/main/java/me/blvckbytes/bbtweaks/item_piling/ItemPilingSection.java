package me.blvckbytes.bbtweaks.item_piling;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.item_piling.command.ItemPilingCommandSection;
import me.blvckbytes.bbtweaks.item_piling.display.ItemPilingDisplaySection;
import org.bukkit.World;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemPilingSection extends ConfigSection {

  public int periodTicks;
  public int minimumAgeTicks;
  public int minimumAgeTicksDroppedByPlayer;

  public int defaultWorldBlockRadius;

  public Map<String, Integer> blockRadiusByWorldName = new HashMap<>();

  public Map<String, Integer> _blockRadiusByWorldNameLower = new HashMap<>();

  @CSAlways
  public ItemPilingCommandSection command;

  @CSAlways
  public ItemPilingDisplaySection display;

  public ComponentMarkup itemEntityName;

  public ItemPilingSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public int getBlockRadius(World world) {
    var blockRadius = _blockRadiusByWorldNameLower.get(world.getName().toLowerCase());
    return blockRadius == null ? defaultWorldBlockRadius : blockRadius;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (periodTicks <= 0)
      throw new MappingError("Property \"periodTicks\" cannot be less than or equal to zero");

    if (defaultWorldBlockRadius <= 0)
      throw new MappingError("Property \"defaultWorldBlockRadius\" cannot be less than or equal to zero");

    for (var radiusEntry : blockRadiusByWorldName.entrySet()) {
      var worldNameLower = radiusEntry.getKey().toLowerCase().trim();
      var radius = radiusEntry.getValue();

      if (radius <= 0)
        throw new MappingError("Radius \"" + worldNameLower + "\" of \"blockRadiusByWorldName\" cannot be less than or equal to zero");

      if (_blockRadiusByWorldNameLower.put(worldNameLower, radius) != null)
        throw new MappingError("Duplicate world-name \"" + worldNameLower + "\" at \"blockRadiusByWorldName\"");
    }

    if (minimumAgeTicks <= 0)
      throw new MappingError("Property \"minimumAgeTicks\" cannot be less than or equal to zero");

    if (minimumAgeTicksDroppedByPlayer <= 0)
      throw new MappingError("Property \"minimumAgeTicksDroppedByPlayer\" cannot be less than or equal to zero");
  }
}
