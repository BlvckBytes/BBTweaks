package me.blvckbytes.bbtweaks.mechanic.util;

public class IntTuple {

  public static long create(int first, int second) {
    return ((long) first & 0xFFFFFFFFL) | ((long) second << 32);
  }

  public static int getFirst(long tuple) {
    return (int) (tuple & 0xFFFFFFFFL);
  }

  public static int getSecond(long tuple) {
    return (int) (tuple >> 32);
  }
}
