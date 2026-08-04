package me.blvckbytes.bbtweaks.custom_commands;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegisterAdditionalCustomCommandsEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  private final List<CustomCommand> commands;

  public RegisterAdditionalCustomCommandsEvent() {
    this.commands = new ArrayList<>();
  }

  public void addCommand(CustomCommand command) {
    this.commands.add(command);
  }

  public List<CustomCommand> getCommands() {
    return Collections.unmodifiableList(commands);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return handlers;
  }
}
