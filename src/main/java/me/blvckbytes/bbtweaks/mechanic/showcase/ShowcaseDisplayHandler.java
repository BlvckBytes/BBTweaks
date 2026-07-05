package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class ShowcaseDisplayHandler extends DisplayHandler<ShowcaseDisplay, ShowcaseDisplayData> {

  public ShowcaseDisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);
  }

  @Override
  public ShowcaseDisplay instantiateDisplay(Player player, ShowcaseDisplayData displayData) {
    return new ShowcaseDisplay(player, displayData, config, plugin);
  }

  @Override
  protected void handleClick(Player player, ShowcaseDisplay display, ClickType clickType, int slot) {
    config.rootSection.mechanic.showcase.cannotModifyInventory.sendMessage(player);
  }
}
