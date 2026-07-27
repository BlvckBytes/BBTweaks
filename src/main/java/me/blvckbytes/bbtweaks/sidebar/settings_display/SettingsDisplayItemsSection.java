package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SettingsDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;
  public GuiItemStackSection enabled;
  public GuiItemStackSection preferencesSlot;
  public GuiItemStackSection previousPage;
  public GuiItemStackSection showTitle;
  public GuiItemStackSection showIcons;
  public GuiItemStackSection doScroll;
  public GuiItemStackSection delimitersMode;
  public GuiItemStackSection allColors;
  public GuiItemStackSection nextSneakMode;
  public GuiItemStackSection openSorting;
  public GuiItemStackSection nextPage;
  public ItemStackSection statisticIcon;

  public SettingsDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
