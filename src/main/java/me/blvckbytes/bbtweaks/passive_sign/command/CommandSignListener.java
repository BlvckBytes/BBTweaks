package me.blvckbytes.bbtweaks.passive_sign.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandSignListener implements Listener {

  private static final int MARKER_LINE = 1;
  private static final int COMMAND_LINE = 2;

  private static final String SIGN_MARKER = "[Command]";
  private static final int EXECUTE_COOLDOWN_MS = 500;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, Long> lastExecutionByPlayerId;

  public CommandSignListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.config = config;
    this.lastExecutionByPlayerId = new HashMap<>();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onSignChange(SignChangeEvent event) {
    var player = event.getPlayer();
    var lines = event.lines();

    if (!containsMarkerLine(lines))
      return;

    if (!player.hasPermission("bbtweaks.command-sign")) {
      config.rootSection.commandSign.noPermission.sendMessage(player);
      cancelAndBreakSign(event);
      return;
    }

    var command = ComponentUtil.asTrimmedText(lines.get(COMMAND_LINE));

    if (command.isEmpty()) {
      config.rootSection.commandSign.missingCommand.sendMessage(player);
      return;
    }

    var block = event.getBlock();

    config.rootSection.commandSign.signCreated.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("x", block.getX())
        .withVariable("y", block.getY())
        .withVariable("z", block.getZ())
        .withVariable("command", command)
    );

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
        if (tryHandleExecution(player, sign))
          event.setCancelled(true);

      }

      if (Tag.BUTTONS.isTagged(block.getType())) {
        if (!(block.getBlockData() instanceof Directional directional))
          return;

        var signBlock = block.getRelative(directional.getFacing().getOppositeFace(), 2);

        if (signBlock.getState(false) instanceof Sign sign) {
          if (tryHandleExecution(player, sign))
            event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lastExecutionByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private boolean tryHandleExecution(Player player, Sign sign) {
    var signSide = sign.getSide(Side.FRONT);
    var command = ComponentUtil.asTrimmedText(signSide.line(COMMAND_LINE));

    while (command.startsWith("/"))
      command = command.substring(1);

    if (command.isEmpty())
      return false;

    var lastExecution = lastExecutionByPlayerId.get(player.getUniqueId());
    var now = System.currentTimeMillis();

    if (lastExecution != null && now - lastExecution < EXECUTE_COOLDOWN_MS)
      return true;

    lastExecutionByPlayerId.put(player.getUniqueId(), now);

    player.performCommand(command);
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean containsMarkerLine(List<Component> lines) {
    var markerContent = ComponentUtil.asTrimmedText(lines.get(MARKER_LINE));
    return markerContent.equalsIgnoreCase(SIGN_MARKER);
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
