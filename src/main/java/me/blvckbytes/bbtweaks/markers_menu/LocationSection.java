package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Field;
import java.util.List;

public class LocationSection extends ConfigSection {

  public double x, y, z, yaw, pitch;
  public String world;

  @CSIgnore
  public Location _location;

  public LocationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (world == null || world.isBlank())
      throw new MappingError("\"world\" cannot be absent or blank");

    var bukkitWorld = Bukkit.getWorld(world);

    if (bukkitWorld == null)
      throw new MappingError("World \"" + world + "\" could not be located");

    _location = new Location(bukkitWorld, (int) x, (int) y, (int) z, (float) yaw, (float) pitch);
  }
}
