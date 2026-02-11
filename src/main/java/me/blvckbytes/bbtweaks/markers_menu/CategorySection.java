package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.markers_menu.display.MarkerDisplayItem;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@CSAlways
public class CategorySection extends ConfigSection implements MarkerDisplayItem {

  public ItemStackSection icon;
  public List<String> members;

  public @Nullable ComponentMarkup displayName;

  @CSIgnore
  private Component _displayName;

  public @Nullable ComponentMarkup description;

  @CSIgnore
  public List<MarkerSection> _members = new ArrayList<>();

  @CSIgnore
  public String _name;

  public CategorySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (members == null || members.isEmpty())
      throw new MappingError("\"members\"-list cannot be absent or empty");

    if (displayName != null)
      _displayName = displayName.interpret(SlotType.SINGLE_LINE_CHAT, null).get(0);
  }

  public Object getDisplayNameOrName() {
    if (_displayName != null)
      return _displayName;

    return _name;
  }

  @Override
  public ItemStack makeRepresentative(InterpretationEnvironment baseEnvironment, ConfigKeeper<MainSection> config) {
    return icon.build(
      baseEnvironment.copy()
        .withVariable("name", getDisplayNameOrName())
        .withVariable("description", description == null ? null : description.markupNode)
    );
  }
}
