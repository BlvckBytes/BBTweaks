package me.blvckbytes.bbtweaks.durability_warnings;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public enum PlayerHand {
  MAIN_HAND {
    @Override
    public ItemStack accessItem(PlayerInventory inventory) {
      return inventory.getItemInMainHand();
    }

    @Override
    public int accessSlotIndex(PlayerInventory inventory) {
      return inventory.getHeldItemSlot();
    }
  },

  OFF_HAND {
    @Override
    public ItemStack accessItem(PlayerInventory inventory) {
      return inventory.getItemInOffHand();
    }

    @Override
    public int accessSlotIndex(PlayerInventory inventory) {
      return OFFHAND_SLOT_INDEX;
    }
  },
  ;

  public static final int OFFHAND_SLOT_INDEX = 40;

  public abstract ItemStack accessItem(PlayerInventory inventory);
  public abstract int accessSlotIndex(PlayerInventory inventory);

  public static @Nullable PlayerHand getFromEquipmentSlot(@Nullable EquipmentSlot equipmentSlot) {
    if (equipmentSlot == null)
      return null;

    return switch (equipmentSlot) {
      case HAND -> MAIN_HAND;
      case OFF_HAND -> OFF_HAND;
      default -> null;
    };
  }
}
