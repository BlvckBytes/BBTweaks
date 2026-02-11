package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

public class MarkerSection extends ConfigSection implements MarkerDisplayItem {

  public @CSAlways ItemStackSection icon;
  public @Nullable LocationSection location;

  public @Nullable ComponentMarkup customCommand;
  public @Nullable ComponentMarkup displayName;

  @CSIgnore
  private Component _displayName;

  @CSIgnore
  public @Nullable String _command;

  @CSIgnore
  public String _name;

  public @Nullable ComponentMarkup description;

  public MarkerSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (customCommand != null)
      _command = customCommand.asPlainString(null);

    if (displayName != null)
      _displayName = displayName.interpret(SlotType.SINGLE_LINE_CHAT, null).get(0);
  }

  public Object getDisplayNameOrName() {
    if (_displayName != null)
      return _displayName;

    return _name;
  }

  public void setLocation(Location bukkitLocation) {
    if (location == null)
      location = new LocationSection(baseEnvironment, interpreterLogger);

    var world = bukkitLocation.getWorld();

    if (world == null)
      world = Objects.requireNonNull(Bukkit.getWorld("world"));

    location.x = bukkitLocation.getX();
    location.y = bukkitLocation.getY();
    location.z = bukkitLocation.getZ();
    location.yaw = bukkitLocation.getYaw();
    location.pitch = bukkitLocation.getPitch();
    location.world = world.getName();
    location._location = bukkitLocation;
  }

  @Override
  public ItemStack makeRepresentative(InterpretationEnvironment baseEnvironment, ConfigKeeper<MainSection> config) {
    var environment = baseEnvironment.copy()
      .withVariable("has_location", location != null)
      .withVariable("command", _command)
      .withVariable("name", getDisplayNameOrName())
      .withVariable("description", description == null ? null : description.markupNode);

    if (location != null) {
      environment
        .withVariable("x", (int) location.x)
        .withVariable("y", (int) location.y)
        .withVariable("z", (int) location.z)
        .withVariable("world", location.world);
    }

    return icon.build(environment);
  }
}
