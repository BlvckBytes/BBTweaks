package me.blvckbytes.bbtweaks.main_command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.RDBreakTool;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

public class MainCommand implements CommandExecutor, TabExecutor {

  private enum Action implements MatchableEnum {
    RELOAD,
    MARK_RD_BREAKER,
    LWC_EXTEND_BLOCKS,
    MARK_AUTO_PICKUP_CONTAINER,
    ;

    static final EnumMatcher<Action> matcher = new EnumMatcher<>(values());
  }

  private final ConfigKeeper<MainSection> config;
  private final RDBreakTool rdBreakTool;
  private final AutoPickupContainerListener autoPickupContainer;
  private final Plugin plugin;

  public MainCommand(
    ConfigKeeper<MainSection> config,
    RDBreakTool rdBreakTool,
    AutoPickupContainerListener autoPickupContainer,
    Plugin plugin
  ) {
    this.config = config;
    this.rdBreakTool = rdBreakTool;
    this.autoPickupContainer = autoPickupContainer;
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("bbtweaks.command")) {
      config.rootSection.mainCommand.noPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<Action> action;

    if (args.length == 0 || (action = Action.matcher.matchFirst(args[0])) == null) {
      config.rootSection.mainCommand.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("command_label", label)
          .withVariable("actions", Action.matcher.createCompletions(null))
      );

      return true;
    }

    switch (action.constant) {
      case RELOAD -> {
        try {
          config.reload();
          config.rootSection.mainCommand.configReloadSuccess.sendMessage(sender);
        } catch (Exception e) {
          config.rootSection.mainCommand.configReloadError.sendMessage(sender);
          plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to reload the config", e);
        }
        return true;
      }

      case MARK_RD_BREAKER -> {
        if (!(sender instanceof Player player)) {
          config.rootSection.mainCommand.playersOnly.sendMessage(sender);
          return true;
        }

        var heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType().isAir()) {
          config.rootSection.mainCommand.setRdBreakerNoValidItem.sendMessage(sender);
          return true;
        }

        rdBreakTool.modifyItemToBecomeRdBreaker(heldItem);

        config.rootSection.mainCommand.setRdBreakerMetadata.sendMessage(sender);
        return true;
      }

      case LWC_EXTEND_BLOCKS -> {
        var configFile = locateLWCCoreConfig();

        if (configFile == null) {
          config.rootSection.mainCommand.lwcExtendBlocks.configNotFound.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("file", new File(new File(plugin.getDataFolder(), "LWC"), "core.yml").getAbsolutePath())
          );

          return true;
        }

        var configYaml = YamlConfiguration.loadConfiguration(configFile);

        if (!configYaml.isSet("protections")) {
          config.rootSection.mainCommand.lwcExtendBlocks.protectionsSectionNotFound.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("file", configFile.getAbsolutePath())
              .withVariable("missing_section", "protections")
          );

          return true;
        }

        var blocksSection = configYaml.getConfigurationSection("protections.blocks");
        var existingMaterials = new HashSet<Material>();

        if (blocksSection == null)
          blocksSection = configYaml.createSection("protections.blocks");

        for (var type : blocksSection.getKeys(false)) {
          try {
            existingMaterials.add(Material.valueOf(type.toUpperCase()));
          } catch (Throwable ignored) {}
        }

        var extendedKeyCount = 0;

        for (var material : config.rootSection.mainCommand.lwcExtendBlocks._materialsToExtend) {
          if (existingMaterials.contains(material))
            continue;

          var type = material.name().toLowerCase();

          blocksSection.set(type + ".enabled", true);

          ++extendedKeyCount;
        }

        var outputFile = new File(plugin.getDataFolder(), "lwc_core.yml");

        try {
          configYaml.options().indent(4);
          configYaml.save(outputFile);
        } catch (Throwable e) {
          plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to write to " + outputFile, e);

          config.rootSection.mainCommand.lwcExtendBlocks.couldNotWriteTemplate.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("file", outputFile.getAbsolutePath())
          );

          return true;
        }

        config.rootSection.mainCommand.lwcExtendBlocks.templateWritten.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("file", outputFile.getAbsolutePath())
            .withVariable("extended_count", extendedKeyCount)
            .withVariable("tag_member_count", config.rootSection.mainCommand.lwcExtendBlocks._materialsToExtend.size())
            .withVariable("new_total_count", existingMaterials.size() + extendedKeyCount)
        );

        return true;
      }

      case MARK_AUTO_PICKUP_CONTAINER -> {
        if (!(sender instanceof Player player)) {
          config.rootSection.mainCommand.playersOnly.sendMessage(sender);
          return true;
        }

        var heldItem = player.getInventory().getItemInMainHand();
        var error = autoPickupContainer.modifyItemToBecomeAutoPickupContainer(heldItem);

        if (error == null) {
          config.rootSection.mainCommand.setAutoPickupContainerSuccess.sendMessage(player);
          return true;
        }

        switch (error) {
          case ALREADY_MARKED -> config.rootSection.mainCommand.setAutoPickupContainerAlreadyMarked.sendMessage(player);
          case WRONG_ITEM_TYPE -> config.rootSection.mainCommand.setAutoPickupContainerNoValidItem.sendMessage(player);
        }

        return true;
      }
    }

    return true;
  }

  private @Nullable File locateLWCCoreConfig() {
    var pluginsFolder = plugin.getDataFolder().getParentFile();

    if (!pluginsFolder.isDirectory())
      return null;

    var lwcFolder = new File(pluginsFolder, "LWC");

    if (!lwcFolder.isDirectory())
      return null;

    var coreConfig = new File(lwcFolder, "core.yml");

    if (!coreConfig.isFile())
      return null;

    return coreConfig;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("bbtweaks.command") || args.length != 1)
      return List.of();

    return Action.matcher.createCompletions(args[0]);
  }
}
