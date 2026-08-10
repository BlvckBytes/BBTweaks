package me.blvckbytes.bbtweaks.experience_bottle_yield_adjust;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;

public class ExperienceBottleYieldAdjust implements Listener {

  private final ConfigKeeper<MainSection> config;

  public ExperienceBottleYieldAdjust(ConfigKeeper<MainSection> config) {
    this.config = config;
  }

  @EventHandler
  public void onExpBottle(ExpBottleEvent event) {
    if (config.rootSection.experienceBottleYieldAdjust.enabled)
      event.setExperience(config.rootSection.experienceBottleYieldAdjust.experience);
  }
}
