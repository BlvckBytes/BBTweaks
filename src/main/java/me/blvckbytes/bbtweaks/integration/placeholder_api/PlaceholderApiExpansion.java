package me.blvckbytes.bbtweaks.integration.placeholder_api;

import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfileStore;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.LegacyColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaceholderApiExpansion extends PlaceholderExpansion implements Relational, Disableable, Tickable, Listener {

  private final DonorSymbolProfileStore donorSymbolProfileStore;
  private final FloodgateIntegration floodgateIntegration;
  private final LuckPerms luckPerms;
  private final Plugin plugin;

  private final Map<UUID, ComputedValueCache> cacheByPlayerId;

  private long relativeTime;

  public PlaceholderApiExpansion(
    DonorSymbolProfileStore donorSymbolProfileStore,
    FloodgateIntegration floodgateIntegration,
    LuckPerms luckPerms,
    Plugin plugin
  ) {
    this.donorSymbolProfileStore = donorSymbolProfileStore;
    this.floodgateIntegration = floodgateIntegration;
    this.luckPerms = luckPerms;
    this.plugin = plugin;

    this.cacheByPlayerId = new HashMap<>();

    register();

    plugin.getLogger().info("Registered placeholder-expansion");
  }

  @Override
  public @Nullable String onPlaceholderRequest(Player viewer, Player target, @NotNull String params) {
    if (params.equalsIgnoreCase("donor_symbol_prepended_luckperms_suffix")) {
      var valueCacheOfTarget = accessPossiblyUpdatedValueCache(target);

      if (valueCacheOfTarget == null)
        return "";

      if (floodgateIntegration.isFloodgatePlayer(viewer))
        return valueCacheOfTarget.donorSymbolPrependedLuckPermsSuffixForBedrock;

      return valueCacheOfTarget.donorSymbolPrependedLuckPermsSuffixForJava;
    }

    return null;
  }

  @Override
  public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
    return onPlaceholderRequest(player, player, params);
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

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    Bukkit.getScheduler().runTaskLater(plugin, () -> cacheByPlayerId.remove(playerId), 5L);
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;
  }

  private @Nullable ComputedValueCache accessPossiblyUpdatedValueCache(Player player) {
    // Let's rather be a bit defensive as we don't know who invokes this placeholder.
    // Otherwise, we'd possibly leak memory, if an old reference is used after quit.
    if (!player.isOnline())
      return null;

    var valueCache = cacheByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> new ComputedValueCache());

    if (!valueCache.touchLastUpdateIfApplicable(relativeTime))
      return valueCache;

    updateCachedValues(player, valueCache);

    return valueCache;
  }

  private void updateCachedValues(Player player, ComputedValueCache output) {
    var profile = donorSymbolProfileStore.accessProfile(player);

    var donorSymbolSuffixForJava = profile.renderJavaSymbolOrEmpty();
    var donorSymbolSuffixForBedrock = profile.renderBedrockSymbolOrEmpty();

    if (!donorSymbolSuffixForJava.isEmpty())
      donorSymbolSuffixForJava = " " + donorSymbolSuffixForJava;

    if (!donorSymbolSuffixForBedrock.isEmpty())
      donorSymbolSuffixForBedrock = " " + donorSymbolSuffixForBedrock;

    var metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
    var luckPermsSuffix = metaData.getSuffix();

    if (luckPermsSuffix == null || LegacyColorUtil.stripColors(luckPermsSuffix).isBlank()) {
      output.donorSymbolPrependedLuckPermsSuffixForJava = donorSymbolSuffixForJava;
      output.donorSymbolPrependedLuckPermsSuffixForBedrock = donorSymbolSuffixForBedrock;
      return;
    }

    luckPermsSuffix = LegacyColorUtil.enableColors(luckPermsSuffix);

    if (donorSymbolSuffixForJava.isEmpty())
      output.donorSymbolPrependedLuckPermsSuffixForJava = luckPermsSuffix;
    else
      output.donorSymbolPrependedLuckPermsSuffixForJava = donorSymbolSuffixForJava + " " + luckPermsSuffix;

    if (donorSymbolSuffixForBedrock.isEmpty())
      output.donorSymbolPrependedLuckPermsSuffixForBedrock = luckPermsSuffix;
    else
      output.donorSymbolPrependedLuckPermsSuffixForBedrock = donorSymbolSuffixForBedrock + " " + luckPermsSuffix;
  }
}
