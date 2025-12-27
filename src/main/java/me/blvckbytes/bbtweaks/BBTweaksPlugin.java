package me.blvckbytes.bbtweaks;

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

  @Override
  public void onEnable() {
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

    return enableColors(value);
  }

  public YamlConfiguration getConfiguration() {
    return configuration;
  }

  public void registerConfigReloadListener(Runnable handler) {
    configReloadListeners.add(handler);
  }

  private static boolean isColorChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= '0' && c <= '9') || (c >= 'k' && c <= 'o') || c == 'r';
  }

  private static boolean isHexChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
  }

  private static String enableColors(String input) {
    var inputLength = input.length();
    var result = new StringBuilder(inputLength);

    for (var charIndex = 0; charIndex < inputLength; ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var remainingChars = inputLength - 1 - charIndex;

      if (currentChar != '&' || remainingChars == 0) {
        result.append(currentChar);
        continue;
      }

      var nextChar = input.charAt(++charIndex);

      // Possible hex-sequence of format &#RRGGBB
      if (nextChar == '#' && remainingChars >= 6 + 1) {
        var r1 = input.charAt(charIndex + 1);
        var r2 = input.charAt(charIndex + 2);
        var g1 = input.charAt(charIndex + 3);
        var g2 = input.charAt(charIndex + 4);
        var b1 = input.charAt(charIndex + 5);
        var b2 = input.charAt(charIndex + 6);

        if (
          isHexChar(r1) && isHexChar(r2)
            && isHexChar(g1) && isHexChar(g2)
            && isHexChar(b1) && isHexChar(b2)
        ) {
          result
            .append('§').append('x')
            .append('§').append(r1)
            .append('§').append(r2)
            .append('§').append(g1)
            .append('§').append(g2)
            .append('§').append(b1)
            .append('§').append(b2);

          charIndex += 6;
          continue;
        }
      }

      // Vanilla color-sequence
      if (isColorChar(nextChar)) {
        result.append('§').append(nextChar);
        continue;
      }

      // Wasn't a color-sequence, store as-is
      result.append(currentChar).append(nextChar);
    }

    return result.toString();
  }
}
