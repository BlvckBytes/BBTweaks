package me.blvckbytes.bbtweaks.mechanic.pipe_fetch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PipeFetchMechanic extends BaseMechanic<PipeFetchInstance> {

  public PipeFetchMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config);
  }

  @Override
  public boolean onInstanceClick(Player player, PipeFetchInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("PipeFetch");
  }

  @Override
  public @Nullable PipeFetchInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.pipe-fetch")) {
      config.rootSection.mechanic.pipeFetch.noPermission.sendMessage(creator);
      return null;
    }

    var instance = new PipeFetchInstance(sign);

    if (instance.getMountBlock().getType() != Material.PISTON) {
      config.rootSection.mechanic.pipeFetch.notOnAPiston.sendMessage(creator);
      return null;
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.pipeFetch.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }
}
