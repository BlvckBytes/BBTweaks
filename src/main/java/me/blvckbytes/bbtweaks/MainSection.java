package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.ab_sleep.ABSleepSection;
import me.blvckbytes.bbtweaks.back.BackOverrideSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelSection;
import me.blvckbytes.bbtweaks.get_uuid.GetUuidSection;

@CSAlways
public class MainSection extends ConfigSection {

  public ABSleepSection abSleep;
  public BackOverrideSection backOverride;
  public FurnaceLevelSection furnaceLevel;
  public GetUuidSection getUuid;

  public MainSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
