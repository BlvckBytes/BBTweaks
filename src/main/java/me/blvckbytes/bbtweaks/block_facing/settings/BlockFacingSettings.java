package me.blvckbytes.bbtweaks.block_facing.settings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BlockFacingSettings {

  public final Player player;

  private final ConfigKeeper<MainSection> config;

  public boolean modifyPlacedBlocks;
  public boolean modifyExistingBlocks;

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

    modifyPlacedBlocks = false;
    modifyExistingBlocks = false;
    return true;
  }

  public void setModifyPlacedBlocks(@Nullable Boolean value) {
    var newValue = value == null ? !modifyPlacedBlocks : value;

    if (newValue == modifyPlacedBlocks) {
      if (newValue) {
        config.rootSection.blockFacing.modifyPlacedBlocksAlreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.blockFacing.modifyPlacedBlocksAlreadyDisabled.sendMessage(player);
      return;
    }

    modifyPlacedBlocks = newValue;

    if (newValue) {
      config.rootSection.blockFacing.modifyPlacedBlocksNowEnabled.sendMessage(player);
      return;
    }

    config.rootSection.blockFacing.modifyPlacedBlocksNowDisabled.sendMessage(player);
  }

  public void setModifyExistingBlocks(@Nullable Boolean value) {
    var newValue = value == null ? !modifyExistingBlocks : value;

    if (newValue == modifyExistingBlocks) {
      if (newValue) {
        config.rootSection.blockFacing.modifyExistingBlocksAlreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.blockFacing.modifyExistingBlocksAlreadyDisabled.sendMessage(player);
      return;
    }

    modifyExistingBlocks = newValue;

    if (newValue) {
      config.rootSection.blockFacing.modifyExistingBlocksNowEnabled.sendMessage(player);
      return;
    }

    config.rootSection.blockFacing.modifyExistingBlocksNowDisabled.sendMessage(player);
  }
}
