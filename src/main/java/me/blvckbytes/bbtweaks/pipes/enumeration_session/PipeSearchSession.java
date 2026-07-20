package me.blvckbytes.bbtweaks.pipes.enumeration_session;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.blvckbytes.bbtweaks.pipes.CachedBlock;
import me.blvckbytes.bbtweaks.pipes.EnumerationBehavior;
import me.blvckbytes.bbtweaks.pipes.EnumerationDecision;
import me.blvckbytes.bbtweaks.pipes.PipesApi;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicateRegistry;
import me.blvckbytes.bbtweaks.util.CompactId;
import me.blvckbytes.bbtweaks.util.MutableInt;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PipeSearchSession extends PipeEnumerationSession<PipeSearchSession> {

  private enum HandleFlag {
    IGNORE_OTHER_CHEST_HALF,
    CHECK_ONLY_FOR_HOPPERS,
  }

  private final PipePredicateRegistry predicateRegistry;
  private final EnumSet<EnumerationBehavior> behaviorFlags;
  private final World world;

  private final List<SearchedInventory> searchedInventories;
  private final LongSet visitedBlocks;

  private int tubeCount;
  private int pistonCount;

  private int chunksWaitingOn;

  private final EnumMap<Material, MutableInt> containerCountByType;

  private PipeSearchSession(
    Block origin, PipesApi pipesApi, Plugin plugin,
    PipePredicateRegistry predicateRegistry,
    EnumSet<EnumerationBehavior> behaviorFlags,
    Consumer<PipeSearchSession> warmupHandler,
    Consumer<PipeSearchSession> completionHandler
  ) {
    super(origin, pipesApi, plugin, warmupHandler, completionHandler);

    this.predicateRegistry = predicateRegistry;
    this.behaviorFlags = behaviorFlags;
    this.world = origin.getWorld();
    this.searchedInventories = new ArrayList<>();
    this.visitedBlocks = new LongOpenHashSet();
    this.containerCountByType = new EnumMap<>(Material.class);
  }

  public static void tryStartSessionOrNotify(
    PipeEnumerationSessionHandler enumerationSessionHandler,
    Player player,
    Block targetBlock,
    EnumSet<EnumerationBehavior> behaviorFlags,
    Consumer<PipeSearchSession> handler
  ) {
    enumerationSessionHandler.tryStartSessionOrNotify(
      player, targetBlock,
      (origin, pipesApi, plugin, predicateRegistry, warmupHandler, completionHandler) -> (
        new PipeSearchSession(
          origin,
          pipesApi,
          plugin,
          predicateRegistry,
          behaviorFlags,
          warmupHandler, completionHandler
        )
      ),
      handler
    );
  }

  public int getTubeCount() {
    return tubeCount;
  }

  public int forEachContainerCountAndGetSum(BiConsumer<Material, Integer> handler) {
    var materials = new ArrayList<>(containerCountByType.keySet());
    var sum = 0;

    materials.sort(Comparator.comparingInt(Enum::ordinal));

    for (var material : materials) {
      var materialCount = containerCountByType.get(material).value;
      sum += materialCount;
      handler.accept(material, materialCount);
    }

    return sum;
  }

  public int getPistonCount() {
    return pistonCount;
  }

  public List<SearchedInventory> getSearchedInventories() {
    return searchedInventories;
  }

  @Override
  protected EnumerationDecision onTube(Block block, int cachedBlock) {
    ++tubeCount;
    return EnumerationDecision.CONTINUE;
  }

  @Override
  protected EnumerationDecision onPiston(Block block, int cachedBlock) {
    var putBlock = block.getRelative(CachedBlock.getFacing(cachedBlock));
    var pistonPredicate = predicateRegistry.getPredicateForPiston(block);

    if (handleBlock(putBlock, pistonPredicate, EnumSet.noneOf(HandleFlag.class)))
      ++pistonCount;

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
    return EnumSet.copyOf(behaviorFlags);
  }

  @Override
  protected void beforeRetry() {
    // Reset the tube-count, since we may need multiple retries to walk the whole pipe.
    // No need to keep a separate visited-set for tubes - would be a waste of resources.
    tubeCount = 0;
  }

  @Override
  protected boolean isDone() {
    return chunksWaitingOn == 0;
  }

  private boolean handleBlock(Block block, @Nullable ItemPredicate pistonPredicate, EnumSet<HandleFlag> flags) {
    var blockId = CompactId.computeWorldlessBlockId(block);

    if (visitedBlocks.contains(blockId))
      return false;

    ensureChunkIsLoaded(block, () -> {
      if (terminated)
        return;

      var blockData = block.getBlockData();

      if (blockData.getMaterial() == Material.HOPPER) {
        visitedBlocks.add(blockId);
        var hopperFacing = ((Directional) blockData).getFacing();
        handleBlock(block.getRelative(hopperFacing), pistonPredicate, EnumSet.noneOf(HandleFlag.class));
      }

      // Do not add block to visited-set if we only looked for hoppers but encountered another type - otherwise,
      // that block will remain unscanned and thus unaccounted for in the list of results.
      else if (flags.contains(HandleFlag.CHECK_ONLY_FOR_HOPPERS)) {
        callIfDone();
        return;
      }

      visitedBlocks.add(blockId);

      if (block.getState(false) instanceof Container container)
        handleContainer(block, blockData, pistonPredicate, container, flags);

      callIfDone();
    });

    return true;
  }

  private void handleContainer(Block block, BlockData blockData, @Nullable ItemPredicate pistonPredicate, Container container, EnumSet<HandleFlag> flags) {
    int slotOffset = 0;

    var ignoreOtherChestHalf = flags.contains(HandleFlag.IGNORE_OTHER_CHEST_HALF);

    Block otherChestBlock = null;

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
          handleBlock(otherChestBlock, pistonPredicate, EnumSet.of(HandleFlag.IGNORE_OTHER_CHEST_HALF));
          --chunksWaitingOn;
        }
      }
    }

    var blockType = block.getType();

    // Do not count individual double-chest halves; if we're not checking for double-chests,
    // that means we're coming from one (as to prevent recursion), so don't increment again.
    if (!ignoreOtherChestHalf)
      containerCountByType.computeIfAbsent(blockType, _ -> new MutableInt()).value++;

    searchedInventories.add(new SearchedInventory(container.getSnapshotInventory(), block, otherChestBlock, blockType, slotOffset, pistonPredicate));

    // Hoppers are only funneling out of containers if they sit right below them, which makes
    // them become part of the chain items may travel down, so they are also walked into.
    handleBlock(block.getRelative(BlockFace.DOWN), pistonPredicate, EnumSet.of(HandleFlag.CHECK_ONLY_FOR_HOPPERS));
  }

  private void ensureChunkIsLoaded(Block block, Runnable handler) {
    var chunkX = block.getX() >> 4;
    var chunkZ = block.getZ() >> 4;

    if (world.isChunkLoaded(chunkX, chunkZ)) {
      handler.run();
      return;
    }

    ++chunksWaitingOn;
    world.getChunkAtAsync(chunkX, chunkZ, true, _ -> {
      if (terminated)
        return;

      --chunksWaitingOn;
      handler.run();
    });
  }
}
