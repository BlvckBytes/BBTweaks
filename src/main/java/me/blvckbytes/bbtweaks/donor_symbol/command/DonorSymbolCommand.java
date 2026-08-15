package me.blvckbytes.bbtweaks.donor_symbol.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.donor_symbol.main_display.DonorSymbolDisplayHandler;
import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfileStore;
import me.blvckbytes.bbtweaks.util.PlayerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class DonorSymbolCommand implements CommandHandler {

  private final PluginCommand command;

  private final ConfigKeeper<MainSection> config;
  private final DonorSymbolProfileStore profileStore;
  private final DonorSymbolDisplayHandler displayHandler;

  public DonorSymbolCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    DonorSymbolProfileStore profileStore,
    DonorSymbolDisplayHandler displayHandler
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(DonorSymbolCommandSection.INITIAL_NAME));

    this.config = config;
    this.profileStore = profileStore;
    this.displayHandler = displayHandler;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.donorSymbol.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.donorSymbol.command.playersOnly.sendMessage(sender);
      return true;
    }

    if (!hasCommandPermission(player)) {
      config.rootSection.donorSymbol.command.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      displayHandler.show(player, profileStore.accessProfile(player));
      return true;
    }

    if (!hasCommandSubPermission(player, "others")) {
      config.rootSection.donorSymbol.command.noPermissionOthers.sendMessage(sender);
      return true;
    }

    if (args.length != 1) {
      config.rootSection.donorSymbol.command.usageOthers.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return true;
    }

    var targetPlayer = PlayerUtil.getPlayerByName(args[0]);

    if (targetPlayer == null) {
      config.rootSection.donorSymbol.command.playerNotOnline.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", args[0])
      );

      return true;
    }

    displayHandler.show(player, profileStore.accessProfile(targetPlayer));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return List.of();

    if (hasCommandSubPermission(player, "others") && args.length == 1)
      return PlayerUtil.suggestPlayerNames(args[0], null);

    return List.of();
  }
}
