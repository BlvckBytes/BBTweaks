package me.blvckbytes.bbtweaks.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public interface TypeNameResolver {

  String resolve(Player player, Material type);

  static TypeNameResolver load(Logger logger) {
    if (Bukkit.getPluginManager().isPluginEnabled("ItemPredicateParser")) {
      var resolver = new ItemPredicateParserTypeNameResolver();
      logger.info("Hooked into ItemPredicateParser, as to enable the language-specific name-resolver");
      return resolver;
    }

    logger.warning("Could not hook into ItemPredicateParser, as to enable the language-specific name-resolver");
    return new DirectTypeNameResolver();
  }
}
