package me.blvckbytes.bbtweaks.block_facing.settings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BlockFacingSettings {

  public final Player player;

  private final ConfigKeeper<MainSection> config;

  public boolean enabled;

  public FacingOverride facingOverride;

  public BlockFacingSettings(
    Player player,
    ConfigKeeper<MainSection> config
  ) {
    this.player = player;
    this.config = config;

    this.facingOverride = FacingOverride.DEFAULT_VALUE;
  }

  public boolean doesLackPermission() {
    if (player.hasPermission("bbtweaks.blockfacing"))
      return false;

    enabled = false;
    return true;
  }

  public void setFacingOverride(FacingOverride facingOverride) {
    if (this.facingOverride == facingOverride) {
      config.rootSection.blockFacing.facingOverrideAlreadySelected.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("facing", FacingOverride.matcher.getNormalizedName(facingOverride))
      );

      return;
    }

    this.facingOverride = facingOverride;

    config.rootSection.blockFacing.facingOverrideNowSelected.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("facing", FacingOverride.matcher.getNormalizedName(facingOverride))
    );
  }

  public void setEnabled(@Nullable Boolean value) {
    var newValue = value == null ? !enabled : value;

    if (newValue == enabled) {
      if (newValue) {
        config.rootSection.blockFacing.alreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.blockFacing.alreadyDisabled.sendMessage(player);
      return;
    }

    enabled = newValue;

    if (newValue) {
      config.rootSection.blockFacing.nowEnabled.sendMessage(player);
      return;
    }

    config.rootSection.blockFacing.nowDisabled.sendMessage(player);
  }
}
