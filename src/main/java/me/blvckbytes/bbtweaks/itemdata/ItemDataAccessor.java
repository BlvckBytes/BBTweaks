package me.blvckbytes.bbtweaks.itemdata;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.itemdata.display.PdcEntry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

public class ItemDataAccessor {

  private static final List<PersistentDataType<?, ?>> KNOWN_PDC_TYPES = Arrays.asList(
    PersistentDataType.BOOLEAN, PersistentDataType.BYTE, PersistentDataType.SHORT,
    PersistentDataType.INTEGER, PersistentDataType.LONG,
    PersistentDataType.FLOAT, PersistentDataType.DOUBLE,
    PersistentDataType.STRING,
    PersistentDataType.BYTE_ARRAY, PersistentDataType.INTEGER_ARRAY, PersistentDataType.LONG_ARRAY
  );

  public static @Nullable InterpretationEnvironment makeEnvironmentIfHasData(ItemStack item) {
    var meta = item.getItemMeta();

    var environment = new InterpretationEnvironment();
    var hasData = false;

    if (meta instanceof Repairable repairable && repairable.hasRepairCost()) {
      environment.withVariable("repair_cost", repairable.getRepairCost());
      hasData = true;
    }

    if (meta instanceof MapMeta map && map.hasMapId()) {
      environment.withVariable("map_id", map.getMapId());
      hasData = true;
    }

    var pdc = meta.getPersistentDataContainer();
    var pdcKeys = pdc.getKeys();

    if (!pdcKeys.isEmpty()) {
      var pdcEntriesByNamespace = new LinkedHashMap<String, List<PdcEntry>>();

      keyLoop:
      for (var pdcKey : pdcKeys) {
        for (var pdcType : KNOWN_PDC_TYPES) {
          if (!pdc.has(pdcKey, pdcType))
            continue;

          var value = pdc.get(pdcKey, pdcType);

          if (value == null)
            throw new IllegalStateException("Encountered null-value for \"" + pdcKey + "\", despite #has having returned true");

          String serializedValue;

          if (value.getClass().isArray()) {
            var result = new StringBuilder("[");

            for (var index = 0; index < Array.getLength(value); ++index) {
              if (index > 0)
                result.append(", ");

              result.append(Array.get(value, index));
            }

            serializedValue = result.append(']').toString();
          }

          else if (value instanceof String)
            serializedValue = "\"" + value + "\"";

          else
            serializedValue = String.valueOf(value);

          pdcEntriesByNamespace
            .computeIfAbsent(pdcKey.namespace(), k -> new ArrayList<>())
              .add(new PdcEntry(pdcKey.key().value(), serializedValue));

          continue keyLoop;
        }
      }

      if (!pdcEntriesByNamespace.isEmpty()) {
        environment.withVariable("pdc_entries_by_namespace", pdcEntriesByNamespace);
        hasData = true;
      }
    }

    if (!hasData)
      return null;

    return environment;
  }
}
