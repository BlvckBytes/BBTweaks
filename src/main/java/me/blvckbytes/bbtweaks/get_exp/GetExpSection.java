package me.blvckbytes.bbtweaks.get_exp;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@CSAlways
public class GetExpSection extends ConfigSection {

  public GetExpCommandSection command;

  public int interactionExpirySeconds;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup alreadyInASession;
  public ComponentMarkup sessionInitialized;
  public ComponentMarkup interactionExpired;
  public @Nullable ComponentMarkup interactionMultiModeActionBarSignal;
  public ComponentMarkup interactionMultiModeEntered;
  public ComponentMarkup interactionMultiModeExited;
  public ComponentMarkup notAFurnace;
  public ComponentMarkup noExperienceStored;
  public ComponentMarkup retrievedFromFurnace;

  public GetExpSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (interactionExpirySeconds <= 0)
      throw new MappingError("\"interactionExpirySeconds\" cannot be less than or equal to zero");
  }
}
