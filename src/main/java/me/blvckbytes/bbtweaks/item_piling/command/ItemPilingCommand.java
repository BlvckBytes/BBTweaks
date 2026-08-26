package me.blvckbytes.bbtweaks.item_piling.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.item_piling.display.ItemPilingDisplayHandler;
import me.blvckbytes.bbtweaks.item_piling.preferences.ItemPilingPreferencesStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ItemPilingCommand implements CommandHandler {

  private final PluginCommand command;

  private final ItemPilingPreferencesStore preferencesStore;
  private final ItemPilingDisplayHandler displayHandler;
  private final ConfigKeeper<MainSection> config;

  public ItemPilingCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    ItemPilingPreferencesStore preferencesStore,
    ItemPilingDisplayHandler displayHandler
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(ItemPilingCommandSection.INITIAL_NAME));

    this.config = config;
    this.preferencesStore = preferencesStore;
    this.displayHandler = displayHandler;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.itemPiling.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.itemPiling.command.playersOnly.sendMessage(sender);
      return true;
    }

    displayHandler.show(player, preferencesStore.accessPreferences(player));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
    return List.of();
  }
}
