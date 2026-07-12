package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.common.OffsetSelectingMechanic;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.bbtweaks.util.SignUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShowcaseMechanic extends OffsetSelectingMechanic<ShowcaseInstance> {

  private static final int MAX_FRAME_SIGN_DISTANCE = 3;

  private static final String DISCRIMINATOR = "Showcase";
  private static final String ITEM_NAME = "[" + DISCRIMINATOR + "]";

  private static final int CHAT_MESSAGE_LINE_ID = 0;
  private static final int OFFSET_VALUES_LINE_ID = 2;
  private static final int INVENTORY_TITLE_LINE_ID = 3;

  private final ShowcaseDisplayHandler displayHandler;
  private final NamespacedKey keyShowcaseEntity;

  private final List<Location> brokenShowcaseEntityLocations = new ArrayList<>();

  public ShowcaseMechanic(
    ShowcaseDisplayHandler displayHandler,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config, OFFSET_VALUES_LINE_ID, () -> config.rootSection.mechanic.showcase);

    this.displayHandler = displayHandler;
    this.keyShowcaseEntity = new NamespacedKey(plugin, "showcase-entity");
  }

  @Override
  public boolean onInstanceClick(Player player, ShowcaseInstance instance, boolean wasLeftClick) {
    if (!player.isSneaking())
      return false;

    if (!wasLeftClick)
      return false;

    var sign = instance.getSign();

    if (!canEditSign(player, sign)) {
      config.rootSection.mechanic.showcase.cannotEditSign.sendMessage(player);
      return true;
    }

    initiateOffsetSelecting(player, instance);
    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of(DISCRIMINATOR);
  }

  @Override
  public @Nullable ShowcaseInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.showcase")) {
      config.rootSection.mechanic.showcase.noPermission.sendMessage(creator);
      return null;
    }

    var environment = getSignEnvironment(sign);

    var inventoryTitle = tryParseMarkup(SignUtil.getPlainTextLine(sign, INVENTORY_TITLE_LINE_ID), error -> {
      if (creator != null)
        config.rootSection.mechanic.showcase.malformedInventoryTitle.sendMessage(creator, addErrorVariables(environment, error));
    });

    var chatMessage = tryParseMarkup(SignUtil.getPlainTextLine(sign, CHAT_MESSAGE_LINE_ID), error -> {
      if (creator != null)
        config.rootSection.mechanic.showcase.malformedChatMessage.sendMessage(creator, addErrorVariables(environment, error));
    });

    var instance = validateOffsetsAndMakeInstance(creator, sign, (newSign, offsets) -> {
      if (sign != newSign)
        newSign.update(true, false);

      return new ShowcaseInstance(newSign, inventoryTitle, chatMessage, offsets);
    });

    if (instance == null)
      return null;

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.showcase.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @EventHandler
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof ItemFrame frame))
      return;

    if (event.getHand() != EquipmentSlot.HAND)
      return;

    var player = event.getPlayer();

    if (player.isSneaking())
      return;

    var mountFace = frame.getFacing().getOppositeFace();
    var frameBlock = frame.getLocation().getBlock();

    ShowcaseInstance instance = null;

    for (var offset = 1; offset <= MAX_FRAME_SIGN_DISTANCE + 1; ++offset) {
      var possibleSignBlock = frameBlock.getRelative(mountFace, offset);

      if (!(possibleSignBlock.getBlockData() instanceof WallSign wallSign))
        continue;

      if (wallSign.getFacing() != mountFace)
        continue;

      instance = instanceBySignPosition.get(possibleSignBlock.getWorld(), possibleSignBlock.getX(), possibleSignBlock.getY(), possibleSignBlock.getZ());

      if (instance != null)
        break;
    }

    if (instance == null) {
      if (!isShowcaseEntity(frame))
        return;
    }

    var frameItem = frame.getItem();

    if (frameItem.getType().isAir())
      return;

    event.setCancelled(true);

    displayHandler.show(player, new ShowcaseDisplayData(instance, frameItem));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
  public void onEntityPlace(HangingPlaceEvent event) {
    var player = event.getPlayer();

    if (player == null)
      return;

    var entityItem = event.getItemStack();

    if (entityItem == null)
      return;

    var itemType = entityItem.getType();

    if (itemType != Material.ITEM_FRAME && itemType != Material.GLOW_ITEM_FRAME)
      return;

    var itemMeta = entityItem.getItemMeta();

    if (itemMeta == null)
      return;

    var itemName = ComponentUtil.asTrimmedText(itemMeta.displayName());

    if (!itemName.equalsIgnoreCase(ITEM_NAME))
      return;

    if (!player.hasPermission("bbtweaks.mechanic.showcase")) {
      config.rootSection.mechanic.showcase.noPermission.sendMessage(player);
      event.setCancelled(true);
      return;
    }

    event.getEntity()
      .getPersistentDataContainer()
      .set(keyShowcaseEntity, PersistentDataType.BOOLEAN, true);

    var location = event.getEntity().getLocation();

    config.rootSection.mechanic.showcase.creationSuccess.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("x", location.getBlockX())
        .withVariable("y", location.getBlockY())
        .withVariable("z", location.getBlockZ())
    );
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onHangingBreak(HangingBreakEvent event) {
    if (!(event.getEntity() instanceof ItemFrame frame))
      return;

    if (isShowcaseEntity(frame))
      brokenShowcaseEntityLocations.add(frame.getLocation());
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onItemSpawn(ItemSpawnEvent event) {
    var item = event.getEntity();
    var itemStack = item.getItemStack();
    var itemType = itemStack.getType();

    if (itemType != Material.ITEM_FRAME && itemType != Material.GLOW_ITEM_FRAME)
      return;

    var location = event.getLocation();

    for (var index = 0; index < brokenShowcaseEntityLocations.size(); ++index) {
      var brokenLocation = brokenShowcaseEntityLocations.get(index);

      if (brokenLocation.getWorld() != location.getWorld())
        continue;

      if (brokenLocation.getBlockX() != location.getBlockX() || brokenLocation.getBlockY() != location.getBlockY() || brokenLocation.getBlockZ() != location.getBlockZ())
        continue;

      brokenShowcaseEntityLocations.remove(index);

      var itemMeta = itemStack.getItemMeta();

      if (itemMeta == null)
        return;

      itemMeta.displayName(Component.text(ITEM_NAME));
      itemStack.setItemMeta(itemMeta);
      item.setItemStack(itemStack);

      return;
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isShowcaseEntity(ItemFrame entity) {
    var isShowcaseEntity = entity.getPersistentDataContainer().get(keyShowcaseEntity, PersistentDataType.BOOLEAN);
    return isShowcaseEntity != null && isShowcaseEntity;
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    if (handleOffsetSelecting(event.getPlayer(), clickedBlock.getLocation()))
      event.setCancelled(true);
  }

  @Override
  protected boolean areOffsetsInvalid(@Nullable Player player, Sign sign, int xOffset, int yOffset, int zOffset) {
    if (super.areOffsetsInvalid(player, sign, xOffset, yOffset, zOffset))
      return true;

    if (!(sign.getBlockData() instanceof Directional directional))
      return true;

    var mountBlock = sign.getBlock().getRelative(directional.getFacing().getOppositeFace());
    var selectedBlock = mountBlock.getRelative(xOffset, yOffset, zOffset);

    if (!(selectedBlock.getState(false) instanceof Container)) {
      config.rootSection.mechanic.showcase.blockSelectionNoContainer.sendMessage(player, getSignEnvironment(sign));
      return true;
    }

    return false;
  }

  @Override
  public void tick(long time) {
    super.tick(time);

    handleOffsetSelectingSessionTimeouts(time);
  }
}

