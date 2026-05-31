package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class StatisticIconData extends ConfigSection {

  public ComponentMarkup name;
  public ComponentMarkup description;
  public @Nullable String iconType;

  public @CSIgnore Material _iconType;

  public StatisticIconData(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (iconType == null || iconType.isBlank())
      throw new MappingError("Property \"iconType\" cannot be absent or blank");

    var matchedMaterial = XMaterial.matchXMaterial(iconType.trim());

    if (matchedMaterial.isEmpty() || (_iconType = matchedMaterial.get().get()) == null)
      throw new MappingError("Invalid \"iconType\" XMaterial-value: " + iconType);
  }
}
