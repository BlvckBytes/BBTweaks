package me.blvckbytes.bbtweaks.pipes.predicates;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.pipes.*;
import me.blvckbytes.bbtweaks.util.CompactId;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.item_predicate_parser.event.*;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.PredicateSourcesReloadEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PipePredicateEventHandler implements Listener {

  private record CachedSign(@Nullable ItemPredicate predicate, int x, int y, int z) {}

  private final PipePredicateDataHandler dataHandler;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  private final Map<UUID, Long2ObjectMap<CachedSign>> cachedSignByPistonIdByWorldId;

  public PipePredicateEventHandler(
    PipePredicateDataHandler dataHandler,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.dataHandler = dataHandler;
    this.config = config;
    this.plugin = plugin;

    this.cachedSignByPistonIdByWorldId = new HashMap<>();
  }

  private void callFakeEvent(Event event) {
    for(var listener : event.getHandlers().getRegisteredListeners()) {
      if(!listener.getPlugin().isEnabled())
        continue;

      if (listener.getListener().equals(this))
        continue;

      var listenerClass = listener.getListener().getClass();

      // Avoid the "pipe created"-message
      if (listenerClass == Pipes.class)
        continue;

      // Avoid invalidating cache-entries
      if (listenerClass == PipeBlockCacheRegistry.class)
        continue;

      try {
        listener.callEvent(event);
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + listener.getPlugin().getPluginMeta().getName(), e);
      }
    }
  }

  private boolean canEditSign(Player player, Sign sign) {
    var signSide = sign.getSide(Side.FRONT);
    //noinspection UnstableApiUsage
    var fakeChangeEvent = new SignChangeEvent(sign.getBlock(), player, signSide.lines(), Side.FRONT);
    callFakeEvent(fakeChangeEvent);
    return !fakeChangeEvent.isCancelled();
  }

  @EventHandler
  public void onPredicateSourcesReload(PredicateSourcesReloadEvent event) {
    for (var worldBucketEntry : cachedSignByPistonIdByWorldId.entrySet()) {
      var signByPistonId = worldBucketEntry.getValue();
      var world = Bukkit.getWorld(worldBucketEntry.getKey());

      if (world == null) {
        signByPistonId.clear();
        continue;
      }

      var cachedSigns = signByPistonId.values();

      // Avoid 1) concurrent modification and 2) needless hash-lookups for invalidation
      var invalidateEvents = new ArrayList<InvalidateCachedBlockEvent>(cachedSigns.size());

      for (var cachedSign : cachedSigns) {
        var signBlock = world.getBlockAt(cachedSign.x, cachedSign.y, cachedSign.z);
        invalidateEvents.add(new InvalidateCachedBlockEvent(signBlock));
      }

      signByPistonId.clear();

      for (var invalidateEvent : invalidateEvents)
        Bukkit.getPluginManager().callEvent(invalidateEvent);
    }
  }

  @EventHandler
  public void onPipeSignCache(PipeSignCacheCreatedEvent event) {
    var sign = event.getPipeSign();
    var predicateData = dataHandler.access(sign);

    if (predicateData == null)
      return;

    var cachedSign = new CachedSign(predicateData.parsedPredicate(), sign.getX(), sign.getY(), sign.getZ());

    cachedSignByPistonIdByWorldId
      .computeIfAbsent(
        event.getPistonBlock().getWorld().getUID(),
        _ -> new Long2ObjectOpenHashMap<>()
      )
      .put(
        CompactId.computeWorldlessBlockId(event.getPistonBlock()),
        cachedSign
      );
  }

  @EventHandler
  public void onPipeSignCacheInvalidated(PipeSignCacheInvalidedEvent event) {
    var worldId = event.getPistonBlock().getWorld().getUID();
    var predicateCache = cachedSignByPistonIdByWorldId.get(worldId);

    if (predicateCache == null)
      return;

    predicateCache.remove(CompactId.computeWorldlessBlockId(event.getPistonBlock()));
  }

  @EventHandler
  public void onPipePredicate(PipePredicateEvent event) {
    var worldId = event.getBlock().getWorld().getUID();
    var cachedSignByPistonId = cachedSignByPistonIdByWorldId.get(worldId);

    if (cachedSignByPistonId == null)
      return;

    var cachedSign = cachedSignByPistonId.get(CompactId.computeWorldlessBlockId(event.getBlock()));

    if (cachedSign == null || cachedSign.predicate == null)
      return;

    event.setPredicate(cachedSign.predicate);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    if (!(clickedBlock.getState() instanceof Sign sign))
      return;

    var signSide = sign.getSide(Side.FRONT);

    if (!ComponentUtil.asTrimmedText(signSide.line(1)).equalsIgnoreCase(Pipes.PIPE_MARKER))
      return;

    if (dataHandler.access(sign) == null)
      return;

    event.setCancelled(true);

    config.rootSection.pipes.predicates.manualEditWhileInPredicateMode.sendMessage(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPredicateGet(PredicateGetEvent event) {
    var sign = tryResolveSignFromEventAndAcknowledge(event);

    if (sign == null)
      return;

    var predicateData = dataHandler.access(sign);

    if (predicateData != null) {
      if (predicateData.parsedPredicate() != null) {
        event.setResult(new PredicateAndLanguage(predicateData.parsedPredicate(), predicateData.predicateLanguage()));
        return;
      }

      if (predicateData.parseException() != null)
        event.setError(predicateData.parseException());
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPredicateRemove(PredicateRemoveEvent event) {
    var sign = tryResolveSignFromEventAndAcknowledge(event);

    if (sign == null)
      return;

    var predicateData = dataHandler.remove(sign);

    if (predicateData != null ) {
      predicateData.restoreLines(sign);

      if (predicateData.parsedPredicate() != null)
        event.setRemovedPredicate(new PredicateAndLanguage(predicateData.parsedPredicate(), predicateData.predicateLanguage()));

      Bukkit.getPluginManager().callEvent(new InvalidateCachedBlockEvent(sign.getBlock()));
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPredicateSet(PredicateSetEvent event) {
    var sign = tryResolveSignFromEventAndAcknowledge(event);

    if (sign == null)
      return;

    setPredicate(sign, event.getValue());
  }

  private void setPredicate(Sign sign, PredicateAndLanguage predicateAndLanguage) {
    var oldPredicateData = dataHandler.access(sign);
    PredicateData newPredicateData;

    if (oldPredicateData == null)
      newPredicateData = PredicateData.makeInitial(predicateAndLanguage.predicate, predicateAndLanguage.language, sign);
    else
      newPredicateData = PredicateData.makeUpdate(predicateAndLanguage.predicate, predicateAndLanguage.language, oldPredicateData);

    var signSide = sign.getSide(Side.FRONT);

    signSide.line(0, Component.text(PipePredicateMarkerConstants.PREDICATE_MARKER).color(PipePredicateMarkerConstants.PREDICATE_OK_COLOR));
    signSide.line(1, Component.text(Pipes.PIPE_MARKER));
    signSide.line(2, Component.empty());
    signSide.line(3, Component.empty());

    // This call already saves the sign, so don't invoke saving twice
    dataHandler.store(newPredicateData, sign);

    Bukkit.getPluginManager().callEvent(new InvalidateCachedBlockEvent(sign.getBlock()));
  }

  private @Nullable Sign tryResolveSignFromEventAndAcknowledge(PredicateEvent predicateEvent) {
    // We're using the highest priority as to let other handlers take precedence - if somebody
    // responded already, that means that the piston's output-block is a predicate-keeper itself
    // and the piston-predicate may only be accessed by interacting with said piston directly.
    if (predicateEvent.isAcknowledged())
      return null;

    var pistonBlock = PipeBlockUtility.resolvePistonBlock(predicateEvent.getBlock());

    if (pistonBlock == null)
      return null;

    var allowInitialize = predicateEvent.getPlayer().hasPermission("bbtweaks.pipes.auto-init-signs");
    var pistonSign = PipeBlockUtility.getPistonSign(pistonBlock, allowInitialize);

    if (pistonSign == null)
      return null;

    predicateEvent.acknowledge();

    if (!canEditSign(predicateEvent.getPlayer(), pistonSign)) {
      predicateEvent.setDeniedAccessBlock(pistonSign.getBlock());
      return null;
    }

    predicateEvent.setDataHoldingBlock(pistonSign.getBlock());

    return pistonSign;
  }
}
