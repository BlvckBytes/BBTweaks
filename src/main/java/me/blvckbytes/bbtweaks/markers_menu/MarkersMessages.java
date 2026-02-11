package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MarkersMessages extends ConfigSection {

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup showingCategories;
  public ComponentMarkup unknownCategory;
  public ComponentMarkup showingMarkers;
  public ComponentMarkup setMarkerUsage;
  public ComponentMarkup unknownMarker;
  public ComponentMarkup markerSetError;
  public ComponentMarkup markerSetSuccessfully;
  public ComponentMarkup teleportedToLocation;
  public ComponentMarkup noLocationSet;

  public MarkersMessages(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
