package me.blvckbytes.bbtweaks.multi_break.parameters;

import java.util.Arrays;
import java.util.List;

public enum BreakDimension {
  WIDTH(BreakExtent.LEFT, BreakExtent.RIGHT),
  HEIGHT(BreakExtent.UP, BreakExtent.DOWN),
  DEPTH(BreakExtent.DEPTH),
  ;

  public static final List<BreakDimension> values = Arrays.asList(values());

  public final BreakExtent[] extents;

  BreakDimension(BreakExtent... extents) {
    this.extents = extents;
  }
}
