package me.blvckbytes.bbtweaks;

import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.un_craft.UnCraftCommand;
import me.blvckbytes.bbtweaks.util.ColorUtil;
import me.blvckbytes.bbtweaks.util.TypeNameResolver;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class BBTweaksPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

  private enum Action implements MatchableEnum {
    RELOAD,
    RD_BREAKER,
    ;

    static final EnumMatcher<Action> matcher = new EnumMatcher<>(values());
  }

  private YamlConfiguration configuration;
  private RDBreakTool rdBreakTool;
  private LastLocationStore lastLocationStore;

  private final List<Runnable> configReloadListeners = new ArrayList<>();

  // TODO: Idea - /empty-out [all]

  @Override
  public void onEnable() {
    try {
      Objects.requireNonNull(getCommand("bbtweaks")).setExecutor(this);

      saveDefaultConfig();
      configuration = loadConfiguration();

      new ActionBarSleepMessage(this);

      rdBreakTool = new RDBreakTool(this);

      getServer().getPluginManager().registerEvents(rdBreakTool, this);

      getServer().getPluginManager().registerEvents(new LavaSponge(), this);

      var getUuidCommand = new GetUuidCommand(this);

      Objects.requireNonNull(getCommand("getuuid")).setExecutor(getUuidCommand);

      getServer().getPluginManager().registerEvents(getUuidCommand, this);

      lastLocationStore = new LastLocationStore(this);

      Bukkit.getScheduler().runTaskTimerAsynchronously(this, lastLocationStore::save, 20L * 60, 20L * 60);

      var backOverrideCommand = new BackOverrideCommand(this, lastLocationStore);

      Objects.requireNonNull(getCommand("back")).setExecutor(backOverrideCommand);

      getServer().getPluginManager().registerEvents(backOverrideCommand, this);

      new AdditionalRecipes(this);

      var typeNameResolver = TypeNameResolver.load(getLogger());

      var unCraftCommand = new UnCraftCommand(this, typeNameResolver);

      Objects.requireNonNull(getCommand("uncraft")).setExecutor(unCraftCommand);

      var pingCommand = Objects.requireNonNull(getCommand("ping"));
      var pingExecutor = new PingCommand(this, pingCommand);

      getServer().getPluginManager().registerEvents(pingExecutor, this);

      pingCommand.setExecutor(pingExecutor);

      var furnaceLevelDisplay = new FurnaceLevelDisplay(this);

      getServer().getPluginManager().registerEvents(furnaceLevelDisplay, this);

      Bukkit.getScheduler().runTaskLater(this, () -> {
        if (!furnaceLevelDisplay.setUp()) {
          getLogger().severe("Failed trying to set up furnace-level displays; disabling!");
          Bukkit.getPluginManager().disablePlugin(this);
          return;
        }

        getLogger().info("Successfully set up furnace-level displays!");
      }, 10L);
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
  }

  private YamlConfiguration loadConfiguration() {
    var configuration = new YamlConfiguration();

    try {
      configuration.load(new File(getDataFolder(), "config.yml"));
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Could not load the configuration-file", e);
    }

    return configuration;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!sender.hasPermission("bbtweaks.command")) {
      sender.sendMessage(accessConfigValue("chat.noCommandPermission"));
      return true;
    }

    NormalizedConstant<Action> action;

    if (args.length == 0 || (action = Action.matcher.matchFirst(args[0])) == null) {
      sender.sendMessage(
        accessConfigValue("chat.commandUsage")
          .replace("{command_label}", label)
          .replace("{action_list}", String.join(", ", Action.matcher.createCompletions(null)))
      );
      return true;
    }

    switch (action.constant) {
      case RELOAD -> {
        configuration = loadConfiguration();
        configReloadListeners.forEach(Runnable::run);
        sender.sendMessage(accessConfigValue("chat.configurationReloaded"));
        return true;
      }

      case RD_BREAKER -> {
        if (!(sender instanceof Player player)) {
          sender.sendMessage("§cThis command is only available to players!");
          return false;
        }

        var heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType().isAir()) {
          sender.sendMessage(accessConfigValue("chat.setRdBreakerNoValidItem"));
          return true;
        }

        rdBreakTool.modifyItemToBecomeRdBreaker(heldItem);

        sender.sendMessage(accessConfigValue("chat.setRdBreakerMetadata"));
        return true;
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
    if (!sender.hasPermission("bbtweaks.command") || args.length != 1)
      return List.of();

    return Action.matcher.createCompletions(args[0]);
  }

  public String accessConfigValue(String path) {
    var value = configuration.getString(path);

    if (value == null)
      return "§cUndefined config-value at " + path;

    return ColorUtil.enableColors(value);
  }

  public YamlConfiguration getConfiguration() {
    return configuration;
  }

  public void registerConfigReloadListener(Runnable handler) {
    configReloadListeners.add(handler);
  }
}
