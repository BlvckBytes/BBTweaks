package me.blvckbytes.bbtweaks.mechanic.transmitter_receiver;

import at.blvckbytes.component_markup.util.TriState;

import java.util.ArrayList;
import java.util.List;

public class TransmitterBucket {

  public final String finalName;
  private final List<TransmitterInstance> transmitters;

  private boolean lastState;

  public TransmitterBucket(String finalName) {
    this.finalName = finalName;
    this.transmitters = new ArrayList<>();
  }

  public void add(TransmitterInstance instance) {
    transmitters.add(instance);
  }

  public void remove(TransmitterInstance instance) {
    transmitters.remove(instance);
  }

  public int size() {
    return transmitters.size();
  }

  public boolean getState() {
    return lastState;
  }

  public boolean updateState() {
    var newState = false;

    for (var transmitter : transmitters) {
      if (transmitter.getLastState() == TriState.TRUE) {
        newState = true;
        break;
      }
    }

    var didChange = lastState != newState;

    lastState = newState;

    return didChange;
  }
}
