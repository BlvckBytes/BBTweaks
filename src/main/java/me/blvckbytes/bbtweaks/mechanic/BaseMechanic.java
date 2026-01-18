package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.IterationDecision;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class BaseMechanic<InstanceType extends MechanicInstance> implements SignMechanic {

  protected final ConfigKeeper<MainSection> config;

  protected final CacheByPosition<InstanceType> instanceBySignPosition;

  public BaseMechanic(ConfigKeeper<MainSection> config) {
    this.config = config;

    this.instanceBySignPosition = new CacheByPosition<>();

    config.registerReloadListener(this::_onConfigReload);
  }

  protected abstract void onConfigReload();

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
