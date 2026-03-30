package me.blvckbytes.bbtweaks.newbie_announce;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.discord.DiscordIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class NewbieAnnounceHandler implements Listener {

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  public NewbieAnnounceHandler(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    if (player.hasPlayedBefore())
      return;

    // Ensure that the join-message as well as various other announcements are sent out
    // first, such that our broadcast isn't spammed away and players forget to greet the newbie.
    Bukkit.getScheduler().runTaskLater(plugin, () -> broadcastNewbieAnnounce(player), 5L);
  }

  private void broadcastNewbieAnnounce(Player newbie) {
    var environment = new InterpretationEnvironment()
      .withVariable("name", newbie.getName());

    if (config.rootSection.newbieAnnounce.enableDiscord) {
      var discordApi = DiscordIntegration.getOrLoadInstance(plugin, plugin.getLogger(), config).getDiscordApi();

      if (discordApi != null)
        discordApi.sendMessage(config.rootSection.newbieAnnounce.discordMessage.asPlainString(environment));
    }

    if (config.rootSection.newbieAnnounce.enableInGame) {
      var components = config.rootSection.newbieAnnounce.inGameMessage.interpret(SlotType.CHAT, environment);

      for (var target : Bukkit.getOnlinePlayers())
        components.forEach(target::sendMessage);

      components.forEach(Bukkit.getConsoleSender()::sendMessage);
    }
  }
}
