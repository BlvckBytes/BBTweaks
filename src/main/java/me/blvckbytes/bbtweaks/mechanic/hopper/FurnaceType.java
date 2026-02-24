package me.blvckbytes.bbtweaks.mechanic.hopper;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public enum FurnaceType {
  FURNACE,
  BLAST_FURNACE,
  SMOKER,
  ;

  public static @Nullable FurnaceType fromBlockMaterial(Material blockMaterial) {
    return switch (blockMaterial) {
      case FURNACE -> FURNACE;
      case BLAST_FURNACE -> BLAST_FURNACE;
      case SMOKER -> SMOKER;
      default -> null;
    };
  }
}
