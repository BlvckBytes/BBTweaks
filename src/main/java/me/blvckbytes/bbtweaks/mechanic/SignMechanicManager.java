package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.AutoWirer;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.AutoCrafterMechanic;
import me.blvckbytes.bbtweaks.mechanic.clock.ClockMechanic;
import me.blvckbytes.bbtweaks.mechanic.auto_dispose.AutoDisposeMechanic;
import me.blvckbytes.bbtweaks.mechanic.command.CommandSignMechanic;
import me.blvckbytes.bbtweaks.mechanic.hidden_switch.HiddenSwitchCommand;
import me.blvckbytes.bbtweaks.mechanic.hidden_switch.HiddenSwitchMechanic;
import me.blvckbytes.bbtweaks.mechanic.hidden_switch.PasswordCommand;
import me.blvckbytes.bbtweaks.mechanic.inv_move.InvMoveMechanic;
import me.blvckbytes.bbtweaks.mechanic.item_notifier.ItemNotifierMechanic;
import me.blvckbytes.bbtweaks.mechanic.lever_array.LeverArrayMechanic;
import me.blvckbytes.bbtweaks.mechanic.magnet.command.MagnetVisualizeCommand;
import me.blvckbytes.bbtweaks.mechanic.magnet.MagnetMechanic;
import me.blvckbytes.bbtweaks.mechanic.planter.PlanterMechanic;
import me.blvckbytes.bbtweaks.mechanic.pool_crafter.PoolCrafterMechanic;
import me.blvckbytes.bbtweaks.mechanic.pulse_extender.PulseExtenderMechanic;
import me.blvckbytes.bbtweaks.mechanic.quick_unload.QuickUnloadMechanic;
import me.blvckbytes.bbtweaks.mechanic.showcase.ShowcaseDisplayHandler;
import me.blvckbytes.bbtweaks.mechanic.showcase.ShowcaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.sign_flipper.SignFlipperMechanic;
import me.blvckbytes.bbtweaks.mechanic.transmitter_receiver.ReceiverMechanic;
import me.blvckbytes.bbtweaks.mechanic.transmitter_receiver.TransmitterMechanic;
import me.blvckbytes.bbtweaks.util.BooleanConsumer;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SignMechanicManager implements Disableable, Listener {

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final Map<String, SignMechanic<?>> signMechanicByDiscriminatorLower;

  public SignMechanicManager(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    AutoWirer autoWirer
  ) throws Throwable {
    this.plugin = plugin;
    this.config = config;

    this.signMechanicByDiscriminatorLower = new HashMap<>();

    registerMechanic(autoWirer.withSingletonAndGet(ClockMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(PulseExtenderMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(MagnetMechanic.class));
    autoWirer.withSingleton(MagnetVisualizeCommand.class);
    registerMechanic(autoWirer.withSingletonAndGet(ReceiverMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(TransmitterMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(AutoDisposeMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(SignFlipperMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(HiddenSwitchMechanic.class));
    autoWirer.withSingleton(HiddenSwitchCommand.class);
    autoWirer.withSingleton(PasswordCommand.class);
    registerMechanic(autoWirer.withSingletonAndGet(QuickUnloadMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(InvMoveMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(LeverArrayMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(PlanterMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(AutoCrafterMechanic.class));
    autoWirer.withSingleton(ShowcaseDisplayHandler.class);
    registerMechanic(autoWirer.withSingletonAndGet(ShowcaseMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(ItemNotifierMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(PoolCrafterMechanic.class));
    registerMechanic(autoWirer.withSingletonAndGet(CommandSignMechanic.class));
  }

  @Override
  public void disable() {
    signMechanicByDiscriminatorLower.clear();
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockPlace(BlockPlaceEvent event) {
    var block = event.getBlock();
    var blockType = block.getType();

    if (!Tag.WALL_SIGNS.isTagged(blockType))
      return;

    if (!(block.getState() instanceof Sign sign))
      return;

    correspondSign(sign, mechanic -> {
      if (mechanic.onSignClick(event.getPlayer(), sign, false))
        event.setCancelled(true);
    });
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockBreak(BlockBreakEvent event) {
    var block = event.getBlock();
    var blockType = block.getType();

    if (!Tag.WALL_SIGNS.isTagged(blockType))
      return;

    if (!(block.getState() instanceof Sign sign))
      return;

    correspondSign(sign, mechanic -> {
      if (mechanic.onSignClick(event.getPlayer(), sign, true)) {
        event.setCancelled(true);
        return;
      }

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Ensure that the block has actually been altered - many plugins like to misuse this event for permission-checks
        if (blockType == block.getType())
          return;

        mechanic.onSignDestroy(event.getPlayer(), sign);
      }, 1);
    });
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
    forEachSignInChunk(event.getChunk(), sign -> {
      correspondSign(sign, mechanic -> {
        if (!Tag.WALL_SIGNS.isTagged(sign.getType())) {
          sign.getBlock().breakNaturally();
          return;
        }

        if (mechanic.onSignLoad(sign) == null)
          sign.getBlock().breakNaturally();
      });
    });
  }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    forEachSignInChunk(event.getChunk(), sign -> {
      correspondSign(sign, mechanic -> mechanic.onSignUnload(sign));
    });
  }

  private void forEachSignInChunk(Chunk chunk, Consumer<Sign> handler) {
    for (var tileEntity : chunk.getTileEntities(block -> Tag.WALL_SIGNS.isTagged(block.getType()), false)) {
      if (tileEntity instanceof Sign sign)
        handler.accept(sign);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
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

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onLeverToggle(BlockRedstoneEvent event) {
    var block = event.getBlock();

    if (block.getType() != Material.LEVER)
      return;

    // Filter out noise, possibly stemming from physics-updates
    if (event.getOldCurrent() == event.getNewCurrent())
      return;

    BooleanConsumer stateSetter = state -> event.setNewCurrent(state ? 15 : 0);

    for (var mechanic : signMechanicByDiscriminatorLower.values())
      mechanic.onLeverToggle(block, event.getNewCurrent() != 0, stateSetter);
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
  }
}
