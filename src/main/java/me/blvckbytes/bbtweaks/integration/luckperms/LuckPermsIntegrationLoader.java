package me.blvckbytes.bbtweaks.integration.luckperms;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;

public class LuckPermsIntegrationLoader {

  @WrappedDependency
  public final LuckPerms luckPerms;

  public LuckPermsIntegrationLoader() {
    var luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);

    if (luckPermsProvider == null)
      throw new IllegalStateException("Could not locate registration for the LuckPerms API");

    this.luckPerms = luckPermsProvider.getProvider();
  }
}
