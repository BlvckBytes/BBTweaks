package me.blvckbytes.bbtweaks.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

public class TeleportUtil {

  public static @Nullable Location findSafeTeleportLocation(World world, int x, int z) {
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
