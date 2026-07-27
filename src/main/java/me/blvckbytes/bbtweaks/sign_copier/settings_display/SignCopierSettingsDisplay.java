package me.blvckbytes.bbtweaks.sign_copier.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.sign_copier.settings.SettingFlag;
import me.blvckbytes.bbtweaks.sign_copier.settings.SignCopierSettings;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SignCopierSettingsDisplay extends Display<SignCopierSettings> {

  public SignCopierSettingsDisplay(
    Player player,
    SignCopierSettings displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.signCopier.settingsDisplay.items.pasteSignColor.renderInto(itemConsumer, environment);
    config.rootSection.signCopier.settingsDisplay.items.pasteSignGlowing.renderInto(itemConsumer, environment);
    config.rootSection.signCopier.settingsDisplay.items.sendCopiedMessage.renderInto(itemConsumer, environment);
    config.rootSection.signCopier.settingsDisplay.items.sendPastedMessage.renderInto(itemConsumer, environment);
    config.rootSection.signCopier.settingsDisplay.items.inkSacAsShortcut.renderInto(itemConsumer, environment);
    config.rootSection.signCopier.settingsDisplay.items.pasteAdditionalAttributes.renderInto(itemConsumer, environment);
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.signCopier.settingsDisplay.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.signCopier.settingsDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("player", displayData.player.getName())
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("paste_sign_color", displayData.flags.contains(SettingFlag.PASTE_SIGN_COLOR))
      .withVariable("paste_sign_glowing", displayData.flags.contains(SettingFlag.PASTE_SIGN_GLOWING))
      .withVariable("send_copied_message", displayData.flags.contains(SettingFlag.SEND_COPIED_MESSAGE))
      .withVariable("send_pasted_message", displayData.flags.contains(SettingFlag.SEND_PASTED_MESSAGE))
      .withVariable("ink_sac_as_shortcut", displayData.flags.contains(SettingFlag.INK_SAC_AS_SHORTCUT))
      .withVariable("paste_additional_attributes", displayData.flags.contains(SettingFlag.PASTE_ADDITIONAL_ATTRIBUTES));
  }
}
