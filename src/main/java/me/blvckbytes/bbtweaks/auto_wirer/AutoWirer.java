package me.blvckbytes.bbtweaks.auto_wirer;

import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AutoWirer implements Listener {

  // NOTE: I am *not* solving a dependency-graph by design, since I want to keep it simple
  //       and enforce proper #withSingleton call-order; all the AutoWirer does is to look
  //       up dependencies and inject them, as to save on all of that glue-code.

  private final JavaPlugin plugin;
  private final CommandUpdater commandUpdater;

  private final List<DependencyInstance> instantiatedDependencies;
  private final List<Tickable> tickableInstances;
  private final List<Disableable> disableableInstances;
  private final List<PendingLateWire> pendingLateWires;

  private boolean didComplete;
  private long relativeTime;

  public AutoWirer(JavaPlugin plugin) {
    this.plugin = plugin;
    this.commandUpdater = new CommandUpdater(plugin);
    this.instantiatedDependencies = new ArrayList<>();
    this.tickableInstances = new ArrayList<>();
    this.disableableInstances = new ArrayList<>();
    this.pendingLateWires = new ArrayList<>();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    for (var dependencyInstance : instantiatedDependencies) {
      if (dependencyInstance.instance() instanceof CommandHandler commandHandler)
        initializeAndUpdateCommand(commandHandler);
    }

    commandUpdater.trySyncCommands();
  }

  public void complete() throws Exception {
    if (didComplete)
      throw new IllegalStateException("Already completed");

    this.didComplete = true;

    processPendingLateWires();

    if (!pendingLateWires.isEmpty())
      throw new IllegalStateException("Failed to late-wire the following fields: " + pendingLateWires.stream().map(it -> String.valueOf(it.field())).collect(Collectors.joining()));

    for (var dependencyInstance : instantiatedDependencies)
      handleInterfaceFeatures(dependencyInstance);

    commandUpdater.trySyncCommands();

    Bukkit.getPluginManager().registerEvents(this, plugin);

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      ++relativeTime;

      for (var tickable : tickableInstances)
        tickable.tick(relativeTime);
    }, 0, 0);

    plugin.getLogger().info("Wired " + instantiatedDependencies.size() + " instances");
  }

  public AutoWirer withSingleton(Class<?> singletonClass) throws Throwable {
    withSingletonAndGet(singletonClass);
    return this;
  }

  public <T> T withSingletonAndGet(Class<T> singletonClass) throws Throwable {
    if (didComplete)
      throw new IllegalStateException("Cannot register a new singleton after having completed already");

    var instance = instantiateClass(singletonClass);

    for (var field : instance.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(LateWired.class)) {
        pendingLateWires.add(new PendingLateWire(instance, field));
        continue;
      }

      if (!field.isAnnotationPresent(WrappedDependency.class))
        continue;

      var value = field.get(instance);

      if (value == null)
        throw new IllegalStateException("The value of " + field + " was null");

      instantiatedDependencies.add(new DependencyInstance(field.getType(), value, field.getGenericType()));
    }

    instantiatedDependencies.add(new DependencyInstance(singletonClass, instance, null));

    processPendingLateWires();

    return instance;
  }

  private void processPendingLateWires() throws Exception {
    for (var iterator = pendingLateWires.iterator(); iterator.hasNext();) {
      var pendingLateWire = iterator.next();
      var dependencyInstance = findMatchingDependencyInstance(pendingLateWire.field());

      if (dependencyInstance == null)
        continue;

      pendingLateWire.field().setAccessible(true);
      pendingLateWire.field().set(pendingLateWire.instance(), dependencyInstance.instance());

      iterator.remove();
    }
  }

  private <T> @NotNull T instantiateClass(Class<T> singletonClass) throws Throwable {
    var declaredConstructors = singletonClass.getDeclaredConstructors();

    Constructor<?> constructor;

    if (declaredConstructors.length != 1 || !Modifier.isPublic((constructor = declaredConstructors[0]).getModifiers()))
      throw new IllegalStateException("Expected " + singletonClass + " to declare exactly one public constructor");

    var parameters = constructor.getParameters();
    var parameterValues = new Object[parameters.length];

    for (var parameterIndex = 0; parameterIndex < parameters.length; ++parameterIndex) {
      var dependencyInstance = findMatchingDependencyInstance(parameters[parameterIndex]);

      if (dependencyInstance == null)
        throw new IllegalStateException("Could not locate an instance for parameter " + (parameterIndex + 1) + " of " + constructor);

      parameterValues[parameterIndex] = dependencyInstance;
    }

    Object instance;

    try {
      instance = constructor.newInstance(parameterValues);
    }

    // Unwrap errors the constructor threw, as to increase readability on stack-traces.
    catch (InvocationTargetException e) {
      throw e.getCause();
    }

    return singletonClass.cast(instance);
  }

  private @Nullable DependencyInstance findMatchingDependencyInstance(Field field) {
    for (var dependencyInstance : instantiatedDependencies) {
      if (dependencyInstance.matches(field))
        return dependencyInstance;
    }

    return null;
  }

  private @Nullable Object findMatchingDependencyInstance(Parameter parameter) {
    if (parameter.getType().isAssignableFrom(plugin.getClass()))
      return plugin;

    if (parameter.getType().isAssignableFrom(getClass()))
      return this;

    if (parameter.getType().isAssignableFrom(CommandUpdater.class))
      return commandUpdater;

    for (var dependencyInstance : instantiatedDependencies) {
      if (dependencyInstance.matches(parameter))
        return dependencyInstance.instance();
    }

    return null;
  }

  public <T> @Nullable T findDependencyInstance(Class<T> type) {
    for (var dependencyInstance : instantiatedDependencies) {
      if (type.isInstance(dependencyInstance.instance()))
        return type.cast(dependencyInstance.instance());
    }

    return null;
  }

  private void handleInterfaceFeatures(DependencyInstance dependencyInstance) {
    if (dependencyInstance.instance() instanceof Listener listener)
      Bukkit.getPluginManager().registerEvents(listener, plugin);

    if (dependencyInstance.instance() instanceof CommandHandler commandHandler)
      initializeAndUpdateCommand(commandHandler);

    if (dependencyInstance.instance() instanceof Tickable tickable)
      tickableInstances.add(tickable);

    if (dependencyInstance.instance() instanceof Disableable disableable)
      disableableInstances.add(disableable);
  }

  public void onDisable() {
    for (var index = disableableInstances.size() - 1; index >= 0; --index)
      disableableInstances.get(index).disable();

    disableableInstances.clear();
    tickableInstances.clear();

    // I could also unregister listeners and commands at this point, but for now, I don't have a need for that.
  }

  private void initializeAndUpdateCommand(CommandHandler commandHandler) {
    commandHandler.getCommand().setExecutor(commandHandler);
    commandHandler.getCommand().setTabCompleter(commandHandler);

    var commandSection = commandHandler.getCommandSection();

    if (commandSection != null)
      commandSection.apply(commandHandler.getCommand(), commandUpdater);
  }
}
