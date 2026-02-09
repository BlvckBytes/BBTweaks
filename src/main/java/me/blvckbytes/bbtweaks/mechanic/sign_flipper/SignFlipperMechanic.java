package me.blvckbytes.bbtweaks.mechanic.sign_flipper;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SignFlipperMechanic extends BaseMechanic<SignFlipperInstance> {

  public SignFlipperMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, SignFlipperInstance instance, boolean wasLeftClick) {
    if (!player.isSneaking() || wasLeftClick)
      return false;

    var lastState = instance.getLastState();

    config.rootSection.mechanic.signFlipper.currentState.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("state", lastState.name())
    );

    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("SignFlipper");
  }

  @Override
  public @Nullable SignFlipperInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.sign-flipper")) {
      config.rootSection.mechanic.signFlipper.noPermission.sendMessage(creator);
      return null;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var oppositeFacing = signFacing.getOppositeFace();

    var adjacentSign = signBlock.getRelative(
      oppositeFacing.getModX() * 2,
      0,
      oppositeFacing.getModZ() * 2
    );

    if (!(Tag.WALL_SIGNS.isTagged(adjacentSign.getType()))) {
      if (creator != null)
        config.rootSection.mechanic.signFlipper.noAdjacentSign.sendMessage(creator, environment);

      return null;
    }

    var instance = new SignFlipperInstance(plugin, sign, adjacentSign);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.signFlipper.creationSuccess.sendMessage(creator, environment);

    return instance;
  }
}
