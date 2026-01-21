package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.clock.ClockMechanic;
import me.blvckbytes.bbtweaks.mechanic.magnet.MagnetMechanic;
import me.blvckbytes.bbtweaks.mechanic.pulse_extender.PulseExtenderMechanic;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SignMechanicManager implements Listener {

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final Map<String, SignMechanic<?>> signMechanicByDiscriminatorLower;

  private @Nullable BukkitTask tickerTask;

  private int time;

  public SignMechanicManager(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;

    this.signMechanicByDiscriminatorLower = new HashMap<>();

    registerMechanic(new ClockMechanic(plugin, config));
    registerMechanic(new PulseExtenderMechanic(plugin, config));
    registerMechanic(new MagnetMechanic(plugin, config));

    tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);

    Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
  }

  public void shutdown() {
    HandlerList.unregisterAll(this);

    if (tickerTask != null) {
      tickerTask.cancel();
      tickerTask = null;
    }

    signMechanicByDiscriminatorLower.values().forEach(mechanic -> {
      mechanic.onMechanicUnload();

      if (mechanic instanceof Listener listener)
        HandlerList.unregisterAll(listener);
    });

    signMechanicByDiscriminatorLower.clear();
  }

  private void tick() {
    ++time;
    signMechanicByDiscriminatorLower.values().forEach(mechanic -> mechanic.tick(time));
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    var block = event.getBlock();
    var blockType = block.getType();

    if (!Tag.WALL_SIGNS.isTagged(blockType))
      return;

    if (!(block.getState() instanceof Sign sign))
      return;

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      // Ensure that the block has actually been altered - many plugins like to misuse this event for permission-checks
      if (blockType == block.getType())
        return;

      correspondSign(sign, mechanic -> mechanic.onSignDestroy(event.getPlayer(), sign));
    }, 1);
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockPhysics(BlockPhysicsEvent event) {
    var block = event.getBlock();
    var blockType = block.getType();

    if (!Tag.WALL_SIGNS.isTagged(blockType))
      return;

    if (!(block.getState() instanceof Sign sign))
      return;

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (block.getType() == blockType)
        return;

      correspondSign(sign, mechanic -> mechanic.onSignDestroy(null, sign));
    }, 1);
  }

  @EventHandler(ignoreCancelled = true)
  public void onSignChange(SignChangeEvent event) {
    var block = event.getBlock();

    if (!(block.getState() instanceof Sign oldSign))
      return;

    correspondSign(oldSign, mechanic -> mechanic.onSignDestroy(event.getPlayer(), oldSign));

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!(event.getBlock().getState() instanceof Sign newSign))
        return;

      correspondSign(newSign, mechanic -> {
        if (!Tag.WALL_SIGNS.isTagged(block.getType())) {
          config.rootSection.mechanic.noWallSign.sendMessage(event.getPlayer());
          block.breakNaturally();
          return;
        }

        if (mechanic.onSignCreate(event.getPlayer(), newSign) == null)
          block.breakNaturally();
      });
    }, 1);
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    for (var tileEntity : event.getChunk().getTileEntities()) {
      if (!(tileEntity instanceof Sign sign))
        continue;

      correspondSign(sign, mechanic -> {
        if (mechanic.onSignLoad(sign) == null)
          sign.getBlock().breakNaturally();
      });
    }
  }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    for (var tileEntity : event.getChunk().getTileEntities()) {
      if (!(tileEntity instanceof Sign sign))
        continue;

      correspondSign(sign, mechanic -> mechanic.onSignUnload(sign));
    }
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    var block = event.getClickedBlock();

    if (block == null || !Tag.WALL_SIGNS.isTagged(block.getType()))
      return;

    if (!(block.getState() instanceof Sign sign))
      return;

    var wasLeftClick = event.getAction() == Action.LEFT_CLICK_BLOCK;

    correspondSign(sign, mechanic -> {
      if (mechanic.onSignClick(event.getPlayer(), sign, wasLeftClick))
        event.setCancelled(true);
    });
  }

  private void correspondSign(Sign sign, Consumer<SignMechanic<?>> handler) {
    var discriminator = SignUtil.getPlainTextLine(sign, 1);
    var length = discriminator.length();

    if (length <= 2 || !(discriminator.charAt(0) == '[' && discriminator.charAt(length - 1) == ']'))
      return;

    discriminator = discriminator.substring(1, length - 1).toLowerCase();

    var mechanic = signMechanicByDiscriminatorLower.get(discriminator);

    if (mechanic == null)
      return;

    handler.accept(mechanic);
  }

  private void registerMechanic(SignMechanic<?> mechanic) {
    for (var discriminator : mechanic.getDiscriminators()) {
      var lowerDiscriminator = discriminator.trim().toLowerCase();
      var existingHandler = signMechanicByDiscriminatorLower.put(lowerDiscriminator, mechanic);

      if (existingHandler != null)
        throw new IllegalArgumentException("Duplicate sign-mechanic for discriminator \"" + discriminator + "\" detected: " + existingHandler.getClass());
    }

    if (mechanic instanceof Listener listener)
      Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);

    mechanic.onMechanicLoad();
  }
}
