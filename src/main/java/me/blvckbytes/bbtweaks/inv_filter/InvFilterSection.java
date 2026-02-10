package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class InvFilterSection extends ConfigSection {

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup usageAction;
  public ComponentMarkup usageLanguage;
  public ComponentMarkup usageFilterDefaultLanguage;
  public ComponentMarkup usageFilterCustomLanguage;
  public ComponentMarkup predicateError;
  public ComponentMarkup filterChanged;
  public ComponentMarkup usageMode;
  public ComponentMarkup currentState;
  public ComponentMarkup modeChanged;

  public InvFilterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);
  }
}
