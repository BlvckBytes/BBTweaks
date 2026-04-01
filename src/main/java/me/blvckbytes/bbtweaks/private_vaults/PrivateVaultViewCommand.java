package me.blvckbytes.bbtweaks.private_vaults;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrivateVaultViewCommand implements CommandExecutor, TabCompleter {

  private final PrivateVaultManager vaultManager;

  public PrivateVaultViewCommand(PrivateVaultManager vaultManager) {
    this.vaultManager = vaultManager;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      // TODO: Config-message
      sender.sendMessage("§cPlayers only");
      return true;
    }

    if (!player.hasPermission("bbtweaks.private-vaults.other")) {
      // TODO: Config-message
      player.sendMessage("§cYou cannot view the vaults of others");
      return true;
    }

    if (args.length != 1) {
      // TODO: Config-message
      player.sendMessage("§cUsage: /" + label + " [name]");
      return true;
    }

    var owner = Bukkit.getOfflinePlayer(args[0]);

    var result = vaultManager.tryOpenVaultInventory(player, owner);

    switch (result) {
      case OWNER_CANNOT_ACCESS_ANY_ROWS -> player.sendMessage("§cThis vault does not have access to any rows which could be displayed!");
      case NOT_EXISTING_AND_OWNER_NOT_ONLINE -> player.sendMessage("§cThe owner has not yet accessed and thereby created their vault!");
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (args.length == 1 && sender.hasPermission("bbtweaks.private-vaults.other"))
      return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> StringUtils.startsWithIgnoreCase(name, args[0])).limit(10).toList();

    return List.of();
  }
}
