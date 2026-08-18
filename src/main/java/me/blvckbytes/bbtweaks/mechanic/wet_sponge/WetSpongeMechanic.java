package me.blvckbytes.bbtweaks.mechanic.wet_sponge;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WetSpongeMechanic extends BaseMechanic<WetSpongeInstance> {

  public WetSpongeMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  public boolean onInstanceClick(Player player, WetSpongeInstance instance, boolean wasLeftClick) {
    if (wasLeftClick)
      return false;

    if (!player.hasPermission("bbtweaks.mechanic.wet-sponge")) {
      config.rootSection.mechanic.wetSponge.noUsePermission.sendMessage(player);
      return true;
    }

    var inventory = player.getInventory();

    // Wet all sponges in inventory
    if (player.isSneaking()) {
      var totalWettedCount = 0;
      var totalWetCount = 0;

      for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
        var currentItem = inventory.getItem(slotIndex);

        if (!ItemUtil.isStackValid(currentItem))
          continue;

        var spongeCount = currentItem.getAmount();

        if (currentItem.getType() != Material.SPONGE) {
          if (currentItem.getType() == Material.WET_SPONGE)
            totalWetCount += spongeCount;

          continue;
        }

        totalWettedCount += spongeCount;

        inventory.setItem(slotIndex, new ItemStack(Material.WET_SPONGE, spongeCount));
      }

      if (totalWetCount == 0 && totalWettedCount == 0) {
        config.rootSection.mechanic.wetSponge.noSpongesInInventory.sendMessage(player);
        return true;
      }

      if (totalWettedCount == 0) {
        config.rootSection.mechanic.wetSponge.allSpongesInInventoryAlreadyWet.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("count", totalWetCount)
        );

        return true;
      }

      config.rootSection.mechanic.wetSponge.allSpongesInInventoryHaveBeenWetted.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("count", totalWettedCount)
      );

      return true;
    }

    // Wet sponges held in the main-hand

    var mainHandItem = inventory.getItemInMainHand();
    var heldType = mainHandItem.getType();

    if (heldType != Material.SPONGE && heldType != Material.WET_SPONGE) {
      config.rootSection.mechanic.wetSponge.notHoldingSponges.sendMessage(player);
      return true;
    }

    var spongeCount = mainHandItem.getAmount();

    if (heldType == Material.WET_SPONGE) {
      config.rootSection.mechanic.wetSponge.heldSpongesAlreadyWet.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("count", spongeCount)
      );

      return true;
    }

    inventory.setItemInMainHand(new ItemStack(Material.WET_SPONGE, spongeCount));

    config.rootSection.mechanic.wetSponge.heldSpongesHaveBeenWetted.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("count", spongeCount)
    );

    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("WetSponge");
  }

  @Override
  public @Nullable WetSpongeInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.wet-sponge")) {
      config.rootSection.mechanic.wetSponge.noPermission.sendMessage(creator);
      return null;
    }

    var instance = new WetSpongeInstance(sign, side);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.wetSponge.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }
}
