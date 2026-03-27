package me.blvckbytes.bbtweaks.newbie_teleport;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NewbieTeleportCommand implements CommandExecutor, TabCompleter {

  private final ConfigKeeper<MainSection> config;

  public final NamespacedKey useCountKey;

  public NewbieTeleportCommand(Plugin plugin, ConfigKeeper<MainSection> config) {
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

    var safeTarget = findSafeTeleportLocation(world, x, z);

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

  private static Location findSafeTeleportLocation(World world, int x, int z) {
    for (var y = world.getHighestBlockYAt(x, z); y >= world.getMinHeight(); y--) {
      var currentBlock = world.getBlockAt(x, y, z);

      if (isUnsafeGround(currentBlock))
        continue;

      if (isUnsafeToPass(currentBlock.getRelative(0, 1, 0)))
        continue;

      if (isUnsafeToPass(currentBlock.getRelative(0, 2, 0)))
        continue;

      return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    return null;
  }

  private static boolean isUnsafeGround(Block block) {
    if (!block.isSolid())
      return true;

    var type = block.getType();

    return type == Material.CACTUS || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE || type == Material.MAGMA_BLOCK;
  }

  private static boolean isUnsafeToPass(Block block) {
    if (!block.isPassable())
      return true;

    var type = block.getType();

    return type == Material.LAVA || type == Material.WATER;
  }
}
