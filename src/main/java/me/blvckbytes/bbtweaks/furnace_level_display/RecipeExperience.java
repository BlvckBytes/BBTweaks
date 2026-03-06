package me.blvckbytes.bbtweaks.furnace_level_display;

public record RecipeExperience<KeyType>(
  float experience,
  KeyType recipeKey,
  String recipePath
) {}