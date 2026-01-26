package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.ConfigHandler;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.gmail.nossr50.util.player.UserManager;
import me.blvckbytes.bbtweaks.ab_sleep.ActionBarSleepMessage;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipesSection;
import me.blvckbytes.bbtweaks.back.BackOverrideCommand;
import me.blvckbytes.bbtweaks.back.LastLocationStore;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.furnace_level_display.McMMOIntegration;
import me.blvckbytes.bbtweaks.get_uuid.GetUuidCommand;
import me.blvckbytes.bbtweaks.main_command.MainCommand;
import me.blvckbytes.bbtweaks.mechanic.SignMechanicManager;
import me.blvckbytes.bbtweaks.ping.PingCommand;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipes;
import me.blvckbytes.bbtweaks.seed.SeedOverrideCommand;
import me.blvckbytes.bbtweaks.un_craft.UnCraftCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class BBTweaksPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

  private LastLocationStore lastLocationStore;
  private SignMechanicManager mechanicManager;
  private WorldGuardFlags worldGuardFlags;

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

      new ActionBarSleepMessage(this, config);

      var rdBreakTool = new RDBreakTool(this);

      getServer().getPluginManager().registerEvents(rdBreakTool, this);

      var mainCommandExecutor = new MainCommand(config, rdBreakTool, getLogger());

      Objects.requireNonNull(getCommand("bbtweaks")).setExecutor(mainCommandExecutor);

      getServer().getPluginManager().registerEvents(new LavaSponge(), this);

      var getUuidCommand = new GetUuidCommand(config);

      Objects.requireNonNull(getCommand("getuuid")).setExecutor(getUuidCommand);

      getServer().getPluginManager().registerEvents(getUuidCommand, this);

      lastLocationStore = new LastLocationStore(this);

      Bukkit.getScheduler().runTaskTimerAsynchronously(this, lastLocationStore::save, 20L * 60, 20L * 60);

      var backOverrideCommand = new BackOverrideCommand(lastLocationStore, config);

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

      mechanicManager = new SignMechanicManager(this, config);

      if (worldGuardFlags != null)
        getServer().getPluginManager().registerEvents(worldGuardFlags, this);

      var seedOverride = new SeedOverrideCommand(config);

      Objects.requireNonNull(getCommand("seed")).setExecutor(seedOverride);

      getServer().getPluginManager().registerEvents(seedOverride, this);
    } catch (Throwable e) {
      getLogger().log(Level.SEVERE, "An error occurred while trying to enable the plugin; disabling!", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (lastLocationStore != null) {
      lastLocationStore.save();
      lastLocationStore = null;
    }

    if (mechanicManager != null) {
      mechanicManager.shutdown();
      mechanicManager = null;
    }
  }
}
