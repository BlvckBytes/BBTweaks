package me.blvckbytes.bbtweaks.emotions;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class MultiDirectedMessagesSection extends ConfigSection {

  public boolean copyBroadcastChatToDiscord = true;

  public @Nullable DisplayedMessagesSection toSender;
  public @Nullable DisplayedMessagesSection toReceiver;
  public @Nullable DisplayedMessagesSection asBroadcast;
  public @Nullable ComponentMarkup toDiscord;

  public MultiDirectedMessagesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (copyBroadcastChatToDiscord && asBroadcast != null && toDiscord == null)
      toDiscord = asBroadcast.chatMessage;
  }
}
