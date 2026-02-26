package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.SISOFlag;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class HiddenSwitchInstance extends SISOInstance {

  public final Inventory keysInventory;
  public final Location interactionPosition;
  private final ComponentMarkup grantedMessage;
  private final ComponentMarkup deniedMessage;
  private final ConfigKeeper<MainSection> config;
  private final boolean hasKeys;

  private int lastSuccessfulInteractTime = -1;
  private int lastInteractTime;

  public HiddenSwitchInstance(
    Sign sign,
    Inventory keysInventory,
    int xOffset, int yOffset, int zOffset,
    @Nullable ComponentMarkup grantedMessage,
    @Nullable ComponentMarkup deniedMessage,
    ConfigKeeper<MainSection> config
  ) {
    super(sign, SISOFlag.ALLOW_OUTPUT_ON_SIGN_PLANE);

    this.keysInventory = keysInventory;
    this.interactionPosition = getMountBlock().getLocation().add(xOffset, yOffset, zOffset);
    this.grantedMessage = grantedMessage == null ? config.rootSection.mechanic.hiddenSwitch.defaultGrantedMessage : grantedMessage;
    this.deniedMessage = deniedMessage == null ? config.rootSection.mechanic.hiddenSwitch.defaultDeniedMessage : deniedMessage;
    this.config = config;
    this.hasKeys = Arrays.stream(keysInventory.getContents()).anyMatch(it -> it != null && !it.getType().isAir());
  }

  public int getKeyCount() {
    return (int) Arrays.stream(keysInventory.getContents())
      .filter(item -> item != null && item.getAmount() > 0 && !item.getType().isAir())
      .count();
  }

  @Override
  public boolean tick(int time) {
    if (lastSuccessfulInteractTime < 0) {
      tryWriteOutputState(false);
      return true;
    }

    var onDuration = config.rootSection.mechanic.hiddenSwitch.onTimeDurationTicks;
    var elapsedTime = time - lastSuccessfulInteractTime;

    tryWriteOutputState(elapsedTime <= onDuration);

    return true;
  }

  public void interact(Player player, int time) {
    if (lastInteractTime > 0 && time - lastInteractTime < 5)
      return;

    lastInteractTime = time;

    if (!hasKeys) {
      lastSuccessfulInteractTime = time;
      sendMessage(player, grantedMessage);
      return;
    }

    var item = player.getInventory().getItemInMainHand();

    if (item.getType().isAir()) {
      sendMessage(player, deniedMessage);
      return;
    }

    for (var slotIndex = 0; slotIndex < keysInventory.getSize(); ++slotIndex) {
      var currentItem = keysInventory.getItem(slotIndex);

      if (currentItem == null)
        continue;

      if (currentItem.isSimilar(item)) {
        lastSuccessfulInteractTime = time;
        sendMessage(player, grantedMessage);
        return;
      }
    }

    sendMessage(player, deniedMessage);
  }

  private void sendMessage(Player player, ComponentMarkup message) {
    if (message == null)
      return;

    message.interpret(
      SlotType.CHAT,
      new InterpretationEnvironment()
        .withVariable("player", player.displayName())
    ).forEach(player::sendMessage);
  }
}
