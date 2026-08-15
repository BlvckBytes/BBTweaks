package me.blvckbytes.bbtweaks.donor_symbol;

import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public class SymbolSection extends ConfigSection {

  public @CSIgnore String _identifierLower;

  public ComponentMarkup displayName;

  public String iconType;
  public @CSIgnore Material _iconType;

  public String javaValue;
  public String bedrockValue;

  public boolean requirePermission;

  public SymbolSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public boolean hasPermission(Player player) {
    if (!requirePermission)
      return true;

    return player.hasPermission("bbtweaks.donor-symbol." + _identifierLower);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (javaValue == null || javaValue.isBlank())
      throw new MappingError("Property \"javaValue\" must not be absent or blank");

    javaValue = javaValue.trim();

    if (bedrockValue == null || bedrockValue.isBlank())
      throw new MappingError("Property \"bedrockValue\" must not be absent or blank");

    bedrockValue = bedrockValue.trim();

    if (iconType == null)
      throw new MappingError("Property \"iconType\" must not be absent");

    _iconType = MaterialMatcher.tryMatch(iconType);

    if (_iconType == null)
      throw new MappingError("Property \"iconType\" is not a valid material");
  }

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("icon_type", _iconType.name())
      .withVariable("display_name", displayName.markupNode)
      .withVariable("java_symbol", javaValue)
      .withVariable("bedrock_symbol", bedrockValue);
  }
}
