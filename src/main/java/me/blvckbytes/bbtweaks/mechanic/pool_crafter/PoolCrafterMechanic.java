package me.blvckbytes.bbtweaks.mechanic.pool_crafter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.RecipeCache;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
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

  public PoolCrafterMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    RecipeCache recipeCache
  ) {
    super(plugin, config);

    this.recipeCache = recipeCache;

    this.similarMaterialsMap = new EnumMap<>(Material.class);

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
  public @Nullable PoolCrafterInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.pool-crafter")) {
      config.rootSection.mechanic.poolCrafter.noPermission.sendMessage(creator);
      return null;
    }

    var instance = new PoolCrafterInstance(sign, recipeCache, this);

    var dropper = instance.getMountBlock();

    if (BlockUtil.isBlockLoaded(dropper)) {
      if (
        dropper.getType() != Material.DROPPER
          || (!(dropper.getState(false) instanceof Container container))
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
              .withVariable("x", dropper.getX())
              .withVariable("y", dropper.getY())
              .withVariable("z", dropper.getZ())
          );
        }

        return null;
      }
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.poolCrafter.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }

  @Override
  public List<Material> resolveSimilarMaterials(Material material) {
    return similarMaterialsMap.computeIfAbsent(material, Collections::singletonList);
  }
}
