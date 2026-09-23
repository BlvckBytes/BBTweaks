package me.blvckbytes.bbtweaks.multi_break;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public enum ToolType {

  SHEAR(
    blockMaterials -> {
      blockMaterials.addAll(Tag.LEAVES.getValues());
      blockMaterials.addAll(Tag.WOOL.getValues());
      blockMaterials.addAll(Tag.WOOL_CARPETS.getValues());
      blockMaterials.add(Material.COBWEB);

      try {
        blockMaterials.addAll(Tag.WOOL_SLABS.getValues());
        blockMaterials.addAll(Tag.WOOL_STAIRS.getValues());
      } catch (Throwable _) {}
    },
    toolItemMaterials -> {
      toolItemMaterials.add(Material.SHEARS);
    }
  ),

  SWORD(
    blockMaterials -> {
      blockMaterials.addAll(Tag.SWORD_INSTANTLY_MINES.getValues());
    },
    toolItemMaterials -> {
      toolItemMaterials.addAll(Tag.ITEMS_SWORDS.getValues());
    }
  ),

  AXE(
    blockMaterials -> {
      blockMaterials.addAll(Tag.MINEABLE_AXE.getValues());
    },
    toolItemMaterials -> {
      toolItemMaterials.addAll(Tag.ITEMS_AXES.getValues());
    }
  ),

  PICKAXE(
    blockMaterials -> {
      blockMaterials.addAll(Tag.MINEABLE_PICKAXE.getValues());
    },
    toolItemMaterials -> {
      toolItemMaterials.addAll(Tag.ITEMS_PICKAXES.getValues());
    }
  ),

  SHOVEL(
    blockMaterials -> {
      blockMaterials.addAll(Tag.MINEABLE_SHOVEL.getValues());
    },
    toolItemMaterials -> {
      toolItemMaterials.addAll(Tag.ITEMS_SHOVELS.getValues());
    }
  ),

  HOE(
    blockMaterials -> {
      blockMaterials.addAll(Tag.MINEABLE_HOE.getValues());
    },
    toolItemMaterials -> {
      toolItemMaterials.addAll(Tag.ITEMS_HOES.getValues());
    }
  ),
  ;

  public static final List<ToolType> ALL_VALUES = List.of(values());

  private final EnumSet<Material> blockMaterials;
  private final EnumSet<Material> toolItemMaterials;

  ToolType(
    Consumer<Set<Material>> blockMaterialsPopulator,
    Consumer<Set<Material>> toolItemMaterialsPopulator
  ) {
    this.blockMaterials = EnumSet.noneOf(Material.class);
    blockMaterialsPopulator.accept(blockMaterials);

    this.toolItemMaterials = EnumSet.noneOf(Material.class);
    toolItemMaterialsPopulator.accept(toolItemMaterials);
  }

  public boolean isRightToolForBlockType(Material blockType) {
    return blockMaterials.contains(blockType);
  }

  public boolean isRepresentedByItemType(Material toolItemType) {
    return toolItemMaterials.contains(toolItemType);
  }

  public static @Nullable ToolType determineForBlock(Material blockType) {
    for (var toolType : ALL_VALUES) {
      if (toolType.isRightToolForBlockType(blockType))
        return toolType;
    }

    return null;
  }
}
