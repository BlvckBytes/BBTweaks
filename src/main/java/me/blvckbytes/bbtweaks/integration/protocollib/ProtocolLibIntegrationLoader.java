package me.blvckbytes.bbtweaks.integration.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;

public class ProtocolLibIntegrationLoader {

  @WrappedDependency
  public final ProtocolManager protocolManager;

  public ProtocolLibIntegrationLoader() {
    this.protocolManager = ProtocolLibrary.getProtocolManager();
  }
}
