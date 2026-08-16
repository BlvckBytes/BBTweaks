package me.blvckbytes.bbtweaks.integration.essentials;

import com.earth2me.essentials.IEssentials;
import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;

public class EssentialsIntegrationLoader {

  @WrappedDependency
  public final IEssentials essentials;

  public EssentialsIntegrationLoader() {
    this.essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

    if (essentials == null)
      throw new IllegalStateException("Expected Essentials to be loaded");
  }
}
