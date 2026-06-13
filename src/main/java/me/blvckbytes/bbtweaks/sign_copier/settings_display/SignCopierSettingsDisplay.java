package me.blvckbytes.bbtweaks.sign_copier.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.sign_copier.settings.SettingFlag;
import me.blvckbytes.bbtweaks.sign_copier.settings.SignCopierSettings;
import me.blvckbytes.bbtweaks.util.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class SignCopierSettingsDisplay extends Display<SignCopierSettings> {

  private final boolean isFloodgate;

  public SignCopierSettingsDisplay(
    Player player,
    SignCopierSettings displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    show();
  }

  @Override
  protected void renderItems() {
    var environment = makeEnvironment();

    config.rootSection.signCopier.settingsDisplay.items.filler.renderInto(inventory, environment);
    config.rootSection.signCopier.settingsDisplay.items.pasteSignColor.renderInto(inventory, environment);
    config.rootSection.signCopier.settingsDisplay.items.pasteSignGlowing.renderInto(inventory, environment);
    config.rootSection.signCopier.settingsDisplay.items.sendCopiedMessage.renderInto(inventory, environment);
    config.rootSection.signCopier.settingsDisplay.items.sendPastedMessage.renderInto(inventory, environment);
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.signCopier.settingsDisplay.createInventory(makeEnvironment());
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
      .withVariable("send_pasted_message", displayData.flags.contains(SettingFlag.SEND_PASTED_MESSAGE));
  }
}
