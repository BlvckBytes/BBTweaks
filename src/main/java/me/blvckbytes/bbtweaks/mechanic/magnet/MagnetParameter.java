package me.blvckbytes.bbtweaks.mechanic.magnet;

import org.jetbrains.annotations.Nullable;

public class MagnetParameter {

  public final String name;

  private int value;
  private @Nullable Integer lastReadValue;

  private final int defaultValue;
  private final ParameterClamp clamp;

  private boolean didLastSetSucceed;

  public MagnetParameter(String name, int defaultValue, ParameterClamp clamp) {
    this.name = name;
    this.defaultValue = clamp.apply(defaultValue);
    this.clamp = clamp;
    this.value = defaultValue;
  }

  public boolean isDirtySinceLastRead() {
    return lastReadValue == null || lastReadValue != value;
  }

  public boolean didLastSetExceedLimit() {
    return !didLastSetSucceed;
  }

  public int getValue() {
    return value;
  }

  public boolean setValueAndGetIfValid(int value) {
    var clampedValue = clamp.apply(value);
    this.value = clampedValue;
    return (didLastSetSucceed = (value == clampedValue));
  }

  public void readFromToken(String token) {
    try {
      lastReadValue = Integer.parseInt(token);
    } catch (NumberFormatException e) {
      lastReadValue = null;
    }

    setValueAndGetIfValid(lastReadValue == null ? defaultValue : lastReadValue);

    // Do not show limits regarding tokens that have been constrained automatically.
    didLastSetSucceed = true;
  }
}
