package me.blvckbytes.bbtweaks.passive_sign.teleporter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.back.BackOverrideCommand;
import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;
import me.blvckbytes.bbtweaks.mechanic.common.UnknownFlagException;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TeleporterSignListener implements Listener {

  private static final int MARKER_LINE = 1;
  private static final int COORDINATES_LINE = 2;
  private static final int FLAGS_LINE = 3;

  private static final String SIGN_MARKER = "[Teleporter]";
  private static final int TELEPORT_COOLDOWN_MS = 750;

  private static final BlockFace[] PRESSURE_PLATE_BELOW_FACES = {
    BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final BackOverrideCommand backCommand;

  private final Map<UUID, Long> lastTeleportByPlayerId;

  public TeleporterSignListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    BackOverrideCommand backCommand
  ) {
    this.plugin = plugin;
    this.config = config;
    this.backCommand = backCommand;

    this.lastTeleportByPlayerId = new HashMap<>();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onSignChange(SignChangeEvent event) {
    var player = event.getPlayer();
    var lines = event.lines();

    if (!containsMarkerLine(lines))
      return;

    if (!player.hasPermission("bbtweaks.teleporter")) {
      config.rootSection.teleporterSign.noPermission.sendMessage(player);
      cancelAndBreakSign(event);
      return;
    }

    preProcessLines(lines);

    var parameters = parseParametersOrNotify(player, lines, true);

    if (parameters == null)
      return;

    config.rootSection.teleporterSign.teleporterCreated.sendMessage(player, parameters.makeEnvironment());

    lines.set(MARKER_LINE, Component.text(SIGN_MARKER));
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();
    var block = event.getClickedBlock();

    if (block == null)
      return;

    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      if (event.getHand() != EquipmentSlot.HAND)
        return;

      if (block.getState(false) instanceof Sign sign) {
        if (tryHandleTeleporter(player, sign))
          event.setCancelled(true);

        return;
      }

      if (Tag.BUTTONS.isTagged(block.getType())) {
        if (!(block.getBlockData() instanceof Directional directional))
          return;

        var signBlock = block.getRelative(directional.getFacing().getOppositeFace(), 2);

        if (signBlock.getState(false) instanceof Sign sign) {
          if (tryHandleTeleporter(player, sign))
            event.setCancelled(true);

          return;
        }

        return;
      }

      return;
    }

    if (event.getAction() == Action.PHYSICAL) {
      if (!Tag.PRESSURE_PLATES.isTagged(block.getType()))
        return;

      var blockBelow = block.getRelative(0, -2, 0);

      for (var face : PRESSURE_PLATE_BELOW_FACES) {
        var signBlock = blockBelow.getRelative(face);

        if (signBlock.getState(false) instanceof Sign sign) {
          if (tryHandleTeleporter(player, sign)) {
            event.setCancelled(true);
            return;
          }
        }
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lastTeleportByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private boolean tryHandleTeleporter(Player player, Sign sign) {
    var lines = sign.getSide(Side.FRONT).lines();

    if (!containsMarkerLine(lines))
      return false;

    preProcessLines(lines);

    var parameters = parseParametersOrNotify(player, lines, false);

    if (parameters == null)
      return false;

    var lastTeleport = lastTeleportByPlayerId.get(player.getUniqueId());
    var now = System.currentTimeMillis();

    if (lastTeleport != null && now - lastTeleport < TELEPORT_COOLDOWN_MS)
      return true;

    lastTeleportByPlayerId.put(player.getUniqueId(), now);

    if (parameters.flags().contains(TeleporterSignFlag.NO_BACK))
      backCommand.temporarilyIgnore(player);

    parameters.teleport(player);

    if (!parameters.flags().contains(TeleporterSignFlag.SILENT))
      config.rootSection.teleporterSign.teleported.sendMessage(player, parameters.makeEnvironment());

    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean containsMarkerLine(List<Component> lines) {
    var markerContent = ComponentUtil.asTrimmedText(lines.get(MARKER_LINE));
    return markerContent.equalsIgnoreCase(SIGN_MARKER);
  }

  private void preProcessLines(List<Component> lines) {
    var flagsContent = ComponentUtil.asTrimmedText(lines.get(FLAGS_LINE));

    // Well... This should really almost be procedurally generated :^)
    flagsContent = flagsContent
      .toLowerCase()
      .replace("norden", "north")
      .replace("osten", "east")
      .replace("süden", "south")
      .replace("westen", "west")
      .replace("nord-ost", "north-east")
      .replace("nordost", "north-east")
      .replace("nord-osten", "north-east")
      .replace("nordosten", "north-east")
      .replace("nord-west", "north-west")
      .replace("nordwest", "north-west")
      .replace("nord-westen", "north-west")
      .replace("nordwesten", "north-west")
      .replace("süd-ost", "south-east")
      .replace("südost", "south-east")
      .replace("süd-osten", "south-east")
      .replace("südosten", "south-east")
      .replace("süd-west", "south-west")
      .replace("südwest", "south-west")
      .replace("süd-westen", "south-west")
      .replace("südwesten", "south-west")
    ;

    lines.set(FLAGS_LINE, Component.text(flagsContent));
  }

  private @Nullable TeleporterSignParameters parseParametersOrNotify(
    Player player,
    List<Component> lines,
    boolean doNotify
  ) {
    var coordinatesContent = ComponentUtil.asTrimmedText(lines.get(COORDINATES_LINE));
    var coordinates = TeleporterSignCoordinates.tryParse(coordinatesContent);

    if (coordinates == null) {
      if (doNotify)
        config.rootSection.teleporterSign.malformedCoordinates.sendMessage(player);

      return null;
    }

    var flagsContent = ComponentUtil.asTrimmedText(lines.get(FLAGS_LINE));

    EnumSet<TeleporterSignFlag> flags;

    try {
      flags = FlagEnum.parse(TeleporterSignFlag.class, flagsContent);
    } catch (UnknownFlagException e) {
      if (doNotify)
        config.rootSection.teleporterSign.unknownFlag.sendMessage(player, e.makeEnvironment());

      return null;
    }

    return new TeleporterSignParameters(coordinates, flags);
  }

  private void cancelAndBreakSign(SignChangeEvent event) {
    event.setCancelled(true);

    var block = event.getBlock();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (Tag.ALL_SIGNS.isTagged(block.getType()))
        block.breakNaturally();
    }, 1);
  }
}
