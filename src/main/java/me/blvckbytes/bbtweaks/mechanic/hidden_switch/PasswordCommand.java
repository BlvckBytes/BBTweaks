package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.StringJoiner;

public class PasswordCommand implements CommandExecutor, TabCompleter {

  private final HiddenSwitchMechanic mechanic;
  private final ConfigKeeper<MainSection> config;

  public PasswordCommand(
    HiddenSwitchMechanic mechanic,
    ConfigKeeper<MainSection> config
  ) {
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
}
