package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

public class UnCraftEntry {

  public final int inputAmount;
  public final Map<Material, Integer> results;
  public final Set<String> exclusionReasons;

  private UnCraftEntry(int inputAmount, Map<Material, Integer> results, Set<String> exclusionReasons) {
    this.inputAmount = inputAmount;
    this.results = results;
    this.exclusionReasons = exclusionReasons;
  }

  public static UnCraftEntry tryCreateWithScaledSingleUnit(int inputAmount, Map<Material, Integer> results, Set<String> exclusionReasons) {
    // All ingredient-counts need to be a multiple of the result-amount for the scaling to succeed with whole numbers
    if (inputAmount == 0 || !results.values().stream().allMatch(resultAmount -> resultAmount % inputAmount == 0))
      return new UnCraftEntry(inputAmount, results, exclusionReasons);

    for (var resultEntry : results.entrySet())
      resultEntry.setValue(resultEntry.getValue() / inputAmount);

    return new UnCraftEntry(1, results, exclusionReasons);
  }
}
