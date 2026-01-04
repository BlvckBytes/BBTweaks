package me.blvckbytes.bbtweaks.un_craft;

import me.blvckbytes.bbtweaks.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record AdditionalRecipe(
  ParsedRecipe recipe,
  List<String> additionalMessages
) {
  public static AdditionalRecipe fromConfig(ConfigurationSection section) {
    var recipeString = section.getString("recipe");

    if (recipeString == null || recipeString.isBlank())
      throw new IllegalStateException("Missing or blank \"recipe\" key");

    ParsedRecipe recipe;

    try {
      recipe = RecipeSyntax.tryParseRecipe(recipeString);
    } catch (Throwable e) {
      throw new IllegalStateException("Could not parse additional recipe-syntax \"" + recipeString + "\": " + e.getMessage());
    }

    var additionalMessages = new ArrayList<String>();

    for (var additionalMessage : section.getStringList("additionalMessages"))
      additionalMessages.add(ColorUtil.enableColors(additionalMessage));

    return new AdditionalRecipe(recipe, additionalMessages);
  }
}
