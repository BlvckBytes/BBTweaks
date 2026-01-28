package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import org.bukkit.entity.Player;

public class VisualizationsBucket {

  private final Player player;
  private final ConfigKeeper<MainSection> config;
  private final Long2ObjectMap<FakeBlocksVisualization> fakeBlocksBySignBlockId;

  public VisualizationsBucket(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;
    this.fakeBlocksBySignBlockId = new Long2ObjectOpenHashMap<>();
  }

  public void update(int time) {
    var maxAgeTicks = config.rootSection.mechanic.magnet.visualization.durationMs / 50;

    for (var fakeBlocksIterator = fakeBlocksBySignBlockId.values().iterator(); fakeBlocksIterator.hasNext();) {
      var fakeBlocks = fakeBlocksIterator.next();
      var ageTicks = time - fakeBlocks.createdAt;

      if (ageTicks >= maxAgeTicks) {
        fakeBlocksIterator.remove();
        fakeBlocks.undo();
        continue;
      }

      fakeBlocks.update();
    }
  }

  public void add(MagnetInstance instance, int time) {
    var sign = instance.getSign();
    var blockId = CacheByPosition.computeWorldlessBlockId(sign.getX(), sign.getY(), sign.getZ());
    fakeBlocksBySignBlockId.put(blockId, new FakeBlocksVisualization(player, instance, time));
  }
}
