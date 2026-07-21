package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.inv_filter.command.CommandAction;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InvFilterProfile {

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  private final List<@Nullable PredicateAndLanguage> filterBySlotIndex;

  private int selectedSlotIndex;
  public boolean enabled;

  public InvFilterProfile(
    Player player,
    ConfigKeeper<MainSection> config,
    List<@Nullable PredicateAndLanguage> filterBySlotIndex
  ) {
    this.player = player;
    this.config = config;
    this.filterBySlotIndex = filterBySlotIndex;
  }

  public void removeCurrentFilterIfSetAndMessage(IPPIntegration ippIntegration, String label) {
    var currentFilter = filterBySlotIndex.get(selectedSlotIndex);

    if (currentFilter == null) {
      config.rootSection.invFilter.removeFilterNoneSet.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("slot", selectedSlotIndex + 1)
      );

      return;
    }

    filterBySlotIndex.set(selectedSlotIndex, null);

    config.rootSection.invFilter.removeFilter.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("slot", selectedSlotIndex + 1)
        .withVariable("filter", currentFilter.getTokenPredicateString())
        .withVariable("set_filter_command", makeSetFilterCommand(ippIntegration, label, currentFilter))
    );
  }

  public String makeSetFilterCommand(IPPIntegration ippIntegration, String label, PredicateAndLanguage filter) {
    var selectedLanguage = ippIntegration.predicateHelper.getSelectedLanguage(player);
    var filterString = filter.getTokenPredicateString();

    if (selectedLanguage == filter.language)
      return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER) + " " + filterString;

    return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER_WITH_LANGUAGE) + " " + TranslationLanguage.matcher.getNormalizedName(filter.language) + " " + filterString;
  }

  public void setFilterToCurrentSlot(@Nullable PredicateAndLanguage filter) {
    filterBySlotIndex.set(selectedSlotIndex, filter);
  }

  public int getSlotCount() {
    return filterBySlotIndex.size();
  }

  public @Nullable PredicateAndLanguage getFilter(int index) {
    if (index < 0 || index >= filterBySlotIndex.size())
      return null;

    return filterBySlotIndex.get(index);
  }

  public @Nullable PredicateAndLanguage getCurrentFilterIfEnabled() {
    if (!enabled)
      return null;

    return filterBySlotIndex.get(selectedSlotIndex);
  }

  public void setEnabledAndMessage(@Nullable Boolean value) {
    var newValue = value == null ? !enabled : value;

    if (newValue == enabled) {
      if (enabled) {
        config.rootSection.invFilter.alreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.invFilter.alreadyDisabled.sendMessage(player);
      return;
    }

    enabled = newValue;

    if (enabled) {
      var selectedFilter = filterBySlotIndex.get(selectedSlotIndex);

      config.rootSection.invFilter.nowEnabled.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("slot", selectedSlotIndex + 1)
          .withVariable("filter", selectedFilter == null ? null : selectedFilter.getTokenPredicateString())
      );

      return;
    }

    config.rootSection.invFilter.nowDisabled.sendMessage(player);
  }

  public void setSelectedSlotIndexAndMessage(int index) {
    var environment = new InterpretationEnvironment().withVariable("slot", index + 1);

    if (index == selectedSlotIndex) {
      config.rootSection.invFilter.slotAlreadySelected.sendMessage(player, environment);
      return;
    }

    setSelectedSlotIndex(index);

    config.rootSection.invFilter.slotNowSelected.sendMessage(player, environment);
  }

  public void setSelectedSlotIndex(int index) {
    if (index < 0 || index >= filterBySlotIndex.size())
      return;

    selectedSlotIndex = index;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getSelectedSlotIndex() {
    return selectedSlotIndex;
  }
}
