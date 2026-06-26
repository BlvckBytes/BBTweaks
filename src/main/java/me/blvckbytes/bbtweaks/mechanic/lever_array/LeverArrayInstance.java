package me.blvckbytes.bbtweaks.mechanic.lever_array;

import at.blvckbytes.component_markup.util.TriState;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.blvckbytes.bbtweaks.mechanic.SISOFlag;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.util.CompactId;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Powerable;
import org.jetbrains.annotations.Nullable;

public class LeverArrayInstance extends SISOInstance {

  private static final int MAX_EXTENT = 64;

  private final int propagationSpeedEnable;
  private final int propagationSpeedDisable;

  private final Long2ObjectMap<TriState> lastKnownPowerStateByCompactId;

  private long lastWriteStamp;
  private int deltaLimit;

  public LeverArrayInstance(
    Sign sign,
    int propagationSpeedEnable,
    int propagationSpeedDisable
  ) {
    super(sign, SISOFlag.ALLOW_OUTPUT_ON_SIGN_PLANE);

    this.propagationSpeedEnable = propagationSpeedEnable;
    this.propagationSpeedDisable = propagationSpeedDisable;

    this.lastKnownPowerStateByCompactId = new Long2ObjectArrayMap<>();
  }

  @Override
  public boolean tick(long time) {
    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    var newState = inputPower > 0;

    var didChangeOutput = tryWriteOutputState(newState);
    var cachedOutputBlock = getCachedOutputBlock();

    if (cachedOutputBlock == null)
      return true;

    var maximumDelta = MAX_EXTENT - 1;
    var propagationSpeed = newState ? propagationSpeedEnable : propagationSpeedDisable;

    if (didChangeOutput) {
      lastWriteStamp = time;
      deltaLimit = propagationSpeed <= 0 ? maximumDelta : 0;
    }

    else if (propagationSpeed > 0) {
      if ((time - lastWriteStamp) % propagationSpeed == 0) {
        if (++deltaLimit > maximumDelta)
          deltaLimit = 0;
      }
    }

    var walkingDirection = determineFaceToGetFromSignTo(cachedOutputBlock);

    if (walkingDirection == null)
      return true;

    for (var delta = 1; delta <= deltaLimit; ++delta) {
      var nextBlock = cachedOutputBlock.getRelative(walkingDirection, delta);
      var compactId = CompactId.computeWorldlessBlockId(nextBlock);

      var lastKnownState = lastKnownPowerStateByCompactId.getOrDefault(compactId, TriState.NULL);

      if (lastKnownState.bool != null && lastKnownState.bool == newState)
        continue;

      if (!isBlockLoaded(nextBlock))
        break;

      if (!(nextBlock.getBlockData() instanceof Powerable powerable))
        break;

      if (powerable.getMaterial() != Material.LEVER)
        break;

      powerable.setPowered(newState);
      nextBlock.setBlockData(powerable);

      tryForceStateOnMountBlockOf(nextBlock, powerable, newState);

      lastKnownPowerStateByCompactId.put(compactId, newState ? TriState.TRUE : TriState.FALSE);
    }

    return true;
  }

  // I'm not feeling it right now to be smart about it - so brute-force it is; super cheap anyway.
  private @Nullable BlockFace determineFaceToGetFromSignTo(Block block) {
    var signX = mountBlock.getX() + signFacing.getModX();
    var signY = mountBlock.getY() + signFacing.getModY();
    var signZ = mountBlock.getZ() + signFacing.getModZ();

    var outputX = block.getX();
    var outputY = block.getY();
    var outputZ = block.getZ();

    for (var face : SISOInstance.DIRECT_FACES) {
      if (signX + face.getModX() != outputX)
        continue;

      if (signY + face.getModY() != outputY)
        continue;

      if (signZ + face.getModZ() != outputZ)
        continue;

      return face;
    }

    return null;
  }
}
