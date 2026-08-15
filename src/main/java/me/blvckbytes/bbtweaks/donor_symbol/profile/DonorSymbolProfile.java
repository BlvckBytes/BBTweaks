package me.blvckbytes.bbtweaks.donor_symbol.profile;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.ColorSection;
import me.blvckbytes.bbtweaks.donor_symbol.SymbolSection;
import org.bukkit.entity.Player;

public class DonorSymbolProfile {

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;
  public SymbolSection symbol;
  public ColorSection color;

  public DonorSymbolProfile(
    Player player,
    ConfigKeeper<MainSection> config
  ) {
    this.player = player;
    this.config = config;

    this.enabled = true;
    this.symbol = config.rootSection.donorSymbol._defaultSymbol;
    this.color = config.rootSection.donorSymbol._defaultColor;
  }

  public void onConfigReload() {
    var newSymbol = config.rootSection.donorSymbol._symbolByIdentifierLower.get(symbol._identifierLower);
    this.symbol = newSymbol != null ? newSymbol : config.rootSection.donorSymbol._defaultSymbol;

    var newColor = config.rootSection.donorSymbol._colorByIdentifierLower.get(color._identifierLower);
    this.color = newColor != null ? newColor : config.rootSection.donorSymbol._defaultColor;
  }
}
