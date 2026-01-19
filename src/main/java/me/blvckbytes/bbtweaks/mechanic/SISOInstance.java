package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.jetbrains.annotations.Nullable;

public abstract class SISOInstance implements MechanicInstance {

  private static final BlockFace[] DIRECT_FACES = new BlockFace[] {
    BlockFace.UP, BlockFace.DOWN,
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  protected final Sign sign;
  protected final BlockFace signFacing;
  protected final Block mountBlock;
  protected final Block inputBlock;

  private @Nullable Block cachedOutputBlock;
  private @Nullable Powerable cachedOutputBlockData;

  private boolean lastOutputState;

  public SISOInstance(Sign sign) {
    this.sign = sign;
    this.signFacing = ((Directional) sign.getBlockData()).getFacing();
    this.mountBlock = sign.getBlock().getRelative(signFacing.getOppositeFace());
    this.inputBlock = sign.getBlock().getRelative(signFacing);
  }

  @Override
  public Sign getSign() {
    return sign;
  }

  protected @Nullable Integer tryReadInputPower() {
    // Undefined input-state
    if (!isBlockLoaded(inputBlock))
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
    if (!isBlockLoaded(block))
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
