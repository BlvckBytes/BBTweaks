package me.blvckbytes.bbtweaks.hotbar_randomizer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.concurrent.ThreadLocalRandom;

public class HotbarRandomizerListener implements Listener {

  private final HotbarRandomizerSettingsStore settingsStore;

  public HotbarRandomizerListener(HotbarRandomizerSettingsStore settingsStore) {
    this.settingsStore = settingsStore;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    var player = event.getPlayer();
    var settings = settingsStore.accessSettings(player);

    if (!settings.enabled)
      return;

    if (settings.doesLackPermission())
      return;

    var playerInventory = player.getInventory();

    var enabledSlotIndices = settings.collectEnabledSlotIndices(slotIndex -> {
      var itemAtSlot = playerInventory.getItem(slotIndex);
      return itemAtSlot != null && !itemAtSlot.getType().isAir();
    });

    if (enabledSlotIndices.isEmpty())
      return;

    var selectedSlotIndex = playerInventory.getHeldItemSlot();

    // Let's not randomize if the player didn't place from an enabled slot, as to
    // allow very granular use of the feature without any disturbances.
    if (!enabledSlotIndices.contains(selectedSlotIndex))
      return;

    var randomIndicesIndex = ThreadLocalRandom.current().nextInt(enabledSlotIndices.size());
    var randomSlotIndex = enabledSlotIndices.getInt(randomIndicesIndex);

    playerInventory.setHeldItemSlot(randomSlotIndex);
  }
}
