package me.blvckbytes.bbtweaks.durability_warnings.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class DurabilityWarningsSection extends ConfigSection {

  public DurabilityWarningCommandSection command;

  public int trackingResetMinDurabilityDelta;

  public ComponentMarkup playersOnly;
  public ComponentMarkup missingPermission;
  public ComponentMarkup commandActionUsage;
  public ComponentMarkup toggleEnabled;
  public ComponentMarkup toggleSound;

  public List<DurabilityWarningSection> warnings = new ArrayList<>();

  @CSIgnore
  private EnumMap<Material, List<DurabilityWarningSection>> _applicativeWarningsByItemType = new EnumMap<>(Material.class);

  public DurabilityWarningsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public @NotNull List<DurabilityWarningSection> getApplicativeWarningsByItemType(Material itemType) {
    var result = _applicativeWarningsByItemType.get(itemType);

    if (result == null)
      return Collections.emptyList();

    return Collections.unmodifiableList(result);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var warning : warnings) {
      for (var itemType : warning._itemTypes) {
        _applicativeWarningsByItemType
          .computeIfAbsent(itemType, k -> new ArrayList<>())
          .add(warning);
      }
    }

    if (trackingResetMinDurabilityDelta <= 0)
      throw new MappingError("Property \"trackingResetMinDurabilityDelta\" cannot be less than or equal to zero");
  }
}
