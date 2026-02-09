package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AutoDisposeMechanic extends BaseMechanic<AutoDisposeInstance> implements Listener {

  public AutoDisposeMechanic(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, AutoDisposeInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("AutoDispose");
  }

  @Override
  public @Nullable AutoDisposeInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.auto-dispose")) {
      config.rootSection.mechanic.autoDispose.noPermission.sendMessage(creator);
      return null;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var mountBlock = signBlock.getRelative(signFacing.getOppositeFace());

    if (!(mountBlock.getState() instanceof Container container)) {
      if (creator != null)
        config.rootSection.mechanic.autoDispose.noContainer.sendMessage(creator, environment);

      return null;
    }

    if (SignUtil.checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
      if (creator != null)
        config.rootSection.mechanic.autoDispose.existingSign.sendMessage(creator, environment);

      return null;
    }

    var instance = new AutoDisposeInstance(sign, mountBlock, config);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.autoDispose.creationSuccess.sendMessage(creator, environment);

    return instance;
  }
}
