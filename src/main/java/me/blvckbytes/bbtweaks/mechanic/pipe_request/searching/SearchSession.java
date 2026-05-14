package me.blvckbytes.bbtweaks.mechanic.pipe_request.searching;

import com.sk89q.craftbook.mechanics.pipe.*;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

public class SearchSession extends EnumerationSession<SearchSession> {

  private static final EnumSet<Material> allowedContainerTypes;

  static {
    allowedContainerTypes = EnumSet.noneOf(Material.class);

    allowedContainerTypes.addAll(Tag.SHULKER_BOXES.getValues());
    allowedContainerTypes.addAll(Tag.COPPER_CHESTS.getValues());
    allowedContainerTypes.add(Material.CHEST);
    allowedContainerTypes.add(Material.BARREL);
  }

  private final World world;

  private final List<SearchedInventory> searchedInventories;
  private final LongSet visitedBlocks;

  private int chunksWaitingOn;

  public SearchSession(
    Block origin, Pipes pipesMechanic, Plugin plugin,
    Consumer<SearchSession> warmupHandler,
    Consumer<SearchSession> completionHandler
  ) {
    super(origin, pipesMechanic, plugin, warmupHandler, completionHandler);

    this.world = origin.getWorld();
    this.searchedInventories = new ArrayList<>();
    this.visitedBlocks = new LongOpenHashSet();
  }

  public List<SearchedInventory> getSearchedInventories() {
    return searchedInventories;
  }

  @Override
  protected EnumerationDecision onTube(Block block, int cachedBlock) {
    return EnumerationDecision.CONTINUE;
  }

  @Override
  protected EnumerationDecision onPiston(Block block, int cachedBlock) {
    handleBlock(block.getRelative(CachedBlock.getFacing(cachedBlock)), false);

    return EnumerationDecision.CONTINUE;
  }

  @Override
  protected void beforeCompletion() {}

  @Override
  protected void beforeSubPipe() {}

  @Override
  protected void afterSubPipe() {}

  @Override
  protected EnumSet<EnumerationBehavior> getEnumerationBehavior() {
    return EnumSet.noneOf(EnumerationBehavior.class);
  }

  @Override
  protected void beforeRetry() {}

  @Override
  protected boolean isDone() {
    return chunksWaitingOn == 0;
  }

  private void handleBlock(Block block, boolean ignoreOtherChestHalf) {
    var blockId = CompactId.computeWorldlessBlockId(block);

    if (visitedBlocks.contains(blockId))
      return;

    ensureChunkIsLoaded(block, () -> {
      if (terminated)
        return;

      var blockData = block.getBlockData();

      visitedBlocks.add(blockId);

      if (!allowedContainerTypes.contains(blockData.getMaterial())) {
        callIfDone();
        return;
      }

      if (block.getState(false) instanceof Container container)
        handleContainer(block, blockData, container, ignoreOtherChestHalf);

      callIfDone();
    });
  }

  private void handleContainer(Block block, BlockData blockData, Container container, boolean ignoreOtherChestHalf) {
    int slotOffset = 0;

    Block otherChestBlock;

    if (blockData instanceof Chest chest) {
      var type = chest.getType();

      if (type == Chest.Type.LEFT)
        slotOffset = 3 * 9;

      if (type != Chest.Type.SINGLE) {
        int dx = 0, dz = 0;

        // Left and right are relative to the chest itself, i.e. opposite to what
        // a player placing the appropriate block would see.

        switch (chest.getFacing()) {
          case NORTH: // -z
            dx = (type == Chest.Type.LEFT) ? 1 : -1;
            break;
          case SOUTH: // +z
            dx = (type == Chest.Type.LEFT) ? -1 : 1;
            break;
          case EAST: // +x
            dz = (type == Chest.Type.LEFT) ? 1 : -1;
            break;
          case WEST: // -x
            dz = (type == Chest.Type.LEFT) ? -1 : 1;
            break;
        }

        otherChestBlock = block.getRelative(dx, 0, dz);

        if (!ignoreOtherChestHalf) {
          // Avoid calling completion if the piston-loop is already done and this block is within
          // the same chunk; simply don't allow; simply don't allow other-halves to call completion.
          ++chunksWaitingOn;
          handleBlock(otherChestBlock, true);
          --chunksWaitingOn;
        }
      }
    }

    searchedInventories.add(new SearchedInventory(container.getSnapshotInventory(), block, slotOffset));
  }

  private void ensureChunkIsLoaded(Block block, Runnable handler) {
    var chunkX = block.getX() >> 4;
    var chunkZ = block.getZ() >> 4;

    if (world.isChunkLoaded(chunkX, chunkZ)) {
      handler.run();
      return;
    }

    ++chunksWaitingOn;
    world.getChunkAtAsync(chunkX, chunkZ, true, chunk -> {
      if (terminated)
        return;

      --chunksWaitingOn;
      handler.run();
    });
  }
}
