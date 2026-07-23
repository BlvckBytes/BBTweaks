package me.blvckbytes.bbtweaks.mechanic.teleporter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.back.BackOverrideCommand;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanicFlag;
import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;
import me.blvckbytes.bbtweaks.mechanic.common.UnknownFlagException;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class TeleporterMechanic extends BaseMechanic<TeleporterInstance> {

  private static final int COORDINATES_LINE_ID = 2;
  private static final int FLAGS_LINE_ID = 3;

  private final BackOverrideCommand backCommand;

  public TeleporterMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    BackOverrideCommand backCommand
  ) {
    super(plugin, config, 12, BaseMechanicFlag.RELAY_BUTTON, BaseMechanicFlag.RELAY_PRESSURE_PLATE);

    this.backCommand = backCommand;
  }

  @Override
  public boolean onInstanceClick(Player player, TeleporterInstance instance, boolean wasLeftClick) {
    if (wasLeftClick)
      return false;

    if (instance.flags.contains(TeleporterFlag.NO_BACK))
      backCommand.temporarilyIgnore(player);

    instance.teleport(player);

    if (!instance.flags.contains(TeleporterFlag.SILENT))
      config.rootSection.mechanic.teleporter.teleported.sendMessage(player, instance.makeEnvironment());

    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Teleporter");
  }

  @Override
  public @Nullable TeleporterInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.teleporter")) {
      config.rootSection.mechanic.teleporter.noPermission.sendMessage(creator);
      return null;
    }

    var flagsLine = ComponentUtil.asTrimmedText(sign.getSide(side).line(FLAGS_LINE_ID));
    var translatedFlagsLine = translateFlags(flagsLine);

    if (!flagsLine.equalsIgnoreCase(translatedFlagsLine)) {
      sign.getSide(side).line(FLAGS_LINE_ID, Component.text(translatedFlagsLine));
      sign.update(true, false);
    }

    EnumSet<TeleporterFlag> flags;

    try {
      flags = FlagEnum.parse(TeleporterFlag.class, translatedFlagsLine);
    } catch (UnknownFlagException e) {
      if (creator != null)
        config.rootSection.mechanic.teleporter.unknownFlag.sendMessage(creator, e.makeEnvironment());

      return null;
    }

    var coordinatesContent = ComponentUtil.asTrimmedText(sign.getSide(side).line(COORDINATES_LINE_ID));
    var coordinates = TeleporterCoordinates.tryParse(coordinatesContent);

    if (coordinates == null) {
      if (creator != null)
        config.rootSection.mechanic.teleporter.malformedCoordinates.sendMessage(creator);

      return null;
    }

    var instance = new TeleporterInstance(sign, side, flags, coordinates);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.teleporter.creationSuccess.sendMessage(
        creator,
        getSignEnvironment(sign)
          .inheritFrom(instance.makeEnvironment(), false)
      );

    return instance;
  }

  private String translateFlags(String input) {
    // Well... This should really almost be procedurally generated :^)
    return input
      .toLowerCase()
      .replace("norden", "north")
      .replace("osten", "east")
      .replace("süden", "south")
      .replace("westen", "west")
      .replace("nord-ost", "north-east")
      .replace("nordost", "north-east")
      .replace("nord-osten", "north-east")
      .replace("nordosten", "north-east")
      .replace("nord-west", "north-west")
      .replace("nordwest", "north-west")
      .replace("nord-westen", "north-west")
      .replace("nordwesten", "north-west")
      .replace("süd-ost", "south-east")
      .replace("südost", "south-east")
      .replace("süd-osten", "south-east")
      .replace("südosten", "south-east")
      .replace("süd-west", "south-west")
      .replace("südwest", "south-west")
      .replace("süd-westen", "south-west")
      .replace("südwesten", "south-west")
    ;
  }
}
