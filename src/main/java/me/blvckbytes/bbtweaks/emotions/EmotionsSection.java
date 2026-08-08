package me.blvckbytes.bbtweaks.emotions;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.emotions.command.EmotionCommandSection;
import me.blvckbytes.bbtweaks.emotions.settings_command.EmotionSettingsCommandSection;
import me.blvckbytes.bbtweaks.emotions.settings_display.EmotionSettingsDisplaySection;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmotionsSection extends ConfigSection {

  public @CSAlways EmotionCommandSection mainCommand;

  public @CSAlways EmotionSettingsCommandSection settingsCommand;

  public @CSAlways EmotionSettingsDisplaySection settingsDisplay;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup unknownEmotion;
  public ComponentMarkup missingEmotionPermission;
  public ComponentMarkup awaitRemainingCooldown;
  public ComponentMarkup unsupportedAllTarget;
  public ComponentMarkup cannotCombineAllSentinelWithNames;
  public ComponentMarkup noReceivingPlayersOnline;
  public ComponentMarkup unsupportedOtherTarget;
  public ComponentMarkup receivingPlayerNotOnline;
  public ComponentMarkup receiverCannotBeSelf;
  public ComponentMarkup receivingPlayerDuplicate;
  public ComponentMarkup maximumNumberOfTargetsExceeded;
  public ComponentMarkup unsupportedPlayingOnSelf;
  public ComponentMarkup noAccessToAnyEmotion;
  public ComponentMarkup commandEmotionHelpScreen;

  public Map<String, EmotionSection> emotionByIdentifier = new HashMap<>();

  @CSIgnore
  public Map<String, EmotionSection> emotionByIdentifierLower = new HashMap<>();

  public EmotionsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var emotionEntry : emotionByIdentifier.entrySet()) {
      var identifierLower = emotionEntry.getKey().toLowerCase();

      if (identifierLower.contains(" "))
        throw new MappingError("Emotion-identifier \"" + emotionEntry.getKey() + "\" contains an illegal space!");

      if (identifierLower.contains(";"))
        throw new MappingError("Emotion-identifier \"" + emotionEntry.getKey() + "\" contains an illegal semicolon!");

      if (identifierLower.contains("="))
        throw new MappingError("Emotion-identifier \"" + emotionEntry.getKey() + "\" contains an illegal equals-sign!");

      var emotion = emotionEntry.getValue();

      emotion.identifierLower = identifierLower;

      emotionByIdentifierLower.put(identifierLower, emotion);
    }
  }
}
