package me.blvckbytes.bbtweaks.infinite_bucket;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class InfiniteWaterbucketListener extends InfiniteBucketListener {

  public InfiniteWaterbucketListener(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(
      Material.WATER_BUCKET,
      new NamespacedKey(plugin, "infinite-waterbucket"),
      "bbtweaks.infinite-waterbucket",
      () -> config.rootSection.infiniteWaterbucket,
      plugin
    );
  }
}
