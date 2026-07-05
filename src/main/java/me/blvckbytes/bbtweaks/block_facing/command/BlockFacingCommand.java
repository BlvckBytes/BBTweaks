package me.blvckbytes.bbtweaks.block_facing.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.block_facing.settings.BlockFacingSettingsStore;
import me.blvckbytes.bbtweaks.block_facing.settings_display.BlockFacingSettingsDisplayHandler;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Orientation;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class BlockFacingCommand implements CommandHandler, Listener {

  private final Plugin plugin;
  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final BlockFacingSettingsStore settingsStore;
  private final BlockFacingSettingsDisplayHandler settingsDisplayHandler;

  public BlockFacingCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    BlockFacingSettingsStore settingsStore,
    BlockFacingSettingsDisplayHandler settingsDisplayHandler
  ) {
    this.plugin = plugin;
    this.command = Objects.requireNonNull(plugin.getCommand(BlockFacingCommandSection.INITIAL_NAME));
    this.config = config;
    this.settingsStore = settingsStore;
    this.settingsDisplayHandler = settingsDisplayHandler;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.blockFacing.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.blockFacing.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.blockFacing.noPermission.sendMessage(sender);
      return true;
    }

    var settings = settingsStore.access(player);

    if (args.length == 0) {
      settingsDisplayHandler.show(player, settings);
      return true;
    }

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null) {
      config.rootSection.blockFacing.actionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    switch (normalizedAction.constant) {
      case PLACING_ON -> settings.setModifyPlacedBlocks(true);
      case PLACING_OFF -> settings.setModifyPlacedBlocks(false);
      case PLACING_TOGGLE -> settings.setModifyPlacedBlocks(null);
      case EXISTING_ON -> settings.setModifyExistingBlocks(true);
      case EXISTING_OFF -> settings.setModifyExistingBlocks(false);
      case EXISTING_TOGGLE -> settings.setModifyExistingBlocks(null);
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !command.testPermission(player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    var player = event.getPlayer();
    var settings = settingsStore.access(event.getPlayer());

    if (!settings.modifyPlacedBlocks)
      return;

    if (settings.doesLackPermission())
      return;

    var placedBlock = event.getBlock();
    var placedBlockData = placedBlock.getBlockData();
    var desiredFacing = settings.facingOverride.getFace(player);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      // Just to make sure that the block really has been placed.
      if (placedBlockData.getMaterial() == placedBlock.getType())
        trySetBlockFacing(placedBlock, desiredFacing);
    }, 1);
  }

  // Do not go up to MONITOR, as to not clash with the obstructed container opener
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent event) {
    if (!event.getAction().isRightClick())
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    var item = event.getItem();

    if (item == null || item.getType() != Material.STICK)
      return;

    var player = event.getPlayer();
    var settings = settingsStore.access(player);

    if (!settings.modifyExistingBlocks)
      return;

    if (settings.doesLackPermission())
      return;

    //noinspection UnstableApiUsage
    var fakeBreakEvent = new BlockBreakEvent(clickedBlock, player);
    Bukkit.getPluginManager().callEvent(fakeBreakEvent);

    if (fakeBreakEvent.isCancelled())
      return;

    event.setCancelled(true);

    var desiredFacing = settings.facingOverride.getFace(player);

    trySetBlockFacing(clickedBlock, desiredFacing);
  }

  private void trySetBlockFacing(Block block, BlockFace desiredFacing) {
    var blockData = block.getBlockData();

    if (blockData instanceof Chest chest) {
      if (chest.getType() != Chest.Type.SINGLE)
        return;
    }

    if (blockData instanceof Bed)
      return;

    if (blockData instanceof Bisected) {
      if (!(blockData instanceof Stairs) && !(blockData instanceof TrapDoor))
        return;
    }

    var blockType = blockData.getMaterial();

    if (blockData instanceof WallSign || blockData instanceof WallHangingSign)
      return;

    if (Tag.BUTTONS.isTagged(blockType))
      return;

    if (blockType == Material.LEVER)
      return;

    if (blockType == Material.REDSTONE_WIRE)
      return;

    switch (blockData) {
      case Directional directional -> {
        try {
          directional.setFacing(desiredFacing);
        } catch (Throwable _) {
          return;
        }
      }
      case Rotatable rotatable -> {
        try {
          rotatable.setRotation(desiredFacing);
        } catch (Throwable _) {
          return;
        }
      }
      case Crafter crafter -> {
        var desiredOrientation = switch (desiredFacing) {
          case DOWN -> Orientation.DOWN_NORTH;
          case UP -> Orientation.UP_NORTH;
          case WEST -> Orientation.WEST_UP;
          case EAST -> Orientation.EAST_UP;
          case NORTH -> Orientation.NORTH_UP;
          case SOUTH -> Orientation.SOUTH_UP;
          default -> null;
        };

        if (desiredOrientation == null)
          return;

        try {
          crafter.setOrientation(desiredOrientation);
        } catch (Throwable _) {
          return;
        }
      }
      case Orientable orientable -> {
        var desiredAxis = switch (desiredFacing) {
          case SOUTH, NORTH -> Axis.Z;
          case WEST, EAST -> Axis.X;
          case UP, DOWN -> Axis.Y;
          default -> null;
        };

        if (desiredAxis == null)
          return;

        try {
          orientable.setAxis(desiredAxis);
        } catch (Throwable _) {
          return;
        }
      }
      default -> {
        return;
      }
    }

    block.setBlockData(blockData);
  }
}
