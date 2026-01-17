package me.blvckbytes.bbtweaks.mechanic.clock;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Powerable;
import org.jetbrains.annotations.Nullable;

public class ClockInstance {

  private static final BlockFace[] DIRECT_FACES = new BlockFace[] {
    BlockFace.UP, BlockFace.DOWN,
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private final int toggleDuration;

  private final Block mountBlock;
  private final Block inputBlock;
  private final BlockFace signFacing;
  private final World world;

  private @Nullable Block cachedOutputBlock;
  private @Nullable Powerable cachedOutputBlockData;

  private boolean lastOutputState;

  public ClockInstance(int periodDuration, Block signBlock, BlockFace signFacing) {
    this.mountBlock = signBlock.getRelative(signFacing.getOppositeFace());
    this.inputBlock = signBlock.getRelative(signFacing);
    this.signFacing = signFacing;
    this.world = signBlock.getWorld();

    this.toggleDuration = periodDuration / 2;
  }

  public void tick(int time) {
    if (time % toggleDuration != 0)
      return;

    // Undefined state of activation - halt operation until the chunk becomes available again
    if (!world.isChunkLoaded(inputBlock.getX() >> 4, inputBlock.getZ() >> 4))
      return;

    if (inputBlock.getBlockPower() == 0) {
      if (lastOutputState)
        tryWriteOutputState(false);

      return;
    }

    tryWriteOutputState(!lastOutputState);
  }

  private void tryWriteOutputState(boolean state) {
    updateOutputBlock();

    if (cachedOutputBlock == null || cachedOutputBlockData == null)
      return;

    cachedOutputBlockData.setPowered(state);
    cachedOutputBlock.setBlockData(cachedOutputBlockData);

    lastOutputState = state;
  }

  private boolean isValidLeverBlock(Block block) {
    if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4))
      return false;

    return block.getType() == Material.LEVER;
  }

  private void updateOutputBlock() {
    if (cachedOutputBlock != null) {
      if (isValidLeverBlock(cachedOutputBlock))
        return;

      cachedOutputBlock = null;
    }

    for (var face : DIRECT_FACES) {
      if (face == signFacing)
        continue;

      var possibleOutputBlock = mountBlock.getRelative(face);

      if (isValidLeverBlock(possibleOutputBlock)) {
        cachedOutputBlock = possibleOutputBlock;
        cachedOutputBlockData = (Powerable) possibleOutputBlock.getBlockData();
        return;
      }
    }
  }
}
