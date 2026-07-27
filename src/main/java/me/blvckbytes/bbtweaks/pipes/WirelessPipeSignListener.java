package me.blvckbytes.bbtweaks.pipes;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.back.BackOverrideCommand;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class WirelessPipeSignListener implements Listener, Tickable {

  private static final long TELEPORT_COOLDOWN_T = 8;

  private final PipeBlockCacheRegistry cacheRegistry;
  private final BackOverrideCommand backCommand;

  private final Object2LongMap<UUID> lastTeleportByPlayerId;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private long relativeTime;

  public WirelessPipeSignListener(
    PipeBlockCacheRegistry cacheRegistry,
    BackOverrideCommand backCommand,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.cacheRegistry = cacheRegistry;
    this.backCommand = backCommand;
    this.plugin = plugin;
    this.config = config;

    this.lastTeleportByPlayerId = new Object2LongOpenHashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;
  }

  @EventHandler(ignoreCancelled = true)
  public void onSignChange(SignChangeEvent event) {
    var markerContents = ComponentUtil.asTrimmedText(event.line(1));

    if (!markerContents.equalsIgnoreCase(WirelessPipeSign.MARKER))
      return;

    var player = event.getPlayer();

    if (!player.hasPermission("bbtweaks.pipes.wireless")) {
      cancelAndBreakSign(event);
      config.rootSection.pipes.wirelessSignCreateNoPermission.sendMessage(player);
      return;
    }

    var signBlock = event.getBlock();

    if (!Tag.WALL_SIGNS.isTagged(signBlock.getType())) {
      config.rootSection.pipes.wirelessSignNotOnGlassBlock.sendMessage(player);
      cancelAndBreakSign(event);
      return;
    }

    if (ComponentUtil.asTrimmedText(event.line(2)).equals("?"))
      event.line(2, Component.text(signBlock.getX() + " " + signBlock.getY() + " " + signBlock.getZ()));

    var wirelessSign = WirelessPipeSign.fromLines(event.lines(), signBlock);

    if (wirelessSign == WirelessPipeSign.NO_SIGN) {
      cancelAndBreakSign(event);
      config.rootSection.pipes.wirelessSignMalformed.sendMessage(player);
      return;
    }

    var mountColor = TubeColor.fromMaterial(wirelessSign.mountBlock.getType());

    if (mountColor.isPane() || mountColor.color() == TubeColor.NONE) {
      config.rootSection.pipes.wirelessSignNotOnGlassBlock.sendMessage(player);
      cancelAndBreakSign(event);
      return;
    }

    event.line(1, Component.text(WirelessPipeSign.MARKER));

    config.rootSection.pipes.wirelessSignCreated.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("x", signBlock.getX())
        .withVariable("y", signBlock.getY())
        .withVariable("z", signBlock.getZ())
        .withVariable("referenced_x", wirelessSign.referencedBlock.getX())
        .withVariable("referenced_y", wirelessSign.referencedBlock.getY())
        .withVariable("referenced_z", wirelessSign.referencedBlock.getZ())
    );
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
      return;

    if (event.getHand() != EquipmentSlot.HAND)
      return;

    var block = event.getClickedBlock();

    if (block == null)
      return;

    var player = event.getPlayer();

    if (!player.isSneaking())
      return;

    if (!(block.getState(false) instanceof Sign sign))
      return;

    var signSide = sign.getSide(Side.FRONT);
    var markerContents = ComponentUtil.asTrimmedText(signSide.line(1));

    if (!markerContents.equalsIgnoreCase(WirelessPipeSign.MARKER))
      return;

    var signBlock = sign.getBlock();
    var blockCache = cacheRegistry.getBlockCache(signBlock.getWorld());

    WirelessPipeSign thisWirelessSign;

    try {
      thisWirelessSign = blockCache.getWirelessPipeSign(sign.getBlock(), blockCache.getCachedBlock(signBlock));
    } catch (LoadingChunkException _) {
      return;
    }

    event.setCancelled(true);

    if (thisWirelessSign == null) {
      config.rootSection.pipes.wirelessSignMalformed.sendMessage(player);
      return;
    }

    WirelessPipeSign otherWirelessSign;

    try {
      // Ensure that the sign is loaded - we can afford doing so synchronously for the event.
      thisWirelessSign.referencedBlock.getState(false);

      otherWirelessSign = blockCache.getWirelessPipeSign(thisWirelessSign.referencedBlock, blockCache.getCachedBlock(thisWirelessSign.referencedBlock));
    } catch (LoadingChunkException _) {
      return;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", signBlock.getX())
      .withVariable("y", signBlock.getY())
      .withVariable("z", signBlock.getZ())
      .withVariable("referenced_x", thisWirelessSign.referencedBlock.getX())
      .withVariable("referenced_y", thisWirelessSign.referencedBlock.getY())
      .withVariable("referenced_z", thisWirelessSign.referencedBlock.getZ())
      .withVariable("is_connected", otherWirelessSign != null);

    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      config.rootSection.pipes.wirelessSignInformation.sendMessage(player, environment);
      return;
    }

    if (!player.hasPermission("bbtweaks.pipes.wireless.teleport")) {
      config.rootSection.pipes.wirelessSignMissingTeleportPermission.sendMessage(player, environment);
      return;
    }

    var lastTeleport = lastTeleportByPlayerId.getLong(player.getUniqueId());

    if (relativeTime - lastTeleport < TELEPORT_COOLDOWN_T)
      return;

    lastTeleportByPlayerId.put(player.getUniqueId(), relativeTime);

    var flagsLine = ComponentUtil.asTrimmedText(signSide.line(3)).toLowerCase();

    if (flagsLine.contains("no-back"))
      backCommand.temporarilyIgnore(player);

    if (!flagsLine.contains("silent"))
      config.rootSection.pipes.wirelessSignTeleported.sendMessage(player, environment);

    var targetBlock = thisWirelessSign.referencedBlock;

    if (targetBlock.getState(false) instanceof Sign targetSign) {
      BlockUtil.teleportPlayerToSign(player, targetSign);
      return;
    }

    player.teleport(thisWirelessSign.referencedBlock.getLocation());
  }


  private void cancelAndBreakSign(SignChangeEvent event) {
    event.setCancelled(true);
    Bukkit.getScheduler().runTaskLater(plugin, () -> event.getBlock().breakNaturally(), 1);
  }
}
