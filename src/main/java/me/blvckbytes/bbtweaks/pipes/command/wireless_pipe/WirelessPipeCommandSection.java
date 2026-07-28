package me.blvckbytes.bbtweaks.pipes.command.wireless_pipe;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class WirelessPipeCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "wirelesspipe";

  public int interactionSessionTimeoutTicks;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup notLookingAtASign;
  public ComponentMarkup lookedAtSignAlreadyConnected;
  public ComponentMarkup lookedAtSignNotMountedOnGlass;
  public ComponentMarkup lookedAtSignIncompatibleMarker;
  public ComponentMarkup cannotEditSign;
  public ComponentMarkup selectedFirstSign;
  public ComponentMarkup cannotSelectFirstSignTwice;
  public ComponentMarkup interactionSessionTimeout;
  public ComponentMarkup secondBlockActionBarPrompt;
  public ComponentMarkup exitUsage;
  public ComponentMarkup notCurrentlyInASession;
  public ComponentMarkup currentSessionExited;
  public ComponentMarkup secondSignDifferentWorld;
  public ComponentMarkup firstSignIsGone;
  public ComponentMarkup firstSignAlreadyConnected;
  public ComponentMarkup firstSignNotMountedOnGlass;
  public ComponentMarkup firstSignIncompatibleMarker;
  public ComponentMarkup wirelessConnectionEstablished;

  public WirelessPipeCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (interactionSessionTimeoutTicks <= 0)
      throw new MappingError("Property \"interactionSessionTimeoutTicks\" cannot be less than or equal to zero");
  }
}
