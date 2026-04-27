package me.blvckbytes.bbtweaks.multi_break.parameters;

import java.util.Arrays;
import java.util.List;

public enum BreakExtent {
  LEFT,
  RIGHT,
  UP,
  DOWN,
  DEPTH,
  ;

  public static final List<BreakExtent> values = Arrays.asList(values());
}
