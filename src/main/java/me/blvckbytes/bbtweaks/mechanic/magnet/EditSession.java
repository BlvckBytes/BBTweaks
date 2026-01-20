package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.mechanic.util.Axis;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EditSession extends VisualizeSession {

  public final MagnetParameters parameters;

  private final Consumer<Boolean> afterWriting;
  private final Runnable afterCancelling;

  public boolean clickDetection;

  private MagnetParameter currentParameter;
  private Cuboid currentCuboid;

  public EditSession(
    Player player, MagnetParameters parameters,
    Consumer<Boolean> afterWriting, Runnable afterCancelling
  ) {
    super(player, parameters.sign, -1);

    this.parameters = parameters;
    this.afterWriting = afterWriting;
    this.afterCancelling = afterCancelling;
    this.currentCuboid = parameters.makeCuboid();
    this.currentParameter = parameters.getFirst();
  }

  public void save() {
    var didWrite = parameters.writeIfDirty();
    afterWriting.accept(didWrite);
    manuallyExpire();
  }

  public void cancel() {
    afterCancelling.run();
    manuallyExpire();
  }

  public void setParameter(MagnetParameter parameter) {
    currentParameter = parameter;
  }

  public MagnetParameter getCurrentParameter() {
    return currentParameter;
  }

  public void increaseParameter() {
    if (!currentParameter.setValueAndGetIfValid(currentParameter.getValue() + 1))
      return;

    parameters.updateAll();
    currentCuboid = parameters.makeCuboid();
  }

  public void decreaseParameter() {
    if (!(currentParameter.setValueAndGetIfValid(currentParameter.getValue() - 1)))
      return;

    parameters.updateAll();
    currentCuboid = parameters.makeCuboid();
  }

  public @Nullable Axis getCurrentlySelectedAxis() {
    if (currentParameter == parameters.extentX || currentParameter == parameters.offsetX)
      return Axis.X;

    if (currentParameter == parameters.extentY || currentParameter == parameters.offsetY)
      return Axis.Y;

    if (currentParameter == parameters.extentZ || currentParameter == parameters.offsetZ)
      return Axis.Z;

    return null;
  }

  @Override
  public Cuboid getCuboid() {
    return currentCuboid;
  }

  public InterpretationEnvironment makeEnvironment() {
    var environment = new InterpretationEnvironment()
      .withVariable("magnet_x", parameters.sign.getX())
      .withVariable("magnet_y", parameters.sign.getY())
      .withVariable("magnet_z", parameters.sign.getZ())
      .withVariable("current_parameter", getCurrentParameter().name)
      .withVariable("click_detection", clickDetection);

    parameters.forEach(parameter -> {
      var variableName = parameter.name.toLowerCase();

      environment
        .withVariable(variableName, parameter.getValue())
        .withVariable(variableName + "_did_exceed_limit", parameter.didLastSetExceedLimit());
    });

    return environment;
  }
}
