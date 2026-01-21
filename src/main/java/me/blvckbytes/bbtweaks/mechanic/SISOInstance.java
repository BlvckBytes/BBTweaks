package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.*;
import org.bukkit.block.data.AnaloguePowerable;
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

    var blockData = inputBlock.getBlockData();
    var blockMaterial = blockData.getMaterial();

    if (isPowerBlock(blockMaterial))
      return 15;

    if (blockData instanceof Directional directional && !isDirectionalInvariant(blockMaterial)) {
      // I have no idea why the facing always points the opposite way - that's just what experiments showed.
      var outputFacing = directional.getFacing().getOppositeFace();

      // Whatever could be powering this input is not facing the sign's way - disregard it.
      if (!inputBlock.getRelative(outputFacing).equals(sign.getBlock()))
        return 0;
    }

    if (blockData instanceof Powerable powerable)
      return powerable.isPowered() ? 15 : 0;

    if (blockData instanceof AnaloguePowerable analoguePowerable)
      return analoguePowerable.getPower();

    // Exclude blocks like glass and other see-through variants
    if (blockMaterial.isAir() || !blockMaterial.isSolid() || !blockMaterial.isOccluding())
      return 0;

    return inputBlock.getBlockPower();
  }

  private boolean isDirectionalInvariant(Material type) {
    return switch (type) {
      case LEVER, TRIPWIRE_HOOK, TRIPWIRE, LECTERN -> true;
      default -> Tag.BUTTONS.isTagged(type);
    };
  }

  private boolean isPowerBlock(Material type) {
    return switch (type) {
      case REDSTONE_BLOCK, REDSTONE_TORCH, REDSTONE_WALL_TORCH -> true;
      default -> false;
    };
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
