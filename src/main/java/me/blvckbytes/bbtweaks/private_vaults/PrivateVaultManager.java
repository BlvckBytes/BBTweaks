package me.blvckbytes.bbtweaks.private_vaults;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrivateVaultManager {

  private static final long OFFLINE_CACHE_TIMEOUT_MS = 1000 * 30;
  private static final long WRITE_PERIOD_T = 20 * 15;

  private final File vaultsDirectory;
  private final Logger logger;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, PrivateVault> vaultByOwnerId;

  public PrivateVaultManager(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.vaultsDirectory = new File(plugin.getDataFolder(), "private_vaults");

    if (!vaultsDirectory.isDirectory()) {
      if (vaultsDirectory.exists())
        throw new IllegalStateException("Expected directory at " + vaultsDirectory);

      if (!vaultsDirectory.mkdirs())
        throw new IllegalStateException("Could not create directory " + vaultsDirectory);
    }

    this.logger = plugin.getLogger();
    this.config = config;

    this.vaultByOwnerId = new HashMap<>();

    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::storeAndCleanUpVaults, WRITE_PERIOD_T, WRITE_PERIOD_T);
  }

  public void onShutdown() {
    storeAndCleanUpVaults();
  }

  private void storeAndCleanUpVaults() {
    var now = System.currentTimeMillis();

    synchronized (vaultByOwnerId) {
      for (var iterator = vaultByOwnerId.values().iterator(); iterator.hasNext();) {
        var vault = iterator.next();

        if (!vault.owner.isOnline() && now - vault.getLastAccessStamp() >= OFFLINE_CACHE_TIMEOUT_MS)
          iterator.remove();

        var items = vault.syncAndGetItems();
        var vaultBytes = items.toBytes();

        if (items.doBytesEqualCurrentlyWrittenData(vaultBytes))
          continue;

        var vaultFile = getVaultFile(vault.owner);

        try (var outputStream = new FileOutputStream(vaultFile)) {
          outputStream.write(vaultBytes);
          items.updateCurrentlyWrittenData(vaultBytes);
        } catch (Throwable e) {
          logger.log(Level.SEVERE, "An error occurred while trying to write the vault of " + vault.owner.getUniqueId() + " (" + vault.owner.getName() + ")", e);
        }
      }
    }
  }

  private VaultAccessResult handleOpeningVault(PrivateVault vault, Player viewer) {
    if (viewer.getUniqueId().equals(vault.owner.getUniqueId())) {
      var excessCount = vault.items.handOutExcessItemsAndGetStackCount(viewer);

      // TODO: Config-message
      if (excessCount > 0)
        viewer.sendMessage("§aHanded out " + excessCount + " excess stacks which were cut off as the vault shrunk!");
    }

    if (vault.inventory == null)
      return VaultAccessResult.OWNER_CANNOT_ACCESS_ANY_ROWS;

    viewer.openInventory(vault.inventory);

    return VaultAccessResult.SUCCESS;
  }

  public VaultAccessResult tryOpenVaultInventory(Player viewer, OfflinePlayer owner) {
    PrivateVault vault;

    synchronized (vaultByOwnerId) {
      vault = vaultByOwnerId.get(owner.getUniqueId());

      if (vault != null) {
        vault.touchLastAccessStamp();
        return handleOpeningVault(vault, viewer);
      }
    }

    var items = tryLoadItemsFromFile(owner);

    if (items == null) {
      if (!owner.isOnline())
        return VaultAccessResult.NOT_EXISTING_AND_OWNER_NOT_ONLINE;

      var numberOfRows = determineNumberOfRows(owner.getPlayer());

      if (numberOfRows <= 0)
        return VaultAccessResult.OWNER_CANNOT_ACCESS_ANY_ROWS;

      items = ItemsAndRows.empty(numberOfRows);
    }

    else if (owner.isOnline())
      items.lastKnownRows = determineNumberOfRows(owner.getPlayer());

    var inventoryTitle = config.rootSection.privateVaults.inventoryTitle.interpret(
      SlotType.INVENTORY_TITLE,
      new InterpretationEnvironment()
        .withVariable("owner", owner.getName())
    ).get(0);

    vault = PrivateVault.loadFromItems(owner, items, inventoryTitle);

    synchronized (vaultByOwnerId) {
      vaultByOwnerId.put(owner.getUniqueId(), vault);
    }

    return handleOpeningVault(vault, viewer);
  }

  private int determineNumberOfRows(Player player) {
    var maxAccessibleSize = 0;

    for (var entry : config.rootSection.privateVaults.rowCounts.entrySet()) {
      if (player.hasPermission("bbtweaks.private-vaults.size." + entry.getKey()))
        maxAccessibleSize = Math.max(maxAccessibleSize, entry.getValue());
    }

    return maxAccessibleSize;
  }

  private File getVaultFile(OfflinePlayer owner) {
    return new File(vaultsDirectory, owner.getUniqueId() + ".bin");
  }

  private @Nullable ItemsAndRows tryLoadItemsFromFile(OfflinePlayer owner) {
    var vaultFile = getVaultFile(owner);

    if (vaultFile.length() <= 0)
      return null;

    try (var inputStream = new FileInputStream(vaultFile)) {
      return ItemsAndRows.fromBytes(inputStream.readAllBytes());
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to load the vault of " + owner.getUniqueId() + " (" + owner.getName() + ")", e);
      return null;
    }
  }
}
