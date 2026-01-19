package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Powerable;
import org.jetbrains.annotations.Nullable;

public abstract class SISOInstance implements MechanicInstance {

  private static final BlockFace[] DIRECT_FACES = new BlockFace[] {
    BlockFace.UP, BlockFace.DOWN,
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  protected final Block signBlock;
  protected final Block mountBlock;
  protected final Block inputBlock;
  protected final BlockFace signFacing;
  protected final World world;

  private @Nullable Block cachedOutputBlock;
  private @Nullable Powerable cachedOutputBlockData;

  private boolean lastOutputState;

  public SISOInstance(Block signBlock, BlockFace signFacing) {
    this.signBlock = signBlock;
    this.mountBlock = signBlock.getRelative(signFacing.getOppositeFace());
    this.inputBlock = signBlock.getRelative(signFacing);
    this.signFacing = signFacing;
    this.world = signBlock.getWorld();
  }

  @Override
  public Block getSignBlock() {
    return signBlock;
  }

  protected @Nullable Integer tryReadInputPower() {
    // Undefined input-state
    if (!world.isChunkLoaded(inputBlock.getX() >> 4, inputBlock.getZ() >> 4))
      return null;

    return inputBlock.getBlockPower();
  }

  protected void tryWriteOutputState(boolean state) {
    if (state == lastOutputState)
      return;

    updateOutputBlock();

    if (cachedOutputBlock == null || cachedOutputBlockData == null)
      return;

    cachedOutputBlockData.setPowered(state);
    cachedOutputBlock.setBlockData(cachedOutputBlockData);

    lastOutputState = state;
  }

  protected boolean getLastOutputState() {
    return lastOutputState;
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
