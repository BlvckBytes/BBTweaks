package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.util.InputView;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.common.InstanceSession;
import me.blvckbytes.bbtweaks.mechanic.common.OffsetSelectingMechanic;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class HiddenSwitchMechanic extends OffsetSelectingMechanic<HiddenSwitchInstance> implements Listener {

  private static final int DENIED_MESSAGE_LINE_ID = 0;
  private static final int OFFSET_VALUES_LINE_ID = 2;
  private static final int GRANTED_MESSAGE_LINE_ID = 3;

  private final CacheByPosition<HiddenSwitchInstance> instanceByInteractionPosition;
  private final NamespacedKey keyItemsKey;
  private final NamespacedKey passwordKey;
  private final NamespacedKey allowKeyOrPasswordKey;

  private final Map<UUID, HiddenSwitchInstance> openKeysInstanceByPlayerId;
  private final Map<UUID, InstanceSession<HiddenSwitchInstance>> passwordPromptByPlayerId;

  private final PluginCommand passwordCommand;

  public HiddenSwitchMechanic(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config, OFFSET_VALUES_LINE_ID, () -> config.rootSection.mechanic.hiddenSwitch);

    Objects.requireNonNull(plugin.getCommand("hiddenswitch")).setExecutor(new HiddenSwitchCommand(this, config));

    this.passwordCommand = Objects.requireNonNull(plugin.getCommand(PasswordCommandSection.INITIAL_NAME));
    passwordCommand.setExecutor(new PasswordCommand(this, config));

    var commandUpdater = new CommandUpdater(plugin);

    Runnable updateCommands = () -> {
      config.rootSection.mechanic.hiddenSwitch.passwordCommand.apply(passwordCommand, commandUpdater);
    };

    updateCommands.run();
    config.registerReloadListener(updateCommands);

    this.instanceByInteractionPosition = new CacheByPosition<>();

    this.keyItemsKey = new NamespacedKey(plugin, "key-items");
    this.passwordKey = new NamespacedKey(plugin, "password");
    this.allowKeyOrPasswordKey = new NamespacedKey(plugin, "key-or-password");

    this.openKeysInstanceByPlayerId = new HashMap<>();
    this.passwordPromptByPlayerId = new HashMap<>();
  }

  private Block getLookedAtSignBlock(Player player) {
    //noinspection UnstableApiUsage
    var rayTraceResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      5.0,
      FluidCollisionMode.NEVER,
      false,
      block -> Tag.WALL_SIGNS.isTagged(block.getType())
    );

    if (rayTraceResult == null || rayTraceResult.getHitBlock() == null)
      return player.getEyeLocation().getBlock();

    return rayTraceResult.getHitBlock();
  }

  public @Nullable HiddenSwitchInstance getLookedAtInstance(Player player) {
    var block = getLookedAtSignBlock(player);

    return instanceBySignPosition.get(block.getWorld(), block.getX(), block.getY(), block.getZ());
  }

  public boolean toggleAllowKeyOrPasswordAndGetNewValue(HiddenSwitchInstance instance) {
    var sign = instance.getSign();
    var pdc = sign.getPersistentDataContainer();

    var priorValue = pdc.get(allowKeyOrPasswordKey, PersistentDataType.BOOLEAN);
    var priorFlag = priorValue != null && priorValue;
    var newFlag = !priorFlag;

    pdc.set(allowKeyOrPasswordKey, PersistentDataType.BOOLEAN, newFlag);

    sign.update(true, false);

    reloadInstanceBySign(instance.getSign());

    return newFlag;
  }

  public @Nullable String updatePasswordAndGetPriorValue(HiddenSwitchInstance instance, @Nullable String password) {
    var sign = instance.getSign();
    var pdc = sign.getPersistentDataContainer();

    var priorValue = pdc.get(passwordKey, PersistentDataType.STRING);

    if (password == null)
      pdc.remove(passwordKey);
    else
      pdc.set(passwordKey, PersistentDataType.STRING, password);

    sign.update(true, false);

    reloadInstanceBySign(instance.getSign());

    return priorValue;
  }

  public PasswordResult onPasswordInput(Player player, String password) {
    var session = passwordPromptByPlayerId.get(player.getUniqueId());

    if (session == null)
      return PasswordResult.NO_ACTIVE_PROMPT;

    if (!password.equals(session.instance().password))
      return PasswordResult.WRONG_PASSWORD;

    session.instance().interactAndSendMessage(player, getCurrentTime());
    passwordPromptByPlayerId.remove(player.getUniqueId());

    return PasswordResult.SUCCESS;
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, HiddenSwitchInstance instance, boolean wasLeftClick) {
    if (!player.isSneaking())
      return false;

    var sign = instance.getSign();

    if (!canEditSign(player, sign)) {
      config.rootSection.mechanic.hiddenSwitch.cannotEditSign.sendMessage(player);
      return true;
    }

    if (!wasLeftClick) {
      if (!instance.keysInventory.getViewers().isEmpty()) {
        config.rootSection.mechanic.hiddenSwitch.anotherIsEditing.sendMessage(player);
        return true;
      }

      player.openInventory(instance.keysInventory);
      openKeysInstanceByPlayerId.put(player.getUniqueId(), instance);

      config.rootSection.mechanic.hiddenSwitch.keysInventoryOpening.sendMessage(
        player,
        getSignEnvironment(sign).withVariable("key_count", instance.getKeyCount())
      );

      return true;
    }

    initiateOffsetSelecting(player, instance);
    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("HiddenSwitch");
  }

  @Override
  public @Nullable HiddenSwitchInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.hidden-switch")) {
      config.rootSection.mechanic.hiddenSwitch.noPermission.sendMessage(creator);
      return null;
    }

    var environment = getSignEnvironment(sign);

    var inventoryTitle = config.rootSection.mechanic.hiddenSwitch.keysInventoryTitle
      .interpret(SlotType.INVENTORY_TITLE, environment).get(0);

    var keysInventory = Bukkit.createInventory(null, 9 * 6, inventoryTitle);

    loadKeyItemsFromPDC(sign, keysInventory);

    var grantedMessage = tryParseMarkup(SignUtil.getPlainTextLine(sign, GRANTED_MESSAGE_LINE_ID), error -> {
      if (creator != null)
        config.rootSection.mechanic.hiddenSwitch.malformedGrantedMessage.sendMessage(creator, addErrorVariables(environment, error));
    });

    var deniedMessage = tryParseMarkup(SignUtil.getPlainTextLine(sign, DENIED_MESSAGE_LINE_ID), error -> {
      if (creator != null)
        config.rootSection.mechanic.hiddenSwitch.malformedDeniedMessage.sendMessage(creator, addErrorVariables(environment, error));
    });

    var pdc = sign.getPersistentDataContainer();
    var password = pdc.get(passwordKey, PersistentDataType.STRING);
    var allowKeyOrPassword = pdc.get(allowKeyOrPasswordKey, PersistentDataType.BOOLEAN);

    var instance = validateOffsetsAndMakeInstance(creator, sign, (newSign, offsets) -> {
      if (sign != newSign)
        newSign.update(true, false);

      return new HiddenSwitchInstance(
        newSign, keysInventory,
        offsets,
        grantedMessage, deniedMessage,
        password, allowKeyOrPassword != null && allowKeyOrPassword,
        config
      );
    });

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    instanceByInteractionPosition.put(
      instance.interactionPosition.getWorld(),
      instance.interactionPosition.getBlockX(),
      instance.interactionPosition.getBlockY(),
      instance.interactionPosition.getBlockZ(),
      instance
    );

    if (creator != null)
      config.rootSection.mechanic.hiddenSwitch.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  private InterpretationEnvironment addErrorVariables(InterpretationEnvironment environment, MarkupParseException e) {
    return environment
      .withVariable("error_message", e.getErrorMessage())
      .withVariable("error_position", e.getErrorPosition() + 1);
  }

  private @Nullable ComponentMarkup tryParseMarkup(String markup, Consumer<MarkupParseException> errorHandler) {
    if (markup.isBlank())
      return null;

    try {
      var ast = MarkupParser.parse(InputView.of(markup), BuiltInTagRegistry.INSTANCE);
      return new ComponentMarkup(ast, new InterpretationEnvironment(), (view, position, message, e) -> {});
    } catch (MarkupParseException e) {
      errorHandler.accept(e);
    }

    return null;
  }

  @Override
  public @Nullable HiddenSwitchInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance == null)
      return null;

    instanceByInteractionPosition.invalidate(
      instance.interactionPosition.getWorld(),
      instance.interactionPosition.getBlockX(),
      instance.interactionPosition.getBlockY(),
      instance.interactionPosition.getBlockZ()
    );

    // Currently, we do not differentiate between destroy/unload and in general, if a sign is edited
    // and thereby causes an unload, we would miss that, which is quite counter-intuitive for players.
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!sign.getWorld().isChunkLoaded(sign.getX() >> 4, sign.getZ() >> 4))
        return;

      if (instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()) != null)
        return;

      if (sign.getBlock().getState() instanceof Sign existingSign) {
        var pdc = existingSign.getPersistentDataContainer();

        if (pdc.has(keyItemsKey)) {
          pdc.remove(keyItemsKey);
          existingSign.update(true, false);
        }
      }

      var signLocation = sign.getLocation();

      for (var item : instance.keysInventory.getContents()) {
        if (isValidItem(item))
          sign.getWorld().dropItemNaturally(signLocation, item);
      }

      instance.keysInventory.clear();

      // Avoid concurrent modification while closing inventories
      new ArrayList<>(instance.keysInventory.getViewers()).forEach(viewer -> {
        config.rootSection.mechanic.hiddenSwitch.destroyedWhileInInventory.sendMessage(viewer, getSignEnvironment(sign));
        viewer.closeInventory();
      });
    }, 2L);

    return instance;
  }

  @Override
  public void tick(int time) {
    super.tick(time);

    InstanceSession.handleSessionTimeouts(
      passwordPromptByPlayerId, time,
      config.rootSection.mechanic.hiddenSwitch.passwordPromptTimeoutSeconds,
      (session, timeoutSeconds) -> {
        config.rootSection.mechanic.hiddenSwitch.passwordPromptTimeout.sendMessage(
          session.player(),
          getSignEnvironment(session.instance().getSign())
            .withVariable("timeout", timeoutSeconds)
        );
      }
    );

    handleOffsetSelectingSessionTimeouts(time);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    if (handleInteraction(event.getPlayer(), clickedBlock.getLocation()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (handleInteraction(event.getPlayer(), event.getRightClicked().getLocation()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onHangingBreak(HangingBreakEvent event) {
    if (handleInteraction(null, event.getEntity().getLocation()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onDamageByEntity(EntityDamageByEntityEvent event) {
    if (handleInteraction(event.getDamager() instanceof Player player ? player : null, event.getEntity().getLocation()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onDamage(EntityDamageEvent event) {
    if (handleInteraction(null, event.getEntity().getLocation()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onInvClose(InventoryCloseEvent event) {
    var player = event.getPlayer();

    var instance = openKeysInstanceByPlayerId.get(player.getUniqueId());

    if (instance == null)
      return;

    if (!player.getOpenInventory().getTopInventory().equals(instance.keysInventory))
      return;

    openKeysInstanceByPlayerId.remove(player.getUniqueId());

    var sign = instance.getSign();

    if (instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()) == null)
      return;

    storeKeyItemsToPDC(sign, instance.keysInventory);
    sign.update(true, false);

    reloadInstanceBySign(sign);

    config.rootSection.mechanic.hiddenSwitch.keysInventoryClosing.sendMessage(
      player,
      getSignEnvironment(sign)
        .withVariable("key_count", instance.getKeyCount())
    );
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var player = event.getPlayer();
    openKeysInstanceByPlayerId.remove(player.getUniqueId());
    handleOffsetSelectingQuitEvent(player);
  }

  private boolean handleInteraction(@Nullable Player player, Location location) {
    var instance = instanceByInteractionPosition.get(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

    if (player != null && instance != null && shouldDebounceInteraction(player, instance))
      return true;

    if (player != null && handleOffsetSelecting(player, location))
      return true;

    if (instance == null)
      return false;

    if (player == null)
      return true;

    if (instance.getKeyCount() == 0 && instance.password == null) {
      instance.interactAndSendMessage(player, getCurrentTime());
      return true;
    }

    if (instance.allowKeyOrPassword) {
      if (!instance.testKeyForFailure(player)) {
        instance.interactAndSendMessage(player, getCurrentTime());
        return true;
      }

      promptForPassword(player, instance);
      return true;
    }

    if (instance.testKeyForFailureAndSendMessage(player))
      return true;

    if (instance.password == null) {
      instance.interactAndSendMessage(player, getCurrentTime());
      return true;
    }

    promptForPassword(player, instance);
    return true;
  }

  private void promptForPassword(Player player, HiddenSwitchInstance instance) {
    passwordPromptByPlayerId.put(player.getUniqueId(), new InstanceSession<>(player, instance, getCurrentTime()));

    var labels = new ArrayList<>(passwordCommand.getAliases());
    labels.add(passwordCommand.getName());

    config.rootSection.mechanic.hiddenSwitch.passwordPrompt.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("command", passwordCommand.getName())
        .withVariable("aliases", passwordCommand.getAliases())
        .withVariable("labels", labels)
        .withVariable("timeout", config.rootSection.mechanic.hiddenSwitch.passwordPromptTimeoutSeconds)
    );
  }

  private void storeKeyItemsToPDC(Sign sign, Inventory keysInventory) {
    byte[] keysBytes;

    try {
      keysBytes = ItemStack.serializeItemsAsBytes(keysInventory.getContents());
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Could not serialize items of hidden-switch at " + sign.getLocation(), e);
      return;
    }

    var keysString = new String(Base64.getEncoder().encode(keysBytes));

    sign.getPersistentDataContainer().set(keyItemsKey, PersistentDataType.STRING, keysString);
  }

  private void loadKeyItemsFromPDC(Sign sign, Inventory keysInventory) {
    var keysString = sign.getPersistentDataContainer().get(keyItemsKey, PersistentDataType.STRING);

    if (keysString == null)
      return;

    byte[] keysBytes;

    try {
      keysBytes = Base64.getDecoder().decode(keysString);
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Could not deserialize base64 keys-string of hidden-switch at " + sign.getLocation(), e);
      return;
    }

    ItemStack[] keys;

    try {
      keys = ItemStack.deserializeItemsFromBytes(keysBytes);
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Could not deserialize items of hidden-switch at " + sign.getLocation(), e);
      return;
    }

    for (var key : keys) {
      if (!isValidItem(key))
        continue;

      keysInventory.addItem(key);
    }
  }

  private boolean isValidItem(ItemStack item) {
    return item != null && item.getAmount() > 0 && !item.getType().isAir();
  }
}
