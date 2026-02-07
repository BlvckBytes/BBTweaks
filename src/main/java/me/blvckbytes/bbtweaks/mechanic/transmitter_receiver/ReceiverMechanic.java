package me.blvckbytes.bbtweaks.mechanic.transmitter_receiver;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.BooleanConsumer;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiverMechanic extends BaseMechanic<ReceiverInstance> {

  private static final int SIGNAL_NAME_LINE_INDEX = 2;
  private static final int NAMESPACE_LINE_INDEX = 3;

  private final Map<String, ReceiverBucket> bucketByFinalName;

  // "Late-init" dependency injection, seeing how it would be circular otherwise
  public @Nullable TransmitterMechanic transmitterMechanic;

  public ReceiverMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    this.bucketByFinalName = new HashMap<>();
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, ReceiverInstance instance, boolean wasLeftClick) {
    if (!player.isSneaking() || wasLeftClick || transmitterMechanic == null)
      return false;

    var loadedReceivers = getLoadedCountForFinalName(instance.finalName);
    var loadedTransmitters = transmitterMechanic.getLoadedCountForFinalName(instance.finalName);
    var currentState = transmitterMechanic.getStateForFinalName(instance.finalName);

    config.rootSection.mechanic.transmitterReceiver.currentBandInformation.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("loaded_receiver_count", loadedReceivers)
        .withVariable("loaded_transmitter_count", loadedTransmitters)
        .withVariable("current_state", currentState)
        .withVariable("signal_name", instance.signalName)
        .withVariable("namespace", instance.namespace)
    );

    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Receiver");
  }

  public void setStateForFinalName(String finalName, boolean state) {
    bucketByFinalName
      .computeIfAbsent(finalName, k -> new ReceiverBucket())
      .setState(state);
  }


  public int getLoadedCountForFinalName(String finalName) {
    var bucket = bucketByFinalName.get(finalName);
    return bucket == null ? 0 : bucket.size();
  }

  @Override
  public @Nullable ReceiverInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.transmitter-receiver")) {
      config.rootSection.mechanic.transmitterReceiver.noPermission.sendMessage(creator);
      return null;
    }

    var signalName = SignUtil.getPlainTextLine(sign, SIGNAL_NAME_LINE_INDEX).trim();

    if (signalName.isBlank()) {
      if (creator != null)
        config.rootSection.mechanic.transmitterReceiver.signalNameAbsent.sendMessage(creator);

      return null;
    }

    var namespace = SignUtil.getPlainTextLine(sign, NAMESPACE_LINE_INDEX).trim();

    if (namespace.isBlank())
      namespace = null;

    var finalName = namespace == null ? signalName : namespace + ":" + signalName;

    var instance = new ReceiverInstance(signalName, namespace, finalName, sign);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);
    bucketByFinalName.computeIfAbsent(finalName, k -> new ReceiverBucket()).add(instance);

    if (creator != null) {
      config.rootSection.mechanic.transmitterReceiver.receiverCreationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("signal_name", signalName)
          .withVariable("namespace", namespace)
          .withVariable("x", sign.getX())
          .withVariable("y", sign.getY())
          .withVariable("z", sign.getZ())
      );
    }

    return instance;
  }

  @Override
  public @Nullable ReceiverInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null) {
      var bucket = bucketByFinalName.get(instance.finalName);

      if (bucket != null)
        bucket.remove(instance);
    }

    return instance;
  }

  @Override
  public void onLeverToggle(Block lever, boolean newState, BooleanConsumer stateSetter) {
    instanceBySignPosition.forEachValue(
      lever.getWorld(),
      instance -> instance.onLeverToggle(lever, newState, stateSetter)
    );
  }
}
