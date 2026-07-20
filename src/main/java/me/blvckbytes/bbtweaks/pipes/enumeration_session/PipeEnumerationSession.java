package me.blvckbytes.bbtweaks.pipes.enumeration_session;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.blvckbytes.bbtweaks.pipes.*;
import me.blvckbytes.bbtweaks.util.CompactId;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.function.Consumer;

public abstract class PipeEnumerationSession<T extends PipeEnumerationSession<T>> {

  private static final int RETRY_DELAY_T = 2;
  public static final int MAX_RETRY_COUNT = 350;

  public final Block origin;

  private final PipesApi pipesApi;
  private final Plugin plugin;
  private final Consumer<T> warmupHandler;
  private final Consumer<T> completionHandler;

  private boolean exceededRetryCount;
  private final LongSet visitedBlocks;

  protected boolean terminated;
  private boolean completedEnumeration;

  private int pistonCount;
  private int tubeCount;

  protected PipeEnumerationSession(
    Block origin, PipesApi pipesApi, Plugin plugin,
    Consumer<T> warmupHandler,
    Consumer<T> completionHandler
  ) {
    this.origin = origin;

    this.pipesApi = pipesApi;
    this.plugin = plugin;
    this.warmupHandler = warmupHandler;
    this.completionHandler = completionHandler;

    this.visitedBlocks = new LongOpenHashSet();
  }

  public boolean didExceedRetryCount() {
    return exceededRetryCount;
  }

  public void terminate() {
    this.terminated = true;
  }

  public void start() {
    _enumerateAllPistons(0);
  }

  public int getPistonCount() {
    return pistonCount;
  }

  public int getTubeCount() {
    return tubeCount;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean didEncounterPipeBlocks() {
    return tubeCount > 0 || pistonCount > 0;
  }

  protected abstract boolean isDone();

  protected abstract void beforeRetry();

  protected abstract EnumerationDecision onTube(Block block, int cachedBlock);

  protected abstract EnumerationDecision onPiston(Block block, int cachedBlock);

  protected abstract void beforeCompletion();

  protected abstract void beforeSubPipe();

  protected abstract void afterSubPipe();

  protected abstract EnumSet<EnumerationBehavior> getEnumerationBehavior();

  protected void callIfDone() {
    if (terminated)
      return;

    if (completedEnumeration && isDone()) {
      beforeCompletion();

      //noinspection unchecked
      completionHandler.accept((T) this);
    }
  }

  private void _enumerateAllPistons(int retryCount) {
    if (terminated)
      return;

    if (retryCount >= MAX_RETRY_COUNT) {
      exceededRetryCount = true;
      completedEnumeration = true;
      callIfDone();
      return;
    }

    beforeRetry();

    visitedBlocks.clear();

    pistonCount = 0;
    tubeCount = 0;

    var enumerationResult = pipesApi.enumeratePipeBlocks(origin, visitedBlocks, getEnumerationBehavior(), this::handlePipeEnumeration);

    if (!terminated && enumerationResult != EnumerationResult.COMPLETED) {
      //noinspection unchecked
      warmupHandler.accept((T) this);
      Bukkit.getScheduler().runTaskLater(plugin, () -> _enumerateAllPistons(retryCount + 1), RETRY_DELAY_T);
      return;
    }

    completedEnumeration = true;
    callIfDone();
  }

  private EnumerationDecision handlePipeEnumeration(Block pipeBlock, int cachedPipeBlock, CachedBlockResolver cache) throws LoadingChunkException {
    if (CachedBlock.isTube(cachedPipeBlock)) {
      ++tubeCount;
      return onTube(pipeBlock, cachedPipeBlock);
    }

    if (!CachedBlock.isMaterial(cachedPipeBlock, Material.PISTON))
      return EnumerationDecision.CONTINUE;

    ++pistonCount;

    if (onPiston(pipeBlock, cachedPipeBlock) != EnumerationDecision.CONTINUE)
      return EnumerationDecision.STOP;

    var putBlock = pipeBlock.getRelative(CachedBlock.getFacing(cachedPipeBlock));
    var cachedPutBlock = cache.getCachedBlock(putBlock);

    var isSubPipe = CachedBlock.isTube(cachedPutBlock) && !CachedBlock.isPane(cachedPutBlock);

    if (!isSubPipe)
      return EnumerationDecision.CONTINUE;

    // Recurse down into the sub-pipe first, as for the piston/tube encounter-order to end
    // up congruent with the actual, item-carrying pipe-process.

    var behaviorFlags = getEnumerationBehavior();
    behaviorFlags.add(EnumerationBehavior.DO_NOT_RESET_CACHE_AND_MAX_COUNTERS);

    visitedBlocks.add(CompactId.computeWorldlessBlockId(putBlock));

    beforeSubPipe();
    pipesApi.enumeratePipeBlocks(putBlock, visitedBlocks, behaviorFlags, this::handlePipeEnumeration);
    afterSubPipe();

    return EnumerationDecision.CONTINUE;
  }
}
