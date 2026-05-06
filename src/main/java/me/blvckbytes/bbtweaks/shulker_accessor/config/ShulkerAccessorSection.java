package me.blvckbytes.bbtweaks.shulker_accessor.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ShulkerAccessorSection extends ConfigSection {

  public int openShulkerCooldownTicks;

  public List<String> additionalAllowedInventoryTitlePatterns = new ArrayList<>();

  @CSIgnore
  public final List<Pattern> _additionalAllowedInventoryTitlePatterns = new ArrayList<>();

  public ShulkerAccessorSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var patternString : additionalAllowedInventoryTitlePatterns) {
      Pattern compiledPattern;

      try {
        compiledPattern = Pattern.compile(patternString);
      } catch (Throwable e) {
        throw new MappingError("Malformed pattern \"" + patternString + "\" encountered: " + e.getMessage());
      }

      _additionalAllowedInventoryTitlePatterns.add(compiledPattern);
    }

    if (openShulkerCooldownTicks <= 0)
      throw new MappingError("Property \"openShulkerCooldownTicks\" cannot be less than or equal to zero");
  }
}
