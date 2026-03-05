package me.blvckbytes.bbtweaks.mechanic.common;

import me.blvckbytes.bbtweaks.mechanic.MechanicInstance;

@FunctionalInterface
public interface TimeoutHandler<InstanceType extends MechanicInstance> {

  void handle(InstanceSession<InstanceType> session, int timeoutSeconds);

}
