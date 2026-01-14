package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.ab_sleep.ABSleepSection;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipesSection;
import me.blvckbytes.bbtweaks.back.BackOverrideSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelSection;
import me.blvckbytes.bbtweaks.get_uuid.GetUuidSection;
import me.blvckbytes.bbtweaks.main_command.MainCommandSection;
import me.blvckbytes.bbtweaks.ping.PingSection;

@CSAlways
public class MainSection extends ConfigSection {

  public MainCommandSection mainCommand;
  public ABSleepSection abSleep;
  public BackOverrideSection backOverride;
  public FurnaceLevelSection furnaceLevel;
  public GetUuidSection getUuid;
  public PingSection ping;
  public AdditionalRecipesSection additionalRecipes;

  public MainSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
