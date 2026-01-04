package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class TypeExclusionRule extends IOTypeRule {

  public final String reason;

  public TypeExclusionRule(ConfigurationSection section) {
    super(section);

    var reason = section.getString("reason");

    if (reason == null || (reason = reason.trim()).isEmpty())
      throw new IllegalStateException("Missing a non-blank reason!");

    this.reason = reason;
  }
}
