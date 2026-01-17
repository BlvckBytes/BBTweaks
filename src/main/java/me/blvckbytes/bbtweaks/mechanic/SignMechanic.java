package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SignMechanic {

  List<String> getDiscriminators();

  boolean onSignLoad(Sign sign);

  boolean onSignCreate(@Nullable Player creator, Sign sign);

  void onSignUnload(Sign sign);

  void onSignDestroy(@Nullable Player destroyer, Sign sign);

  void onMechanicLoad();

  void onMechanicUnload();

  void tick(int time);

}
