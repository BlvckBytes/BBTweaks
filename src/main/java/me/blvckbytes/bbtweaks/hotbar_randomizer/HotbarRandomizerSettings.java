package me.blvckbytes.bbtweaks.hotbar_randomizer;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

public class HotbarRandomizerSettings {

  public static final int HOTBAR_SLOT_COUNT = 9;

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;
  private final boolean[] enableStateBySlotId;

  public HotbarRandomizerSettings(
    Player player,
    ConfigKeeper<MainSection> config
  ) {
    this.player = player;
    this.config = config;
    this.enableStateBySlotId = new boolean[HOTBAR_SLOT_COUNT];
  }

  public boolean doesLackPermission() {
    if (player.hasPermission("bbtweaks.hotbar-randomizer"))
      return false;

    enabled = false;
    return true;
  }

  public IntList collectEnabledSlotIndices(IntPredicate predicate) {
    var result = new IntArrayList();

    for (var index = 0; index < HOTBAR_SLOT_COUNT; ++index) {
      if (enableStateBySlotId[index] && predicate.test(index))
        result.add(index);
    }

    return result;
  }

  public void setEnabledAndSendMessage(@Nullable Boolean value) {
    var newValue = value == null ? !enabled : value;

    if (enabled == newValue) {
      if (newValue) {
        config.rootSection.hotbarRandomizer.alreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.hotbarRandomizer.alreadyDisabled.sendMessage(player);
      return;
    }

    enabled = newValue;

    if (newValue) {
      config.rootSection.hotbarRandomizer.nowEnabled.sendMessage(player);
      return;
    }

    config.rootSection.hotbarRandomizer.nowDisabled.sendMessage(player);
  }

  public boolean getSlotEnableState(int slotId) {
    if (slotId < 0 || slotId >= HOTBAR_SLOT_COUNT)
      return false;

    return enableStateBySlotId[slotId];
  }

  public void setSlotEnableState(int slotId, boolean enableState) {
    if (slotId >= 0 && slotId < HOTBAR_SLOT_COUNT)
      enableStateBySlotId[slotId] = enableState;
  }

  public void toggleSlotEnableState(int slotId) {
    if (slotId >= 0 && slotId < HOTBAR_SLOT_COUNT)
      enableStateBySlotId[slotId] ^= true;
  }
}
