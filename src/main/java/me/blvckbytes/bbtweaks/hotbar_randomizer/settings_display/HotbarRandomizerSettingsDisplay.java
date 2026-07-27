package me.blvckbytes.bbtweaks.hotbar_randomizer.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.hotbar_randomizer.HotbarRandomizerSettings;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntConsumer;

public class HotbarRandomizerSettingsDisplay extends Display<HotbarRandomizerSettings> {

  public HotbarRandomizerSettingsDisplay(
    Player player,
    HotbarRandomizerSettings displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.hotbarRandomizer.settingsDisplay.items.enabled.renderInto(itemConsumer, environment);

    var enabledSlotItem = config.rootSection.hotbarRandomizer.settingsDisplay.items.enabledSlot;
    var hotbarItemPatch = config.rootSection.hotbarRandomizer.settingsDisplay.items.hotbarItemPatch;

    var playerInventory = player.getInventory();

    for (var index = 0; index < HotbarRandomizerSettings.HOTBAR_SLOT_COUNT; ++index) {
      var enabledState = displayData.getSlotEnableState(index);

      environment
        .withVariable("enabled", enabledState)
        .withVariable("slot", index + 1);

      tryGetListItem(enabledSlotItem.getDisplaySlots(), index, itemIndex -> {
        itemConsumer.handle(itemIndex, enabledSlotItem.build(environment));
      });

      var itemAtSlot = playerInventory.getItem(index);

      tryGetListItem(hotbarItemPatch.getDisplaySlots(), index, itemIndex -> {
        ItemStack displayItem;

        if (itemAtSlot == null || itemAtSlot.getType().isAir())
          displayItem = config.rootSection.hotbarRandomizer.settingsDisplay.items.emptySlotPlaceholder.build(environment);
        else {
          displayItem = new ItemStack(itemAtSlot);
          hotbarItemPatch.patch(displayItem, environment);
        }

        itemConsumer.handle(itemIndex, displayItem);
      });
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.hotbarRandomizer.settingsDisplay.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.hotbarRandomizer.settingsDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("player", player.getName())
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("enabled", displayData.enabled);
  }

  private void tryGetListItem(List<Integer> list, int index, IntConsumer handler) {
    if (index >= 0 && index < list.size())
      handler.accept(list.get(index));
  }
}
