package me.blvckbytes.bbtweaks.mechanic.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanicFlag;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandSignMechanic extends BaseMechanic<CommandSignInstance> {

  private static final int COMMAND_LINE_ID = 2;

  public CommandSignMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config, BaseMechanicFlag.RELAY_BUTTON);
  }

  @Override
  public boolean onInstanceClick(Player player, CommandSignInstance instance, boolean wasLeftClick) {
    if (wasLeftClick)
      return false;

    plugin.getLogger().info(player.getName() + " used a command-sign: /" + instance.command);
    player.performCommand(instance.command);
    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Command");
  }

  @Override
  public @Nullable CommandSignInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.command")) {
      config.rootSection.mechanic.commandSign.noPermission.sendMessage(creator);
      return null;
    }

    var command = trimWhitespaceAndLeadingSlashes(SignUtil.getPlainTextLine(sign, COMMAND_LINE_ID));

    if (command.isEmpty()) {
      if (creator != null)
        config.rootSection.mechanic.commandSign.missingCommand.sendMessage(creator);

      return null;
    }

    var instance = new CommandSignInstance(sign, command);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.commandSign.creationSuccess.sendMessage(
        creator,
        getSignEnvironment(sign)
          .withVariable("command", command)
      );

    return instance;
  }

  private String trimWhitespaceAndLeadingSlashes(String input) {
    input = input.trim();

    while (input.startsWith("/"))
      input = input.substring(1).trim();

    return input;
  }
}
