package me.blvckbytes.bbtweaks.private_vaults;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrivateVaultCommand implements CommandExecutor, TabCompleter {

  private final PrivateVaultManager vaultManager;
  private final ConfigKeeper<MainSection> config;

  public PrivateVaultCommand(
    PrivateVaultManager vaultManager,
    ConfigKeeper<MainSection> config
  ) {
    this.vaultManager = vaultManager;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.privateVaults.playersOnly.sendMessage(sender);
      return true;
    }

    var result = vaultManager.tryOpenVaultInventory(player, player);

    var environment = new InterpretationEnvironment()
      .withVariable("name", player.getName());

    switch (result) {
      case OWNER_CANNOT_ACCESS_ANY_ROWS -> config.rootSection.privateVaults.vaultHasNoActiveRowsSelf.sendMessage(player, environment);
      case NOT_EXISTING_AND_OWNER_NOT_ONLINE -> config.rootSection.privateVaults.vaultNotExistingAndOwnerNotOnline.sendMessage(player, environment);
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }
}
