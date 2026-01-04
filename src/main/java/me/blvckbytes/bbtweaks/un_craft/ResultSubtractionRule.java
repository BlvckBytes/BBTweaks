package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResultSubtractionRule extends TypeRule {

  public final List<TypeRule> subtractedMaterials;

  public ResultSubtractionRule(ConfigurationSection section, Logger logger) {
    super(section);

    this.subtractedMaterials = new ArrayList<>();

    var temporaryConfig = new YamlConfiguration();
    var entryNumber = 0;

    for (var subtractionEntry : section.getMapList("subtractedMaterials")) {
      ++entryNumber;

      try {
        subtractedMaterials.add(new TypeRule(temporaryConfig.createSection("root", subtractionEntry)));
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Could not parse the #" + entryNumber + " entry of " + section.getCurrentPath() + ".subtractedMaterials", e);
      }
    }
  }
}
