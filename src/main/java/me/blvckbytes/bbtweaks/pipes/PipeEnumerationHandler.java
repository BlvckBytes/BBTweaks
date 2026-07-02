package me.blvckbytes.bbtweaks.pipes;

import org.bukkit.block.Block;

@FunctionalInterface
public interface PipeEnumerationHandler {

    EnumerationDecision handle(Block pipeBlock, int cachedPipeBlock, CachedBlockResolver cache) throws LoadingChunkException;

}
