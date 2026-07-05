package me.blvckbytes.bbtweaks.durability_warnings.config;

import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class DurabilityWarningSection extends ConfigSection {

  public List<String> itemTypes = new ArrayList<>();
  public @CSIgnore Set<Material> _itemTypes = new HashSet<>();

  public Map<String, WarningNotificationSection> notificationByPercentage = new HashMap<>();

  @CSIgnore
  private final @Nullable WarningNotificationSection[] _notificationByPercentage = new WarningNotificationSection[100];

  public DurabilityWarningSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public WarningNotificationSection getNotificationAtPercentage(int percentage) {
    return _notificationByPercentage[percentage];
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var itemType : itemTypes) {
      var material = MaterialMatcher.tryMatch(itemType);

      if (material == null)
        throw new MappingError("Invalid Material-value: " + itemType);

      if (!_itemTypes.add(material))
        throw new MappingError("Duplicate material: " + itemType);
    }

    var encounteredPercentages = new IntOpenHashSet();

    var notifications = new ArrayList<WarningNotificationSection>();

    for (var entry : notificationByPercentage.entrySet()) {
      for (var percentageString : entry.getKey().split(",")) {
        var notification = entry.getValue().copy();

        if (!percentageString.endsWith("%"))
          throw new MappingError("Percentage " + percentageString + " does not end in a %");

        int percentage;

        try {
          percentage = Integer.parseInt(percentageString.substring(0, percentageString.length() - 1));

          if (percentage <= 0 || percentage > 100)
            throw new IllegalStateException();
        } catch (Throwable e) {
          throw new MappingError("Malformed percentage (positive integer in [1;100] expected): " + percentageString);
        }

        if (!encounteredPercentages.add(percentage))
          throw new MappingError("Duplicate percentage encountered: " + percentageString);

        notification.percentage = percentage;

        notifications.add(notification);
      }
    }

    notifications.sort((a, b) -> -Integer.compare(a.percentage, b.percentage));

    for (var notification : notifications) {
      for (var affectedPercentage = notification.percentage; affectedPercentage >= 0; --affectedPercentage)
        _notificationByPercentage[affectedPercentage] = notification;
    }
  }
}
