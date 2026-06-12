package me.blvckbytes.bbtweaks.sign_copier;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SignCopierSection extends ConfigSection {

  public @CSAlways SignCopyCommandSection signCopyCommand;
  public @CSAlways SignEditCommandSection signEditCommand;

  public boolean alsoHandleCancelledInteractions;

  public ComponentMarkup playersOnly;
  public ComponentMarkup helpScreen;
  public ComponentMarkup noSignCopied;
  public ComponentMarkup copiedSignRemoved;
  public ComponentMarkup actionEditUsage;
  public ComponentMarkup signEditUsage;
  public ComponentMarkup malformedLineNumber;
  public ComponentMarkup outOfBoundsLineNumber;
  public ComponentMarkup setLineToBlank;
  public ComponentMarkup setLineToContents;
  public ComponentMarkup previewScreen;
  public ComponentMarkup signCopied;
  public ComponentMarkup signPasted;
  public ComponentMarkup signPasteWasCancelled;

  public SignCopierSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
