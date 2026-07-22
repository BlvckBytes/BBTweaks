package me.blvckbytes.bbtweaks.inv_magnet.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
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

  public String preExistingWorldGroupName;

  public Map<String, InvMagnetWorldGroupSection> worldGroupByName = new HashMap<>();

  @CSIgnore
  public List<InvMagnetWorldGroupSection> worldGroups = new ArrayList<>();

  public ComponentMarkup playersOnly;
  public ComponentMarkup missingPermission;
  public ComponentMarkup actionUsage;
  public ComponentMarkup radiusUsage;
  public ComponentMarkup currentlyHasNoAccess;
  public ComponentMarkup nowEnabled;
  public ComponentMarkup alreadyEnabled;
  public ComponentMarkup nowDisabled;
  public ComponentMarkup alreadyDisabled;
  public ComponentMarkup invalidRadius;
  public ComponentMarkup exceededRadiusLimit;
  public ComponentMarkup updatedRadius;
  public ComponentMarkup status;

  public InvMagnetSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    var seenWorldNames = new HashSet<String>();
    var seenIdentifyingNames = new HashSet<String>();

    for (var worldGroupEntry : worldGroupByName.entrySet()) {
      var worldGroup = worldGroupEntry.getValue();

      worldGroup.identifyingName = worldGroupEntry.getKey().trim().toLowerCase();

      if (!seenIdentifyingNames.add(worldGroup.identifyingName))
        throw new MappingError("Duplicate identifying-name encountered: \"" + worldGroup.identifyingName + "\"");

      worldGroups.add(worldGroup);

      for (var worldName : worldGroup.worlds) {
        if (!seenWorldNames.add(worldName))
          throw new MappingError("Duplicate world-name encountered: \"" + worldName + "\"");
      }

      for (var limitEntry : worldGroup.maxRadiusByTierName.entrySet()) {
        var maxRadius = limitEntry.getValue();
        worldGroup.limitsInDescendingOrder.add(new InvMagnetLimits(maxRadius, limitEntry.getKey(), worldGroup.identifyingName, worldGroup._displayName));
      }

      worldGroup.limitsInDescendingOrder.sort((a, b) -> -Integer.compare(a.maxRadius(), b.maxRadius()));
    }
  }
}
