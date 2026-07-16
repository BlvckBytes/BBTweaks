package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;
import me.blvckbytes.bbtweaks.mechanic.common.UnknownFlagException;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;

public class AutoCrafterMechanic extends BaseMechanic<AutoCrafterInstance> implements RecipeCache {

  static {
    if (Material.values().length > 4096)
      throw new IllegalStateException("There are more than 4K materials, which exceeds our expectation - cannot bit-pack the matrix into two longs without a loss of information!");
  }

  private static final int FLAGS_LINE = 2;

  private final List<CachedRecipe> cachedRecipes = new ArrayList<>();

  private final CacheByPosition<AutoCrafterInstance> instanceByCrafterPosition;

  private final NamespacedKey keyMetaRecipe;

  public AutoCrafterMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    this.instanceByCrafterPosition = new CacheByPosition<>();

    this.keyMetaRecipe = new NamespacedKey(plugin, "auto-crafter-meta-recipe");

    // Let's give other plugins plenty of time to register additional recipes.
    Bukkit.getScheduler().runTaskLater(plugin, this::updateRecipeCache, 20L);
  }

  @Override
  public boolean onInstanceClick(Player player, AutoCrafterInstance instance, boolean wasLeftClick) {
    if (wasLeftClick || !player.isSneaking())
      return false;

    if (!instance.hasFlag(AutoCrafterFlag.ENABLE_META_RECIPE))
      return false;

    var sign = instance.getSign();

    if (!canEditSign(player, sign)) {
      config.rootSection.mechanic.autoCrafter.cannotEditSign.sendMessage(player);
      return true;
    }

    if (!instance.metaRecipeInventory.getViewers().isEmpty()) {
      config.rootSection.mechanic.autoCrafter.anotherIsEditing.sendMessage(player);
      return true;
    }

    player.openInventory(instance.metaRecipeInventory);
    config.rootSection.mechanic.autoCrafter.metaRecipeInventoryOpening.sendMessage(player, getSignEnvironment(sign));

    return true;
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

    EnumSet<AutoCrafterFlag> flags;

    try {
      flags = FlagEnum.parse(AutoCrafterFlag.class, SignUtil.getPlainTextLine(sign, FLAGS_LINE));
    } catch (UnknownFlagException exception) {
      if (creator != null)
        config.rootSection.mechanic.autoCrafter.unknownFlag.sendMessage(creator, exception.makeEnvironment());

      return null;
    }

    var metaRecipeInventoryHolder = new MetaRecipeInventoryHolder();

    var metaRecipeInventory = Bukkit.createInventory(
      metaRecipeInventoryHolder,
      InventoryType.DROPPER,
      config.rootSection.mechanic.autoCrafter.metaRecipeInventoryTitle.interpret(SlotType.INVENTORY_TITLE, null).getFirst()
    );

    var metaRecipeBytes = sign.getPersistentDataContainer().get(keyMetaRecipe, PersistentDataType.BYTE_ARRAY);

    if (metaRecipeBytes != null) {
      try {
        var metaRecipeItems = ItemStack.deserializeItemsFromBytes(metaRecipeBytes);

        if (metaRecipeItems.length != AutoCrafterInstance.MATRIX_SIZE)
          throw new IllegalStateException("Unexpected length: " + metaRecipeItems.length);

        metaRecipeInventory.setContents(metaRecipeItems);
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to parse the meta-recipe at " + sign.getX() + " " + sign.getY() + " " + sign.getZ() + " " + sign.getWorld().getName(), e);
      }
    }

    var instance = new AutoCrafterInstance(sign, metaRecipeInventory, flags, this);

    metaRecipeInventoryHolder.instance = instance;

    var crafter = instance.getMountBlock();

    if (BlockUtil.isBlockLoaded(crafter)) {
      if (!(crafter.getState(false) instanceof Crafter crafterState)) {
        if (creator != null)
          config.rootSection.mechanic.autoCrafter.notOnACrafter.sendMessage(creator);

        return null;
      }

      if (SignUtil.checkIfAnyContainerSignMatches(crafterState, this::isSignRegistered)) {
        if (creator != null) {
          config.rootSection.mechanic.autoCrafter.existingSign.sendMessage(
            creator,
            new InterpretationEnvironment()
              .withVariable("x", crafter.getX())
              .withVariable("y", crafter.getY())
              .withVariable("z", crafter.getZ())
          );
        }

        return null;
      }
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceByCrafterPosition.put(crafter.getWorld(), crafter.getX(), crafter.getY(), crafter.getZ(), instance);

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
  public @Nullable AutoCrafterInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null) {
      var crafter = instance.getMountBlock();
      instanceByCrafterPosition.invalidate(crafter.getWorld(), crafter.getX(), crafter.getY(), crafter.getZ());
    }

    return instance;
  }

  @Override
  public List<CachedRecipe> getRecipes() {
    return Collections.unmodifiableList(cachedRecipes);
  }

  @EventHandler
  public void onCrafterCraft(CrafterCraftEvent event) {
    var crafter = event.getBlock();
    var crafterInstance = instanceByCrafterPosition.get(crafter.getWorld(), crafter.getX(), crafter.getY(), crafter.getZ());

    if (crafterInstance != null)
      event.setCancelled(true);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    if (!(event.getInventory().getHolder(false) instanceof MetaRecipeInventoryHolder metaRecipeInventoryHolder))
      return;

    var crafterInstance = metaRecipeInventoryHolder.instance;

    if (crafterInstance == null)
      return;

    var sign = crafterInstance.getSign();

    var metaRecipeBytes = ItemStack.serializeItemsAsBytes(crafterInstance.metaRecipeInventory.getContents());

    sign.getPersistentDataContainer().set(keyMetaRecipe, PersistentDataType.BYTE_ARRAY, metaRecipeBytes);
    sign.update(true, false);

    reloadInstanceBySign(sign);

    config.rootSection.mechanic.autoCrafter.metaRecipeInventorySaved.sendMessage(player, getSignEnvironment(sign));
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
