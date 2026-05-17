package me.blvckbytes.bbtweaks.durability_warnings;

import org.bukkit.entity.Player;

public class WarningsProfile {

  public boolean playSound = true;
  public boolean enabled = true;

  public final Player player;

  public WarningsProfile(Player player) {
    this.player = player;
  }
}
