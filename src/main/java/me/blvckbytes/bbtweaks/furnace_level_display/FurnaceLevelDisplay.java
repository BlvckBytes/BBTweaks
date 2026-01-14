package me.blvckbytes.bbtweaks.furnace_level_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.objects.*;
import me.blvckbytes.bbtweaks.MainSection;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FurnaceLevelDisplay implements Listener {

  private static class PlayerData {
    long lastSendStamp;
    long lastFurnaceBlockId;
  }

  private final @Nullable McMMOIntegration mcMMOIntegration;
  private final ConfigKeeper<MainSection> config;
  private final Logger logger;

  private final Map<Class<? extends BlockState>, RecipesUsedAccessor> accessorByType;
  private final Map<UUID, PlayerData> dataByPlayerId;
  private final Object2FloatMap<String> recipeExperienceByKey;
  private final CacheByPosition<Reference2IntMap<?>> recipesUsedCache;

  private final MethodHandle resourceKeyGetIdentifier;
  private final MethodHandle identifierGetPath;

  public FurnaceLevelDisplay(
    Plugin plugin,
    @Nullable McMMOIntegration mcMMOIntegration,
    ConfigKeeper<MainSection> config
  ) throws Exception {
    this.logger = plugin.getLogger();
    this.mcMMOIntegration = mcMMOIntegration;
    this.config = config;

    this.accessorByType = new HashMap<>();
    this.dataByPlayerId = new HashMap<>();
    this.recipeExperienceByKey = new Object2FloatOpenHashMap<>();
    this.recipeExperienceByKey.defaultReturnValue(-1);
    this.recipesUsedCache = new CacheByPosition<>();

    var publicLookup = MethodHandles.publicLookup();

    var resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
    var identifierClass = Class.forName("net.minecraft.resources.Identifier");

    // Identifier net.minecraft.resources.ResourceKey#identifier
    resourceKeyGetIdentifier = publicLookup.findVirtual(resourceKeyClass, "identifier", MethodType.methodType(identifierClass));

    // String net.minecraft.resources.Identifier#getPath
    identifierGetPath = publicLookup.findVirtual(identifierClass, "getPath", MethodType.methodType(String.class));

    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::handleDisplays, 0L, 1L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    dataByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    for (var tileEntity : event.getChunk().getTileEntities()) {
      if (!(tileEntity instanceof Furnace))
        continue;

      recipesUsedCache.invalidate(tileEntity.getWorld(), tileEntity.getX(), tileEntity.getY(), tileEntity.getZ());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBreak(BlockBreakEvent event) {
    var block = event.getBlock();
    recipesUsedCache.invalidate(block.getWorld(), block.getX(), block.getY(), block.getZ());
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    var block = event.getBlock();
    recipesUsedCache.invalidate(block.getWorld(), block.getX(), block.getY(), block.getZ());
  }

  public boolean setUp() {
    if (!setUpAccessors())
      return false;

    loadRecipes();
    return true;
  }

  private void loadRecipes() {
    for (var recipeIterator = Bukkit.recipeIterator(); recipeIterator.hasNext();) {
      if (!(recipeIterator.next() instanceof CookingRecipe<?> cookingRecipe))
        continue;

      recipeExperienceByKey.put(cookingRecipe.getKey().getKey(), cookingRecipe.getExperience());
    }
  }

  private boolean setUpAccessors() {
    var mainWorld = Bukkit.getWorlds().get(0);
    var probingLocation = new Location(mainWorld, 0, mainWorld.getMinHeight(), 0);

    var probingBlock = probingLocation.getBlock();
    var dataBackup = probingBlock.getBlockData();

    // Just to make absolutely sure that we cover all cases, since they may
    // alter abstractions based on the furnace-type at hand in the future.
    var targetTypes = new Material[] { Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER };

    for (var targetType : targetTypes) {
      probingBlock.setType(targetType, false);
      var probingState = probingBlock.getState();

      try {
        registerRecipesUsedAccessorFor(probingState);
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "An error occurred while trying to set up the recipesUsed-accessor for " + targetType, e);
        return false;
      }
    }

    probingBlock.setBlockData(dataBackup);
    return true;
  }

  private void registerRecipesUsedAccessorFor(BlockState blockState) {
    var stateClass = blockState.getClass();

    var getHandleMethod = tryLocateNonStaticMember(stateClass, Class::getDeclaredMethods, method -> method.getName().equals("getBlockEntity"));

    if (getHandleMethod == null)
      throw new IllegalArgumentException("Could not locate the get-handle method on " + stateClass);

    Object handle;

    try {
      getHandleMethod.setAccessible(true);
      handle = getHandleMethod.invoke(blockState);
    } catch (Throwable e) {
      throw new IllegalArgumentException("An error occurred while trying to invoke the get-handle method on " + stateClass, e);
    }

    if (handle == null)
      throw new IllegalArgumentException("Invoking the get-handle method yielded null on " + stateClass);

    var usedRecipesField = tryLocateNonStaticMember(handle.getClass(), Class::getDeclaredFields, field -> Reference2IntMap.class.isAssignableFrom(field.getType()));

    if (usedRecipesField == null)
      throw new IllegalArgumentException("Could not locate field of type " + Reference2IntMap.class + " on " + stateClass);

    try {
      usedRecipesField.setAccessible(true);
      if (usedRecipesField.get(handle) == null)
        throw new IllegalArgumentException("Expected non-null used-recipes-map");
    } catch (Throwable e) {
      throw new IllegalArgumentException("An error occurred while trying to access the used-recipes field on " + stateClass, e);
    }

    accessorByType.put(stateClass, targetState -> {
      try {
        var targetHandle = getHandleMethod.invoke(targetState);
        return (Reference2IntOpenHashMap<?>) usedRecipesField.get(targetHandle);
      } catch (Throwable e) {
        logger.log(Level.WARNING, "An error occurred while trying to access the used-recipes field on " + (targetState == null ? null : targetState.getClass()));
        return null;
      }
    });
  }

  private <T extends Member> @Nullable T tryLocateNonStaticMember(Class<?> targetClass, Function<Class<?>, T[]> memberAccessor, Predicate<T> predicate) {
    for (var member : memberAccessor.apply(targetClass)) {
      if (Modifier.isStatic(member.getModifiers()))
        continue;

      if (predicate.test(member))
        return member;
    }

    var superClass = targetClass.getSuperclass();

    if (superClass != Object.class)
      return tryLocateNonStaticMember(superClass, memberAccessor, predicate);

    return null;
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

          player.sendActionBar(Component.text());
        }

        continue;
      }

      var blockId = CacheByPosition.computeWorldlessBlockId(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());

      // Send roughly about twice a second, but allow for instant redraw if the player's looking at a different furnace
      if (playerData != null && playerData.lastFurnaceBlockId == blockId && System.currentTimeMillis() - playerData.lastSendStamp < 500)
        continue;

      var recipesUsed = recipesUsedCache.computeIfAbsent(
        targetBlock.getWorld(),
        targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(),
        () -> {
          var furnaceState = targetBlock.getState();
          var accessor = accessorByType.get(furnaceState.getClass());

          if (accessor == null)
            return null;

          return accessor.access(furnaceState);
        }
      );

      if (recipesUsed == null)
        continue;

      if (playerData == null) {
        playerData = new PlayerData();
        dataByPlayerId.put(playerId, playerData);
      }

      playerData.lastSendStamp = System.currentTimeMillis();
      playerData.lastFurnaceBlockId = blockId;

      displayForPlayer(player, recipesUsed);
    }
  }

  private void displayForPlayer(Player player, Reference2IntMap<?> recipesUsed) {
    float totalExperience = 0;

    for (var recipeEntry : recipesUsed.reference2IntEntrySet()) {
      var recipeKey = recipeEntry.getKey();

      String recipePath;

      try {
        var recipeIdentifier = resourceKeyGetIdentifier.invoke(recipeKey);
        recipePath = (String) identifierGetPath.invoke(recipeIdentifier);
      } catch (Throwable e) {
        logger.log(Level.WARNING, "An error occurred while trying to access the recipe-path of recipeKey=" + recipeKey);
        continue;
      }

      var recipeExperience = recipeExperienceByKey.getFloat(recipePath);

      if (recipeExperience < 0) {
        logger.log(Level.WARNING, "Could not access experience for recipe of path " + recipePath);
        continue;
      }

      var totalRecipeSmeltCount = recipeEntry.getIntValue();

      totalExperience += recipeExperience * totalRecipeSmeltCount;
    }

    if (totalExperience == 0) {
      config.rootSection.furnaceLevel.noLevelsStored.sendActionBar(player);
      return;
    }

    if (mcMMOIntegration != null) {
      var wholePart = (int) Math.floor(totalExperience);
      var fractionalPart = totalExperience - wholePart;
      totalExperience = mcMMOIntegration.vanillaXPBoost(player, wholePart) + fractionalPart;
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
