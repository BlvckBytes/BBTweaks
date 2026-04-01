package me.blvckbytes.bbtweaks.private_vaults;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
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
  private final ConfigKeeper<MainSection> config;

  public PrivateVaultViewCommand(
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

    if (!player.hasPermission("bbtweaks.private-vaults.other")) {
      config.rootSection.privateVaults.viewCommandNoPermission.sendMessage(sender);
      return true;
    }

    if (args.length != 1) {
      config.rootSection.privateVaults.viewCommandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return true;
    }

    var owner = Bukkit.getOfflinePlayer(args[0]);

    var environment = new InterpretationEnvironment()
      .withVariable("name", owner.getName());

    var result = vaultManager.tryOpenVaultInventory(player, owner);

    switch (result) {
      case OWNER_CANNOT_ACCESS_ANY_ROWS -> {
        if (player.getUniqueId().equals(owner.getUniqueId())) {
          config.rootSection.privateVaults.vaultHasNoActiveRowsSelf.sendMessage(player);
          return true;
        }

        config.rootSection.privateVaults.vaultHasNoActiveRowsOther.sendMessage(player, environment);
      }

      case NOT_EXISTING_AND_OWNER_NOT_ONLINE -> config.rootSection.privateVaults.vaultNotExistingAndOwnerNotOnline.sendMessage(player, environment);
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
