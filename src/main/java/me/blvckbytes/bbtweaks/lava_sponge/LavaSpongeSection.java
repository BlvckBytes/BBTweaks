package me.blvckbytes.bbtweaks.lava_sponge;

import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LavaSpongeSection extends ConfigSection {

  public Map<String, LimitsSection> limitTiers = new HashMap<>();

  public final @CSIgnore List<LimitsSection> limitsDescending = new ArrayList<>();

  public LavaSpongeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var entry : limitTiers.entrySet()) {
      var limitSection = entry.getValue();
      limitSection.tierName = entry.getKey().toLowerCase().trim();
      limitsDescending.add(limitSection);
    }

    limitsDescending.sort((a, b) -> -Integer.compare(a.maxDistance, b.maxDistance));
  }
}
