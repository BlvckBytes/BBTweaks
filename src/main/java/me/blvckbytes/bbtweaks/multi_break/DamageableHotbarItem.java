package me.blvckbytes.bbtweaks.multi_break;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.Nullable;

public record DamageableHotbarItem(int slotIndex, ItemStack item, Damageable itemMeta) {

  // One point will always be removed after the origin-block's break-event returns.
  // Let's rather keep some tolerance on this than to try and maximize usage.
  private static final int MIN_TOOL_HEALTH = 10;

  public boolean safelyIncrementDamageAndSet(Player player) {
    if (itemMeta.isUnbreakable())
      return true;

    int unbreakingLevel = itemMeta.getEnchantLevel(Enchantment.UNBREAKING);

    if (unbreakingLevel > 0) {
      // If the random falls within the upper interval, unbreaking doesn't protect the item for this action.
      if (Math.random() >= 1.0 / (unbreakingLevel + 1))
        return true;
    }

    var originalDamage = itemMeta.getDamage();
    var maxAddedDamage = item.getType().getMaxDurability() - MIN_TOOL_HEALTH;

    if (originalDamage >= maxAddedDamage)
      return false;

    //noinspection UnstableApiUsage
    var damageEvent = new PlayerItemDamageEvent(player, item, 1, originalDamage);
    Bukkit.getPluginManager().callEvent(damageEvent);

    if (damageEvent.isCancelled())
      return false;

    var addedDamage = damageEvent.getDamage();

    // Let's clamp the value here, as we must not destroy automatically used items,
    // so we force-override the possible intent of the event-handlers.
    if (addedDamage > maxAddedDamage)
      addedDamage = maxAddedDamage;

    itemMeta.setDamage(originalDamage + addedDamage);
    item.setItemMeta(itemMeta);

    return true;
  }

  public static @Nullable DamageableHotbarItem determineToolFromHotbar(Block block, PlayerInventory inventory) {
    DamageableHotbarItem result;

    var heldSlot = inventory.getHeldItemSlot();

    // Prioritize the currently-held tool, as to respect the player's choice.
    if ((result = tryGetToolAtSlot(block, inventory, heldSlot)) != null)
      return result;

    for (var slotIndex = 0; slotIndex < 9; ++slotIndex) {
      if (slotIndex == heldSlot)
        continue;

      if ((result = tryGetToolAtSlot(block, inventory, slotIndex)) != null)
        return result;
    }

    return null;
  }

  private static @Nullable DamageableHotbarItem tryGetToolAtSlot(Block block, PlayerInventory inventory, int slotIndex) {
    var currentItem = inventory.getItem(slotIndex);

    if (currentItem == null || currentItem.getType().isAir())
      return null;

    if (!isRightToolForBlock(currentItem, block))
      return null;

    if (!(currentItem.getItemMeta() instanceof Damageable damageable))
      return null;

    var remainingDamage = currentItem.getType().getMaxDurability() - damageable.getDamage();

    if (!damageable.isUnbreakable() && remainingDamage <= MIN_TOOL_HEALTH)
      return null;

    return new DamageableHotbarItem(slotIndex, currentItem, damageable);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isRightToolForBlock(ItemStack tool, Block block) {
    var blockType = block.getType();

    if (tool.getType() == Material.SHEARS) {
      return (
        Tag.LEAVES.isTagged(blockType)
          || Tag.WOOL.isTagged(blockType)
          || Tag.WOOL_CARPETS.isTagged(blockType)
          || blockType == Material.COBWEB
      );
    }

    if (Tag.MINEABLE_AXE.isTagged(blockType) && Tag.ITEMS_AXES.isTagged(tool.getType()))
      return true;

    if (Tag.MINEABLE_PICKAXE.isTagged(blockType) && Tag.ITEMS_PICKAXES.isTagged(tool.getType()))
      return true;

    if (Tag.MINEABLE_SHOVEL.isTagged(blockType) && Tag.ITEMS_SHOVELS.isTagged(tool.getType()))
      return true;

    if (Tag.MINEABLE_HOE.isTagged(blockType) && Tag.ITEMS_HOES.isTagged(tool.getType()))
      return true;

    return Tag.SWORD_INSTANTLY_MINES.isTagged(blockType) && Tag.ITEMS_SWORDS.isTagged(tool.getType());
  }
}
