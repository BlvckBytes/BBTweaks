package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.sidebar.preferences.ColorAndFormats;
import me.blvckbytes.bbtweaks.sidebar.preferences.Format;

import java.util.EnumSet;
import java.util.Map;

public class ColorAndFormatsSection extends ConfigSection {

  public ComponentMarkup color;
  public boolean bold;
  public boolean italic;
  public boolean underlined;

  public ColorAndFormatsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public ColorAndFormats toModel(SidebarStatistic statistic, Map<String, NamedColor> knownColorByNameLower) {
    var colorString = color.asPlainString(null).trim();
    var namedColor = knownColorByNameLower.get(colorString.toLowerCase());

    if (namedColor == null)
      throw new MappingError("Could not find a named color of \"" + colorString + "\" for the defaultStyle of statistic " + statistic);

    var formats = EnumSet.noneOf(Format.class);

    if (bold)
      formats.add(Format.BOLD);

    if (italic)
      formats.add(Format.ITALIC);

    if (underlined)
      formats.add(Format.UNDERLINED);

    return new ColorAndFormats(namedColor, formats);
  }
}
