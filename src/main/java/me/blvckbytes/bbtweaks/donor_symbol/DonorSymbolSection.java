package me.blvckbytes.bbtweaks.donor_symbol;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.donor_symbol.color_display.DonorSymbolColorDisplaySection;
import me.blvckbytes.bbtweaks.donor_symbol.command.DonorSymbolCommandSection;
import me.blvckbytes.bbtweaks.donor_symbol.main_display.DonorSymbolDisplaySection;
import me.blvckbytes.bbtweaks.donor_symbol.symbol_display.DonorSymbolSymbolDisplaySection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DonorSymbolSection extends ConfigSection {

  public Map<String, SymbolSection> symbols = new LinkedHashMap<>();
  public @CSIgnore Map<String, SymbolSection> _symbolByIdentifierLower = new LinkedHashMap<>();
  public @CSIgnore List<SymbolSection> _symbolsInOrder = new ArrayList<>();

  public String defaultSymbol;
  public @CSIgnore SymbolSection _defaultSymbol;

  public Map<String, ColorSection> colors = new LinkedHashMap<>();
  public @CSIgnore Map<String, ColorSection> _colorByIdentifierLower = new LinkedHashMap<>();
  public @CSIgnore List<ColorSection> _colorsInOrder = new ArrayList<>();

  public String defaultColor;
  public @CSIgnore ColorSection _defaultColor;

  @CSAlways
  public DonorSymbolCommandSection command;

  @CSAlways
  public DonorSymbolDisplaySection mainDisplay;

  @CSAlways
  public DonorSymbolSymbolDisplaySection symbolDisplay;

  @CSAlways
  public DonorSymbolColorDisplaySection colorDisplay;

  public DonorSymbolSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var symbolEntry : symbols.entrySet()) {
      var symbol = symbolEntry.getValue();
      symbol._identifierLower = symbolEntry.getKey().strip().toLowerCase();

      if (_symbolByIdentifierLower.put(symbol._identifierLower, symbol) != null)
        throw new MappingError("Duplicate symbol identifier \"" + symbol._identifierLower + "\" encountered");

      _symbolsInOrder.add(symbol);
    }

    if (defaultSymbol == null || defaultSymbol.isBlank())
      throw new MappingError("Property \"defaultSymbol\" cannot be absent or blank");

    _defaultSymbol = _symbolByIdentifierLower.get(defaultSymbol.strip().toLowerCase());

    if (_defaultSymbol == null)
      throw new MappingError("Property \"defaultSymbol\" specifies an unknown identifier");

    for (var colorEntry : colors.entrySet()) {
      var color = colorEntry.getValue();
      color._identifierLower = colorEntry.getKey().strip().toLowerCase();

      if (_colorByIdentifierLower.put(color._identifierLower, color) != null)
        throw new MappingError("Duplicate color identifier \"" + color._identifierLower + "\" encountered");

      _colorsInOrder.add(color);
    }

    if (defaultColor == null || defaultColor.isBlank())
      throw new MappingError("Property \"defaultColor\" cannot be absent or blank");

    _defaultColor = _colorByIdentifierLower.get(defaultColor.strip().toLowerCase());

    if (_defaultColor == null)
      throw new MappingError("Property \"defaultColor\" specifies an unknown identifier");
  }
}
