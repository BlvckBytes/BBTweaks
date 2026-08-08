package me.blvckbytes.bbtweaks.emotions.settings_command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.emotions.settings_display.EmotionSettingsDisplayHandler;
import me.blvckbytes.bbtweaks.emotions.user_profile.EmotionUserProfileStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class EmotionSettingsCommand implements CommandHandler {

  private final PluginCommand command;

  private final EmotionUserProfileStore profileStore;
  private final EmotionSettingsDisplayHandler settingsDisplayHandler;
  private final ConfigKeeper<MainSection> config;

  public EmotionSettingsCommand(
    JavaPlugin plugin,
    EmotionUserProfileStore profileStore,
    EmotionSettingsDisplayHandler settingsDisplayHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(EmotionSettingsCommandSection.INITIAL_NAME));

    this.profileStore = profileStore;
    this.settingsDisplayHandler = settingsDisplayHandler;
    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.emotion.settingsCommand;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.emotion.playersOnly.sendMessage(sender);
      return true;
    }

    settingsDisplayHandler.show(player, profileStore.accessUserProfile(player));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }
}
