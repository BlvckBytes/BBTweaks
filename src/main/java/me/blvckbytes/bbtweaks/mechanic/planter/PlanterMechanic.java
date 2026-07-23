package me.blvckbytes.bbtweaks.mechanic.planter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlanterMechanic extends BaseMechanic<PlanterInstance> {

  private static final int RADIUS_LINE_INDEX = 3;

  public PlanterMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  public boolean onInstanceClick(Player player, PlanterInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Planter");
  }

  @Override
  public @Nullable PlanterInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.planter")) {
      config.rootSection.mechanic.planter.noPermission.sendMessage(creator);
      return null;
    }

    var radiusString = SignUtil.getPlainTextLine(sign, side, RADIUS_LINE_INDEX).trim();

    if (radiusString.isEmpty()) {
      if (creator != null)
        config.rootSection.mechanic.planter.missingRadius.sendMessage(creator);

      return null;
    }

    int radius;

    try {
      radius = Integer.parseInt(radiusString);

      if (radius <= 0)
        throw new IllegalArgumentException();
    } catch (Throwable e) {
      if (creator != null) {
        config.rootSection.mechanic.planter.nonPositiveRadius.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("input", radiusString)
        );
      }

      return null;
    }

    if (radius > config.rootSection.mechanic.planter.maximumRadius) {
      if (creator != null) {
        config.rootSection.mechanic.planter.exceededMaximumRadius.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("radius", radius)
            .withVariable("maximum", config.rootSection.mechanic.planter.maximumRadius)
        );
      }
      return null;
    }

    var instance = new PlanterInstance(sign, side, radius);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null) {
      config.rootSection.mechanic.planter.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("x", sign.getX())
          .withVariable("y", sign.getY())
          .withVariable("z", sign.getZ())
      );
    }

    return instance;
  }
}
