package me.blvckbytes.bbtweaks.donor_symbol.profile;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.ColorSection;
import me.blvckbytes.bbtweaks.donor_symbol.SymbolSection;
import me.blvckbytes.bbtweaks.util.LegacyColorUtil;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class DonorSymbolProfile {

  public final Player player;
  private final Command donorSymbolCommand;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;
  public SymbolSection symbol;
  public ColorSection color;

  public DonorSymbolProfile(
    Player player,
    Command donorSymbolCommand,
    ConfigKeeper<MainSection> config
  ) {
    this.player = player;
    this.donorSymbolCommand = donorSymbolCommand;
    this.config = config;

    this.enabled = true;
    this.symbol = config.rootSection.donorSymbol.getDefaultSymbol(player);
    this.color = config.rootSection.donorSymbol.getDefaultColor(player);
  }

  public String renderJavaSymbolOrEmpty() {
    if (!enabled || !hasPermission())
      return "";

    return LegacyColorUtil.enableColors(prefixHexColorByAmpersand(color.javaValue)) + symbol.javaValue;
  }

  public String renderBedrockSymbolOrEmpty() {
    if (!enabled || !hasPermission())
      return "";

    return LegacyColorUtil.enableColors(prefixHexColorByAmpersand(color.bedrockValue)) + symbol.bedrockValue;
  }

  public boolean hasPermission() {
    var permission = donorSymbolCommand.getPermission();

    if (permission == null)
      return true;

    return player.hasPermission(permission);
  }

  public void onConfigReload() {
    var newSymbol = config.rootSection.donorSymbol._symbolByIdentifierLower.get(symbol._identifierLower);
    this.symbol = newSymbol != null ? newSymbol : config.rootSection.donorSymbol.getDefaultSymbol(player);

    var newColor = config.rootSection.donorSymbol._colorByIdentifierLower.get(color._identifierLower);
    this.color = newColor != null ? newColor : config.rootSection.donorSymbol.getDefaultColor(player);
  }

  private static String prefixHexColorByAmpersand(String input) {
    if (input.startsWith("#") && input.length() == 7)
      return "&" + input;

    return input;
  }
}
