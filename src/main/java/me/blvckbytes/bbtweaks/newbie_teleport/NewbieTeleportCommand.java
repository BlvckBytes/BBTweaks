package me.blvckbytes.bbtweaks.newbie_teleport;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.util.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class NewbieTeleportCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;

  public final NamespacedKey useCountKey;

  public NewbieTeleportCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(NewbieTeleportCommandSection.INITIAL_NAME));
    this.config = config;

    this.useCountKey = new NamespacedKey(plugin, "newbie-teleport-use-count");
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.newbieTeleport.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.newbieteleport")) {
      config.rootSection.newbieTeleport.missingPermissionMainCommand.sendMessage(sender);
      return true;
    }

    var pdc = player.getPersistentDataContainer();

    var useCount = pdc.get(useCountKey, PersistentDataType.INTEGER);

    if (useCount == null)
      useCount = 0;

    var useLimit = config.rootSection.newbieTeleport.useCountLimit;

    if (useCount >= useLimit) {
      config.rootSection.newbieTeleport.noMoreUsesAvailable.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("limit", useLimit)
      );

      return true;
    }

    var world = player.getWorld();

    if (!config.rootSection.newbieTeleport._worldsLower.contains(world.getName().toLowerCase())) {
      config.rootSection.newbieTeleport.unsupportedWorld.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("world", world.getName())
          .withVariable("supported_worlds", config.rootSection.newbieTeleport.worlds)
      );

      return true;
    }

    if (args.length != 2) {
      config.rootSection.newbieTeleport.usageMainCommand.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("remaining_uses", useLimit - useCount)
      );

      return true;
    }

    Integer x, z;

    if ((x = tryParseInteger(args[0])) == null) {
      config.rootSection.newbieTeleport.malformedCoordinate.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("axis", "x")
          .withVariable("value", args[0])
      );

      return true;
    }

    if ((z = tryParseInteger(args[1])) == null) {
      config.rootSection.newbieTeleport.malformedCoordinate.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("axis", "z")
          .withVariable("value", args[1])
      );

      return true;
    }

    var border = world.getWorldBorder();

    if (!border.isInside(new Location(world, x, 0, z))) {
      var halfSize = border.getSize() / 2.0;
      var centerX = border.getCenter().getX();
      var centerZ = border.getCenter().getZ();

      config.rootSection.newbieTeleport.outsideOfWorldBorder.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("x", x)
          .withVariable("z", z)
          .withVariable("min_x", (int) (centerX - halfSize))
          .withVariable("max_x", (int) (centerX + halfSize))
          .withVariable("min_z", (int) (centerZ - halfSize))
          .withVariable("max_z", (int) (centerZ + halfSize))
      );

      return true;
    }

    var safeTarget = TeleportUtil.findSafeTeleportLocation(world, x, z);

    if (safeTarget == null) {
      config.rootSection.newbieTeleport.unsafeDestination.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("x", x)
          .withVariable("z", z)
      );

      return true;
    }

    var disallowedRegion = findDisallowedTargetRegion(safeTarget);

    if (disallowedRegion != null) {
      config.rootSection.newbieTeleport.disallowedRegion.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("x", x)
          .withVariable("z", z)
          .withVariable("region", disallowedRegion.getId())
      );

      return true;
    }

    pdc.set(useCountKey, PersistentDataType.INTEGER, ++useCount);

    player.teleport(safeTarget);

    config.rootSection.newbieTeleport.successfulTeleport.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("x", safeTarget.getBlockX())
        .withVariable("y", safeTarget.getBlockY())
        .withVariable("z", safeTarget.getBlockZ())
        .withVariable("use_count", useCount)
        .withVariable("use_limit", useLimit)
    );

    return true;
  }

  private @Nullable ProtectedRegion findDisallowedTargetRegion(Location location) {
    var regionManager = WorldGuard.getInstance().getPlatform()
      .getRegionContainer()
      .get(BukkitAdapter.adapt(location.getWorld()));

    if (regionManager == null)
      return null;

    for (var region : regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location))) {
      var nameLower = region.getId().toLowerCase();

      if (!config.rootSection.newbieTeleport._allowedRegionsLower.contains(nameLower))
        return region;
    }

    return null;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  private @Nullable Integer tryParseInteger(String input) {
    try {
      return Integer.parseInt(input);
    } catch (Throwable e) {
      return null;
    }
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.newbieTeleport.mainCommand;
  }
}
