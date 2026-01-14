package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.un_craft.ParsedRecipe;
import me.blvckbytes.bbtweaks.un_craft.RecipeSyntax;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AdditionalRecipe extends ConfigSection {

  public String recipe;

  @CSIgnore
  public ParsedRecipe _recipe;

  public List<ComponentMarkup> additionalMessages = new ArrayList<>();

  public AdditionalRecipe(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    try {
      _recipe = RecipeSyntax.tryParseRecipe(recipe);
    } catch (Throwable e) {
      throw new IllegalStateException("Could not parse additional recipe-syntax \"" + recipe + "\": " + e.getMessage());
    }
  }
}
