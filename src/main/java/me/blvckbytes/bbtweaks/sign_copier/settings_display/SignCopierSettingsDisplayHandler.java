package me.blvckbytes.bbtweaks.sign_copier.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.sign_copier.settings.SettingFlag;
import me.blvckbytes.bbtweaks.sign_copier.settings.SignCopierSettings;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SignCopierSettingsDisplayHandler extends DisplayHandler<SignCopierSettingsDisplay, SignCopierSettings> {

  private final FloodgateIntegration floodgateIntegration;

  public SignCopierSettingsDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public SignCopierSettingsDisplay instantiateDisplay(Player player, SignCopierSettings displayData) {
    return new SignCopierSettingsDisplay(player, displayData, floodgateIntegration, config, plugin);
  }

  @Override
  protected void handleClick(Player player, SignCopierSettingsDisplay display, ClickType clickType, int slot) {
    if (clickType != ClickType.LEFT)
      return;

    var targetFlag = getTargetFlag(slot);

    if (targetFlag == null)
      return;

    toggleFlag(display.displayData, targetFlag);

    display.renderItems();
  }

  private void toggleFlag(SignCopierSettings settings, SettingFlag flag) {
    if (settings.flags.contains(flag)) {
      settings.flags.remove(flag);
      return;
    }

    settings.flags.add(flag);
  }

  private @Nullable SettingFlag getTargetFlag(int slot) {
    if (config.rootSection.signCopier.settingsDisplay.items.pasteSignColor.getDisplaySlots().contains(slot))
      return SettingFlag.PASTE_SIGN_COLOR;

    if (config.rootSection.signCopier.settingsDisplay.items.pasteSignGlowing.getDisplaySlots().contains(slot))
      return SettingFlag.PASTE_SIGN_GLOWING;

    if (config.rootSection.signCopier.settingsDisplay.items.sendCopiedMessage.getDisplaySlots().contains(slot))
      return SettingFlag.SEND_COPIED_MESSAGE;

    if (config.rootSection.signCopier.settingsDisplay.items.sendPastedMessage.getDisplaySlots().contains(slot))
      return SettingFlag.SEND_PASTED_MESSAGE;

    if (config.rootSection.signCopier.settingsDisplay.items.inkSacAsShortcut.getDisplaySlots().contains(slot))
      return SettingFlag.INK_SAC_AS_SHORTCUT;

    return null;
  }
}
