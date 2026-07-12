package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.common.OffsetSelectingMechanic;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShowcaseMechanic extends OffsetSelectingMechanic<ShowcaseInstance> {

  // TODO: Item mit [Showcase]-Name wird zur Minimalversion mit default UI

  private static final int CHAT_MESSAGE_LINE_ID = 0;
  private static final int OFFSET_VALUES_LINE_ID = 2;
  private static final int INVENTORY_TITLE_LINE_ID = 3;

  private final ShowcaseDisplayHandler displayHandler;
  private final CacheByPosition<ShowcaseInstance> instanceByInteractionPosition;

  public ShowcaseMechanic(
    ShowcaseDisplayHandler displayHandler,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config, OFFSET_VALUES_LINE_ID, () -> config.rootSection.mechanic.showcase);

    this.displayHandler = displayHandler;
    this.instanceByInteractionPosition = new CacheByPosition<>();
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
    return List.of("Showcase");
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

    instanceByInteractionPosition.put(
      instance.interactionPosition.getWorld(),
      instance.interactionPosition.getBlockX(),
      instance.interactionPosition.getBlockY(),
      instance.interactionPosition.getBlockZ(),
      instance
    );

    if (creator != null)
      config.rootSection.mechanic.showcase.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  public @Nullable ShowcaseInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance == null)
      return null;

    instanceByInteractionPosition.invalidate(
      instance.interactionPosition.getWorld(),
      instance.interactionPosition.getBlockX(),
      instance.interactionPosition.getBlockY(),
      instance.interactionPosition.getBlockZ()
    );

    return instance;
  }

  @EventHandler
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof ItemFrame frame))
      return;

    var player = event.getPlayer();

    if (player.isSneaking())
      return;

    var location = frame.getLocation();
    var instance = instanceByInteractionPosition.get(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

    if (instance == null || shouldDebounceInteraction(player, instance))
      return;

    var frameItem = frame.getItem();

    if (frameItem.getType().isAir())
      return;

    event.setCancelled(true);

    displayHandler.show(player, new ShowcaseDisplayData(instance, frameItem));
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

