package me.blvckbytes.bbtweaks.mechanic.sign_flipper;

import at.blvckbytes.component_markup.util.TriState;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class SignFlipperInstance extends SISOInstance {

  private final NamespacedKey lastStateKey;
  private final Block adjacentSignBlock;

  private TriState lastState = TriState.NULL;

  public SignFlipperInstance(Plugin plugin, Sign sign, Block adjacentSignBlock) {
    super(sign);

    this.lastStateKey = new NamespacedKey(plugin, "last-sign-flipper-state");
    this.adjacentSignBlock = adjacentSignBlock;

    if (adjacentSignBlock.getState() instanceof Sign adjacentSign) {
      var lastStoredState = adjacentSign
        .getPersistentDataContainer()
        .get(lastStateKey, PersistentDataType.BOOLEAN);

      if (lastStoredState != null)
        this.lastState = lastStoredState ? TriState.TRUE : TriState.FALSE;
    }
  }

  public TriState getLastState() {
    return lastState;
  }

  @Override
  public boolean tick(int time) {
    if (!isBlockLoaded(adjacentSignBlock))
      return true;

    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    var currentState = inputPower != 0 ? TriState.TRUE : TriState.FALSE;

    if (currentState == lastState)
      return true;

    if (!(adjacentSignBlock.getState() instanceof Sign adjacentSign))
      return false;

    swapSides(adjacentSign, inputPower != 0);

    lastState = currentState;
    return true;
  }

  private void swapSides(Sign adjacentSign, boolean currentState) {
    var frontSide = adjacentSign.getSide(Side.FRONT);
    var backSide = adjacentSign.getSide(Side.BACK);

    var frontLines = new ArrayList<>(frontSide.lines());
    var backLines = new ArrayList<>(backSide.lines());

    for (var index = 0; index < frontLines.size(); ++index)
      backSide.line(index, frontLines.get(index));

    for (var index = 0; index < backLines.size(); ++index)
      frontSide.line(index, backLines.get(index));

    var frontColor = frontSide.getColor();
    var frontGlow = frontSide.isGlowingText();

    frontSide.setColor(backSide.getColor());
    frontSide.setGlowingText(backSide.isGlowingText());

    backSide.setColor(frontColor);
    backSide.setGlowingText(frontGlow);

    adjacentSign
      .getPersistentDataContainer()
      .set(lastStateKey, PersistentDataType.BOOLEAN, currentState);

    adjacentSign.update();
  }
}
