package me.blvckbytes.bbtweaks.pipes;

import org.bukkit.Chunk;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkTicket {

    private @Nullable Chunk chunk;
    private long expiryTicksStamp;

    public void setChunk(Plugin plugin, @NotNull Chunk chunk, long relativeTime, ChunkLoadReason loadReason) {
        if (this.chunk != null)
            throw new IllegalStateException("Tried to override an already set chunk");

        this.chunk = chunk;
        this.expiryTicksStamp = relativeTime + loadReason.expiryTimeTicks;

        if (!chunk.addPluginChunkTicket(plugin))
            plugin.getLogger().warning("Could not add plugin-ticket to chunk at " + chunk.getX() + " " + chunk.getZ());
    }

    public boolean handleExpiration(Plugin plugin, int ticksNow, boolean force) {
        if (this.chunk == null)
            return false;

        if (!force && ticksNow < expiryTicksStamp)
            return false;

        if (!this.chunk.removePluginChunkTicket(plugin))
            plugin.getLogger().warning("Could not remove plugin-ticket from chunk at " + chunk.getX() + " " + chunk.getZ());

        this.chunk = null;
        return true;
    }
}
