package me.blvckbytes.bbtweaks.multi_break;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Skull;
import org.bukkit.block.data.type.WallSkull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public record DamageableHotbarItem(int slotIndex, ItemStack item, Damageable itemMeta, boolean hasSilkTouch) {

  // One point will always be removed after the origin-block's break-event returns.
  // Let's rather keep some tolerance on this than to try and maximize usage.
  private static final int MIN_TOOL_HEALTH = 10;

  private static final EnumSet<Material> ANY_TOOL_BLOCK_TYPES;

  static {
    ANY_TOOL_BLOCK_TYPES = EnumSet.noneOf(Material.class);

    // There are so many exceptions... But we cannot just allow all of them, as they also contain
    // blocks which absolutely must not be batch-destroyable with multi-break. For now, this
    // explicit, manually maintained list must do.

    ANY_TOOL_BLOCK_TYPES.addAll(Tag.BEDS.getValues());
    ANY_TOOL_BLOCK_TYPES.addAll(Tag.SAPLINGS.getValues());
    ANY_TOOL_BLOCK_TYPES.addAll(Tag.CANDLES.getValues());
    ANY_TOOL_BLOCK_TYPES.addAll(Tag.CANDLE_CAKES.getValues());
    ANY_TOOL_BLOCK_TYPES.addAll(Tag.FLOWER_POTS.getValues());
    ANY_TOOL_BLOCK_TYPES.addAll(Tag.FLOWERS.getValues());
    ANY_TOOL_BLOCK_TYPES.addAll(Tag.CROPS.getValues());

    ANY_TOOL_BLOCK_TYPES.add(Material.SLIME_BLOCK);
    ANY_TOOL_BLOCK_TYPES.add(Material.SEA_LANTERN);
    ANY_TOOL_BLOCK_TYPES.add(Material.GLOWSTONE);
    ANY_TOOL_BLOCK_TYPES.add(Material.TNT);
    ANY_TOOL_BLOCK_TYPES.add(Material.LEVER);
    ANY_TOOL_BLOCK_TYPES.add(Material.LEAF_LITTER);
    ANY_TOOL_BLOCK_TYPES.add(Material.POWDER_SNOW);
    ANY_TOOL_BLOCK_TYPES.add(Material.SCAFFOLDING);
    ANY_TOOL_BLOCK_TYPES.add(Material.REDSTONE_LAMP);
    ANY_TOOL_BLOCK_TYPES.add(Material.REDSTONE_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.REDSTONE_WALL_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.REDSTONE_WIRE);
    ANY_TOOL_BLOCK_TYPES.add(Material.REPEATER);
    ANY_TOOL_BLOCK_TYPES.add(Material.COMPARATOR);
    ANY_TOOL_BLOCK_TYPES.add(Material.TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.WALL_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.SOUL_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.SOUL_WALL_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.COPPER_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.COPPER_WALL_TORCH);
    ANY_TOOL_BLOCK_TYPES.add(Material.RESIN_BLOCK);
    ANY_TOOL_BLOCK_TYPES.add(Material.RESIN_CLUMP);

    for (var material : Material.values()) {
      if (!material.isBlock())
        continue;

      if (material.name().contains("FROGLIGHT")) {
        ANY_TOOL_BLOCK_TYPES.add(material);
        continue;
      }

      var blockData = material.createBlockData();

      // This then properly includes skulls, wall-skulls, player-head, etc.
      if (blockData instanceof Skull || blockData instanceof WallSkull)
        ANY_TOOL_BLOCK_TYPES.add(material);
    }
  }

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

    return new DamageableHotbarItem(slotIndex, currentItem, damageable, damageable.hasEnchant(Enchantment.SILK_TOUCH));
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isRightToolForBlock(ItemStack tool, Block block) {
    if (tool.getType().isAir())
      return false;

    var blockType = block.getType();

    if (ANY_TOOL_BLOCK_TYPES.contains(blockType))
      return true;

    var requiredToolType = ToolType.determineForBlock(blockType);

    if (requiredToolType == null) {
      var toolSpeed = getToolSpeed(tool, blockType);
      return toolSpeed != null && toolSpeed > 1;
    }

    if (!requiredToolType.isRepresentedByItemType(tool.getType()))
      return false;

    // Make sure to not drop, say, diamonds when breaking diamond-ore with a wooden pickaxe.
    if (requiredToolType == ToolType.PICKAXE)
      return block.isPreferredTool(tool);

    return true;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static @Nullable Float getToolSpeed(ItemStack item, Material block) {
    var meta = item.getItemMeta();

    if (!meta.hasTool())
      return null;

    for (var rule : meta.getTool().getRules()) {
      if (!rule.getBlocks().contains(block))
        continue;

      var speed = rule.getSpeed();

      if (speed != null)
        return speed;
    }

    return null;
  }
}
