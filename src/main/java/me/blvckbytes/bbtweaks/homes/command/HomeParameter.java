package me.blvckbytes.bbtweaks.homes.command;

import me.blvckbytes.bbtweaks.homes.storage.KnownPlayer;

public record HomeParameter(
  KnownPlayer target,
  String homeName
) {}
