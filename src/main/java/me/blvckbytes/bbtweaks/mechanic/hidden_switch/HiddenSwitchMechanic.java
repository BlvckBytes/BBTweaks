package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.util.InputView;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.SignMechanicManager;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.bbtweaks.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class HiddenSwitchMechanic extends BaseMechanic<HiddenSwitchInstance> implements Listener {

  private static final int DENIED_MESSAGE_LINE_ID = 0;
  private static final int OFFSET_VALUES_LINE_ID = 2;
  private static final int GRANTED_MESSAGE_LINE_ID = 3;

  private final SignMechanicManager signMechanicManager;
  private final CacheByPosition<HiddenSwitchInstance> instanceByInteractionPosition;
  private final NamespacedKey keyItemsKey;

  private record OffsetSelecting(Player player, HiddenSwitchInstance instance, int creationTime) {}

  private final Map<UUID, HiddenSwitchInstance> openKeysInstanceByPlayerId;
  private final Map<UUID, OffsetSelecting> offsetSelectingByPlayerId;

  public HiddenSwitchMechanic(
    SignMechanicManager signMechanicManager,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config);

    this.signMechanicManager = signMechanicManager;
    this.instanceByInteractionPosition = new CacheByPosition<>();
    this.keyItemsKey = new NamespacedKey(plugin, "key-items");
    this.openKeysInstanceByPlayerId = new HashMap<>();
    this.offsetSelectingByPlayerId = new HashMap<>();
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

    offsetSelectingByPlayerId.put(player.getUniqueId(), new OffsetSelecting(player, instance, signMechanicManager.getTime()));

    config.rootSection.mechanic.hiddenSwitch.blockSelectionPrompt.sendMessage(
      player,
      getSignEnvironment(sign).withVariable("timeout", config.rootSection.mechanic.hiddenSwitch.offsetSelectingTimeoutSeconds)
    );

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

    int xOffset = 0, yOffset = 0, zOffset = 0;
    var offsetTokens = StringUtil.getTokens(SignUtil.getPlainTextLine(sign, OFFSET_VALUES_LINE_ID));

    if (!offsetTokens.isEmpty()) {
      try {
        xOffset = Integer.parseInt(offsetTokens.get(0));
        yOffset = Integer.parseInt(offsetTokens.get(1));
        zOffset = Integer.parseInt(offsetTokens.get(2));

        if (areOffsetsInvalid(creator, sign, xOffset, yOffset, zOffset))
          throw new IllegalArgumentException();
      } catch (Throwable e) {
        sign.getSide(Side.FRONT).line(OFFSET_VALUES_LINE_ID, Component.text("0 0 0"));
        sign.update(true, false);
        sign = (Sign) sign.getBlock().getState();
        xOffset = yOffset = zOffset = 0;
      }
    }

    var instance = new HiddenSwitchInstance(sign, keysInventory, xOffset, yOffset, zOffset, grantedMessage, deniedMessage, config);

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

  private InterpretationEnvironment getSignEnvironment(Sign sign) {
    return new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());
  }

  private boolean areOffsetsInvalid(@Nullable Player player, Sign sign, int xOffset, int yOffset, int zOffset) {
    var offsetLimit = config.rootSection.mechanic.hiddenSwitch.maximumAxisOffset;

    if (Math.abs(xOffset) > offsetLimit || Math.abs(yOffset) > offsetLimit || Math.abs(zOffset) > offsetLimit) {
      if (player != null) {
        config.rootSection.mechanic.hiddenSwitch.axisOffsetLimitExceeded.sendMessage(
          player,
          getSignEnvironment(sign)
            .withVariable("limit", offsetLimit)
        );
      }

      return true;
    }

    var signFacing = ((Directional) sign.getBlockData()).getFacing();

    if (yOffset == 0 && xOffset == signFacing.getModX() && zOffset == signFacing.getModZ()) {
      if (player != null)
        config.rootSection.mechanic.hiddenSwitch.triedBindingToSign.sendMessage(player, getSignEnvironment(sign));

      return true;
    }

    return false;
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

    var timeoutSeconds = config.rootSection.mechanic.hiddenSwitch.offsetSelectingTimeoutSeconds;

    for (var iterator = offsetSelectingByPlayerId.values().iterator(); iterator.hasNext();) {
      var offsetSelecting = iterator.next();

      if (time - offsetSelecting.creationTime < timeoutSeconds * 20)
        continue;

      iterator.remove();

      config.rootSection.mechanic.hiddenSwitch.blockSelectionTimeout.sendMessage(
        offsetSelecting.player,
        getSignEnvironment(offsetSelecting.instance.getSign())
          .withVariable("timeout", timeoutSeconds)
      );
    }
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    var player = event.getPlayer();
    var offsetSelecting = offsetSelectingByPlayerId.remove(player.getUniqueId());

    if (offsetSelecting != null) {
      event.setCancelled(true);

      var mountBlock = offsetSelecting.instance.getMountBlock();

      var xOffset = clickedBlock.getX() - mountBlock.getX();
      var yOffset = clickedBlock.getY() - mountBlock.getY();
      var zOffset = clickedBlock.getZ() - mountBlock.getZ();

      var sign = offsetSelecting.instance.getSign();

      if (areOffsetsInvalid(player, sign, xOffset, yOffset, zOffset))
        return;

      sign.getSide(Side.FRONT).line(OFFSET_VALUES_LINE_ID, Component.text(xOffset + " " + yOffset + " " + zOffset));
      sign.update(true, false);
      reloadInstanceBySign(sign);

      config.rootSection.mechanic.hiddenSwitch.blockSelectionSuccess.sendMessage(
        player,
        getSignEnvironment(offsetSelecting.instance.getSign())
          .withVariable("x_offset", xOffset)
          .withVariable("y_offset", yOffset)
          .withVariable("z_offset", zOffset)
      );

      return;
    }

    var instance = instanceByInteractionPosition.get(clickedBlock.getWorld(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

    if (instance == null)
      return;

    instance.interact(player, signMechanicManager.getTime());
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
    var playerId = event.getPlayer().getUniqueId();
    openKeysInstanceByPlayerId.remove(playerId);
    offsetSelectingByPlayerId.remove(playerId);
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
