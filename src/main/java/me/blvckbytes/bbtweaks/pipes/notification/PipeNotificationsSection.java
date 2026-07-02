package me.blvckbytes.bbtweaks.pipes.notification;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PipeNotificationsSection extends ConfigSection {

  public int notificationRadius;
  public boolean notifyOwnersOfRegion;
  public boolean notifyMembersOfRegion;
  public List<String> notifyIgnoredRegions;
  public int debounceSeconds;

  @CSIgnore
  public Set<String> ignoredRegionsLower = new HashSet<>();

  public ComponentMarkup malformedSignToken;
  public ComponentMarkup noSignEncountered;
  public ComponentMarkup pistonLimitExceeded;
  public ComponentMarkup tubeLimitExceeded;
  public ComponentMarkup warmingUp;

  public PipeNotificationsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (notifyIgnoredRegions != null) {
      for (var notifyIgnoredRegion : notifyIgnoredRegions)
        ignoredRegionsLower.add(notifyIgnoredRegion.toLowerCase().trim());
    }
  }
}
