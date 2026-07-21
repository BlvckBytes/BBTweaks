package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.inv_filter.command.InvFilterCommandSection;
import me.blvckbytes.bbtweaks.inv_filter.display.InvFilterDisplaySection;

import java.lang.reflect.Field;
import java.util.List;

public class InvFilterSection extends ConfigSection {

  @CSAlways
  public InvFilterCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup usageSelectSlot;
  public ComponentMarkup invalidFilterSlot;
  public ComponentMarkup usageAction;
  public ComponentMarkup usageGetFilter;
  public ComponentMarkup getFilterNoneSet;
  public ComponentMarkup getFilter;
  public ComponentMarkup removeFilterNoneSet;
  public ComponentMarkup removeFilter;
  public ComponentMarkup usageLanguage;
  public ComponentMarkup usageFilterDefaultLanguage;
  public ComponentMarkup usageFilterCustomLanguage;
  public ComponentMarkup predicateError;
  public ComponentMarkup alreadyEnabled;
  public ComponentMarkup nowEnabled;
  public ComponentMarkup alreadyDisabled;
  public ComponentMarkup nowDisabled;
  public ComponentMarkup slotNowSelected;
  public ComponentMarkup slotAlreadySelected;
  public ComponentMarkup filterSet;
  public ComponentMarkup activeFilterWarning;

  @CSAlways
  public InvFilterDisplaySection display;

  public InvFilterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);
  }
}
