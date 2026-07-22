package me.blvckbytes.bbtweaks.inv_magnet.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetLimits;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvMagnetWorldGroupSection extends ConfigSection {

  @CSIgnore
  public String identifyingName;

  public ComponentMarkup displayName;

  public Component _displayName;

  public List<String> worlds = new ArrayList<>();

  public Map<String, Integer> maxRadiusByTierName = new HashMap<>();

  @CSIgnore
  public List<InvMagnetLimits> limitsInDescendingOrder = new ArrayList<>();

  public InvMagnetWorldGroupSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _displayName = displayName.interpret(SlotType.SINGLE_LINE_CHAT, null).getFirst();

    worlds.replaceAll(name -> name.toLowerCase().trim());
  }
}
