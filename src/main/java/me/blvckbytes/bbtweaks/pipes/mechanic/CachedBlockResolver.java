package me.blvckbytes.bbtweaks.pipes.mechanic;

import org.bukkit.block.Block;

public interface CachedBlockResolver {

  int getCachedBlock(Block block) throws LoadingChunkException;

}
