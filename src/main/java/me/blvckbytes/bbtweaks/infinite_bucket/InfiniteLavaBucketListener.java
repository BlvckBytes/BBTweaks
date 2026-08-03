package me.blvckbytes.bbtweaks.infinite_bucket;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.plugin.Plugin;

public class InfiniteLavaBucketListener extends InfiniteBucketListener {

  public InfiniteLavaBucketListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(
      Material.LAVA_BUCKET,
      new NamespacedKey(plugin, "infinite-lavabucket"),
      "bbtweaks.infinite-lavabucket",
      () -> config.rootSection.infiniteLavaBucket,
      plugin
    );
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onFurnaceBurn(FurnaceBurnEvent event) {
    if (doesContainMarker(event.getFuel().getPersistentDataContainer()))
      event.setCancelled(true);
  }
}
