package me.blvckbytes.bbtweaks.lava_sponge;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;

public class LavaSponge implements Listener {

  private static final BlockFace[] DIRECT_FACES = {
    BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
    BlockFace.UP, BlockFace.DOWN
  };

  private record Node(Block block, int originDistance) {}

  private final ConfigKeeper<MainSection> config;

  public LavaSponge(ConfigKeeper<MainSection> config) {
    this.config = config;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() != Material.WET_SPONGE)
      return;

    var limits = getLimits(event.getPlayer());

    if (limits == null)
      return;

    if (drain(event.getBlock(), limits))
      event.getBlock().setType(Material.SPONGE);
  }

  private static boolean drain(Block origin, LimitsSection limits) {
    var enumerationQueue = new ArrayDeque<Node>();
    var visited = new HashSet<Block>();

    int removed = 0;

    enumerationQueue.add(new Node(origin, 0));
    visited.add(origin);

    while (!enumerationQueue.isEmpty()) {
      Node currentOrigin = enumerationQueue.poll();

      if (currentOrigin.originDistance > limits.maxDistance)
        continue;

      for (var face : DIRECT_FACES) {
        Block neighbor = currentOrigin.block.getRelative(face);

        if (!visited.add(neighbor))
          continue;

        var type = neighbor.getType();

        if (type != Material.LAVA)
          continue;

        neighbor.setType(Material.AIR);

        if (++removed >= limits.maxBlocks)
          return true;

        enumerationQueue.add(new Node(neighbor, currentOrigin.originDistance + 1));
      }
    }

    return removed > 0;
  }

  private @Nullable LimitsSection getLimits(Player player) {
    for (var limit : config.rootSection.lavaSponge.limitsDescending) {
      if (player.hasPermission("bbtweaks.lava-sponge." + limit.tierName))
        return limit;
    }

    return null;
  }
}
