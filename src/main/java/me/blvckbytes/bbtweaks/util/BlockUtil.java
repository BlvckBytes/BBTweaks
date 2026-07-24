package me.blvckbytes.bbtweaks.util;

import me.blvckbytes.bbtweaks.mechanic.MechanicSignInfo;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  public static @Nullable Inventory tryAccessBlockInventory(Block block) {
    var blockData = block.getBlockData();

    // Make sure that both halves are loaded as otherwise, paper may just return an
    // inventory with half the size - yes, actually, despite calling #getState and
    // accessing the live and block-backed inventory, instead of the snapshot.
    if (blockData instanceof Chest chest) {
      var otherChestBlock = BlockUtil.getOtherChestBlock(block, chest.getType(), chest.getFacing());

      if (otherChestBlock != null && !isBlockLoaded(otherChestBlock))
        otherChestBlock.getWorld().loadChunk(otherChestBlock.getX() >> 4, otherChestBlock.getZ() >> 4);
    }

    if (!(block.getState(false) instanceof Container container))
      return null;

    return container.getInventory();
  }

  public static List<Block> getInventoryBackingBlocks(Inventory inventory) {
    if (inventory instanceof DoubleChestInventory doubleInventory) {
      var result = new ArrayList<Block>();

      if (doubleInventory.getRightSide().getHolder(false) instanceof Container rightContainer)
        result.add(rightContainer.getBlock());

      if (doubleInventory.getLeftSide().getHolder(false) instanceof Container leftContainer)
        result.add(leftContainer.getBlock());

      return result;
    }

    if (inventory.getHolder(false) instanceof Container container)
      return Collections.singletonList(container.getBlock());

    return Collections.emptyList();
  }
}
