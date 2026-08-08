package me.blvckbytes.bbtweaks.emotions;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import at.blvckbytes.playtime_rewards.com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EmotionSection extends ConfigSection {

  @CSAlways
  public String identifierLower;

  public boolean tryRegisterDirectly;
  public List<String> directAliases;
  public ComponentMarkup description;
  public boolean doesNoTargetEqualsAll;

  public int cooldownSeconds;
  public boolean supportsSelf;
  public boolean supportsOthers;
  public boolean supportsAll;
  public int maximumNumberOfTargets;
  public boolean broadcastToConsole;

  private @Nullable String sound;

  public double soundPitch = 1;
  public double soundVolume = 1;

  @CSIgnore
  public @Nullable XSound _sound;

  public List<DisplayedEffectSection> effects;

  private MultiDirectedMessagesSection atSelfMessages;
  private @Nullable List<MultiDirectedMessagesSection> additionalAtSelfMessages;

  private MultiDirectedMessagesSection atOthersMessages;
  private @Nullable List<MultiDirectedMessagesSection> additionalAtOthersMessages;

  public EmotionSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.effects = new ArrayList<>();
    this.directAliases = new ArrayList<>();
    this.maximumNumberOfTargets = 1;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (!(supportsSelf || supportsOthers || supportsAll))
      throw new MappingError("At least one of the properties \"supportsSelf\", \"supportsOthers\" or \"supportsAll\" must be enabled!");

    if (supportsSelf && atSelfMessages == null)
      throw new MappingError("Property \"atSelfMessages\" cannot be absent if \"supportsSelf\" is set");

    if (supportsOthers) {
      if (atOthersMessages == null)
        throw new MappingError("If \"supportsOther\" is set, \"atOthersMessages\" must be present");
    }

    if (supportsOthers) {
      if (maximumNumberOfTargets <= 0)
        throw new MappingError("Property \"maximumNumberOfTargets\" cannot be less than or equal to zero");
    }

    if (description == null)
      throw new MappingError("Property \"description\" was absent, but is required");

    if (sound != null) {
      var xSound = XSound.of(sound);

      if (xSound.isEmpty())
        throw new MappingError("Property \"sound\" of value \"" + sound + "\" could not be corresponded to an XSound");

      _sound = xSound.get();
    }

    if (soundPitch < 0)
      throw new MappingError("The property \"soundPitch\" cannot be less than zero");

    if (soundVolume < 0)
      throw new MappingError("The property \"soundVolume\" cannot be less than zero");

  }

  public boolean hasUsePermission(Player player) {
    return player.hasPermission("bbtweaks.emotion.use." + identifierLower);
  }

  public boolean hasCooldownBypassPermission(Player player) {
    return player.hasPermission("bbtweaks.emotion.bypass-cooldown." + identifierLower);
  }

  public MultiDirectedMessagesSection accessAtSelfMessages() {
    return chooseRandomizedMessages(atSelfMessages, additionalAtSelfMessages);
  }

  public MultiDirectedMessagesSection accessAtOthersMessages() {
    return chooseRandomizedMessages(atOthersMessages, additionalAtOthersMessages);
  }

  private static MultiDirectedMessagesSection chooseRandomizedMessages(MultiDirectedMessagesSection main, @Nullable List<MultiDirectedMessagesSection> additional) {
    if (additional == null)
      return main;

    var additionalCount = additional.size();
    var itemIndex = ThreadLocalRandom.current().nextInt(additionalCount + 1);

    if (itemIndex == additionalCount)
      return main;

    return additional.get(itemIndex);
  }
}
