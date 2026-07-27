package me.blvckbytes.bbtweaks.sidebar;

import java.util.EnumSet;
import java.util.List;

public enum SidebarSortingGroup {
  MCMMO_SKILLS(
    SidebarStatistic.MCMMO_ACROBATICS_LEVEL,
    SidebarStatistic.MCMMO_ALCHEMY_LEVEL,
    SidebarStatistic.MCMMO_ARCHERY_LEVEL,
    SidebarStatistic.MCMMO_AXES_LEVEL,
    SidebarStatistic.MCMMO_CROSSBOWS_LEVEL,
    SidebarStatistic.MCMMO_EXCAVATION_LEVEL,
    SidebarStatistic.MCMMO_FISHING_LEVEL,
    SidebarStatistic.MCMMO_HERBALISM_LEVEL,
    SidebarStatistic.MCMMO_MACES_LEVEL,
    SidebarStatistic.MCMMO_MINING_LEVEL,
    SidebarStatistic.MCMMO_REPAIR_LEVEL,
    SidebarStatistic.MCMMO_SALVAGE_LEVEL,
    SidebarStatistic.MCMMO_SMELTING_LEVEL,
    SidebarStatistic.MCMMO_SPEARS_LEVEL,
    SidebarStatistic.MCMMO_SWORDS_LEVEL,
    SidebarStatistic.MCMMO_TAMING_LEVEL,
    SidebarStatistic.MCMMO_TRIDENTS_LEVEL,
    SidebarStatistic.MCMMO_UNARMED_LEVEL,
    SidebarStatistic.MCMMO_WOODCUTTING_LEVEL
  )
  ;

  public static final List<SidebarSortingGroup> ALL_VALUES = List.of(values());

  public final EnumSet<SidebarStatistic> members;

  SidebarSortingGroup(SidebarStatistic... statistics) {
    this.members = statistics.length == 0 ? EnumSet.noneOf(SidebarStatistic.class) : EnumSet.of(statistics[0], statistics);
  }
}
