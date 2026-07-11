package me.blvckbytes.bbtweaks.homes;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.homes.command.DelHomeCommandSection;
import me.blvckbytes.bbtweaks.homes.command.HomeCommandSection;
import me.blvckbytes.bbtweaks.homes.command.HomesCommandSection;
import me.blvckbytes.bbtweaks.homes.command.SetHomeCommandSection;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CSAlways
public class HomesSection extends ConfigSection {

  public HomeCommandSection homeCommand;
  public DelHomeCommandSection delHomeCommand;
  public SetHomeCommandSection setHomeCommand;
  public HomesCommandSection homesCommand;

  public Map<String, ComponentMarkup> worldDisplayNames;
  public @CSIgnore Map<String, Component> _worldDisplayNameByNameLower = new HashMap<>();

  public ComponentMarkup playersOnly;
  public ComponentMarkup cannotUseColonInName;
  public ComponentMarkup targetPlayerNameCannotBeEmpty;
  public ComponentMarkup targetPlayerNameIsUnknown;
  public ComponentMarkup targetHomeNameCannotBeEmpty;
  public ComponentMarkup unknownIconMaterial;

  public HomesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (worldDisplayNames != null) {
      for (var nameEntry : worldDisplayNames.entrySet()) {
        _worldDisplayNameByNameLower.put(
          nameEntry.getKey().toLowerCase().trim(),
          nameEntry.getValue().interpret(SlotType.SINGLE_LINE_CHAT, null).getFirst()
        );
      }
    }
  }
}
