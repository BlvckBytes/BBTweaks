package me.blvckbytes.bbtweaks.integration.craftbook;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CraftBookIntegrationSingleton {

  private static CraftBookIntegrationSingleton instance;

  private @Nullable CraftBookIntegration integration;

  public CraftBookIntegrationSingleton(Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("CraftBook"))
      return;

    var listener = new CraftBookIntegrationImpl();
    Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
    this.integration = listener;
  }

  public List<ItemStack> requestPipeAndGetLeftovers(Block inputPistonBlock, List<ItemStack> items) {
    if (integration == null)
      return items;

    return integration.requestPipeAndGetLeftovers(inputPistonBlock, items);
  }

  public static @Nullable CraftBookIntegrationSingleton getInstance() {
    return instance;
  }

  public static void initializeInstance(Plugin plugin) {
    if (instance != null)
      return;

    instance = new CraftBookIntegrationSingleton(plugin);
  }
}
