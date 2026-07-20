package me.blvckbytes.bbtweaks.pipes.enumeration_session;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.pipes.PipesApi;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicateEventHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PipeEnumerationSessionHandler implements Listener {

  private final Plugin plugin;
  private final PipesApi pipesApi;
  private final PipePredicateEventHandler predicateEventHandler;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, PipeEnumerationSession<?>> enumerationSessionByPlayerId;

  public PipeEnumerationSessionHandler(
    Plugin plugin,
    PipesApi pipesApi,
    PipePredicateEventHandler predicateEventHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.pipesApi = pipesApi;
    this.predicateEventHandler = predicateEventHandler;
    this.config = config;

    this.enumerationSessionByPlayerId = new HashMap<>();
  }

  public <T extends PipeEnumerationSession<T>> void tryStartSessionOrNotify(
    Player player,
    Block targetBlock,
    SessionConstructor<T> sessionConstructor,
    Consumer<T> completionHandler
  ) {
    if (enumerationSessionByPlayerId.containsKey(player.getUniqueId())) {
      config.rootSection.pipes.enumerationAlreadyInASession.sendMessage(player);
      return;
    }

    if (!canBuildAt(player, targetBlock)) {
      config.rootSection.pipes.enumerationCannotBuildThere.sendMessage(player);
      return;
    }

    var enumerationSession = sessionConstructor.construct(
      targetBlock, pipesApi, plugin, predicateEventHandler,
      session -> handleWarmup(session, player),
      session -> {
        enumerationSessionByPlayerId.remove(player.getUniqueId());

        if (!session.didEncounterPipeBlocks())
          return;

        // Notify the player, but still call the handler, as to offer a partial progress.
        if (session.didExceedRetryCount()) {
          config.rootSection.pipes.enumerationExceededRetries.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("limit", PipeSearchSession.MAX_RETRY_COUNT)
          );
        }

        completionHandler.accept(session);
      }
    );

    enumerationSessionByPlayerId.put(player.getUniqueId(), enumerationSession);
    enumerationSession.start();

    if (!enumerationSession.didEncounterPipeBlocks()) {
      enumerationSessionByPlayerId.remove(player.getUniqueId());
      config.rootSection.pipes.enumerationNotAPipeBlock.sendMessage(player);
    }
  }

  private void handleWarmup(PipeEnumerationSession<?> session, Player player) {
    config.rootSection.pipes.notifications.warmingUp.sendActionBar(
      player,
      new InterpretationEnvironment()
        .withVariable("tubes", session.getTubeCount())
        .withVariable("pistons", session.getPistonCount())
    );
  }

  private boolean canBuildAt(Player player, Block block) {
    //noinspection UnstableApiUsage
    var fakePlaceEvent = new BlockPlaceEvent(block, block.getState(), block, new ItemStack(Material.DIRT), player, false, EquipmentSlot.HAND);
    predicateEventHandler.callFakeEvent(fakePlaceEvent);
    return !fakePlaceEvent.isCancelled();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var enumerationSession = enumerationSessionByPlayerId.remove(event.getPlayer().getUniqueId());

    if (enumerationSession != null)
      enumerationSession.terminate();
  }
}
