package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.inv_filter.command.CommandAction;
import me.blvckbytes.bbtweaks.inv_filter.command.InvFilterCommand;
import me.blvckbytes.bbtweaks.inv_magnet.PreAttractItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class InvFilterListener implements Listener {

  private final Plugin plugin;
  private final InvFilterProfileStore profileStore;
  private final InvFilterCommand invFilterCommand;
  private final ConfigKeeper<MainSection> config;

  public InvFilterListener(
    Plugin plugin,
    InvFilterProfileStore profileStore,
    InvFilterCommand invFilterCommand,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.profileStore = profileStore;
    this.invFilterCommand = invFilterCommand;
    this.config = config;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPickup(PlayerAttemptPickupItemEvent event) {
    if (isExcludedByFilter(event.getPlayer(), event.getItem().getItemStack()))
      event.setCancelled(true);
  }

  // Important! Check this condition first, since all other checks are more expensive.
  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPreAttractItem(PreAttractItemEvent event) {
    if (isExcludedByFilter(event.getPlayer(), event.getAttractedItem()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    var profile = profileStore.access(player);
    var activeFilter = profile.getCurrentFilterIfEnabled();

    if (activeFilter == null)
      return;

    // Ensure that this important message is not spammed away by Essentials' MOTD and the like.
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var disableCommand = "/" + invFilterCommand.getShortestNameOrAlias() + " " + CommandAction.matcher.getNormalizedName(CommandAction.OFF);

      config.rootSection.invFilter.activeFilterWarning.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("filter", activeFilter.getTokenPredicateString())
          .withVariable("disable_command", disableCommand)
      );
    }, 5L);
  }

  private boolean isExcludedByFilter(Player player, ItemStack item) {
    var filter = profileStore.access(player).getCurrentFilterIfEnabled();

    if (filter == null)
      return false;

    return !filter.predicate.test(item);
  }
}
