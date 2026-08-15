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
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;

public class DonorSymbolSection extends ConfigSection {

  public Map<String, SymbolSection> symbols = new LinkedHashMap<>();
  public @CSIgnore Map<String, SymbolSection> _symbolByIdentifierLower = new LinkedHashMap<>();
  public @CSIgnore List<SymbolSection> _symbolsInOrder = new ArrayList<>();

  public String defaultSymbol;
  public @CSIgnore SymbolSection _defaultSymbol;

  public Map<String, String> defaultSymbolByPlayerName = new HashMap<>();
  public @CSIgnore Map<String, SymbolSection> _defaultSymbolByPlayerNameLower = new HashMap<>();

  public Map<String, ColorSection> colors = new LinkedHashMap<>();
  public @CSIgnore Map<String, ColorSection> _colorByIdentifierLower = new LinkedHashMap<>();
  public @CSIgnore List<ColorSection> _colorsInOrder = new ArrayList<>();

  public String defaultColor;
  public @CSIgnore ColorSection _defaultColor;

  public Map<String, String> defaultColorByPlayerName = new HashMap<>();
  public @CSIgnore Map<String, ColorSection> _defaultColorByPlayerNameLower = new HashMap<>();

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

  public SymbolSection getDefaultSymbol(Player player) {
    var personalizedDefault = _defaultSymbolByPlayerNameLower.get(player.getName().toLowerCase());

    if (personalizedDefault != null && personalizedDefault.hasPermission(player))
      return personalizedDefault;

    return _defaultSymbol;
  }

  public ColorSection getDefaultColor(Player player) {
    var personalizedDefault = _defaultColorByPlayerNameLower.get(player.getName().toLowerCase());

    if (personalizedDefault != null)
      return personalizedDefault;

    return _defaultColor;
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

    for (var defaultSymbolEntry : defaultSymbolByPlayerName.entrySet()) {
      var playerNameLower = defaultSymbolEntry.getKey().strip().toLowerCase();
      var symbolNameLower = defaultSymbolEntry.getValue().strip().toLowerCase();
      var symbol = _symbolByIdentifierLower.get(symbolNameLower);

      if (symbol == null)
        throw new MappingError("Could not find default-symbol \"" + symbolNameLower + "\" for player-default of \"" + playerNameLower + "\"");

      if (_defaultSymbolByPlayerNameLower.put(playerNameLower, symbol) != null)
        throw new MappingError("Duplicate player-name \"" + playerNameLower + "\" for symbol-defaults");
    }

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

    for (var defaultColorEntry : defaultColorByPlayerName.entrySet()) {
      var playerNameLower = defaultColorEntry.getKey().strip().toLowerCase();
      var colorNameLower = defaultColorEntry.getValue().strip().toLowerCase();
      var color = _colorByIdentifierLower.get(colorNameLower);

      if (color == null)
        throw new MappingError("Could not find default-color \"" + colorNameLower + "\" for player-default of \"" + playerNameLower + "\"");

      if (_defaultColorByPlayerNameLower.put(playerNameLower, color) != null)
        throw new MappingError("Duplicate player-name \"" + playerNameLower + "\" for color-defaults");
    }
  }
}
