package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.mechanic.clock.ClockSection;
import me.blvckbytes.bbtweaks.mechanic.pulse_extender.PulseExtenderSection;

@CSAlways
public class MechanicSection extends ConfigSection {

  public ClockSection clock;
  public PulseExtenderSection pulseExtender;

  public MechanicSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
