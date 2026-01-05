package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RecipeSyntax {

  // The recipes-file takes one recipe per line, with blank lines simply being ignored.
  // A line is of the following syntax:
  // <amount> <input-type> -> [-]<amount> <output-type> [, [-]<amount> <output-type>]
  // Where negative amounts mark subtracted (excluded) results.

  public static ParsedRecipe tryParseRecipe(String line) {
    var tokens = tokenizeRecipeLine(line);

    if (tokens.isEmpty())
      throw new IllegalStateException("Cannot parse an empty input");

    if (tokens.get(0).startsWith("#"))
      throw new IllegalStateException("Refusing to parse a commented-out line");

    if (tokens.size() < 5)
      throw new IllegalStateException("Requiring at least five tokens per line: <amount> <input-type> -> <amount> <output-type>");

    int inputAmount;

    try {
      inputAmount = Integer.parseInt(tokens.get(0));
    } catch (Throwable e) {
      throw new IllegalStateException("Malformed input-amount: " + tokens.get(0));
    }

    if (inputAmount <= 0)
      throw new IllegalStateException("Input-amount cannot be less than or equal to zero");

    Material inputMaterial;

    try {
      inputMaterial = Material.valueOf(tokens.get(1));
    } catch (Throwable e) {
      throw new IllegalStateException("Malformed input-type: " + tokens.get(1));
    }

    if (!"->".equals(tokens.get(2)))
      throw new IllegalStateException("Expected arrow-operator after input-description: ->");

    var results = new HashMap<Material, Integer>();
    var subtractedResults = new HashSet<Material>();

    for (var tokenIndex = 3; tokenIndex < tokens.size(); ++tokenIndex) {
      if (tokenIndex != 3) {
        var comma = tokens.get(tokenIndex);

        if (!",".equals(comma))
          throw new IllegalStateException("Expecting output-entries to be comma-separated");

        ++tokenIndex;
      }

      int outputAmount;

      try {
        outputAmount = Integer.parseInt(tokens.get(tokenIndex));
      } catch (Throwable e) {
        throw new IllegalStateException("Malformed output-amount: " + tokens.get(tokenIndex));
      }

      if (outputAmount == 0)
        throw new IllegalStateException("Output-amount cannot be equal to zero");

      if (tokenIndex == tokens.size() - 1)
        throw new IllegalStateException("Line cannot end with an amount - missing corresponding type");

      Material outputMaterial;

      try {
        outputMaterial = Material.valueOf(tokens.get(tokenIndex + 1));
      } catch (Throwable e) {
        throw new IllegalStateException("Malformed output-type: " + tokens.get(tokenIndex + 1));
      }

      if (results.put(outputMaterial, Math.abs(outputAmount)) != null)
        throw new IllegalStateException("Duplicate output-material: " + outputMaterial);

      if (outputAmount < 0)
        subtractedResults.add(outputMaterial);

      ++tokenIndex;
    }

    return new ParsedRecipe(inputMaterial, inputAmount, results, subtractedResults);
  }

  private static List<String> tokenizeRecipeLine(String line) {
    var result = new ArrayList<String>();

    // Yes - this is a bit crafty, but it gets the job done and performance is basically irrelevant.

    var firstLevelTokens = line.split(",");

    for (var index = 0; index < firstLevelTokens.length; ++index) {
      if (index != 0)
        result.add(",");

      var firstLevelToken = firstLevelTokens[index];

      var secondLevelTokens = firstLevelToken.split(" ");

      for (String levelToken : secondLevelTokens) {
        var secondLevelToken = levelToken.trim();

        if (!secondLevelToken.isBlank())
          result.add(secondLevelToken);
      }
    }

    return result;
  }
}
