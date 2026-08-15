package me.blvckbytes.bbtweaks.emotions;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class DisplayedMessagesSection extends ConfigSection {

  public @Nullable ComponentMarkup chatMessage;
  public @Nullable ComponentMarkup actionBarMessage;
  public @Nullable ComponentMarkup titleMessage;
  public @Nullable ComponentMarkup subTitleMessage;

  public int titleFadeIn;
  public int titleStay;
  public int titleFadeOut;

  public boolean copyChatToActionBar;

  public DisplayedMessagesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.titleFadeIn = 5;
    this.titleStay = 35;
    this.titleFadeOut = 5;
    this.copyChatToActionBar = true;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (copyChatToActionBar && actionBarMessage == null)
      actionBarMessage = chatMessage;
  }
}
