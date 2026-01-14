package me.blvckbytes.bbtweaks.un_craft;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import org.bukkit.Material;

import java.util.*;

public class UnCraftEntry {

  public final int inputAmount;
  public final int minRequiredAmount;
  public final Map<Material, Integer> results;
  public final Set<Material> subtractedResults;
  public final Set<String> exclusionReasons;
  public final List<ComponentMarkup> additionalMessages;

  private UnCraftEntry(int inputAmount, Map<Material, Integer> results, Set<String> exclusionReasons) {
    this.inputAmount = inputAmount;
    this.results = results;
    this.exclusionReasons = exclusionReasons;
    this.subtractedResults = new HashSet<>();
    this.additionalMessages = new ArrayList<>();

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

  public List<SignEncodedResultEntry> getNonZeroResults() {
    return makeScaledNonZeroResults(1);
  }

  public List<SignEncodedResultEntry> getScaledNonZeroResults(int providedAmount) {
    return makeScaledNonZeroResults((double) providedAmount / inputAmount);
  }

  private List<SignEncodedResultEntry> makeScaledNonZeroResults(double scalingFactor) {
    var entries = new ArrayList<SignEncodedResultEntry>();

    for (var resultEntry : results.entrySet()) {
      var resultType = resultEntry.getKey();
      var resultAmount = (int) Math.floor(resultEntry.getValue() * scalingFactor);

      if (resultAmount > 0) {
        if (subtractedResults.contains(resultType))
          resultAmount *= -1;

        entries.add(new SignEncodedResultEntry(resultType, resultAmount));
      }
    }

    return entries;
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
