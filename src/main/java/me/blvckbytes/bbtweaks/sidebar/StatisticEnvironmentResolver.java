package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;

public interface StatisticEnvironmentResolver {

  InterpretationEnvironment resolve(BoardHolder holder, SidebarStatistic statistic);

}
