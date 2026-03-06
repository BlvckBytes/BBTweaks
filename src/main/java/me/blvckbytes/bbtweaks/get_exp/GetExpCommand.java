package me.blvckbytes.bbtweaks.get_exp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.furnace_level_display.RecipeExperience;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GetExpCommand implements CommandExecutor, TabCompleter {

  // TODO: Add to config
  private static final int EXP_PER_BOTTLE = 16;

  private final FurnaceLevelDisplay furnaceLevelDisplay;
  private final ConfigKeeper<MainSection> config;

  public GetExpCommand(
    FurnaceLevelDisplay furnaceLevelDisplay,
    ConfigKeeper<MainSection> config
  ) {
    this.furnaceLevelDisplay = furnaceLevelDisplay;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.getExp.playersOnly.sendMessage(sender);
      return true;
    }

    if (!(player.hasPermission("bbtweaks.getexp"))) {
      // TODO: Config-message
      player.sendMessage("§cNo permission!");
      return true;
    }

    var recipeMap = getLookedAtFurnaceBlockRecipeMap(player);

    if (recipeMap == null) {
      // TODO: Config-message
      player.sendMessage("§cNot looking at a furnace!");
      return true;
    }

    var experienceLimit = -1F;
    var isLimitAPercentage = false;

    if (args.length >= 1) {
      var numberString = args[0];

      if (numberString.endsWith("%")) {
        numberString = numberString.substring(0, numberString.length() - 1);
        isLimitAPercentage = true;
      }

      try {
        experienceLimit = Float.parseFloat(numberString);

        if (experienceLimit <= 0)
          throw new IllegalArgumentException();
      } catch (Throwable e) {
        // TODO: Config-message
        player.sendMessage("§cNot a strictly positive float: " + numberString);
        return true;
      }

      if (isLimitAPercentage && experienceLimit > 100) {
        // TODO: Config-message
        player.sendMessage("§cPercentage cannot be greater than 100");
        return true;
      }
    }

    var doBottleExperience = args.length >= 2 && args[1].equalsIgnoreCase("bottles");

    handOutExperience(player, recipeMap, experienceLimit, isLimitAPercentage, doBottleExperience);

    return true;
  }

  private <T> void handOutExperience(
    Player player,
    Reference2IntMap<T> recipeMap,
    float experienceLimit,
    boolean isLimitAPercentage,
    boolean doBottleExperience
  ) {
    var recipeExperiences = new ArrayList<RecipeExperience<T>>();

    for (var recipeEntry : recipeMap.reference2IntEntrySet()) {
      var recipeExperience = furnaceLevelDisplay.tryAccessRecipeExperienceByKey(recipeEntry.getKey());

      if (recipeExperience != null)
        recipeExperiences.add(recipeExperience);
    }

    // Convert percentage to absolute value, as to then try to approximate with what's available
    if (isLimitAPercentage) {
      var totalExperience = 0F;

      for (var recipeExperience : recipeExperiences)
        totalExperience += recipeExperience.experience() * recipeMap.getInt(recipeExperience.recipeKey());

      experienceLimit = totalExperience * (experienceLimit / 100F);
    }

    // We want to use up the recipes of highest experience first when handing out partial amounts
    recipeExperiences.sort((a, b) -> -Float.compare(a.experience(), b.experience()));

    var experienceAccumulator = 0F;
    var encounteredExperience = false;

    var playerInventory = player.getInventory();
    var encounteredFullInventory = false;
    var totalBottleCount = 0;

    for (var recipeExperience : recipeExperiences) {
      var availableCount = recipeMap.getInt(recipeExperience.recipeKey());

      if (availableCount == 0)
        continue;

      var originalCount = availableCount;

      encounteredExperience = true;

      collector: while (availableCount > 0) {
        if (experienceLimit > 0 && experienceLimit - (experienceAccumulator + recipeExperience.experience()) <= .01)
          break;

        experienceAccumulator += recipeExperience.experience();
        --availableCount;

        if (doBottleExperience) {
          while (experienceAccumulator > EXP_PER_BOTTLE) {
            experienceAccumulator -= EXP_PER_BOTTLE;

            if (!addExperienceBottleToPlayerInventory(playerInventory)) {
              encounteredFullInventory = true;
              break collector;
            }

            ++totalBottleCount;
          }
        }
      }

      if (originalCount != availableCount)
        recipeMap.put(recipeExperience.recipeKey(), availableCount);

      if (encounteredFullInventory)
        break;
    }

    if (experienceAccumulator == 0) {
      if (!encounteredExperience) {
        // TODO: Config-message
        player.sendMessage("§cNo stored experience");
        return;
      }

      // TODO: Config-message
      player.sendMessage("§cLimit too low - it's less than any single stored recipe");
      return;
    }

    if (doBottleExperience) {
      // TODO: Config-message
      if (encounteredFullInventory)
        player.sendMessage("§cInventory full - stopping!");

      // TODO: Config-message
      if (totalBottleCount > 0)
        player.sendMessage("§aHanded out " + totalBottleCount + " bottles!");

      // TODO: Config-message
      else
        player.sendMessage("§cCould not hand out any bottles, as the total experience was less than " + EXP_PER_BOTTLE + "exp, which would make for a single bottle.");
    }

    var handedOutExperience = Math.round(experienceAccumulator);
    var approximated = Math.abs(experienceAccumulator - experienceLimit) > .1;

    // TODO: Config-message
    player.sendMessage("§aHanded out " + handedOutExperience + "exp" + (approximated ? " (approximated limit as best as possible)" : "") + ".");

    player.giveExp(handedOutExperience);
  }

  private boolean addExperienceBottleToPlayerInventory(Inventory inventory) {
    for (var slot = 0; slot < 4 * 9; ++slot) {
      var currentItem = inventory.getItem(slot);

      if (currentItem == null) {
        inventory.setItem(slot, new ItemStack(Material.EXPERIENCE_BOTTLE, 1));
        return true;
      }

      if (currentItem.getType() != Material.EXPERIENCE_BOTTLE)
        continue;

      if (currentItem.getAmount() == currentItem.getMaxStackSize())
        continue;

      currentItem.setAmount(currentItem.getAmount() + 1);
      return true;
    }

    return false;
  }

  private @Nullable Reference2IntMap<?> getLookedAtFurnaceBlockRecipeMap(Player player) {
    //noinspection UnstableApiUsage
    var rayTraceResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      5.0,
      FluidCollisionMode.NEVER,
      true,
      block -> isFurnaceType(block.getType())
    );

    if (rayTraceResult == null || rayTraceResult.getHitBlock() == null)
      return null;

    var blockState = rayTraceResult.getHitBlock().getState(false);

    return furnaceLevelDisplay.tryAccessFurnaceRecipeMap(blockState);
  }

  private boolean isFurnaceType(Material material) {
    return material == Material.FURNACE || material == Material.SMOKER || material == Material.BLAST_FURNACE;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !player.hasPermission("bbtweaks.getexp"))
      return List.of();

    if (args.length == 1 && args[0].isBlank()) {
      var recipeMap = getLookedAtFurnaceBlockRecipeMap(player);

      var totalExperience = 0F;

      if (recipeMap != null) {
        for (var entry : recipeMap.reference2IntEntrySet()) {
          var recipeExperience = furnaceLevelDisplay.tryAccessRecipeExperienceByKey(entry.getKey());

          if (recipeExperience != null)
            totalExperience += recipeExperience.experience() * entry.getIntValue();
        }
      }

      return List.of(String.valueOf(totalExperience));
    }

    if (args.length == 2)
      return List.of("bottles");

    return List.of();
  }
}
