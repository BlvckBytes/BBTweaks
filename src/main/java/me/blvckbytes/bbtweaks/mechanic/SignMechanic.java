package me.blvckbytes.bbtweaks.mechanic;

import me.blvckbytes.bbtweaks.util.BooleanConsumer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SignMechanic<InstanceType extends MechanicInstance> {

  List<String> getDiscriminators();

  /**
   * @return null if the sign was invalid and is to be destroyed
   */
  @Nullable InstanceType onSignLoad(Sign sign);

  /**
   * @return null if the sign was invalid and is to be destroyed
   */
  @Nullable InstanceType onSignCreate(@Nullable Player creator, Sign sign);

  /**
   * @return The unloaded instance, if any
   */
  @Nullable InstanceType onSignUnload(Sign sign);

  /**
   * @return The unloaded instance, if any
   */
  @Nullable InstanceType onSignDestroy(@Nullable Player destroyer, Sign sign);

  /**
   * @return Whether to cancel the corresponding event
   */
  boolean onSignClick(Player player, Sign sign, boolean wasLeftClick);

  default void onLeverToggle(Block lever, boolean newState, BooleanConsumer stateSetter) {}

  void onMechanicLoad();

  void onMechanicUnload();

  void tick(int time);

}
