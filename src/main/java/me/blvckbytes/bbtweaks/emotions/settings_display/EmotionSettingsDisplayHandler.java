package me.blvckbytes.bbtweaks.emotions.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.emotions.NotificationOrigin;
import me.blvckbytes.bbtweaks.emotions.NotificationPart;
import me.blvckbytes.bbtweaks.emotions.user_profile.EmotionUserProfile;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class EmotionSettingsDisplayHandler extends DisplayHandler<EmotionSettingsDisplay, EmotionUserProfile> {

  private final FloodgateIntegration floodgateIntegration;

  public EmotionSettingsDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin, EmotionSettingsDisplay.class);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected EmotionSettingsDisplay instantiateDisplay(Player player, EmotionUserProfile displayData) {
    return new EmotionSettingsDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, EmotionSettingsDisplay display, ClickType clickType, int slot) {
    if (clickType == ClickType.LEFT) {
      if (tryHandleStatusButtonClick(display.displayData, slot))
        display.updateItems();
    }
  }

  private boolean tryHandleStatusButtonClick(EmotionUserProfile userProfile, int slot) {
    var statusButton = config.rootSection.emotion.settingsDisplay.items.statusButtons;
    var indexOfSlot = statusButton.getDisplaySlots().indexOf(slot);

    if (indexOfSlot < 0)
      return false;

    var part = NotificationPart.byOrdinalOrNull(indexOfSlot / NotificationOrigin.ALL_VALUES.size());

    if (part == null)
      return false;

    var origin = NotificationOrigin.byOrdinalOrNull(indexOfSlot % NotificationOrigin.ALL_VALUES.size());

    if (origin == null)
      return false;

    userProfile.toggleReceiving(part, origin);
    return true;
  }
}
