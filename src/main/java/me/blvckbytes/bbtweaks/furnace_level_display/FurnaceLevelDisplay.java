package me.blvckbytes.bbtweaks.furnace_level_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.integration.mc_mmo.McMMOIntegration;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.*;

public class FurnaceLevelDisplay implements Listener, Tickable {

  private static class PlayerData {
    long lastSendStamp;
    long lastFurnaceBlockId;
  }

  private static final List<Tag<Material>> ORE_TAGS = List.of(
    Tag.ITEMS_COAL_ORES,
    Tag.ITEMS_COPPER_ORES,
    Tag.ITEMS_DIAMOND_ORES,
    Tag.ITEMS_GOLD_ORES,
    Tag.ITEMS_IRON_ORES,
    Tag.ITEMS_LAPIS_ORES,
    Tag.ITEMS_EMERALD_ORES,
    Tag.ITEMS_REDSTONE_ORES
  );

  private final McMMOIntegration mcMMOIntegration;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, PlayerData> dataByPlayerId;

  public FurnaceLevelDisplay(
    McMMOIntegration mcMMOIntegration,
    ConfigKeeper<MainSection> config
  ) {
    this.mcMMOIntegration = mcMMOIntegration;
    this.config = config;

    this.dataByPlayerId = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    handleDisplays();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    dataByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private boolean isOreRecipe(CookingRecipe<?> cookingRecipe) {
    if (!(cookingRecipe.getInputChoice() instanceof RecipeChoice.MaterialChoice materialChoice))
      return false;

    for (var inputChoice : materialChoice.getChoices()) {
      for (var oreTag : ORE_TAGS) {
        if (oreTag.isTagged(inputChoice))
          return true;
      }
    }

    return false;
  }

  private void handleDisplays() {
    for (var player : Bukkit.getOnlinePlayers()) {
      var targetBlock = getTargetedFurnaceBlock(player);
      var playerId = player.getUniqueId();
      var playerData = dataByPlayerId.get(playerId);

      if (targetBlock == null) {
        // Immediately clear out the action-bar
        if (playerData != null && System.currentTimeMillis() - playerData.lastSendStamp < 3000) {
          playerData.lastSendStamp = 0;
          playerData.lastFurnaceBlockId = 0;

          player.sendActionBar(Component.text(" "));
        }

        continue;
      }

      var blockId = CacheByPosition.computeWorldlessBlockId(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());

      // Send roughly about twice a second, but allow for instant redraw if the player's looking at a different furnace
      if (playerData != null && playerData.lastFurnaceBlockId == blockId && System.currentTimeMillis() - playerData.lastSendStamp < 500)
        continue;

      if (!(targetBlock.getState() instanceof Furnace furnace))
        continue;

      if (playerData == null) {
        playerData = new PlayerData();
        dataByPlayerId.put(playerId, playerData);
      }

      playerData.lastSendStamp = System.currentTimeMillis();
      playerData.lastFurnaceBlockId = blockId;

      displayForPlayer(player, furnace);
    }
  }

  public double calculateExperience(Player player, Map<CookingRecipe<?>, Integer> recipesUsed) {
    double totalExperience = 0;

    for (var recipeEntry : recipesUsed.entrySet()) {
      var recipeExperience = recipeEntry.getKey().getExperience();
      var totalRecipeSmeltCount = recipeEntry.getValue();
      var totalRecipeExperience = recipeExperience * totalRecipeSmeltCount;

      if (isOreRecipe(recipeEntry.getKey())) {
        var wholePart = (int) Math.floor(totalRecipeExperience);
        var fractionalPart = totalRecipeExperience - wholePart;
        totalRecipeExperience = mcMMOIntegration.applySmeltingRecipeExpBoost(player, wholePart) + fractionalPart;
      }

      totalExperience += totalRecipeExperience;
    }

    return totalExperience;
  }

  private void displayForPlayer(Player player, Furnace furnace) {
    var totalExperience = calculateExperience(player, furnace.getRecipesUsed());

    if (totalExperience == 0) {
      config.rootSection.furnaceLevel.noLevelsStored.sendActionBar(player);
      return;
    }

    var formattedTotalExperience = String.format("%.1f", totalExperience);

    var levelBefore = player.getLevel();
    var levelAfter = levelBefore;

    // getExp in [0;1], getExpToLevel - experience required to complete the current level
    var ownedExperience = player.getExp() * player.getExpToLevel();

    // Simulate levelling up with the total sum the player would have access to once taking the items out
    var remainingAvailableExperience = totalExperience + ownedExperience;

    int required;

    while ((required = getExperiencePointsNeededForLevel(levelAfter)) <= remainingAvailableExperience) {
      remainingAvailableExperience -= required;
      ++levelAfter;
    }

    var progressNextLevel = remainingAvailableExperience / (float) getExperiencePointsNeededForLevel(levelAfter) * 100;
    var formattedProgress = String.format("%.1f", progressNextLevel);

    var environment = new InterpretationEnvironment()
      .withVariable("old_level", levelBefore)
      .withVariable("new_level", levelAfter)
      .withVariable("next_level_progress", formattedProgress)
      .withVariable("stored_experience", formattedTotalExperience);

    config.rootSection.furnaceLevel.levelsStored.sendActionBar(player, environment);
  }

  // See: https://minecraft.wiki/w/Experience#Leveling_up
  int getExperiencePointsNeededForLevel(int lvl) {
    if (lvl <= 15)
      return 2 * lvl + 7;

    if (lvl <= 30)
      return 5 * lvl - 38;

    return 9 * lvl - 158;
  }

  private Block getTargetedFurnaceBlock(Player player) {
    var rayTraceResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      4.5,
      FluidCollisionMode.NEVER,
      true
    );

    if (rayTraceResult == null || rayTraceResult.getHitBlock() == null)
      return null;

    var targetBlock = rayTraceResult.getHitBlock();
    var blockType = targetBlock.getType();

    if (blockType != Material.FURNACE && blockType != Material.BLAST_FURNACE && blockType != Material.SMOKER)
      return null;

    return targetBlock;
  }
}
