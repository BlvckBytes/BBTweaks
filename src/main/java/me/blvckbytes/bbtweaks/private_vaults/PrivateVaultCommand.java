package me.blvckbytes.bbtweaks.private_vaults;

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

  public PrivateVaultCommand(PrivateVaultManager vaultManager) {
    this.vaultManager = vaultManager;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      // TODO: Config-message
      sender.sendMessage("§cPlayers only");
      return true;
    }

    var result = vaultManager.tryOpenVaultInventory(player, player);

    switch (result) {
      case OWNER_CANNOT_ACCESS_ANY_ROWS -> player.sendMessage("§cThis vault does not have access to any rows which could be displayed!");
      case NOT_EXISTING_AND_OWNER_NOT_ONLINE -> player.sendMessage("§cThe owner has not yet accessed and thereby created their vault!");
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }
}
