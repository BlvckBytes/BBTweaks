package me.blvckbytes.bbtweaks;

import me.blvckbytes.bbtweaks.ab_sleep.ActionBarSleepMessage;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipesSection;
import me.blvckbytes.bbtweaks.auto_fly.AutoFlyCommand;
import me.blvckbytes.bbtweaks.auto_pickup_container.command.AutoPickupContainerCommand;
import me.blvckbytes.bbtweaks.auto_pickup_container.settings.AutoPickupContainerSettingsStore;
import me.blvckbytes.bbtweaks.auto_tool.AutoToolCommand;
import me.blvckbytes.bbtweaks.auto_wirer.AutoWirer;
import me.blvckbytes.bbtweaks.back.BackOverrideCommand;
import me.blvckbytes.bbtweaks.back.BacktrackCommand;
import me.blvckbytes.bbtweaks.back.LocationHistoryStore;
import me.blvckbytes.bbtweaks.bottlexp.BottleXpCommand;
import me.blvckbytes.bbtweaks.command_items.CommandItemListener;
import me.blvckbytes.bbtweaks.custom_commands.CustomCommandsManager;
import me.blvckbytes.bbtweaks.durability_warnings.DurabilityWarningsListener;
import me.blvckbytes.bbtweaks.durability_warnings.WarningsProfileStore;
import me.blvckbytes.bbtweaks.durability_warnings.command.DurabilityWarningCommand;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.infinite_waterbucket.InfiniteWaterbucketListener;
import me.blvckbytes.bbtweaks.integration.mc_mmo.McMMOIntegrationLoader;
import me.blvckbytes.bbtweaks.get_exp.GetExpCommand;
import me.blvckbytes.bbtweaks.get_uuid.GetUuidCommand;
import me.blvckbytes.bbtweaks.integration.craftbook.CraftBookIntegrationLoader;
import me.blvckbytes.bbtweaks.integration.discord.DiscordIntegrationLoader;
import me.blvckbytes.bbtweaks.integration.ipp.PredicateHelperIntegrationLoader;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterCommand;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.inv_magnet.InvMagnetCommand;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import me.blvckbytes.bbtweaks.list_chunk_tickets.ListChunkTicketsCommand;
import me.blvckbytes.bbtweaks.main_command.MainCommand;
import me.blvckbytes.bbtweaks.markers_menu.MarkersCommand;
import me.blvckbytes.bbtweaks.markers_menu.SetMarkerCommand;
import me.blvckbytes.bbtweaks.markers_menu.display.MarkerDisplayHandler;
import me.blvckbytes.bbtweaks.mechanic.SignMechanicManager;
import me.blvckbytes.bbtweaks.mechanic.magnet.edit_display.MagnetEditDisplayHandler;
import me.blvckbytes.bbtweaks.multi_break.command.MultiBreakCommand;
import me.blvckbytes.bbtweaks.multi_break.MultiBreakListener;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import me.blvckbytes.bbtweaks.multi_break.display.MultiBreakDisplayHandler;
import me.blvckbytes.bbtweaks.newbie_announce.NewbieAnnounceHandler;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportCommand;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportResetCommand;
import me.blvckbytes.bbtweaks.ping.PingCommand;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipes;
import me.blvckbytes.bbtweaks.rd_breaker.RDBreakerListener;
import me.blvckbytes.bbtweaks.seed.SeedOverrideCommand;
import me.blvckbytes.bbtweaks.shulker_accessor.change_detection.InventoryChangeDetector;
import me.blvckbytes.bbtweaks.shulker_accessor.ShulkerAccessorListener;
import me.blvckbytes.bbtweaks.sidebar.SidebarBoardManager;
import me.blvckbytes.bbtweaks.integration.arm.ArmIntegrationLoader;
import me.blvckbytes.bbtweaks.sidebar.color_display.SidebarColorDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.command.SidebarCommand;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferencesStore;
import me.blvckbytes.bbtweaks.sidebar.settings_display.SidebarSettingsDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SidebarSortingDisplayHandler;
import me.blvckbytes.bbtweaks.sign_copier.SignCopyCommand;
import me.blvckbytes.bbtweaks.sign_copier.SignEditCommand;
import me.blvckbytes.bbtweaks.sign_copier.settings.SignCopierSettingsStore;
import me.blvckbytes.bbtweaks.sign_copier.settings_display.SignCopierSettingsDisplayHandler;
import me.blvckbytes.bbtweaks.un_craft.UnCraftCommand;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegrationLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class BBTweaksPlugin extends JavaPlugin {

  // TODO: Idea - /empty-out [all]

  private final AutoWirer autoWirer;
  private @Nullable Throwable loadError;

  public BBTweaksPlugin() {
    autoWirer = new AutoWirer(this);
  }

  @Override
  public void onLoad() {
    try {
      // It's important that we do this in onLoad, as WorldGuard only allows to register flags at this point.
      autoWirer.withSingleton(WorldGuardFlags.class);
    } catch (Throwable e) {
      loadError = e;
    }
  }

  @Override
  public void onEnable() {
    AdditionalRecipesSection.plugin = this;

    try {
      if (loadError != null)
        throw loadError;

      autoWirer
        .withSingleton(MainConfigLoader.class)
        .withSingleton(PredicateHelperIntegrationLoader.class)
        .withSingleton(McMMOIntegrationLoader.class)
        .withSingleton(FloodgateIntegrationLoader.class)
        .withSingleton(ArmIntegrationLoader.class)
        .withSingleton(DiscordIntegrationLoader.class)
        .withSingleton(CraftBookIntegrationLoader.class)
        .withSingleton(ActionBarSleepMessage.class)
        .withSingleton(RDBreakerListener.class)
        .withSingleton(LavaSponge.class)
        .withSingleton(GetUuidCommand.class)
        .withSingleton(LocationHistoryStore.class)
        .withSingleton(BackOverrideCommand.class)
        .withSingleton(BacktrackCommand.class)
        .withSingleton(AdditionalRecipes.class)
        .withSingleton(UnCraftCommand.class)
        .withSingleton(PingCommand.class)
        .withSingleton(FurnaceLevelDisplay.class)
        .withSingleton(MagnetEditDisplayHandler.class)
        .withSingleton(SignMechanicManager.class)
        .withSingleton(SeedOverrideCommand.class)
        .withSingleton(CustomCommandsManager.class)
        .withSingleton(InvFilterCommand.class)
        .withSingleton(MarkerDisplayHandler.class)
        .withSingleton(MarkersCommand.class)
        .withSingleton(SetMarkerCommand.class)
        .withSingleton(AutoFlyCommand.class)
        .withSingleton(NewbieTeleportCommand.class)
        .withSingleton(NewbieTeleportResetCommand.class)
        .withSingleton(MultiBreakParametersStore.class)
        .withSingleton(MultiBreakListener.class)
        .withSingleton(MultiBreakDisplayHandler.class)
        .withSingleton(MultiBreakCommand.class)
        .withSingleton(InvMagnetParametersStore.class)
        .withSingleton(InvMagnetCommand.class)
        .withSingleton(GetExpCommand.class)
        .withSingleton(WarningsProfileStore.class)
        .withSingleton(DurabilityWarningCommand.class)
        .withSingleton(DurabilityWarningsListener.class)
        .withSingleton(AutoToolCommand.class)
        .withSingleton(InventoryChangeDetector.class)
        .withSingleton(ShulkerAccessorListener.class)
        .withSingleton(AutoPickupContainerSettingsStore.class)
        .withSingleton(AutoPickupContainerCommand.class)
        .withSingleton(AutoPickupContainerListener.class)
        .withSingleton(SidebarPreferencesStore.class)
        .withSingleton(SidebarColorDisplayHandler.class)
        .withSingleton(SidebarSortingDisplayHandler.class)
        .withSingleton(SidebarSettingsDisplayHandler.class)
        .withSingleton(SidebarCommand.class)
        .withSingleton(SidebarBoardManager.class)
        .withSingleton(NewbieAnnounceHandler.class)
        .withSingleton(InfiniteWaterbucketListener.class)
        .withSingleton(MainCommand.class)
        .withSingleton(CommandItemListener.class)
        .withSingleton(SignCopierSettingsStore.class)
        .withSingleton(SignCopierSettingsDisplayHandler.class)
        .withSingleton(SignCopyCommand.class)
        .withSingleton(SignEditCommand.class)
        .withSingleton(BottleXpCommand.class)
        .withSingleton(ListChunkTicketsCommand.class)
        .complete();
    } catch (Throwable e) {
      getLogger().log(Level.SEVERE, "An error occurred while trying to set up the plugin", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    autoWirer.onDisable();
  }
}
