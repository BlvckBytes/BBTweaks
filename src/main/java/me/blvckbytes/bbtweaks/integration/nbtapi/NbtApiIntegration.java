package me.blvckbytes.bbtweaks.integration.nbtapi;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Consumer;

public interface NbtApiIntegration {

  boolean isAvailable();

  void tryLoadOfflineInventory(File playerDataFile, Consumer<@Nullable OfflineInventorySnapshot> handler);

}
