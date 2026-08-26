package me.blvckbytes.bbtweaks.integration.lwc;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.scripting.Module;
import com.griefcraft.scripting.event.*;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.auto_wirer.LateWired;
import me.blvckbytes.bbtweaks.item_piling.ItemPilingListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class LWCIntegrationHandler implements Module, Disableable {

  @LateWired
  private ItemPilingListener pilingListener;

  private final Plugin plugin;
  private final LWC lwc;

  public LWCIntegrationHandler(
    Plugin plugin
  ) {
    this.plugin = plugin;
    this.lwc = Objects.requireNonNull((LWCPlugin) Bukkit.getPluginManager().getPlugin("LWC")).getLWC();

    lwc.getModuleLoader().registerModule(plugin, this);
  }

  @Override
  public void disable() {
    lwc.getModuleLoader().removeModules(plugin);
  }

  @Override
  public void load(LWC lwc) {}

  @Override
  public void onReload(LWCReloadEvent event) {}

  @Override
  public void onAccessRequest(LWCAccessEvent event) {}

  @Override
  public void onDropItem(LWCDropItemEvent event) {}

  @Override
  public void onCommand(LWCCommandEvent event) {}

  @Override
  public void onRedstone(LWCRedstoneEvent event) {}

  @Override
  public void onDestroyProtection(LWCProtectionDestroyEvent event) {}

  @Override
  public void onProtectionInteract(LWCProtectionInteractEvent event) {}

  @Override
  public void onBlockInteract(LWCBlockInteractEvent event) {}

  @Override
  public void onEntityInteract(LWCEntityInteractEvent event) {}

  @Override
  public void onRegisterProtection(LWCProtectionRegisterEvent event) {}

  @Override
  public void onEntityInteractProtection(LWCProtectionInteractEntityEvent event) {}

  @Override
  public void onPostRegistration(LWCProtectionRegistrationPostEvent event) {}

  @Override
  public void onPostRemoval(LWCProtectionRemovePostEvent event) {}

  @Override
  public void onSendLocale(LWCSendLocaleEvent event) {}

  @Override
  public void onMagnetPull(LWCMagnetPullEvent event) {
    var pulledItem = event.getItem();

    var itemPile = pilingListener.getPile(pulledItem);

    // I don't see a clean way to integrate item-piles with LWC. In the case that there is
    // an additional amount present, which would be destroyed by the plugin, let's simply
    // deny pulling this very item. Players should use modern magnets anyways.
    if (itemPile.getAdditionalAmount() > 0) {
      event.setCancelled(true);
      return;
    }

    var itemEntity = itemPile.getItemEntity();
    var amountBefore = itemEntity.getItemStack().getAmount();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (itemEntity.isDead() || !itemEntity.isValid())
        return;

      var amountAfter = itemEntity.getItemStack().getAmount();

      if (amountBefore == amountAfter)
        return;

      itemPile.updateItemName();
    }, 1L);
  }

  @Override
  public void onRegisterEntity(LWCProtectionRegisterEntityEvent event) {}
}
