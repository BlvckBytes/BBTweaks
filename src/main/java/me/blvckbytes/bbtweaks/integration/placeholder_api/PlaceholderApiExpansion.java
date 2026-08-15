package me.blvckbytes.bbtweaks.integration.placeholder_api;

import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfileStore;
import me.blvckbytes.bbtweaks.util.LegacyColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderApiExpansion extends PlaceholderExpansion implements Disableable {

  private final DonorSymbolProfileStore donorSymbolProfileStore;
  private final LuckPerms luckPerms;
  private final Plugin plugin;

  public PlaceholderApiExpansion(
    DonorSymbolProfileStore donorSymbolProfileStore,
    LuckPerms luckPerms,
    Plugin plugin
  ) {
    this.donorSymbolProfileStore = donorSymbolProfileStore;
    this.luckPerms = luckPerms;
    this.plugin = plugin;

    register();

    plugin.getLogger().info("Registered placeholder-expansion");
  }

  @Override
  public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
    if (params.equalsIgnoreCase("donor_symbol_prepended_luckperms_suffix")) {
      var donorSymbolProfile = donorSymbolProfileStore.accessProfile(player);
      var donorSymbol = donorSymbolProfile.renderJavaSymbolOrEmpty();

      var metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
      var suffix = metaData.getSuffix();

      if (suffix == null || LegacyColorUtil.stripColors(suffix).isBlank()) {
        if (donorSymbol.isEmpty())
          return "";

        return " " + donorSymbol;
      }

      suffix = LegacyColorUtil.enableColors(suffix);

      if (donorSymbol.isEmpty())
        return suffix;

      return " " + donorSymbol + " " + suffix.trim();
    }

    return super.onPlaceholderRequest(player, params);
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @NotNull String getIdentifier() {
    return plugin.getPluginMeta().getName().toLowerCase();
  }

  @Override
  public @NotNull String getAuthor() {
    return String.join(",", plugin.getPluginMeta().getAuthors());
  }

  @Override
  public @NotNull String getVersion() {
    return plugin.getPluginMeta().getVersion();
  }

  @Override
  public void disable() {
    unregister();
  }
}
