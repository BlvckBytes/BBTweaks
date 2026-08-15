package me.blvckbytes.bbtweaks.donor_symbol.profile;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.LateWired;
import me.blvckbytes.bbtweaks.donor_symbol.command.DonorSymbolCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DonorSymbolProfileStore implements Listener {

  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, DonorSymbolProfile> profileByPlayerId;

  private final NamespacedKey keyEnabled, keySelectedSymbol, keySelectedColor;

  @LateWired
  private DonorSymbolCommand donorSymbolCommand;

  public DonorSymbolProfileStore(
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.config = config;

    this.profileByPlayerId = new HashMap<>();

    this.keyEnabled = new NamespacedKey(plugin, "donor-symbol-enabled");
    this.keySelectedSymbol = new NamespacedKey(plugin, "donor-symbol-selected-symbol");
    this.keySelectedColor = new NamespacedKey(plugin, "donor-symbol-selected-color");
  }

  public DonorSymbolProfile accessProfile(Player player) {
    return profileByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper != config)
      return;

    for (var profile : profileByPlayerId.values())
      profile.onConfigReload();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var profile = profileByPlayerId.remove(event.getPlayer().getUniqueId());

    if (profile != null)
      save(profile);
  }

  private DonorSymbolProfile load(Player player) {
    var result = new DonorSymbolProfile(player, donorSymbolCommand.getCommand(), config);
    var pdc = player.getPersistentDataContainer();

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      result.enabled = enabledValue;

    var symbolIdentifier = pdc.get(keySelectedSymbol, PersistentDataType.STRING);

    if (symbolIdentifier != null) {
      var symbol = config.rootSection.donorSymbol._symbolByIdentifierLower.get(symbolIdentifier);

      if (symbol != null && symbol.hasPermission(player))
        result.symbol = symbol;
    }

    var colorIdentifier = pdc.get(keySelectedColor, PersistentDataType.STRING);

    if (colorIdentifier != null) {
      var color = config.rootSection.donorSymbol._colorByIdentifierLower.get(colorIdentifier);

      if (color != null)
        result.color = color;
    }

    return result;
  }

  private void save(DonorSymbolProfile profile) {
    var pdc = profile.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, profile.enabled);
    pdc.set(keySelectedSymbol, PersistentDataType.STRING, profile.symbol._identifierLower);
    pdc.set(keySelectedColor, PersistentDataType.STRING, profile.color._identifierLower);
  }
}
