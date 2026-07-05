package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.AutoCrafterSection;
import me.blvckbytes.bbtweaks.mechanic.clock.ClockSection;
import me.blvckbytes.bbtweaks.mechanic.auto_dispose.AutoDisposeSection;
import me.blvckbytes.bbtweaks.mechanic.hidden_switch.HiddenSwitchSection;
import me.blvckbytes.bbtweaks.mechanic.inv_move.InvMoveSection;
import me.blvckbytes.bbtweaks.mechanic.lever_array.LeverArraySection;
import me.blvckbytes.bbtweaks.mechanic.magnet.config.MagnetSection;
import me.blvckbytes.bbtweaks.mechanic.planter.PlanterSection;
import me.blvckbytes.bbtweaks.mechanic.pulse_extender.PulseExtenderSection;
import me.blvckbytes.bbtweaks.mechanic.quick_unload.QuickUnloadSection;
import me.blvckbytes.bbtweaks.mechanic.showcase.ShowcaseSection;
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
  public HiddenSwitchSection hiddenSwitch;
  public QuickUnloadSection quickUnload;
  public InvMoveSection invMove;
  public LeverArraySection leverArray;
  public PlanterSection planter;
  public AutoCrafterSection autoCrafter;
  public ShowcaseSection showcase;

  public ComponentMarkup noWallSign;

  public MechanicSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
