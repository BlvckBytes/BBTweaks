package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.ConfigHandler;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ReloadPriority;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import com.gmail.nossr50.util.player.UserManager;
import me.blvckbytes.bbtweaks.ab_sleep.ActionBarSleepMessage;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipesSection;
import me.blvckbytes.bbtweaks.auto_fly.AutoFlyCommand;
import me.blvckbytes.bbtweaks.auto_fly.AutoFlyCommandSection;
import me.blvckbytes.bbtweaks.back.BackOverrideCommand;
import me.blvckbytes.bbtweaks.back.BacktrackCommand;
import me.blvckbytes.bbtweaks.back.BacktrackCommandSection;
import me.blvckbytes.bbtweaks.back.LocationHistoryStore;
import me.blvckbytes.bbtweaks.custom_commands.CustomCommandsManager;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.furnace_level_display.McMMOIntegration;
import me.blvckbytes.bbtweaks.get_uuid.GetUuidCommand;
import me.blvckbytes.bbtweaks.integration.craftbook.CraftBookIntegration;
import me.blvckbytes.bbtweaks.integration.discord.DiscordIntegration;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterCommand;
import me.blvckbytes.bbtweaks.main_command.MainCommand;
import me.blvckbytes.bbtweaks.markers_menu.MarkersCommand;
import me.blvckbytes.bbtweaks.markers_menu.MarkersCommandSection;
import me.blvckbytes.bbtweaks.markers_menu.SetMarkerCommand;
import me.blvckbytes.bbtweaks.markers_menu.SetMarkerCommandSection;
import me.blvckbytes.bbtweaks.markers_menu.display.MarkerDisplayHandler;
import me.blvckbytes.bbtweaks.mechanic.SignMechanicManager;
import me.blvckbytes.bbtweaks.newbie_announce.NewbieAnnounceHandler;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportCommand;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportCommandSection;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportResetCommand;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportResetCommandSection;
import me.blvckbytes.bbtweaks.ping.PingCommand;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipes;
import me.blvckbytes.bbtweaks.private_vaults.PrivateVaultCommand;
import me.blvckbytes.bbtweaks.private_vaults.PrivateVaultManager;
import me.blvckbytes.bbtweaks.private_vaults.PrivateVaultViewCommand;
import me.blvckbytes.bbtweaks.seed.SeedOverrideCommand;
import me.blvckbytes.bbtweaks.un_craft.UnCraftCommand;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.NameScopedKeyValueStore;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public class BBTweaksPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

  private LocationHistoryStore locationHistoryStore;
  private SignMechanicManager mechanicManager;
  private WorldGuardFlags worldGuardFlags;
  private MarkerDisplayHandler markerDisplayHandler;
  private NameScopedKeyValueStore preferencesStore;
  private PrivateVaultManager vaultManager;

  // TODO: Idea - /empty-out [all]

  @Override
  public void onLoad() {
    // It's important that we do this in onLoad, as WorldGuard only allows to register flags at this point.
    worldGuardFlags = new WorldGuardFlags();
  }

  @Override
  public void onEnable() {
    AdditionalRecipesSection.plugin = this;

    try {
      var configHandler = new ConfigHandler(this, "config");
      var config = new ConfigKeeper<>(configHandler, "config.yml", MainSection.class);

      DiscordIntegration.getOrLoadInstance(this, getLogger(), config);

      preferencesStore = new NameScopedKeyValueStore(getFileAndEnsureExistence("user-preferences.json"), getLogger());
      Bukkit.getScheduler().runTaskTimerAsynchronously(this, preferencesStore::saveToDisk, 20 * 60L, 20 * 60L);

      new ActionBarSleepMessage(this, config);

      var rdBreakTool = new RDBreakTool(this);

      getServer().getPluginManager().registerEvents(rdBreakTool, this);

      var mainCommandExecutor = new MainCommand(config, rdBreakTool, this);

      Objects.requireNonNull(getCommand("bbtweaks")).setExecutor(mainCommandExecutor);

      getServer().getPluginManager().registerEvents(new LavaSponge(), this);

      var getUuidCommand = new GetUuidCommand(config);

      Objects.requireNonNull(getCommand("getuuid")).setExecutor(getUuidCommand);

      getServer().getPluginManager().registerEvents(getUuidCommand, this);

      locationHistoryStore = new LocationHistoryStore(this);

      Bukkit.getScheduler().runTaskTimerAsynchronously(this, locationHistoryStore::save, 20L * 60, 20L * 60);

      var backtrackCommandExecutor = new BacktrackCommand(this, locationHistoryStore, config);
      var backtrackCommand = Objects.requireNonNull(getCommand(BacktrackCommandSection.INITIAL_NAME));

      backtrackCommand.setExecutor(backtrackCommandExecutor);

      getServer().getPluginManager().registerEvents(backtrackCommandExecutor, this);

      var backOverrideCommand = new BackOverrideCommand(locationHistoryStore, config);

      Objects.requireNonNull(getCommand("back")).setExecutor(backOverrideCommand);

      getServer().getPluginManager().registerEvents(backOverrideCommand, this);

      new AdditionalRecipes(getLogger(), config);

      var unCraftCommand = new UnCraftCommand(this, config);

      Objects.requireNonNull(getCommand("uncraft")).setExecutor(unCraftCommand);

      var pingCommand = Objects.requireNonNull(getCommand("ping"));
      var pingExecutor = new PingCommand(pingCommand, config);

      getServer().getPluginManager().registerEvents(pingExecutor, this);

      pingCommand.setExecutor(pingExecutor);

      McMMOIntegration mcMMOIntegration = null;

      if (Bukkit.getPluginManager().isPluginEnabled("mcMMO")) {
        mcMMOIntegration = ((player, experience) -> {
          var internalPlayer = UserManager.getPlayer(player);

          if (internalPlayer == null)
            return experience;

          return internalPlayer.getSmeltingManager().vanillaXPBoost(experience);
        });

        getLogger().info("Integrated with mcMMO as to boost XP");
      }

      var furnaceLevelDisplay = new FurnaceLevelDisplay(this, mcMMOIntegration, config);

      getServer().getPluginManager().registerEvents(furnaceLevelDisplay, this);

      Bukkit.getScheduler().runTaskLater(this, () -> {
        if (!furnaceLevelDisplay.setUp()) {
          getLogger().severe("Failed trying to set up furnace-level displays; disabling!");
          Bukkit.getPluginManager().disablePlugin(this);
          return;
        }

        getLogger().info("Successfully set up furnace-level displays!");
      }, 10L);

      ItemPredicateParserPlugin ipp;

      if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ItemPredicateParser") || (ipp = ItemPredicateParserPlugin.getInstance()) == null)
        throw new IllegalArgumentException("Expected plugin ItemPredicateParser to have been loaded at this point");

      var predicateHelper = ipp.getPredicateHelper();

      mechanicManager = new SignMechanicManager(this, config, predicateHelper);

      if (worldGuardFlags != null)
        getServer().getPluginManager().registerEvents(worldGuardFlags, this);

      var seedOverride = new SeedOverrideCommand(config);

      Objects.requireNonNull(getCommand("seed")).setExecutor(seedOverride);

      getServer().getPluginManager().registerEvents(seedOverride, this);

      var commandUpdater = new CommandUpdater(this);

      var customCommandsManager = new CustomCommandsManager(commandUpdater, this, config);
      getServer().getPluginManager().registerEvents(customCommandsManager, this);

      var invFilterCommand = Objects.requireNonNull(getCommand("invfilter"));
      var invFilterCommandExecutor = new InvFilterCommand(invFilterCommand, this, config);

      getServer().getPluginManager().registerEvents(invFilterCommandExecutor, this);

      invFilterCommand.setExecutor(invFilterCommandExecutor);

      markerDisplayHandler = new MarkerDisplayHandler(config, FloodgateIntegration.load(getLogger()), this);
      getServer().getPluginManager().registerEvents(markerDisplayHandler, this);

      var markerCommand = Objects.requireNonNull(getCommand(MarkersCommandSection.INITIAL_NAME));
      markerCommand.setExecutor(new MarkersCommand(markerDisplayHandler, config));

      var setMarkerCommand = Objects.requireNonNull(getCommand(SetMarkerCommandSection.INITIAL_NAME));
      setMarkerCommand.setExecutor(new SetMarkerCommand(config, getLogger()));

      var autoFlyCommandExecutor = new AutoFlyCommand(this, preferencesStore, config);
      Bukkit.getServer().getPluginManager().registerEvents(autoFlyCommandExecutor, this);

      var autoFlyCommand = Objects.requireNonNull(getCommand(AutoFlyCommandSection.INITIAL_NAME));
      autoFlyCommand.setExecutor(autoFlyCommandExecutor);

      var newbieTeleportCommand = Objects.requireNonNull(getCommand(NewbieTeleportCommandSection.INITIAL_NAME));
      var newbieTeleportExecutor = new NewbieTeleportCommand(this, config);
      newbieTeleportCommand.setExecutor(newbieTeleportExecutor);

      var newbieTeleportResetCommand = Objects.requireNonNull(getCommand(NewbieTeleportResetCommandSection.INITIAL_NAME));
      newbieTeleportResetCommand.setExecutor(new NewbieTeleportResetCommand(newbieTeleportExecutor, config));

      vaultManager = new PrivateVaultManager(this, config);

      var privateVaultCommand = Objects.requireNonNull(getCommand("pv"));
      privateVaultCommand.setExecutor(new PrivateVaultCommand(vaultManager, config));

      var privateVaultViewCommand = Objects.requireNonNull(getCommand("pvv"));
      privateVaultViewCommand.setExecutor(new PrivateVaultViewCommand(vaultManager, config));

      Runnable updateCommands = () -> {
        config.rootSection.markersMenu.markersCommand.apply(markerCommand, commandUpdater);
        config.rootSection.markersMenu.setMarkerCommand.apply(setMarkerCommand, commandUpdater);
        config.rootSection.autoFly.command.apply(autoFlyCommand, commandUpdater);
        config.rootSection.backOverride.backtrackCommand.apply(backtrackCommand, commandUpdater);
        config.rootSection.newbieTeleport.mainCommand.apply(newbieTeleportCommand, commandUpdater);
        config.rootSection.newbieTeleport.resetCommand.apply(newbieTeleportResetCommand, commandUpdater);
      };

      updateCommands.run();
      commandUpdater.trySyncCommands();

      config.registerReloadListener(updateCommands);
      config.registerReloadListener(commandUpdater::trySyncCommands, ReloadPriority.LOWEST);

      getServer().getPluginManager().registerEvents(CraftBookIntegration.INSTANCE, this);

      getServer().getPluginManager().registerEvents(new NewbieAnnounceHandler(this, config), this);
    } catch (Throwable e) {
      getLogger().log(Level.SEVERE, "An error occurred while trying to enable the plugin; disabling!", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (preferencesStore != null) {
      catchAll(preferencesStore::saveToDisk);
      preferencesStore = null;
    }

    if (locationHistoryStore != null) {
      catchAll(locationHistoryStore::save);
      locationHistoryStore = null;
    }

    if (mechanicManager != null) {
      catchAll(mechanicManager::shutdown);
      mechanicManager = null;
    }

    if (markerDisplayHandler != null) {
      catchAll(markerDisplayHandler::onShutdown);
      markerDisplayHandler = null;
    }

    if (vaultManager != null) {
      catchAll(vaultManager::onShutdown);
      vaultManager = null;
    }
  }

  private void catchAll(Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      getLogger().log(Level.SEVERE, "An internal error occurred", e);
    }
  }

  private File getFileAndEnsureExistence(String name) throws Exception {
    var file = new File(getDataFolder(), name);

    if (!file.exists()) {
      var parentDirectory = file.getParentFile();

      if (!parentDirectory.exists() && !parentDirectory.mkdirs())
        throw new IllegalStateException("Could not create parent-directories of the file " + file);

      if (!file.createNewFile())
        throw new IllegalStateException("Could not create the file " + file);
    }

    return file;
  }
}
