package me.blvckbytes.bbtweaks.mechanic.transmitter_receiver;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class TransmitterReceiverSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup signalNameAbsent;
  public ComponentMarkup receiverCreationSuccess;
  public ComponentMarkup transmitterCreationSuccess;
  public ComponentMarkup currentBandInformation;

  public TransmitterReceiverSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);
  }
}
