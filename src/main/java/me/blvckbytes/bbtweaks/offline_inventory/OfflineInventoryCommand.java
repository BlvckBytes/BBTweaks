package me.blvckbytes.bbtweaks.offline_inventory;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegrationLoader;
import me.blvckbytes.bbtweaks.integration.nbtapi.NbtApiIntegration;
import me.blvckbytes.bbtweaks.integration.nbtapi.NbtApiIntegrationLoader;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class OfflineInventoryCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final OfflinePlayerRegistry offlinePlayerRegistry;
  private final NbtApiIntegration nbtApiIntegration;
  private final FloodgateIntegration floodgateIntegration;
  private final ConfigKeeper<MainSection> config;

  private final File playerDataFolder;

  public OfflineInventoryCommand(
    JavaPlugin plugin,
    OfflinePlayerRegistry offlinePlayerRegistry,
    NbtApiIntegrationLoader nbtApiIntegrationLoader,
    FloodgateIntegrationLoader floodgateIntegrationLoader,
    ConfigKeeper<MainSection> config
  ) {
    this.offlinePlayerRegistry = offlinePlayerRegistry;
    this.nbtApiIntegration = nbtApiIntegrationLoader.nbtApiIntegration;
    this.floodgateIntegration = floodgateIntegrationLoader.floodgateIntegration;

    this.command = Objects.requireNonNull(plugin.getCommand(OfflineInventoryCommandSection.INITIAL_NAME));
    this.config = config;

    var serverFolder = Bukkit.getServer().getPluginsFolder().getParentFile();

    var worldFolder = new File(serverFolder, "world");

    if (!worldFolder.isDirectory())
      throw new IllegalStateException("Expected folder at " + worldFolder);

    var playersFolder = new File(worldFolder, "players");

    if (!playersFolder.isDirectory())
      throw new IllegalStateException("Expected folder at " + playersFolder);

    playerDataFolder = new File(playersFolder, "data");

    if (!playerDataFolder.isDirectory())
      throw new IllegalStateException("Expected folder at " + playerDataFolder);
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.offlineInventory.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.offlineInventory.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.offlineInventory.noPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<OfflineInventoryType> inventoryType;

    if (args.length != 2 || (inventoryType = OfflineInventoryType.matcher.matchFirst(args[1])) == null) {
      config.rootSection.offlineInventory.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("inventory_types", OfflineInventoryType.matcher.createCompletions(null))
      );

      return true;
    }

    if (!nbtApiIntegration.isAvailable()) {
      config.rootSection.offlineInventory.nbtApiNotAvailable.sendMessage(player);
      return true;
    }

    var playerName = args[0];

    offlinePlayerRegistry.tryResolvePlayerId(playerName, playerId -> {
      var environment = new InterpretationEnvironment()
        .withVariable("name", playerName)
        .withVariable("uuid", playerId)
        .withVariable("inventory_type", inventoryType.getNormalizedName());

      if (playerId == null) {
        config.rootSection.offlineInventory.invalidUsername.sendMessage(player, environment);
        return;
      }

      var playerDataFile = new File(playerDataFolder, playerId + ".dat");

      if (!playerDataFile.isFile()) {
        config.rootSection.offlineInventory.hasNotPlayedBefore.sendMessage(player, environment);
        return;
      }

      nbtApiIntegration.tryLoadOfflineInventory(playerDataFile, offlineInventory -> {
        if (offlineInventory == null) {
          config.rootSection.offlineInventory.failedToLoadFile.sendMessage(player, environment);
          return;
        }

        var inventoryHolder = new ViewInventoryHolder();

        Inventory viewInventory;

        switch (inventoryType.constant) {
          case PLAYER_INVENTORY -> {
            viewInventory = Bukkit.createInventory(
              inventoryHolder, 9 * 5,
              config.rootSection.offlineInventory.playerInventoryTitle
                .interpret(SlotType.INVENTORY_TITLE, environment)
                .getFirst()
            );

            viewInventory.setItem(0, itemOrPlaceholder(player, offlineInventory.head(), "head"));
            viewInventory.setItem(1, itemOrPlaceholder(player, offlineInventory.chest(), "chest"));
            viewInventory.setItem(2, itemOrPlaceholder(player, offlineInventory.legs(), "legs"));
            viewInventory.setItem(3, itemOrPlaceholder(player, offlineInventory.feet(), "feet"));
            viewInventory.setItem(4, itemOrPlaceholder(player, offlineInventory.offHand(), "off-hand"));

            for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
              if (slotIndex >= offlineInventory.inventoryContents().length)
                break;

              var item = offlineInventory.inventoryContents()[slotIndex];

              viewInventory.setItem(9 + slotIndex, itemOrPlaceholder(player, item, "slot-" + (slotIndex + 1)));
            }
          }

          case ENDER_CHEST -> {
            viewInventory = Bukkit.createInventory(
              inventoryHolder, 9 * 3,
              config.rootSection.offlineInventory.playerEnderChestTitle
                .interpret(SlotType.INVENTORY_TITLE, environment)
                .getFirst()
            );

            for (var slotIndex = 0; slotIndex < 9 * 3; ++slotIndex) {
              if (slotIndex >= offlineInventory.enderChestContents().length)
                break;

              var item = offlineInventory.enderChestContents()[slotIndex];

              viewInventory.setItem(slotIndex, itemOrPlaceholder(player, item, "slot-" + (slotIndex + 1)));
            }
          }

          default -> throw new IllegalStateException("Unaccounted-for inventory-type: " + inventoryType.constant);
        }

        inventoryHolder.setInventory(viewInventory);

        player.openInventory(viewInventory);

        config.rootSection.offlineInventory.openingInventoryView.sendMessage(player, environment);
      });
    });

    return false;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player) || !command.testPermission(sender))
      return List.of();

    if (args.length == 1) {
      return offlinePlayerRegistry.streamKnownNames()
        .filter(it -> StringUtils.startsWithIgnoreCase(it, args[0]))
        .toList();
    }

    if (args.length == 2)
      return OfflineInventoryType.matcher.createCompletions(args[1]);

    return List.of();
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var topInventory = player.getOpenInventory().getTopInventory();

    if (!(topInventory.getHolder() instanceof ViewInventoryHolder))
      return;

    event.setCancelled(true);

    config.rootSection.offlineInventory.cannotModifyView.sendMessage(player);
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var topInventory = player.getOpenInventory().getTopInventory();

    if (!(topInventory.getHolder() instanceof ViewInventoryHolder))
      return;

    event.setCancelled(true);

    config.rootSection.offlineInventory.cannotModifyView.sendMessage(player);
  }

  private ItemStack itemOrPlaceholder(Player player, ItemStack item, String slotId) {
    if (!item.getType().isAir())
      return item;

    return config.rootSection.offlineInventory.airPlaceholderItem.build(
      new InterpretationEnvironment()
        .withVariable("is_floodgate", floodgateIntegration.isFloodgatePlayer(player))
        .withVariable("slot_id", slotId)
    );
  }
}
