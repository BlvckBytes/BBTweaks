package me.blvckbytes.bbtweaks.pipes.mechanic;

import org.bukkit.block.Block;

@FunctionalInterface
public interface PipeEnumerationHandler {

    EnumerationDecision handle(Block pipeBlock, int cachedPipeBlock, CachedBlockResolver cache) throws LoadingChunkException;

}
