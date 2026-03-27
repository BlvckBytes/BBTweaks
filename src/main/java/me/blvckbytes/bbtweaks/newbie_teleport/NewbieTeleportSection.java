package me.blvckbytes.bbtweaks.newbie_teleport;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class NewbieTeleportSection extends ConfigSection {

  public @CSAlways NewbieTeleportCommandSection mainCommand;
  public @CSAlways NewbieTeleportResetCommandSection resetCommand;

  public int useCountLimit;

  public List<String> worlds = new ArrayList<>();
  public @CSIgnore List<String> _worldsLower = new ArrayList<>();

  public List<String> allowedRegions = new ArrayList<>();
  public @CSIgnore List<String> _allowedRegionsLower = new ArrayList<>();

  public ComponentMarkup missingPermissionResetCommand;
  public ComponentMarkup usageResetCommand;
  public ComponentMarkup playerNotOnline;
  public ComponentMarkup useCountAlreadyAtZero;
  public ComponentMarkup useCountReset;
  public ComponentMarkup playersOnly;
  public ComponentMarkup missingPermissionMainCommand;
  public ComponentMarkup usageMainCommand;
  public ComponentMarkup noMoreUsesAvailable;
  public ComponentMarkup unsupportedWorld;
  public ComponentMarkup malformedCoordinate;
  public ComponentMarkup outsideOfWorldBorder;
  public ComponentMarkup unsafeDestination;
  public ComponentMarkup disallowedRegion;
  public ComponentMarkup successfulTeleport;

  public NewbieTeleportSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (useCountLimit <= 0)
      throw new MappingError("\"useCountLimit\" cannot be less than or equal to zero");

    for (var world : worlds)
      _worldsLower.add(world.toLowerCase().trim());

    for (var allowedRegion : allowedRegions)
      _allowedRegionsLower.add(allowedRegion.toLowerCase().trim());
  }
}
