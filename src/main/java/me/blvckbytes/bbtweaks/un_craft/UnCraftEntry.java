package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

public class UnCraftEntry {

  public final int inputAmount;
  public final int minRequiredAmount;
  public final Map<Material, Integer> results;
  public final Set<String> exclusionReasons;

  private UnCraftEntry(int inputAmount, Map<Material, Integer> results, Set<String> exclusionReasons) {
    this.inputAmount = inputAmount;
    this.results = results;
    this.exclusionReasons = exclusionReasons;

    var _minRequiredAmount = inputAmount;

    while (_minRequiredAmount > 1) {
      var scalingFactor = (double) (_minRequiredAmount - 1) / inputAmount;

      // Refuse to uncraft items which would not yield any results at all
      if (results.values().stream().allMatch(amount -> Math.floor(amount * scalingFactor) == 0))
        break;

      --_minRequiredAmount;
    }

    this.minRequiredAmount = _minRequiredAmount;
  }

  public boolean matchesResultTypes(UnCraftEntry other) {
    return results.keySet().equals(other.results.keySet());
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
