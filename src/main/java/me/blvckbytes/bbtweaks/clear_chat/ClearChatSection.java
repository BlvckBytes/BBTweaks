package me.blvckbytes.bbtweaks.clear_chat;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class ClearChatSection extends ConfigSection {

  public @CSAlways ClearChatCommandSection command;

  public int blankLineCount;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup actionUsage;
  public ComponentMarkup unknownFlag;
  public ComponentMarkup otherUsage;
  public ComponentMarkup targetNotOnline;
  public ComponentMarkup clearedChatOfTarget;
  public ComponentMarkup otherCannotBeSelf;
  public ComponentMarkup clearedChatSelf;
  public ComponentMarkup clearedChatByOther;
  public ComponentMarkup clearedChatByOtherGlobal;
  public ComponentMarkup clearedChatGlobalSilent;

  public ClearChatSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (blankLineCount <= 0)
      throw new MappingError("Property \"blankLineCount\" cannot be less than or equal to zero");
  }
}
