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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrivateVaultManager {

  private static final long OFFLINE_CACHE_TIMEOUT_MS = 1000 * 30;
  private static final long WRITE_PERIOD_T = 20 * 15;
  private static final long SIZE_UPDATE_PERIOD_T = 20;

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
    Bukkit.getScheduler().runTaskTimer(plugin, this::updateVaultSizesToPermissions, SIZE_UPDATE_PERIOD_T, SIZE_UPDATE_PERIOD_T);
  }

  public void onShutdown() {
    storeAndCleanUpVaults();
  }

  private void updateVaultSizesToPermissions() {
    synchronized (vaultByOwnerId) {
      for (var vault : vaultByOwnerId.values()) {
        if (vault.owner.isOnline())
          vault.updateNumberOfRows(determineNumberOfRows(vault.owner.getPlayer()), config);
      }
    }
  }

  private void storeAndCleanUpVaults() {
    var now = System.currentTimeMillis();

    synchronized (vaultByOwnerId) {
      for (var iterator = vaultByOwnerId.values().iterator(); iterator.hasNext();) {
        var vault = iterator.next();

        if (!vault.owner.isOnline() && now - vault.getLastAccessStamp() >= OFFLINE_CACHE_TIMEOUT_MS)
          iterator.remove();

        var items = vault.syncAndGetItems();
        var vaultYamlString = items.toYamlString();

        if (items.doesEqualCurrentlyWrittenYamlString(vaultYamlString))
          continue;

        var vaultFile = getVaultFile(vault.owner);

        try (var outputStream = new FileOutputStream(vaultFile)) {
          outputStream.write(vaultYamlString.getBytes(StandardCharsets.UTF_8));
          items.updateCurrentlyWrittenYamlString(vaultYamlString);
        } catch (Throwable e) {
          logger.log(Level.SEVERE, "An error occurred while trying to write the vault of " + vault.owner.getUniqueId() + " (" + vault.owner.getName() + ")", e);
        }
      }
    }
  }

  public VaultAccessResult tryOpenVaultInventory(Player viewer, OfflinePlayer owner) {
    PrivateVault vault;

    synchronized (vaultByOwnerId) {
      vault = vaultByOwnerId.get(owner.getUniqueId());

      if (vault != null) {
        vault.touchLastAccessStamp();

        if (owner.isOnline())
          vault.updateNumberOfRows(determineNumberOfRows(owner.getPlayer()), config);

        return vault.openInventoryIfExistsAndHandOutExcesses(viewer, config);
      }
    }

    var items = tryLoadItemsFromFile(owner);

    if (items == null) {
      if (!owner.isOnline())
        return VaultAccessResult.NOT_EXISTING_AND_OWNER_NOT_ONLINE;

      var numberOfRows = determineNumberOfRows(owner.getPlayer());

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

    return vault.openInventoryIfExistsAndHandOutExcesses(viewer, config);
  }

  private int determineNumberOfRows(Player player) {
    for (var rowCount = 6; rowCount > 0; --rowCount) {
      if (player.hasPermission("bbtweaks.private-vaults.rows." + rowCount))
        return rowCount;
    }

    return 0;
  }

  private File getVaultFile(OfflinePlayer owner) {
    return new File(vaultsDirectory, owner.getUniqueId() + ".yml");
  }

  private @Nullable ItemsAndRows tryLoadItemsFromFile(OfflinePlayer owner) {
    var vaultFile = getVaultFile(owner);

    if (vaultFile.length() <= 0)
      return null;

    try (var inputStream = new FileInputStream(vaultFile)) {
      return ItemsAndRows.fromYamlString(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to load the vault of " + owner.getUniqueId() + " (" + owner.getName() + ")", e);
      return null;
    }
  }
}
