package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class AutoCrafterMechanic extends BaseMechanic<AutoCrafterInstance> implements RecipeCache {

  static {
    if (Material.values().length > 4096)
      throw new IllegalStateException("There are more than 4K materials, which exceeds our expectation - cannot bit-pack the matrix into two longs without a loss of information!");
  }

  private final List<CachedRecipe> cachedRecipes = new ArrayList<>();

  public AutoCrafterMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    // Let's give other plugins plenty of time to register additional recipes.
    Bukkit.getScheduler().runTaskLater(plugin, this::updateRecipeCache, 20L);
  }

  @Override
  public boolean onInstanceClick(Player player, AutoCrafterInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("AutoCrafter");
  }

  @Override
  public @Nullable AutoCrafterInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.auto-crafter")) {
      config.rootSection.mechanic.autoCrafter.noPermission.sendMessage(creator);
      return null;
    }

    var instance = new AutoCrafterInstance(sign, this);

    if (creator != null && instance.getMountBlock().getType() != Material.CRAFTER) {
      config.rootSection.mechanic.autoCrafter.notOnACrafter.sendMessage(creator);
      return null;
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null) {
      config.rootSection.mechanic.autoCrafter.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("x", sign.getX())
          .withVariable("y", sign.getY())
          .withVariable("z", sign.getZ())
      );
    }

    return instance;
  }

  @Override
  public List<CachedRecipe> getRecipes() {
    return Collections.unmodifiableList(cachedRecipes);
  }

  private void updateRecipeCache() {
    cachedRecipes.clear();

    var recipes = Bukkit.recipeIterator();

    while (recipes.hasNext()) {
      var recipe = recipes.next();

      try {
        if (recipe instanceof ShapedRecipe shapedRecipe) {
          var cachedRecipe = CachedShapedRecipe.createIfValid(shapedRecipe);

          if (cachedRecipe == null) {
            plugin.getLogger().warning("Ignored malformed shaped recipe: " + shapedRecipe.getKey());
            continue;
          }

          cachedRecipes.add(cachedRecipe);
          continue;
        }

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
          var cachedRecipe = CachedShapelessRecipe.createIfValid(shapelessRecipe);

          if (cachedRecipe == null) {
            plugin.getLogger().warning("Ignored malformed shapeless recipe: " + shapelessRecipe.getKey());
            continue;
          }

          cachedRecipes.add(cachedRecipe);
        }
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to update the recipe-cache of the AutoCrater mechanic", e);
      }
    }
  }
}
