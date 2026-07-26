package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SidebarPreferencesSlots {

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public final List<SidebarPreferences> preferencesBySlotIndex;

  private int selectedSlotIndex;

  public boolean enabled;

  public SidebarPreferencesSlots(
    Player player,
    ConfigKeeper<MainSection> config,
    List<SidebarPreferences> preferencesBySlotIndex
  ) {
    this.player = player;
    this.config = config;
    this.preferencesBySlotIndex = preferencesBySlotIndex;
  }

  public void onConfigReload() {
    for (var preferences : preferencesBySlotIndex)
      preferences.onConfigReload();
  }

  public SidebarPreferences getSelectedPreferences() {
    return preferencesBySlotIndex.get(selectedSlotIndex);
  }

  public void setSelectedSlotIndex(int selectedSlotIndex, boolean sendMessages) {
    if (getSelectedSlotIndex() == selectedSlotIndex) {
      if (sendMessages)
        config.rootSection.sidebar.slotAlreadySelected.sendMessage(player, getSelectedPreferences().makeEnvironment());
      return;
    }

    if (selectedSlotIndex < 0)
      selectedSlotIndex = 0;

    if (selectedSlotIndex >= preferencesBySlotIndex.size())
      selectedSlotIndex = preferencesBySlotIndex.size() - 1;

    this.selectedSlotIndex = selectedSlotIndex;

    if (sendMessages)
      config.rootSection.sidebar.slotSelected.sendMessage(player, getSelectedPreferences().makeEnvironment());
  }

  public int getSelectedSlotIndex() {
    return selectedSlotIndex;
  }

  public void setEnabled(@Nullable Boolean value) {
    var newValue = value == null ? !enabled : value;

    if (newValue == enabled) {
      if (newValue) {
        config.rootSection.sidebar.sidebarAlreadyEnabled.sendMessage(player, getSelectedPreferences().makeEnvironment());
        return;
      }

      config.rootSection.sidebar.sidebarAlreadyDisabled.sendMessage(player, getSelectedPreferences().makeEnvironment());
      return;
    }

    enabled = newValue;

    if (enabled) {
      config.rootSection.sidebar.sidebarNowEnabled.sendMessage(player, getSelectedPreferences().makeEnvironment());
      return;
    }

    config.rootSection.sidebar.sidebarNowDisabled.sendMessage(player, getSelectedPreferences().makeEnvironment());
  }
}
