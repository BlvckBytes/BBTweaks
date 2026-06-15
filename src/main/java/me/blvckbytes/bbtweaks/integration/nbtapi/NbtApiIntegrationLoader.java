package me.blvckbytes.bbtweaks.integration.nbtapi;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Consumer;

public class NbtApiIntegrationLoader {

  private static final NbtApiIntegration STUBBED_NBT_API_INTEGRATION = new NbtApiIntegration() {

    @Override
    public boolean isAvailable() {
      return false;
    }

    @Override
    public void tryLoadOfflineInventory(File playerDataFile, Consumer<@Nullable OfflineInventorySnapshot> handler) {
      throw new UnsupportedOperationException();
    }
  };

  @WrappedDependency
  public final NbtApiIntegration nbtApiIntegration;

  public NbtApiIntegrationLoader(Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("NBTAPI")) {
      plugin.getLogger().warning("Could not integrate with NBTAPI, seeing how it was not loaded");
      nbtApiIntegration = STUBBED_NBT_API_INTEGRATION;
      return;
    }

    nbtApiIntegration = new NbtApiIntegrationImpl(plugin);

    plugin.getLogger().info("Integrated with NBTAPI");
  }
}
