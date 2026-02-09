package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.mechanic.clock.ClockSection;
import me.blvckbytes.bbtweaks.mechanic.auto_dispose.AutoDisposeSection;
import me.blvckbytes.bbtweaks.mechanic.magnet.config.MagnetSection;
import me.blvckbytes.bbtweaks.mechanic.pulse_extender.PulseExtenderSection;
import me.blvckbytes.bbtweaks.mechanic.sign_flipper.SignFlipperSection;
import me.blvckbytes.bbtweaks.mechanic.transmitter_receiver.TransmitterReceiverSection;

@CSAlways
public class MechanicSection extends ConfigSection {

  public ClockSection clock;
  public PulseExtenderSection pulseExtender;
  public MagnetSection magnet;
  public TransmitterReceiverSection transmitterReceiver;
  public AutoDisposeSection autoDispose;
  public SignFlipperSection signFlipper;

  public ComponentMarkup noWallSign;

  public MechanicSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
