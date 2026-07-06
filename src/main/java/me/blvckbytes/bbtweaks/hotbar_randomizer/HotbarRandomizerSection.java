package me.blvckbytes.bbtweaks.hotbar_randomizer;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.hotbar_randomizer.command.HotbarRandomizerCommandSection;
import me.blvckbytes.bbtweaks.hotbar_randomizer.settings_display.HotbarRandomizerSettingsDisplaySection;

public class HotbarRandomizerSection extends ConfigSection {

  @CSAlways
  public HotbarRandomizerCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup commandActionUsage;
  public ComponentMarkup nowEnabled;
  public ComponentMarkup alreadyEnabled;
  public ComponentMarkup nowDisabled;
  public ComponentMarkup alreadyDisabled;

  @CSAlways
  public HotbarRandomizerSettingsDisplaySection settingsDisplay;

  public HotbarRandomizerSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
