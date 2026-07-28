package me.blvckbytes.bbtweaks.pipes.command.wireless_pipe;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public record InteractionSession(
  Player player,
  long createdAt,
  Block firstSignBlock,
  String commandLabel
) {}
