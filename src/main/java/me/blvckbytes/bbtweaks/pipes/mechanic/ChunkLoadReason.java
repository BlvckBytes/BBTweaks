package me.blvckbytes.bbtweaks.pipes.mechanic;

public enum ChunkLoadReason {
  UPDATE_BLOCK_CACHE(30),
  ACCESS_BLOCK_INVENTORY(60),
  ;

  public final long expiryTimeTicks;

  ChunkLoadReason(long expiryTimeTicks) {
    this.expiryTimeTicks = expiryTimeTicks;
  }
}
