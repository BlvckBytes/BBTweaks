package me.blvckbytes.bbtweaks.mechanic.util;

import io.papermc.paper.math.BlockPosition;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PositionOfBlock implements BlockPosition, Comparable<PositionOfBlock> {

  public final Block block;

  public PositionOfBlock(Block block) {
    this.block = block;
  }

  @Override
  public int blockX() {
    return block.getX();
  }

  @Override
  public int blockY() {
    return block.getY();
  }

  @Override
  public int blockZ() {
    return block.getZ();
  }

  @Override
  public int compareTo(@NotNull PositionOfBlock other) {
    int result;

    if ((result = Integer.compare(blockX(), other.blockX())) != 0)
      return result;

    if ((result = Integer.compare(blockY(), other.blockY())) != 0)
      return result;

    return Integer.compare(blockZ(), other.blockZ());
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof PositionOfBlock other)
      return other.blockX() == blockX() && other.blockY() == blockY() && other.blockZ() == blockZ();

    return false;
  }

  @Override
  public int hashCode() {
    return 31 * (31 * blockX() + blockY()) + blockZ();
  }
}
