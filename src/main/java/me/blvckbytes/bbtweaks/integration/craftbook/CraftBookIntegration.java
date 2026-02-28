package me.blvckbytes.bbtweaks.integration.craftbook;

import com.sk89q.craftbook.mechanics.pipe.PipeFinishEvent;
import com.sk89q.craftbook.mechanics.pipe.PipeRequestEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CraftBookIntegration implements Listener {

  public static final CraftBookIntegration INSTANCE = new CraftBookIntegration();

  private final boolean hasCraftBook;

  private @Nullable Block lastRequestInputPistonBlock;
  private @Nullable List<ItemStack> leftovers;

  private CraftBookIntegration() {
    this.hasCraftBook = Bukkit.getPluginManager().isPluginEnabled("CraftBook");
  }

  public List<ItemStack> requestPipeAndGetLeftovers(Block inputPistonBlock, List<ItemStack> items) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Only call this method on the main thread");

    if (!hasCraftBook)
      return items;

    var event = new PipeRequestEvent(inputPistonBlock, items, inputPistonBlock);

    this.lastRequestInputPistonBlock = inputPistonBlock;
    this.leftovers = new ArrayList<>(items.size());

    Bukkit.getPluginManager().callEvent(event);

    var eventLeftovers = this.leftovers;

    this.lastRequestInputPistonBlock = null;
    this.leftovers = null;

    return eventLeftovers == null ? Collections.emptyList() : eventLeftovers;
  }

  @EventHandler
  public void onPipeFinish(PipeFinishEvent event) {
    if (leftovers == null || lastRequestInputPistonBlock == null)
      return;

    if (event.getBlock() == lastRequestInputPistonBlock) {
      var eventLeftovers = event.getItems();
      leftovers.addAll(eventLeftovers);
      eventLeftovers.clear();
    }
  }
}
