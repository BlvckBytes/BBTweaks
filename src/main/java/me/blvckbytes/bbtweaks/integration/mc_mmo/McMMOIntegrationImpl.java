package me.blvckbytes.bbtweaks.integration.mc_mmo;

import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class McMMOIntegrationImpl implements McMMOIntegration {

  private final McMMOSpecifics specifics = new McMMOSpecifics();

  @Override
  public int applySmeltingRecipeExpBoost(Player player, int experience) {
    var mmoPlayer = UserManager.getPlayer(player);

    if (mmoPlayer == null)
      return experience;

    return mmoPlayer.getSmeltingManager().vanillaXPBoost(experience);
  }

  @Override
  public @Nullable Object getSpecificsApi() {
    return specifics;
  }
}
