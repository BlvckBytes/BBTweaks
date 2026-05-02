package me.blvckbytes.bbtweaks.inv_magnet.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetLimits;

import java.lang.reflect.Field;
import java.util.*;

public class InvMagnetSection extends ConfigSection {

  @CSAlways
  public InvMagnetCommandSection command;

  public Set<String> worlds = new HashSet<>();

  public Map<String, InvMagnetLimitsSection> limitsByTierName = new HashMap<>();

  public ComponentMarkup missingPermission;
  public ComponentMarkup unallowedWorld;
  public ComponentMarkup nowEnabled;
  public ComponentMarkup nowDisabled;
  public ComponentMarkup invalidRadius;
  public ComponentMarkup exceededRadiusLimit;
  public ComponentMarkup updatedRadius;

  @CSIgnore
  public List<InvMagnetLimits> limitsInDescendingOrder = new ArrayList<>();

  public InvMagnetSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var limitEntry : limitsByTierName.entrySet()) {
      var limits = limitEntry.getValue();
      limitsInDescendingOrder.add(new InvMagnetLimits(limits.maxRadius, limitEntry.getKey()));
    }

    limitsInDescendingOrder.sort((a, b) -> -Integer.compare(a.maxRadius(), b.maxRadius()));
  }
}
