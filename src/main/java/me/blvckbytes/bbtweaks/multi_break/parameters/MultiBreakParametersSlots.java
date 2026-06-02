package me.blvckbytes.bbtweaks.multi_break.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.config.MultiBreakLimits;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class MultiBreakParametersSlots {

  public final Player player;
  public final List<MultiBreakParameters> parametersBySlotIndex;

  public final ConfigKeeper<MainSection> config;

  public boolean enabled;

  private int selectedSlotIndex;

  private MultiBreakLimits limits;

  public MultiBreakParametersSlots(
    Player player,
    ConfigKeeper<MainSection> config,
    List<MultiBreakParameters> parametersBySlotIndex
  ) {
    this.player = player;
    this.config = config;
    this.parametersBySlotIndex = Collections.unmodifiableList(parametersBySlotIndex);

    updateLimits();
  }

  public MultiBreakLimits getLimits() {
    return limits;
  }

  public void updateLimits() {
    for (var limits : config.rootSection.multiBreak.limitsInDescendingOrder) {
      if (!player.hasPermission("bbtweaks.multibreak.tier." + limits.tierName()))
        continue;

      this.limits = limits;
      return;
    }

    this.limits = MultiBreakLimits.ZERO;
  }

  public MultiBreakParameters getSelectedParameters() {
    return parametersBySlotIndex.get(selectedSlotIndex);
  }

  public void setSelectedSlotIndex(int selectedSlotIndex) {
    if (selectedSlotIndex < 0)
      selectedSlotIndex = 0;

    if (selectedSlotIndex >= parametersBySlotIndex.size())
      selectedSlotIndex = parametersBySlotIndex.size() - 1;

    this.selectedSlotIndex = selectedSlotIndex;
  }

  public int getSelectedSlotIndex() {
    return selectedSlotIndex;
  }

  public void setEnabled(@Nullable Boolean value) {
    if (value != null) {
      if (enabled == value) {
        if (enabled) {
          config.rootSection.multiBreak.alreadyEnabled.sendMessage(player);
          return;
        }

        config.rootSection.multiBreak.alreadyDisabled.sendMessage(player);
        return;
      }

      enabled = value;
    }
    else
      enabled ^= true;

    if (enabled) {
      config.rootSection.multiBreak.nowEnabled.sendMessage(player, getSelectedParameters().makeEnvironment());
      return;
    }

    config.rootSection.multiBreak.nowDisabled.sendMessage(player);
  }
}
