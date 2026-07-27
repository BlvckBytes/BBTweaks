package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;

public record EnvironmentAndSortingValue(
  InterpretationEnvironment environment,
  int sortingValue
) {}
