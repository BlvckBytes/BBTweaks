package me.blvckbytes.bbtweaks.multi_break.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.un_craft.config.TypeRule;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.*;

@CSAlways
public class MultiBreakSection extends ConfigSection {

  public MultiBreakCommandSection command;

  public Map<String, MultiBreakLimitsSection> limitsByTierName = new HashMap<>();
  public Set<String> allowedWorlds = new HashSet<>();

  public double perAdditionalBlockDurabilityDecreaseChance;

  public int customPickupDelay;

  public List<TypeRule> blockExclusions = new ArrayList<>();

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
  public ComponentMarkup filterAlreadyEnabled;
  public ComponentMarkup filterNowEnabled;
  public ComponentMarkup filterAlreadyDisabled;
  public ComponentMarkup filterNowDisabled;
  public ComponentMarkup hotbarNotification;
  public ComponentMarkup sizeSetExceededDimensions;
  public ComponentMarkup sizeSet;
  public ComponentMarkup selectSlotUsage;
  public ComponentMarkup slotSelected;
  public ComponentMarkup slotAlreadySelected;
  public ComponentMarkup slotIsLocked;
  public ComponentMarkup slotNowLocked;
  public ComponentMarkup slotNowUnlocked;

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

  public boolean isBlockExcluded(Material material) {
    if (blockExclusions == null)
      return false;

    for (var exclusion : blockExclusions) {
      if (exclusion.matches(material))
        return true;
    }

    return false;
  }
}
