package me.blvckbytes.bbtweaks.get_exp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.furnace_level_display.RecipeExperience;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
      config.rootSection.getExp.noPermission.sendMessage(player);
      return true;
    }

    var recipeMapAndBlock = getLookedAtFurnaceRecipeMapAndBlock(player);

    if (recipeMapAndBlock == null) {
      config.rootSection.getExp.notLookingAtAFurnace.sendMessage(player);
      return true;
    }

    var experienceLimit = -1F;
    var isLimitAPercentage = false;

    if (args.length == 0) {
      config.rootSection.getExp.commandUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return true;
    }

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
      config.rootSection.getExp.invalidNumberProvided.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("value", numberString)
      );

      return true;
    }

    if (isLimitAPercentage && experienceLimit > 100) {
      config.rootSection.getExp.invalidPercentageProvided.sendMessage(player);
      return true;
    }

    var doBottleExperience = args.length >= 2 && args[1].equalsIgnoreCase("bottles");

    handOutExperience(player, recipeMapAndBlock.recipeMap(), recipeMapAndBlock.block(), experienceLimit, isLimitAPercentage, doBottleExperience);

    return true;
  }

  private <T> void handOutExperience(
    Player player,
    Reference2IntMap<T> recipeMap,
    Block furnaceBlock,
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

    for (var recipeExperience : recipeExperiences) {
      var availableCount = recipeMap.getInt(recipeExperience.recipeKey());

      if (availableCount == 0)
        continue;

      var originalCount = availableCount;

      encounteredExperience = true;

      while (availableCount > 0) {
        if (experienceLimit > 0 && experienceLimit - (experienceAccumulator + recipeExperience.experience()) <= .01)
          break;

        experienceAccumulator += recipeExperience.experience();

        --availableCount;
      }

      if (originalCount == availableCount)
        continue;

      if (availableCount <= 0) {
        recipeMap.removeInt(recipeExperience.recipeKey());
        continue;
      }

      recipeMap.put(recipeExperience.recipeKey(), availableCount);
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", furnaceBlock.getX())
      .withVariable("y", furnaceBlock.getY())
      .withVariable("z", furnaceBlock.getZ());

    if (experienceAccumulator == 0) {
      if (!encounteredExperience) {
        config.rootSection.getExp.noStoredExperience.sendMessage(player, environment);
        return;
      }

      config.rootSection.getExp.limitTooLow.sendMessage(
        player,
        environment
          .withVariable("limit", experienceLimit)
      );

      return;
    }

    var handedOutExperience = Math.round(experienceAccumulator);

    if (doBottleExperience) {
      var remainingExperience = handOutBottlesAndGetRemainingExperience(player.getInventory(), handedOutExperience);
      // TODO: Config-message
      player.sendMessage("Handed out " + (handedOutExperience - remainingExperience) + "exp as bottles and " + remainingExperience + "exp as-is");
      player.giveExp(remainingExperience);
      return;
    }

    // TODO: Config-message
    player.sendMessage("§aHanded out " + handedOutExperience + "exp");
    player.giveExp(handedOutExperience);
  }

  private int handOutBottlesAndGetRemainingExperience(Inventory inventory, int totalExperience) {
    var expPerBottle = config.rootSection.getExp.experiencePerBottle;
    var maxStackSize = Material.EXPERIENCE_BOTTLE.getMaxStackSize();
    var remainingCount = totalExperience / expPerBottle;

    for (var slot = 0; slot < 4 * 9; ++slot) {
      if (remainingCount <= 0)
        break;

      var currentItem = inventory.getItem(slot);

      if (currentItem == null) {
        var handedOutAmount = Math.min(remainingCount, maxStackSize);
        inventory.setItem(slot, new ItemStack(Material.EXPERIENCE_BOTTLE, handedOutAmount));
        remainingCount -= handedOutAmount;
        continue;
      }

      if (currentItem.getType() != Material.EXPERIENCE_BOTTLE)
        continue;

      var currentAmount = currentItem.getAmount();
      var remainingSpace = maxStackSize - currentAmount;

      if (remainingSpace <= 0)
        continue;

      var handedOutAmount = Math.min(remainingCount, remainingSpace);
      currentItem.setAmount(currentAmount + handedOutAmount);
      remainingCount -= handedOutAmount;
    }

    return totalExperience % expPerBottle + remainingCount * expPerBottle;
  }

  private @Nullable RecipeMapAndBlock getLookedAtFurnaceRecipeMapAndBlock(Player player) {
    //noinspection UnstableApiUsage
    var rayTraceResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      5.0,
      FluidCollisionMode.NEVER,
      true,
      block -> isFurnaceType(block.getType())
    );

    if (rayTraceResult == null)
      return null;

    var hitBlock = rayTraceResult.getHitBlock();

    if (hitBlock == null)
      return null;

    var blockState = hitBlock.getState(false);
    var recipeMap = furnaceLevelDisplay.tryAccessFurnaceRecipeMap(blockState);

    if (recipeMap == null)
      return null;

    return new RecipeMapAndBlock(recipeMap, hitBlock);
  }

  private boolean isFurnaceType(Material material) {
    return material == Material.FURNACE || material == Material.SMOKER || material == Material.BLAST_FURNACE;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !player.hasPermission("bbtweaks.getexp"))
      return List.of();

    if (args.length == 1 && args[0].isBlank()) {
      var recipeMapAndBlock = getLookedAtFurnaceRecipeMapAndBlock(player);

      var totalExperience = 0F;

      if (recipeMapAndBlock != null) {
        for (var entry : recipeMapAndBlock.recipeMap().reference2IntEntrySet()) {
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
