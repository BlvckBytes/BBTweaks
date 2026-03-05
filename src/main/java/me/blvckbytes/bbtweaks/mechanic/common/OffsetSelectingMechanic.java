package me.blvckbytes.bbtweaks.mechanic.common;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.MechanicInstance;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.bbtweaks.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class OffsetSelectingMechanic<InstanceType extends MechanicInstance> extends BaseMechanic<InstanceType> implements Listener {

  private final int offsetsLineIndex;
  private final Supplier<OffsetSelectingSection> sectionAccessor;

  private final Map<UUID, InstanceSession<InstanceType>> offsetSelectingByPlayerId;

  public OffsetSelectingMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    int offsetsLineIndex,
    Supplier<OffsetSelectingSection> sectionAccessor
  ) {
    super(plugin, config);

    this.offsetsLineIndex = offsetsLineIndex;
    this.sectionAccessor = sectionAccessor;
    this.offsetSelectingByPlayerId = new HashMap<>();
  }

  protected void handleOffsetSelectingQuitEvent(Player player) {
    offsetSelectingByPlayerId.remove(player.getUniqueId());
  }

  protected void handleOffsetSelectingSessionTimeouts(int time) {
    InstanceSession.handleSessionTimeouts(
      offsetSelectingByPlayerId, time,
      sectionAccessor.get().offsetSelectingTimeoutSeconds(),
      (session, timeoutSeconds) -> {
        sectionAccessor.get().blockSelectionTimeout().sendMessage(
          session.player(),
          getSignEnvironment(session.instance().getSign())
            .withVariable("timeout", timeoutSeconds)
        );
      }
    );
  }

  protected void initiateOffsetSelecting(Player player, InstanceType instance) {
    offsetSelectingByPlayerId.put(player.getUniqueId(), new InstanceSession<>(player, instance, getCurrentTime()));

    sectionAccessor.get().blockSelectionPrompt().sendMessage(
      player,
      getSignEnvironment(instance.getSign())
        .withVariable("timeout", sectionAccessor.get().offsetSelectingTimeoutSeconds())
    );
  }

  protected InstanceType validateOffsetsAndMakeInstance(@Nullable Player creator, Sign sign, BiFunction<Sign, Offsets, InstanceType> handler) {
    var offsetTokens = StringUtil.getTokens(SignUtil.getPlainTextLine(sign, offsetsLineIndex));

    if (offsetTokens.isEmpty())
      return handler.apply(sign, Offsets.ZERO);

    try {
      var xOffset = Integer.parseInt(offsetTokens.get(0));
      var yOffset = Integer.parseInt(offsetTokens.get(1));
      var zOffset = Integer.parseInt(offsetTokens.get(2));

      if (areOffsetsInvalid(creator, sign, xOffset, yOffset, zOffset))
        throw new IllegalArgumentException();

      return handler.apply(sign, new Offsets(xOffset, yOffset, zOffset));
    } catch (Throwable e) {
      sign.getSide(Side.FRONT).line(offsetsLineIndex, Component.text("0 0 0"));
      return handler.apply((Sign) sign.getBlock().getState(), Offsets.ZERO);
    }
  }

  protected boolean areOffsetsInvalid(@Nullable Player player, Sign sign, int xOffset, int yOffset, int zOffset) {
    var offsetLimit = sectionAccessor.get().maximumAxisOffset();

    if (Math.abs(xOffset) > offsetLimit || Math.abs(yOffset) > offsetLimit || Math.abs(zOffset) > offsetLimit) {
      if (player != null) {
        sectionAccessor.get().axisOffsetLimitExceeded().sendMessage(
          player,
          getSignEnvironment(sign)
            .withVariable("limit", offsetLimit)
        );
      }

      return true;
    }

    var signFacing = ((Directional) sign.getBlockData()).getFacing();

    if (yOffset == 0 && xOffset == signFacing.getModX() && zOffset == signFacing.getModZ()) {
      if (player != null)
        sectionAccessor.get().triedBindingToSign().sendMessage(player, getSignEnvironment(sign));

      return true;
    }

    return false;
  }

  protected boolean handleOffsetSelecting(Player player, Location location) {
    var offsetSelecting = offsetSelectingByPlayerId.remove(player.getUniqueId());

    if (offsetSelecting == null)
      return false;

    var mountBlock = offsetSelecting.instance().getMountBlock();

    var xOffset = location.getBlockX() - mountBlock.getX();
    var yOffset = location.getBlockY() - mountBlock.getY();
    var zOffset = location.getBlockZ() - mountBlock.getZ();

    var sign = offsetSelecting.instance().getSign();

    if (areOffsetsInvalid(player, sign, xOffset, yOffset, zOffset))
      return true;

    sign.getSide(Side.FRONT).line(offsetsLineIndex, Component.text(xOffset + " " + yOffset + " " + zOffset));
    sign.update(true, false);
    reloadInstanceBySign(sign);

    sectionAccessor.get().blockSelectionSuccess().sendMessage(
      player,
      getSignEnvironment(sign)
        .withVariable("x_offset", xOffset)
        .withVariable("y_offset", yOffset)
        .withVariable("z_offset", zOffset)
    );

    return true;
  }
}
