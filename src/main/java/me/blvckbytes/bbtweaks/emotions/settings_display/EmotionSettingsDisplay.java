package me.blvckbytes.bbtweaks.emotions.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.emotions.NotificationOrigin;
import me.blvckbytes.bbtweaks.emotions.NotificationPart;
import me.blvckbytes.bbtweaks.emotions.user_profile.EmotionUserProfile;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EmotionSettingsDisplay extends Display<EmotionUserProfile> {

  public EmotionSettingsDisplay(
    Player player,
    EmotionUserProfile displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.emotion.settingsDisplay.items.information.renderInto(itemConsumer, environment);

    config.rootSection.emotion.settingsDisplay.items.descriptionChatPart.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionActionBarPart.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionTitlePart.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionSoundPart.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionEffectPart.renderInto(itemConsumer, environment);

    config.rootSection.emotion.settingsDisplay.items.descriptionAllOrigin.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionDirectOrigin.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionIsSenderOrigin.renderInto(itemConsumer, environment);
    config.rootSection.emotion.settingsDisplay.items.descriptionBroadcastOrigin.renderInto(itemConsumer, environment);

    partLoop:
    for (var notificationPart : NotificationPart.ALL_VALUES) {
      var partOrdinal = notificationPart.ordinal();
      var enabledOrigins = displayData.getEnabledOriginsForPart(notificationPart);

      for (var notificationOrigin : NotificationOrigin.ALL_VALUES) {
        var originOrdinal = notificationOrigin.ordinal();

        var statusButton = config.rootSection.emotion.settingsDisplay.items.statusButtons;
        var displaySlots = statusButton.getDisplaySlots();

        var targetSlotIndex = partOrdinal * NotificationOrigin.ALL_VALUES.size() + originOrdinal;

        if (targetSlotIndex >= displaySlots.size())
          break partLoop;

        var displaySlot = displaySlots.get(targetSlotIndex);

        var buttonItem = statusButton.build(
          environment
            .withVariable("enabled", enabledOrigins.contains(notificationOrigin))
            .withVariable("notification_part", notificationPart.name())
            .withVariable("notification_origin", notificationOrigin.name())
        );

        itemConsumer.handle(displaySlot, buttonItem);
      }
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.emotion.settingsDisplay.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.emotion.settingsDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate);
  }
}
