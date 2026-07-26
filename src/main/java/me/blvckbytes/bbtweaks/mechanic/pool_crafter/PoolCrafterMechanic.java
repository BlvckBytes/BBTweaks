package me.blvckbytes.bbtweaks.mechanic.pool_crafter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.RecipeCache;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PoolCrafterMechanic extends BaseMechanic<PoolCrafterInstance> implements SimilarMaterialsResolver {

  private static final Set<Tag<Material>> SIMILAR_MATERIAL_TAGS = Set.of(
    Tag.PLANKS,
    Tag.WOODEN_SLABS,
    Tag.WOOL,
    Tag.ITEMS_DYES,
    Tag.ITEMS_STONE_CRAFTING_MATERIALS
  );

  private final RecipeCache recipeCache;
  private final EnumMap<Material, List<Material>> similarMaterialsMap;

  private final CacheByPosition<PoolCrafterInstance> instanceByMountBlockPosition;

  public PoolCrafterMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    RecipeCache recipeCache
  ) {
    super(plugin, config);

    this.recipeCache = recipeCache;

    this.similarMaterialsMap = new EnumMap<>(Material.class);

    this.instanceByMountBlockPosition = new CacheByPosition<>();

    for (var material : Material.values()) {
      if (!material.isItem())
        continue;

      for (var tag : SIMILAR_MATERIAL_TAGS) {
        if (!tag.isTagged(material))
          continue;

        if (similarMaterialsMap.containsKey(material))
          throw new IllegalArgumentException("A material matched multiple tags: " + material);

        similarMaterialsMap.put(material, List.copyOf(tag.getValues()));
      }
    }
  }

  @Override
  public boolean onInstanceClick(Player player, PoolCrafterInstance instance, boolean wasLeftClick) {
    if (!player.isSneaking() || wasLeftClick)
      return false;

    var sign = instance.getSign();

    if (!canEditSign(player, sign)) {
      config.rootSection.mechanic.poolCrafter.cannotEditSign.sendMessage(player);
      return true;
    }

    config.rootSection.mechanic.poolCrafter.selectedRecipeResults.sendMessage(
      player,
      getSignEnvironment(sign)
        .withVariable(
          "recipe_result_type_keys",
          instance.getCachedRecipes().stream()
            .map(it -> it.getResultCopy().getType().translationKey())
            .toList()
        )
    );

    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("PoolCrafter");
  }

  @Override
  public @Nullable PoolCrafterInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.pool-crafter")) {
      config.rootSection.mechanic.poolCrafter.noPermission.sendMessage(creator);
      return null;
    }

    var instance = new PoolCrafterInstance(sign, side, recipeCache, this);

    var mountBlock = instance.getMountBlock();

    if (BlockUtil.isBlockLoaded(mountBlock)) {
      if (
        mountBlock.getType() != Material.DROPPER
          || (!(mountBlock.getState(false) instanceof Container container))
      ) {
        if (creator != null)
          config.rootSection.mechanic.poolCrafter.notOnADropper.sendMessage(creator);

        return null;
      }

      if (checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
        if (creator != null) {
          config.rootSection.mechanic.poolCrafter.existingSign.sendMessage(
            creator,
            new InterpretationEnvironment()
              .withVariable("x", mountBlock.getX())
              .withVariable("y", mountBlock.getY())
              .withVariable("z", mountBlock.getZ())
          );
        }

        return null;
      }
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceByMountBlockPosition.put(mountBlock.getWorld(), mountBlock.getX(), mountBlock.getY(), mountBlock.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.poolCrafter.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }

  @Override
  public @Nullable PoolCrafterInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null) {
      var mountBlock = instance.getMountBlock();
      instanceByMountBlockPosition.invalidate(mountBlock.getWorld(), mountBlock.getX(), mountBlock.getY(), mountBlock.getZ());
    }

    return instance;
  }

  @Override
  public List<Material> resolveSimilarMaterials(Material material) {
    return similarMaterialsMap.computeIfAbsent(material, Collections::singletonList);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
  public void onDropperDrop(BlockDispenseEvent event) {
    var block = event.getBlock();

    if (instanceByMountBlockPosition.get(block.getWorld(), block.getX(), block.getY(), block.getZ()) != null)
      event.setCancelled(true);
  }
}
