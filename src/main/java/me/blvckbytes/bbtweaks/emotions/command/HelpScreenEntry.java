package me.blvckbytes.bbtweaks.emotions.command;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import net.kyori.adventure.text.Component;

import java.util.List;

public record HelpScreenEntry(
  String identifier,
  Component description,
  List<String> aliases,
  boolean supportsSelf,
  boolean supportsOthers,
  boolean supportsAll
) {

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("identifier", identifier)
      .withVariable("description", description)
      .withVariable("aliases", aliases)
      .withVariable("supports_self", supportsSelf)
      .withVariable("supports_others", supportsOthers)
      .withVariable("supports_all", supportsAll);
  }
}
