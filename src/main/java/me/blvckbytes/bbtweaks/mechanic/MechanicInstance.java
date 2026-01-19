package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.block.Block;

public interface MechanicInstance {

  Block getSignBlock();

  /**
   * @return Whether the tick was successful; unsuccessful ticks will result in self-destruction.
   */
  boolean tick(int time);

}
