package me.blvckbytes.bbtweaks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayDeque;
import java.util.HashSet;

public class LavaSponge implements Listener {

  private static final int MAX_DISTANCE = 5;
  private static final int MAX_BLOCKS = 60;

  private static final BlockFace[] DIRECT_FACES = {
    BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
    BlockFace.UP, BlockFace.DOWN
  };

  private record Node(Block block, int originDistance) {}

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() != Material.WET_SPONGE)
      return;

    if (!event.getPlayer().hasPermission("bbtweaks.lava-sponge"))
      return;

    if (drain(event.getBlock()))
      event.getBlock().setType(Material.SPONGE);
  }

  private static boolean drain(Block origin) {
    var enumerationQueue = new ArrayDeque<Node>();
    var visited = new HashSet<Block>();

    int removed = 0;

    enumerationQueue.add(new Node(origin, 0));
    visited.add(origin);

    while (!enumerationQueue.isEmpty()) {
      Node currentOrigin = enumerationQueue.poll();

      if (currentOrigin.originDistance > MAX_DISTANCE)
        continue;

      for (var face : DIRECT_FACES) {
        Block neighbor = currentOrigin.block.getRelative(face);

        if (!visited.add(neighbor))
          continue;

        var type = neighbor.getType();

        if (type != Material.LAVA)
          continue;

        neighbor.setType(Material.AIR);

        if (++removed >= MAX_BLOCKS)
          return true;

        enumerationQueue.add(new Node(neighbor, currentOrigin.originDistance + 1));
      }
    }

    return removed > 0;
  }
}
