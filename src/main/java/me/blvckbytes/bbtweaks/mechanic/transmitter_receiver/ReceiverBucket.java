package me.blvckbytes.bbtweaks.mechanic.transmitter_receiver;

import java.util.ArrayList;
import java.util.List;

public class ReceiverBucket {

  private final List<ReceiverInstance> receivers;

  private boolean lastState;

  public ReceiverBucket() {
    this.receivers = new ArrayList<>();
  }

  public void add(ReceiverInstance instance) {
    receivers.add(instance);
    instance.setState(lastState);
  }

  public void remove(ReceiverInstance instance) {
    receivers.remove(instance);
  }

  public int size() {
    return receivers.size();
  }

  public void setState(boolean state) {
    lastState = state;
    receivers.forEach(receiver -> receiver.setState(state));
  }
}
