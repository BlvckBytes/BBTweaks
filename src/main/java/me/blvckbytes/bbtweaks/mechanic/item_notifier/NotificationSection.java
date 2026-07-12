package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XSound;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

public class NotificationSection extends ConfigSection {

  public ComponentMarkup chat;
  public ComponentMarkup title;
  public ComponentMarkup subtitle;

  public int notificationTitleDurationMs;

  public @Nullable String sound;
  private @CSIgnore @Nullable XSound _sound;

  public NotificationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (sound != null)
      _sound = XSound.of(sound).orElseThrow(() -> new MappingError("Unknown sound \"" + sound + "\""));

    if (notificationTitleDurationMs <= 0)
      throw new MappingError("The property \"notificationTitleDurationMs\" must not be less than or equal to zero");
  }

  public void sendTo(Iterable<Player> recipients, InterpretationEnvironment environment) {
    var chatComponents = chat.interpret(SlotType.CHAT, environment);
    var titleComponent = title.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst();
    var subtitleComponent = subtitle.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst();

    for (var recipient : recipients) {
      chatComponents.forEach(recipient::sendMessage);

      recipient.sendTitlePart(TitlePart.TITLE, titleComponent);
      recipient.sendTitlePart(TitlePart.SUBTITLE, subtitleComponent);
      recipient.sendTitlePart(
        TitlePart.TIMES,
        Title.Times.times(
          Duration.ofMillis(100),
          Duration.ofMillis(notificationTitleDurationMs),
          Duration.ofMillis(100)
        )
      );

      if (_sound != null)
        _sound.play(recipient);
    }
  }
}
