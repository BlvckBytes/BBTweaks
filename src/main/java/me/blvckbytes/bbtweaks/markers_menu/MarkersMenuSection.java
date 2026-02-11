package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.markers_menu.display.MarkerDisplayGuiSection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@CSAlways
public class MarkersMenuSection extends ConfigSection {

  public MarkersCommandSection markersCommand;
  public SetMarkerCommandSection setMarkerCommand;

  public Map<String, MarkerSection> markers;
  public Map<String, CategorySection> categories;

  public MarkersMessages messages;
  public MarkerDisplayGuiSection display;

  public MarkersMenuSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (markers == null)
      throw new MappingError("Missing key \"markers\"");

    for (var markerEntry : markers.entrySet())
      markerEntry.getValue()._name = markerEntry.getKey();

    if (categories == null)
      throw new MappingError("Missing key \"categories\"");

    for (var categoryEntry : categories.entrySet()) {
      var categoryName = categoryEntry.getKey();
      var category = categoryEntry.getValue();

      category._name = categoryName;

      for (var memberName : category.members) {
        var member = getMarkerByNameIgnoreCase(memberName);

        if (member == null)
          throw new MappingError("Member \"" + memberName + "\" of category \"" + categoryName + "\" is not a known marker");

        category._members.add(member);
      }
    }
  }

  public @Nullable CategorySection getCategoryByNameIgnoreCase(String name) {
    if (categories == null)
      return null;

    for (var categoryEntry : categories.entrySet()) {
      if (categoryEntry.getKey().equalsIgnoreCase(name))
        return categoryEntry.getValue();
    }

    return null;
  }

  public @Nullable MarkerSection getMarkerByNameIgnoreCase(String name) {
    if (markers == null)
      return null;

    for (var markerEntry : markers.entrySet()) {
      if (markerEntry.getKey().equalsIgnoreCase(name))
        return markerEntry.getValue();
    }

    return null;
  }
}
