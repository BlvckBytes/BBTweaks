package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class PasswordCommand implements CommandHandler {

  private final PluginCommand command;
  private final HiddenSwitchMechanic mechanic;
  private final ConfigKeeper<MainSection> config;

  public PasswordCommand(
    JavaPlugin plugin,
    HiddenSwitchMechanic mechanic,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(PasswordCommandSection.INITIAL_NAME));

    this.mechanic = mechanic;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.mechanic.hiddenSwitch.commandPlayerOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.mechanic.hidden-switch")) {
      config.rootSection.mechanic.hiddenSwitch.noPermission.sendMessage(player);
      return true;
    }

    var passwordBuilder = new StringJoiner(" ");

    for (String arg : args)
      passwordBuilder.add(arg);

    var password = passwordBuilder.toString();

    if (password.isBlank()) {
      config.rootSection.mechanic.hiddenSwitch.commandPasswordMissingValue.sendMessage(player);
      return true;
    }

    var passwordResult = mechanic.onPasswordInput(player, password);

    if (passwordResult == PasswordResult.NO_ACTIVE_PROMPT) {
      config.rootSection.mechanic.hiddenSwitch.commandPasswordNoPrompt.sendMessage(player);
      return true;
    }

    if (passwordResult == PasswordResult.WRONG_PASSWORD) {
      config.rootSection.mechanic.hiddenSwitch.commandPasswordWrongPassword.sendMessage(player);
      return true;
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.mechanic.hiddenSwitch.passwordCommand;
  }
}
