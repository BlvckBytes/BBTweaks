package me.blvckbytes.bbtweaks.main_command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.google.gson.*;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.infinite_waterbucket.InfiniteWaterbucketListener;
import me.blvckbytes.bbtweaks.rd_breaker.RDBreakerListener;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.logging.Level;

public class MainCommand implements CommandHandler {

  private enum Action implements MatchableEnum {
    RELOAD,
    MARK_RD_BREAKER,
    LWC_EXTEND_BLOCKS,
    MARK_AUTO_PICKUP_CONTAINER,
    PATCH_SIGNS_FROM_FILE,
    MARK_INFINITE_WATERBUCKET,
    ;

    static final EnumMatcher<Action> matcher = new EnumMatcher<>(values());
  }

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final RDBreakerListener rdBreakerListener;
  private final AutoPickupContainerListener autoPickupContainer;
  private final InfiniteWaterbucketListener infiniteWaterbucket;
  private final Plugin plugin;

  public MainCommand(
    JavaPlugin plugin,
    RDBreakerListener rdBreakerListener,
    AutoPickupContainerListener autoPickupContainer,
    InfiniteWaterbucketListener infiniteWaterbucket,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand("bbtweaks"));
    this.config = config;
    this.rdBreakerListener = rdBreakerListener;
    this.autoPickupContainer = autoPickupContainer;
    this.infiniteWaterbucket = infiniteWaterbucket;
    this.plugin = plugin;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return null;
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

        rdBreakerListener.modifyItemToBecomeRdBreaker(heldItem);

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

      // Seeing how that's yet-another internal tool, I won't bother with fancy messages.
      case PATCH_SIGNS_FROM_FILE -> {
        var inputFile = new File(plugin.getDataFolder(), "sign_patch_data.json");

        if (!inputFile.isFile() || inputFile.length() == 0) {
          sender.sendMessage("§cExpected data to be present at " + inputFile);
          return true;
        }

        var gson = new GsonBuilder().create();

        JsonArray patchEntriesArray;

        try (var fileReader = new FileReader(inputFile)) {
          patchEntriesArray = gson.fromJson(fileReader, JsonArray.class);
        } catch (Throwable e) {
          plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to parse JSON-file " + inputFile);
          sender.sendMessage("§cAn error occurred; see console!");
          return true;
        }

        entryLoop:
        for (var patchIndex = 0; patchIndex < patchEntriesArray.size(); ++patchIndex) {
          if (!(patchEntriesArray.get(patchIndex) instanceof JsonObject patchEntry)) {
            plugin.getLogger().log(Level.WARNING, "Expected json-object at index=" + patchIndex + "; skipping");
            continue;
          }

          var x = tryAccessJsonInteger(patchEntry.get("x"));
          var y = tryAccessJsonInteger(patchEntry.get("y"));
          var z = tryAccessJsonInteger(patchEntry.get("z"));
          var worldName = tryAccessJsonString(patchEntry.get("world"));

          if (x == null || y == null || z == null || worldName == null) {
            plugin.getLogger().log(Level.WARNING, "Missing valid coordinates in json-object at index=" + patchIndex + "; skipping");
            continue;
          }

          var world = Bukkit.getWorld(worldName);

          if (world == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown world \"" + worldName + "\" in json-object at index=" + patchIndex + "; skipping");
            continue;
          }

          if (!(world.getBlockAt(x, y, z).getState() instanceof Sign sign)) {
            plugin.getLogger().log(Level.WARNING, "Coordinates do not point at a sign in json-object at index=" + patchIndex + "; skipping");
            continue;
          }

          var expectedLines = tryAccessJsonStringArray(patchEntry.get("expectedLines"));

          if (expectedLines.size() != 4) {
            plugin.getLogger().log(Level.WARNING, "Missing or malformed \"expectedLines\" in json-object at index=" + patchIndex + "; skipping");
            continue;
          }

          for (var lineIndex = 0; lineIndex < expectedLines.size(); ++lineIndex) {
            var expectedLine = expectedLines.get(lineIndex);
            var actualLine = ComponentUtil.asTrimmedText(sign.getSide(Side.FRONT).line(lineIndex));

            if (!(expectedLine.trim().equals(actualLine))) {
              plugin.getLogger().log(Level.WARNING, "Mismatched on expected line " + (lineIndex + 1) + " in json-object at index=" + patchIndex + "; skipping");
              continue entryLoop;
            }
          }

          var replacementLines = tryAccessJsonStringArray(patchEntry.get("replacementLines"));

          if (replacementLines.size() != 4) {
            plugin.getLogger().log(Level.WARNING, "Missing or malformed \"replacementLines\" in json-object at index=" + patchIndex + "; skipping");
            continue;
          }

          var frontSide = sign.getSide(Side.FRONT);

          for (var lineIndex = 0; lineIndex < replacementLines.size(); ++lineIndex)
            frontSide.line(lineIndex, Component.text(replacementLines.get(lineIndex)));

          sign.update(true, false);

          plugin.getLogger().log(Level.INFO, "Patched sign at " + x + " " + y + " " + z + " " + worldName + " (" + (patchIndex + 1) + "/" + patchEntriesArray.size() + ")");
        }
      }

      case MARK_INFINITE_WATERBUCKET -> {
        if (!(sender instanceof Player player)) {
          config.rootSection.mainCommand.playersOnly.sendMessage(sender);
          return true;
        }

        var heldItem = player.getInventory().getItemInMainHand();
        var error = infiniteWaterbucket.modifyItemToBecomeInfiniteWaterBucket(heldItem);

        if (error == null) {
          config.rootSection.mainCommand.setInfiniteWaterbucketSuccess.sendMessage(player);
          return true;
        }

        switch (error) {
          case ALREADY_MARKED -> config.rootSection.mainCommand.setInfiniteWaterbucketAlreadyMarked.sendMessage(player);
          case WRONG_ITEM_TYPE -> config.rootSection.mainCommand.setInfiniteWaterbucketNoValidItem.sendMessage(player);
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

  private static List<String> tryAccessJsonStringArray(JsonElement element) {
    if (!(element instanceof JsonArray array))
      return Collections.emptyList();

    var result = new ArrayList<String>();

    for (var entry : array) {
      var string = tryAccessJsonString(entry);

      if (string == null)
        return Collections.emptyList();

      result.add(string);
    }

    return result;
  }

  private static @Nullable String tryAccessJsonString(JsonElement element) {
    if (!(element instanceof JsonPrimitive primitive))
      return null;

    if (!primitive.isString())
      return null;

    return primitive.getAsString();
  }

  private static @Nullable Integer tryAccessJsonInteger(JsonElement element) {
    if (!(element instanceof JsonPrimitive primitive))
      return null;

    if (!primitive.isNumber())
      return null;

    return primitive.getAsInt();
  }
}
