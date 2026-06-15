package me.blvckbytes.bbtweaks.integration.nbtapi;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Level;

public class NbtApiIntegrationImpl implements NbtApiIntegration {

  private final Plugin plugin;

  public NbtApiIntegrationImpl(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public void tryLoadOfflineInventory(File playerDataFile, Consumer<@Nullable OfflineInventorySnapshot> handler) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var inventoryContents = new ItemStack[9 * 4];
      var enderChestContents = new ItemStack[9 * 4];

      try {
        var nbtFile = NBT.readFile(playerDataFile);

        var enderItemsList = nbtFile.getCompoundList("EnderItems");

        if (enderItemsList == null)
          throw new IllegalStateException("Could not locate the \"EnderItems\"-tag");

        for (var itemCompound : enderItemsList) {
          var slot = itemCompound.getByte("Slot");

          if (slot == null)
            throw new IllegalStateException("An ender-chest item was missing the \"Slot\"-value");

          if (slot >= 0 && slot < enderChestContents.length)
            enderChestContents[slot] = compoundToItem(itemCompound);
        }

        for (var i = 0; i < enderChestContents.length; ++i) {
          if (enderChestContents[i] == null)
            enderChestContents[i] = makeAirStack();
        }

        var inventoryItemsList = nbtFile.getCompoundList("Inventory");

        if (inventoryItemsList == null)
          throw new IllegalStateException("Could not locate the \"Inventory\"-tag");

        for (var itemCompound : inventoryItemsList) {
          var slot = itemCompound.getByte("Slot");

          if (slot == null)
            throw new IllegalStateException("An inventory item was missing the \"Slot\"-value");

          if (slot >= 0 && slot < inventoryContents.length)
            inventoryContents[slot] = compoundToItem(itemCompound);
        }

        for (var i = 0; i < inventoryContents.length; ++i) {
          if (inventoryContents[i] == null)
            inventoryContents[i] = makeAirStack();
        }

        var equipmentTag = nbtFile.getCompound("equipment");

        var offlineInventory = new OfflineInventorySnapshot(
          inventoryContents,
           equipmentTag == null ? makeAirStack() : compoundToItem(equipmentTag.getCompound("head")),
           equipmentTag == null ? makeAirStack() : compoundToItem(equipmentTag.getCompound("chest")),
           equipmentTag == null ? makeAirStack() : compoundToItem(equipmentTag.getCompound("legs")),
           equipmentTag == null ? makeAirStack() : compoundToItem(equipmentTag.getCompound("feet")),
           equipmentTag == null ? makeAirStack() : compoundToItem(equipmentTag.getCompound("offhand")),
          enderChestContents
        );

        Bukkit.getScheduler().runTask(plugin, () -> handler.accept(offlineInventory));
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to read a player-data file", e);
        Bukkit.getScheduler().runTask(plugin, () -> handler.accept(null));
      }
    });
  }

  private ItemStack compoundToItem(@Nullable ReadWriteNBT compound) {
    if (compound == null)
      return makeAirStack();

    return NBT.itemStackFromNBT(compound);
  }

  private ItemStack makeAirStack() {
    return new ItemStack(Material.AIR, 1);
  }
}
