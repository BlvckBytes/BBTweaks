package me.blvckbytes.bbtweaks.durability_warnings;

import java.util.Arrays;

public class IntRingbuffer {

  private final int[] values;
  private int nextWriteIndex;

  public IntRingbuffer(int size) {
    this.values = new int[size];
  }

  public void clear() {
    Arrays.fill(values, 0);
    nextWriteIndex = 0;
  }

  public int calculateSum() {
    var result = 0;

    for (var value : values)
      result += value;

    return result;
  }

  public void add(int value) {
    values[nextWriteIndex] = value;

    if (++nextWriteIndex == this.values.length)
      nextWriteIndex = 0;
  }
}
