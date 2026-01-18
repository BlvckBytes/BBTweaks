package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.block.Block;

public interface MechanicInstance {

  Block getSignBlock();

  void tick(int time);

}
