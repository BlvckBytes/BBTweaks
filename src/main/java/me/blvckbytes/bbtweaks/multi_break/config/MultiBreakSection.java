package me.blvckbytes.bbtweaks.multi_break.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.*;

@CSAlways
public class MultiBreakSection extends ConfigSection {

  public MultiBreakCommandSection command;

  public Map<String, MultiBreakLimitsSection> limitsByTierName = new HashMap<>();
  public Set<String> allowedWorlds = new HashSet<>();

  public double perAdditionalBlockDurabilityDecreaseChance;

  public ComponentMarkup missingPermission;
  public ComponentMarkup unallowedWorld;
  public ComponentMarkup noAccessToAnyVolume;
  public ComponentMarkup openingSettingsMenu;
  public ComponentMarkup commandActionUsage;
  public ComponentMarkup commandFilterLanguageUsage;
  public ComponentMarkup commandFilterUsageDefaultLanguage;
  public ComponentMarkup commandFilterUsageCustomLanguage;
  public ComponentMarkup commandSizeUsage;
  public ComponentMarkup predicateError;
  public ComponentMarkup filterSet;
  public ComponentMarkup filterRemoved;
  public ComponentMarkup alreadyEnabled;
  public ComponentMarkup nowEnabled;
  public ComponentMarkup alreadyDisabled;
  public ComponentMarkup nowDisabled;
  public ComponentMarkup noFilterSet;
  public ComponentMarkup currentFilter;
  public ComponentMarkup noToolsInHotbarFor;
  public ComponentMarkup sizeSetExceededDimensions;
  public ComponentMarkup sizeSet;

  public EnabledJoinWarningSection enabledJoinWarning;

  public MultiBreakDisplaySection display;

  @CSIgnore
  public List<MultiBreakLimits> limitsInDescendingOrder = new ArrayList<>();

  public MultiBreakSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var limitEntry : limitsByTierName.entrySet()) {
      var limits = limitEntry.getValue();
      limitsInDescendingOrder.add(new MultiBreakLimits(limits.maxDimension, limitEntry.getKey()));
    }

    limitsInDescendingOrder.sort((a, b) -> -Integer.compare(a.maxDimension(), b.maxDimension()));
  }
}
