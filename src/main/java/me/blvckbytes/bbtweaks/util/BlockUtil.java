package me.blvckbytes.bbtweaks.util;

import me.blvckbytes.bbtweaks.mechanic.MechanicSignInfo;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BlockUtil {

  public static boolean isBlockLoaded(Block block) {
    return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean areAllContainerBlocksLoaded(Block block, @Nullable BlockData blockData) {
    if (!isBlockLoaded(block))
      return false;

    if (blockData == null)
      blockData = block.getBlockData();

    if (blockData instanceof org.bukkit.block.data.type.Chest chest) {
      var otherChestBlock = BlockUtil.getOtherChestBlock(block, chest.getType(), chest.getFacing());
      return otherChestBlock == null || isBlockLoaded(otherChestBlock);
    }

    return true;
  }

  public static @Nullable Block getOtherChestBlock(Block chestBlock, Chest.Type chestType, BlockFace chestFacing) {
    if (chestType == Chest.Type.SINGLE)
      return null;

    int dx = 0, dz = 0;

    // Left and right are relative to the chest itself, i.e. opposite to what
    // a player placing the appropriate block would see.

    switch (chestFacing) {
      case NORTH: // -z
        dx = (chestType == Chest.Type.LEFT) ? 1 : -1;
        break;
      case SOUTH: // +z
        dx = (chestType == Chest.Type.LEFT) ? -1 : 1;
        break;
      case EAST: // +x
        dz = (chestType == Chest.Type.LEFT) ? 1 : -1;
        break;
      case WEST: // -x
        dz = (chestType == Chest.Type.LEFT) ? -1 : 1;
        break;
    }

    return chestBlock.getRelative(dx, 0, dz);
  }

  public static void teleportPlayerToSign(Player player, Sign sign) {
    var signInfo = MechanicSignInfo.createFromSign(sign);
    var signLocation = sign.getLocation();

    var signCenter = signLocation.clone();
    var footLocation = signLocation.clone();

    switch (signInfo.signFacing()) {
      case NORTH -> {
        footLocation.add(.5, 0, -.1);
        signCenter.add(.5, .5, .9);
      }

      case SOUTH -> {
        footLocation.add(.5, 0, 1);
        signCenter.add(.5, .5, 0);
      }

      case WEST -> {
        footLocation.add(-.1, 0, .5);
        signCenter.add(.9, .5, .5);
      }

      case EAST -> {
        footLocation.add(1, 0, .5);
        signCenter.add(0, .5, .5);
      }
    }

    var eyeLocation = footLocation.clone().add(0, 1.6, 0);
    var direction = signCenter.toVector().subtract(eyeLocation.toVector()).normalize();
    footLocation.setDirection(direction);

    player.teleport(footLocation);
  }
}
