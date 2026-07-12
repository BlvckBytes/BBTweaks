package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemNotifierSection extends ConfigSection {

  public int radiusExtent;
  public int notificationCooldownMs;

  public Map<TriggerMode, NotificationSection> notificationByTrigger = new HashMap<>();

  public ComponentMarkup noPermission;
  public ComponentMarkup noContainer;
  public ComponentMarkup missingName;
  public ComponentMarkup unknownFlag;
  public ComponentMarkup creationSuccess;

  public ItemNotifierSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (radiusExtent <= 0)
      throw new MappingError("The property \"radiusExtent\" must not be less than or equal to zero");

    if (notificationCooldownMs <= 0)
      throw new MappingError("The property \"notificationCooldownMs\" must not be less than or equal to zero");

    for (var triggerMode : TriggerMode.values()) {
      if (!notificationByTrigger.containsKey(triggerMode))
        throw new MappingError("The property \"notificationByTrigger\" misses an entry for " + triggerMode.name());
    }
  }
}
