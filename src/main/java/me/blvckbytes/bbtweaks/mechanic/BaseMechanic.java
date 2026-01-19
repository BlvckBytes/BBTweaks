package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.IterationDecision;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class BaseMechanic<InstanceType extends MechanicInstance> implements SignMechanic<InstanceType> {

  protected final Plugin plugin;
  protected final ConfigKeeper<MainSection> config;

  protected final CacheByPosition<InstanceType> instanceBySignPosition;

  public BaseMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;

    this.instanceBySignPosition = new CacheByPosition<>();

    config.registerReloadListener(this::_onConfigReload);
  }

  protected abstract void onConfigReload();

  @Override
  public @Nullable InstanceType onSignLoad(Sign sign) {
    return onSignCreate(null, sign);
  }

  @Override
  public @Nullable InstanceType onSignUnload(Sign sign) {
    return onSignDestroy(null, sign);
  }

  @Override
  public @Nullable InstanceType onSignDestroy(@Nullable Player destroyer, Sign sign) {
    return instanceBySignPosition.invalidate(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
  }

  @Override
  public boolean onSignClick(Player player, Sign sign, boolean wasLeftClick) {
    var instance = instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());

    if (instance != null)
      return onInstanceClick(player, instance, wasLeftClick);

    return false;
  }

  public abstract boolean onInstanceClick(Player player, InstanceType instance, boolean wasLeftClick);

  @Override
  public void onMechanicLoad() {}

  @Override
  public void onMechanicUnload() {
    instanceBySignPosition.clear();
  }

  @Override
  public void tick(int time) {
    instanceBySignPosition.forEachValue(instance -> {
      if (!instance.tick(time)) {
        instance.getSignBlock().breakNaturally();
        return IterationDecision.REMOVE_AND_CONTINUE;
      }

      return IterationDecision.CONTINUE;
    });
  }

  private void _onConfigReload() {
    var mechanicSigns = new ArrayList<Sign>();

    instanceBySignPosition.forEachValue(instance -> {
      var signBlock = instance.getSignBlock();

      if (!(signBlock.getState() instanceof Sign sign))
        return IterationDecision.REMOVE_AND_CONTINUE;

      mechanicSigns.add(sign);
      return IterationDecision.CONTINUE;
    });

    for (var sign : mechanicSigns)
      onSignUnload(sign);

    for (var sign : mechanicSigns)
      onSignLoad(sign);

    onConfigReload();
  }
}
