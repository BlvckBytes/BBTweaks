package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import org.bukkit.entity.Player;

public class EditSession extends VisualizeSession {

  public final MagnetParameters parameters;

  private MagnetParameter currentParameter;
  private Cuboid currentCuboid;

  public EditSession(Player player, MagnetParameters parameters) {
    super(player, -1);

    this.parameters = parameters;
    this.currentCuboid = parameters.makeCuboid();

    currentParameter = parameters.getFirst();
  }

  public void nextParameter() {
    currentParameter = parameters.getNext(currentParameter);
  }

  public MagnetParameter getCurrentParameter() {
    return currentParameter;
  }

  public boolean increaseParameter() {
    if (!currentParameter.setValueAndGetIfValid(currentParameter.getValue() + 1))
      return false;

    currentCuboid = parameters.makeCuboid();
    return true;
  }

  public boolean decreaseParameter() {
    if (!(currentParameter.setValueAndGetIfValid(currentParameter.getValue() - 1)))
      return false;

    currentCuboid = parameters.makeCuboid();
    return true;
  }

  @Override
  public Cuboid getCuboid() {
    return currentCuboid;
  }
}
