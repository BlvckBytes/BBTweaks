package me.blvckbytes.bbtweaks.chat_format;

import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFormatSection extends ConfigSection {

  public String defaultFormat;

  public boolean squeezeSpaces;

  public Map<String, String> groupFormats = new HashMap<>();

  public @CSIgnore Map<String, String> _groupFormatByGroupNameLower = new HashMap<>();

  public ChatFormatSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var formatEntry : groupFormats.entrySet())
      _groupFormatByGroupNameLower.put(formatEntry.getKey().toLowerCase().strip(), formatEntry.getValue());
  }
}
