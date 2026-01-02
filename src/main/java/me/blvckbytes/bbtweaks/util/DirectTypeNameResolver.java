package me.blvckbytes.bbtweaks.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DirectTypeNameResolver implements TypeNameResolver {

  @Override
  public String resolve(Player player, Material type) {
    return type.name();
  }
}
