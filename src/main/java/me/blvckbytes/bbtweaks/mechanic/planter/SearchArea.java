package me.blvckbytes.bbtweaks.mechanic.planter;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import me.blvckbytes.bbtweaks.util.CompactId;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class SearchArea {

    private final Location center;
    private final int radius;

    public SearchArea(Location center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public @Nullable Block tryGetRandomBlockInArea() {
        var xMin = Math.min(center.getBlockX() - radius, center.getBlockX() + radius);
        var xMax = Math.max(center.getBlockX() - radius, center.getBlockX() + radius);
        var yMin = Math.min(center.getBlockY() - radius, center.getBlockY() + radius);
        var yMax = Math.max(center.getBlockY() - radius, center.getBlockY() + radius);
        var zMin = Math.min(center.getBlockZ() - radius, center.getBlockZ() + radius);
        var zMax = Math.max(center.getBlockZ() - radius, center.getBlockZ() + radius);

        var random = ThreadLocalRandom.current();

        var x = xMin + random.nextInt(xMax - xMin + 1);
        var y = yMin + random.nextInt(yMax - yMin + 1);
        var z = zMin + random.nextInt(zMax - zMin + 1);

        var location = new Location(center.getWorld(), x, y, z);

        if(isOutsideOfArea(location))
            return null;

        return location.getBlock();
    }

    public List<Entity> getEntitiesInArea() {
        var entities = new ArrayList<Entity>();

        forEachLoadedChunkInArea(chunk -> {
            for (var entity : chunk.getEntities()) {
                if (!entity.isValid() || isOutsideOfArea(entity.getLocation()))
                    continue;

                entities.add(entity);
            }
        });

        return entities;
    }

    public boolean isOutsideOfArea(Location location) {
        var distanceToCenter = getMaxAxisBlockDistance(center, location);
        return distanceToCenter > radius * radius;
    }

    private int getMaxAxisBlockDistance(Location a, Location b) {
        if(!a.getWorld().equals(b.getWorld()))
            return Integer.MAX_VALUE;

        var deltaX = Math.abs(a.getBlockX() - b.getBlockX());
        var deltaY = Math.abs(a.getBlockY() - b.getBlockY());
        var deltaZ = Math.abs(a.getBlockZ() - b.getBlockZ());

        if (deltaX >= deltaY && deltaX >= deltaZ)
            return deltaX;

        return Math.max(deltaY, deltaZ);
    }

    private void forEachLoadedChunkInArea(Consumer<Chunk> handler) {
        var chunkRadiusX = radius < 16 ? 1 : radius / 16;
        var chunkRadiusZ = radius < 16 ? 1 : radius / 16;

        var world = center.getWorld();
        var seenChunkIds = new LongArraySet();

        for (var dx = -chunkRadiusX; dx <= chunkRadiusX; dx++) {
            for (var dz = -chunkRadiusZ; dz <= chunkRadiusZ; dz++) {
                var chunkX = (center.getBlockX() + dx * 16) >> 4;
                var chunkZ = (center.getBlockZ() + dz * 16) >> 4;

                if (!seenChunkIds.add(CompactId.computeWorldlessChunkId(chunkX, chunkZ)))
                    continue;

                if (!world.isChunkLoaded(chunkX, chunkZ))
                    continue;

                handler.accept(world.getChunkAt(chunkX, chunkZ));
            }
        }
    }
}