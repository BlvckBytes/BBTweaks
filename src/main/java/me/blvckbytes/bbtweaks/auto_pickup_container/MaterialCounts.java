package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.util.MutableInt;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.*;

public record MaterialCounts(Map<Material, MutableInt> counts) {

  public static MaterialCounts EMPTY = new MaterialCounts(Collections.emptyMap());

  public List<TranslatedMaterialCount> asTranslatedCountList(IPPIntegration ippIntegration) {
    var result = new ArrayList<TranslatedMaterialCount>();

    // Sadly, there is no way to reliably wrap translate-components in the lore where these
    // counts are displayed, so we will have to display server-side evaluated german names
    // to every client, no matter their configured locale - a compromise solution.

    var translationRegistry = ippIntegration.languageRegistry.getTranslationRegistry(TranslationLanguage.GERMAN_DE);

    for (var entry : counts.entrySet()) {
      var material = entry.getKey();
      var translation = translationRegistry.getTranslationBySingleton(material);

      if (translation == null)
        translation = material.name();

      result.add(new TranslatedMaterialCount(translation, entry.getValue().value));
    }

    return result;
  }

  public static MaterialCounts fromInventory(Inventory inventory) {
    var counts = new EnumMap<Material, MutableInt>(Material.class);
    var inventorySize = inventory.getSize();

    for (var index = 0; index < inventorySize; ++index) {
      var item = inventory.getItem(index);

      if (item == null || item.getType().isAir())
        continue;

      var amount = item.getAmount();

      if (amount <= 0)
        continue;

      counts.computeIfAbsent(item.getType(), k -> new MutableInt()).value += amount;
    }

    return new MaterialCounts(counts);
  }
}
