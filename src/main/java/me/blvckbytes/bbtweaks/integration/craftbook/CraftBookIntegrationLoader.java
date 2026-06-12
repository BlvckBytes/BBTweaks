package me.blvckbytes.bbtweaks.integration.craftbook;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CraftBookIntegrationLoader {

  private static final CraftBookIntegration STUBBED_CRAFT_BOOK_INTEGRATION = new CraftBookIntegration() {

    @Override
    public List<ItemStack> requestPipeAndGetLeftovers(Block inputPistonBlock, List<ItemStack> items) {
      return items;
    }
  };

  @WrappedDependency
  public final CraftBookIntegration integration;

  public CraftBookIntegrationLoader() {
    if (!Bukkit.getPluginManager().isPluginEnabled("CraftBook")) {
      integration = STUBBED_CRAFT_BOOK_INTEGRATION;
      return;
    }

    integration = new CraftBookIntegrationImpl();
  }
}
