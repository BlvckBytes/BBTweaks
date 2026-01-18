package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.IterationDecision;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMechanic<InstanceType extends MechanicInstance> implements SignMechanic {

  // TODO: Do not pop off signs, but rather auto-migrate to sane defaults. This will be useful when
  //       editing the config, reloading and not having all signs become unusable just because a
  //       parameter-limit changed.

  // TODO: Reload all currently loaded mechanics on a config-reload

  protected final ConfigKeeper<MainSection> config;

  protected final CacheByPosition<InstanceType> instanceBySignPosition;

  public BaseMechanic(ConfigKeeper<MainSection> config) {
    this.config = config;

    this.instanceBySignPosition = new CacheByPosition<>();

    this.loadConfig();
    config.registerReloadListener(this::loadConfig);
  }

  protected void loadConfig() {}

  @Override
  public boolean onSignLoad(Sign sign) {
    return onSignCreate(null, sign);
  }

  @Override
  public void onSignUnload(Sign sign) {
    onSignDestroy(null, sign);
  }

  @Override
  public void onSignDestroy(@Nullable Player destroyer, Sign sign) {
    instanceBySignPosition.invalidate(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
  }

  @Override
  public void onMechanicLoad() {}

  @Override
  public void onMechanicUnload() {
    instanceBySignPosition.clear();
  }

  @Override
  public void tick(int time) {
    instanceBySignPosition.forEachValue(instance -> {
      instance.tick(time);
      return IterationDecision.CONTINUE;
    });
  }
}
