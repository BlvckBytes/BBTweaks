package me.blvckbytes.bbtweaks.sidebar;

import org.bukkit.entity.Player;

public record BoardHolder(
  Player bukkitPlayer,
  com.earth2me.essentials.User essentialsUser
) {}
