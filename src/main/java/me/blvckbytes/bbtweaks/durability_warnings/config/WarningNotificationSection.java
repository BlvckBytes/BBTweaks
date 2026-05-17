package me.blvckbytes.bbtweaks.durability_warnings.config;

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

public class WarningNotificationSection extends ConfigSection {

  public @CSIgnore int percentage;

  public @Nullable String sound;
  public @CSIgnore XSound _sound;

  public @Nullable ComponentMarkup title;
  public @Nullable ComponentMarkup subtitle;
  public @Nullable ComponentMarkup chat;
  public int titleStayMs = 2000;

  public WarningNotificationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (sound != null && !sound.isBlank()) {
      var xSound = XSound.of(sound.trim());

      if (xSound.isEmpty())
        throw new MappingError("Unknown XSound: " + sound);

      _sound = xSound.get();
    }
  }

  public void displayTo(Player player, boolean playSound, InterpretationEnvironment environment) {
    if (playSound && _sound != null)
      _sound.play(player, 1, 1);

    if (title != null || subtitle != null) {
      if (title != null)
        player.sendTitlePart(TitlePart.TITLE, title.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst());

      if (subtitle != null)
        player.sendTitlePart(TitlePart.SUBTITLE, subtitle.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst());

      player.sendTitlePart(
        TitlePart.TIMES,
        Title.Times.times(
          Duration.ofMillis(100),
          Duration.ofMillis(titleStayMs),
          Duration.ofMillis(100)
        )
      );
    }

    if (chat != null)
      chat.sendMessage(player, environment);
  }

  public WarningNotificationSection copy() {
    var result = new WarningNotificationSection(baseEnvironment, interpreterLogger);

    result._sound = _sound;
    result.title = title;
    result.subtitle = subtitle;
    result.chat = chat;
    result.titleStayMs = titleStayMs;

    return result;
  }
}
