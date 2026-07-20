package me.blvckbytes.bbtweaks.pipes.enumeration_session;

import me.blvckbytes.bbtweaks.pipes.PipesApi;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicateRegistry;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

@FunctionalInterface
public interface SessionConstructor<T extends PipeEnumerationSession<T>> {

  T construct(
    Block origin,
    PipesApi pipesApi,
    Plugin plugin,
    PipePredicateRegistry predicateRegistry,
    Consumer<T> warmupHandler,
    Consumer<T> completionHandler
  );
}
