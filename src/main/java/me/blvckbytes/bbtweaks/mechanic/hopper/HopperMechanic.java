package me.blvckbytes.bbtweaks.mechanic.hopper;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.ReflectUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class HopperMechanic extends BaseMechanic<HopperInstance> implements Listener, ItemCompatibilities {

  private static final BlockFace[] SIGN_MOUNT_FACES = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private final CacheByPosition<HopperInstance> instanceByHopperPosition;

  private final EnumSet<Material> smokerInputTypes;
  private final EnumSet<Material> blastFurnaceInputTypes;
  private final EnumSet<Material> furnaceInputTypes;
  private final EnumSet<Material> furnaceFuelTypes;
  private final EnumSet<Material> potionIngredientTypes;

  public HopperMechanic(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    instanceByHopperPosition = new CacheByPosition<>();

    smokerInputTypes = EnumSet.noneOf(Material.class);
    blastFurnaceInputTypes = EnumSet.noneOf(Material.class);
    furnaceInputTypes = EnumSet.noneOf(Material.class);
    furnaceFuelTypes = EnumSet.noneOf(Material.class);
    potionIngredientTypes = EnumSet.noneOf(Material.class);

    loadTypeSets();
  }

  private void loadTypeSets() {
    try {
      var bukkitServer = Bukkit.getServer();
      var getServerMethod = Objects.requireNonNull(ReflectUtil.tryLocateNonStaticMember(bukkitServer.getClass(), Class::getDeclaredMethods, method -> method.getName().equals("getServer")));
      var minecraftServer = getServerMethod.invoke(bukkitServer);

      var potionBrewingMethod = Objects.requireNonNull(ReflectUtil.tryLocateNonStaticMember(minecraftServer.getClass(), Class::getDeclaredMethods, method -> StringUtils.containsIgnoreCase(method.getName(), "potion") && StringUtils.containsIgnoreCase(method.getName(), "brew")));
      var potionBrewing = potionBrewingMethod.invoke(minecraftServer);

      var isPotionIngredientMethod = Objects.requireNonNull(ReflectUtil.tryLocateNonStaticMember(potionBrewing.getClass(), Class::getDeclaredMethods, method -> method.getName().equals("isPotionIngredient")));

      for (Material material : Material.values()) {
        if (!material.isItem())
          continue;

        var nmsStack = ReflectUtil.asNMSCopy(new ItemStack(material));

        if ((boolean) isPotionIngredientMethod.invoke(potionBrewing, nmsStack))
          potionIngredientTypes.add(material);
      }
    } catch (Throwable e) {
      throw new IllegalStateException("Could not access the potion-brewing registry of the server", e);
    }

    for (var recipeIterator = Bukkit.recipeIterator(); recipeIterator.hasNext();) {
      var recipe = recipeIterator.next();

      if (recipe instanceof SmokingRecipe smokingRecipe)
        addAllChoices(smokingRecipe.getInputChoice(), smokerInputTypes);

      if (recipe instanceof BlastingRecipe blastingRecipe)
        addAllChoices(blastingRecipe.getInputChoice(), blastFurnaceInputTypes);

      if (recipe instanceof FurnaceRecipe furnaceRecipe)
        addAllChoices(furnaceRecipe.getInputChoice(), furnaceInputTypes);
    }

    for (Material material : Material.values()) {
      if (material.isFuel())
        furnaceFuelTypes.add(material);
    }
  }

  private static void addAllChoices(RecipeChoice recipeChoice, EnumSet<Material> output) {
    if (recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice)
      output.addAll(materialChoice.getChoices());
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, HopperInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Hopper");
  }

  @Override
  public @Nullable HopperInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.hopper")) {
      config.rootSection.mechanic.hopper.noPermission.sendMessage(creator);
      return null;
    }

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var mountBlock = signBlock.getRelative(signFacing.getOppositeFace());

    if (mountBlock.getType() != Material.HOPPER) {
      if (creator != null)
        config.rootSection.mechanic.hopper.notOnAHopper.sendMessage(creator);

      return null;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    for (var blockFace : SIGN_MOUNT_FACES) {
      if (mountBlock.getRelative(blockFace).getState() instanceof Sign otherSign && this.isSignRegistered(otherSign)) {
        if (creator != null)
          config.rootSection.mechanic.hopper.multipleHoppers.sendMessage(creator, environment);

        return null;
      }
    }

    // TODO: Load predicate from PDC
    var instance = new HopperInstance(sign, predicate, this, config);
    var world = sign.getWorld();

    instanceBySignPosition.put(world, sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceByHopperPosition.put(world, mountBlock.getX(), mountBlock.getY(), mountBlock.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.hopper.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  public @Nullable HopperInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null) {
      var hopperBlock = instance.getMountBlock();
      instanceByHopperPosition.invalidate(hopperBlock.getWorld(), hopperBlock.getX(), hopperBlock.getY(), hopperBlock.getZ());
    }

    return instance;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onHopperSearch(HopperInventorySearchEvent event) {
    var hopperBlock = event.getBlock();
    var hopperInstance = instanceByHopperPosition.get(hopperBlock.getWorld(), hopperBlock.getX(), hopperBlock.getY(), hopperBlock.getZ());

    // Disable vanilla push/pull-behavior for all hoppers on which an instance is mounted.
    if (hopperInstance != null)
      event.setInventory(null);
  }

  @Override
  public boolean isBrewingIngredient(ItemStack item) {
    return potionIngredientTypes.contains(item.getType());
  }

  @Override
  public boolean isBrewingFuel(ItemStack item) {
    return item.getType() == Material.BLAZE_POWDER;
  }

  @Override
  public boolean isPotion(ItemStack item) {
    var type = item.getType();
    return type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION;
  }

  @Override
  public boolean isFurnaceIngredient(FurnaceType furnaceType, ItemStack item) {
    return switch (furnaceType) {
      case FURNACE -> furnaceInputTypes.contains(item.getType());
      case BLAST_FURNACE -> blastFurnaceInputTypes.contains(item.getType());
      case SMOKER -> smokerInputTypes.contains(item.getType());
    };
  }

  @Override
  public boolean isFurnaceFuel(FurnaceType furnaceType, ItemStack item) {
    return furnaceFuelTypes.contains(item.getType());
  }
}
