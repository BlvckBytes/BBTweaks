package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.block.Block;

public interface MechanicInstance {

  Block getSignBlock();

  /**
   * @return Whether the tick was successful; unsuccessful ticks will result in self-destruction.
   */
  boolean tick(int time);

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  default boolean isBlockLoaded(Block block) {
    var signBlock = getSignBlock();

    var signChunkX = signBlock.getX() >> 4;
    var signChunkZ = signBlock.getZ() >> 4;

    var blockChunkX = block.getX() >> 4;
    var blockChunkZ = block.getZ() >> 4;

    // Fast-path, as we know that the chunk of the sign is always loaded. Sadly
    // enough, Bukkit's APIs aren't quick enough to make this obsolete.
    if (blockChunkX == signChunkX && blockChunkZ == signChunkZ)
      return true;

    return signBlock.getWorld().isChunkLoaded(blockChunkX, blockChunkZ);
  }
}
