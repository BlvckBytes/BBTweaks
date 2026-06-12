package me.blvckbytes.bbtweaks.auto_tool;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.multi_break.DamageableHotbarItem;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AutoToolCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final NamespacedKey keyEnabled;
  private final Object2BooleanMap<UUID> enabledStateByPlayerId;

  public AutoToolCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(AutoToolCommandSection.INITIAL_NAME));
    this.config = config;
    this.keyEnabled = new NamespacedKey(plugin, "auto-tool-enabled");
    this.enabledStateByPlayerId = new Object2BooleanOpenHashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (!command.testPermission(player)) {
      config.rootSection.autoTool.noPermission.sendMessage(player);
      return true;
    }

    var playerId = player.getUniqueId();
    var newEnableState = !enabledStateByPlayerId.getOrDefault(playerId, false);

    enabledStateByPlayerId.put(playerId, newEnableState);
    player.getPersistentDataContainer().set(keyEnabled, PersistentDataType.BOOLEAN, newEnableState);

    if (newEnableState) {
      config.rootSection.autoTool.nowEnabled.sendMessage(player);
      return true;
    }

    config.rootSection.autoTool.nowDisabled.sendMessage(player);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  public boolean isEnabled(Player player) {
    return enabledStateByPlayerId.getOrDefault(player.getUniqueId(), false);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    var enabledState = false;

    if (player.hasPermission("bbtweaks.autotool")) {
      var storedValue = player.getPersistentDataContainer().get(keyEnabled, PersistentDataType.BOOLEAN);
      enabledState = storedValue != null && storedValue;
    }

    enabledStateByPlayerId.put(player.getUniqueId(), enabledState);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    enabledStateByPlayerId.removeBoolean(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onBlockDamage(BlockDamageEvent event) {
    if (!isEnabled(event.getPlayer()))
      return;

    var playerInventory = event.getPlayer().getInventory();
    var block = event.getBlock();

    if (DamageableHotbarItem.isRightToolForBlock(playerInventory.getItemInMainHand(), block))
      return;

    var heldSlotIndex = playerInventory.getHeldItemSlot();

    for (var slotIndex = 0; slotIndex < 9; ++slotIndex) {
      if (slotIndex == heldSlotIndex)
        continue;

      var currentItem = playerInventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir())
        continue;

      if (!DamageableHotbarItem.isRightToolForBlock(currentItem, block))
        continue;

      playerInventory.setHeldItemSlot(slotIndex);
      break;
    }
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.autoTool.command;
  }
}
