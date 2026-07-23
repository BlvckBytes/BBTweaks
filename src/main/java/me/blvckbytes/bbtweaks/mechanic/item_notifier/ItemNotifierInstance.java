package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.pipes.WorldGuardUtil;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;

public class ItemNotifierInstance extends SISOInstance {

  private final String name;
  private final @Nullable ItemPredicate predicate;
  private final EnumSet<ItemNotifierFlag> flags;
  private final ConfigKeeper<MainSection> config;

  private SlotData @Nullable [] lastSlotStates;

  private long lastNotificationSendStamp;

  public ItemNotifierInstance(
    Sign sign,
    Side side,
    String name,
    @Nullable ItemPredicate predicate,
    EnumSet<ItemNotifierFlag> flags,
    ConfigKeeper<MainSection> config
  ) {
    super(sign, side);

    this.name = name;
    this.predicate = predicate;
    this.flags = flags;
    this.config = config;
  }

  @Override
  public boolean tick(long time) {
    if (time % 5 != 0)
      return true;

    var block = getMountBlock();

    if (!BlockUtil.areAllContainerBlocksLoaded(block, null))
      return true;

    if (!(getMountBlock().getState(false) instanceof Container container))
      return false;

    var currentSlotStates = makeSlotStates(container.getInventory());

    if (lastSlotStates == null || currentSlotStates.length != lastSlotStates.length) {
      lastSlotStates = currentSlotStates;
      return true;
    }

    onPossibleStateChange(lastSlotStates, currentSlotStates);

    lastSlotStates = currentSlotStates;
    return true;
  }

  private void onPossibleStateChange(SlotData[] previous, SlotData[] current) {
    var lastSendDeltaMs = System.currentTimeMillis() - lastNotificationSendStamp;

    if (lastSendDeltaMs < config.rootSection.mechanic.itemNotifier.notificationCooldownMs)
      return;

    if (flags.contains(ItemNotifierFlag.ADD) && didTriggerModeAndSend(TriggerMode.ADD, previous, current))
      return;

    if (flags.contains(ItemNotifierFlag.REMOVE) && didTriggerModeAndSend(TriggerMode.REMOVE, previous, current))
      return;

    if (flags.contains(ItemNotifierFlag.FULL_SLOTS) && didTriggerModeAndSend(TriggerMode.FULL_SLOTS, previous, current))
      return;

    if (flags.contains(ItemNotifierFlag.FULL_STACKS) && didTriggerModeAndSend(TriggerMode.FULL_STACKS, previous, current))
      return;

    if (flags.contains(ItemNotifierFlag.EMPTY))
      didTriggerModeAndSend(TriggerMode.EMPTY, previous, current);
  }

  private boolean didTriggerModeAndSend(TriggerMode mode, SlotData[] previous, SlotData[] current) {
    var data = mode.getDataIfTriggered(previous, current, predicate);

    if (data == null)
      return false;

    var notification = config.rootSection.mechanic.itemNotifier.notificationByTrigger.get(mode);

    if (notification == null)
      return false;

    notification.sendTo(
      getNotificationRecipients(),
      new InterpretationEnvironment()
        .withVariable("x", getSign().getX())
        .withVariable("y", getSign().getY())
        .withVariable("z", getSign().getZ())
        .withVariable("name", name)
        .withVariable("slot", data.slotIndex() >= 0 ? data.slotIndex() + 1 : data.slotIndex())
        .withVariable("changed_type_key", data.changedType().translationKey())
    );

    lastNotificationSendStamp = System.currentTimeMillis();

    return true;
  }

  private Iterable<Player> getNotificationRecipients() {
    var recipients = new HashSet<Player>();

    var signLocation = sign.getLocation();

    if (!flags.contains(ItemNotifierFlag.MEMBER) && !flags.contains(ItemNotifierFlag.OWNER)) {
      var radiusExtent = config.rootSection.mechanic.itemNotifier.radiusExtent;

      for (var player : sign.getWorld().getPlayers()) {
        var distanceSquared = player.getLocation().distanceSquared(signLocation);

        if (distanceSquared <= radiusExtent * radiusExtent)
          recipients.add(player);
      }
    }

    else {
      for (var applicableRegion : WorldGuardUtil.getApplicableRegions(signLocation)) {
        if (flags.contains(ItemNotifierFlag.MEMBER))
          WorldGuardUtil.forEachOnlineDomainPlayer(applicableRegion.getMembers(), recipients::add);

        if (flags.contains(ItemNotifierFlag.OWNER))
          WorldGuardUtil.forEachOnlineDomainPlayer(applicableRegion.getOwners(), recipients::add);
      }
    }

    return recipients;
  }

  private SlotData[] makeSlotStates(Inventory inventory) {
    var result = new SlotData[inventory.getSize()];

    for (var index = 0; index < result.length; ++index) {
      var currentItem = inventory.getItem(index);

      if (currentItem == null) {
        result[index] = SlotData.EMPTY;
        continue;
      }

      result[index] = new SlotData(currentItem.getType(), currentItem.getAmount());
    }

    return result;
  }
}
