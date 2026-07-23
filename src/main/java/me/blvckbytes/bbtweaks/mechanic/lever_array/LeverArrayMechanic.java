package me.blvckbytes.bbtweaks.mechanic.lever_array;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.interpreter.ValueInterpreter;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LeverArrayMechanic extends BaseMechanic<LeverArrayInstance> {

  private static final int PROPAGATION_SPEED_ENABLE_LINE_INDEX = 2;
  private static final int PROPAGATION_SPEED_DISABLE_LINE_INDEX = 3;

  public LeverArrayMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config);
  }

  @Override
  public boolean onInstanceClick(Player player, LeverArrayInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("LeverArray");
  }

  @Override
  public @Nullable LeverArrayInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.lever-array")) {
      config.rootSection.mechanic.leverArray.noPermission.sendMessage(creator);
      return null;
    }

    var propagationSpeedEnableExpression = ComponentUtil.asTrimmedText(sign.getSide(side).line(PROPAGATION_SPEED_ENABLE_LINE_INDEX));
    int propagationSpeedEnable = -1;

    if (!propagationSpeedEnableExpression.isBlank()) {
      var parseResult = parseExpression(propagationSpeedEnableExpression, new InterpretationEnvironment(), ValueInterpreter::asLong);

      if (parseResult.isEmpty()) {
        config.rootSection.mechanic.leverArray.propagationSpeedEnableMalformedExpression.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("input", propagationSpeedEnableExpression)
        );

        return null;
      }

      propagationSpeedEnable = parseResult.get().intValue();

      if (propagationSpeedEnable < 0) {
        config.rootSection.mechanic.leverArray.propagationSpeedEnableNegative.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("speed", propagationSpeedEnable)
        );

        return null;
      }
    }

    var propagationSpeedDisableExpression = ComponentUtil.asTrimmedText(sign.getSide(side).line(PROPAGATION_SPEED_DISABLE_LINE_INDEX));
    int propagationSpeedDisable = -1;

    if (!propagationSpeedDisableExpression.isBlank()) {
      var parseResult = parseExpression(propagationSpeedDisableExpression, new InterpretationEnvironment(), ValueInterpreter::asLong);

      if (parseResult.isEmpty()) {
        config.rootSection.mechanic.leverArray.propagationSpeedDisableMalformedExpression.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("input", propagationSpeedDisableExpression)
        );

        return null;
      }

      propagationSpeedDisable = parseResult.get().intValue();

      if (propagationSpeedDisable < 0) {
        config.rootSection.mechanic.leverArray.propagationSpeedDisableNegative.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("speed", propagationSpeedDisable)
        );

        return null;
      }
    }

    var instance = new LeverArrayInstance(sign, side, propagationSpeedEnable, propagationSpeedDisable);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null) {
      config.rootSection.mechanic.leverArray.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("x", sign.getX())
          .withVariable("y", sign.getY())
          .withVariable("z", sign.getZ())
          .withVariable("propagation_speed_enable", propagationSpeedEnable)
          .withVariable("propagation_speed_disable", propagationSpeedDisable)
      );
    }

    return instance;
  }
}
