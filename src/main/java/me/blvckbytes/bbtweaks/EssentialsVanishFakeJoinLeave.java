package me.blvckbytes.bbtweaks;

import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EssentialsVanishFakeJoinLeave implements Listener {

  // Honestly, I'm done waiting on my PR getting merged; let's update regularly from this point onward again.

  private final IEssentials essentials;

  public EssentialsVanishFakeJoinLeave(
    IEssentials essentials
  ) {
    this.essentials = essentials;
  }

  @EventHandler
  public void onVanishStatusChange(VanishStatusChangeEvent event) {
    // Note: getController() returns the vanished player due to a long-standing parameter swap in Commandvanish.
    var user = event.getController();

    if (!user.isAuthorized("bbtweaks.fake-vanish-join-leave-messages"))
      return;

    var fakeMessage = event.getValue() ? buildQuitMessage(user) : buildJoinMessage(user);

    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(fakeMessage));
  }

  private String buildJoinMessage(IUser user) {
    return essentials.getSettings().getCustomJoinMessage()
      .replace("{PLAYER}", user.getDisplayName())
      .replace("{USERNAME}", user.getName());
  }

  private String buildQuitMessage(IUser user) {
    return essentials.getSettings().getCustomQuitMessage()
      .replace("{PLAYER}", user.getDisplayName())
      .replace("{USERNAME}", user.getName());
  }
}
