package me.blvckbytes.bbtweaks.sidebar.arm_integration;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Pattern;

public class ArmRegionSection extends ConfigSection {

  public String regionWorld;
  public @CSIgnore World _regionWorld;

  public String regionNamePattern;
  public @CSIgnore Pattern _regionNamePattern;

  public ArmRegionSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (regionWorld == null)
      throw new MappingError("Missing mandatory property \"regionWorld\"");

    _regionWorld = Bukkit.getWorld(regionWorld);

    if (_regionWorld == null)
      throw new MappingError("Could not locate world \"" + regionWorld + "\" of property \"regionWorld\"");

    try {
       _regionNamePattern = Pattern.compile(regionNamePattern);
    } catch (Throwable e) {
      throw new MappingError("Malformed pattern \"" + regionNamePattern + "\" of property \"regionNamePattern\" encountered: " + e.getMessage());
    }
  }

  public boolean matches(World regionWorld, String regionName) {
    if (regionWorld != this._regionWorld)
      return false;

    return _regionNamePattern.matcher(regionName).matches();
  }
}
