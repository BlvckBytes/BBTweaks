package me.blvckbytes.bbtweaks.integration.mc_mmo;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface McMMOIntegration {

  int applySmeltingRecipeExpBoost(Player player, int experience);

  @Nullable Object getSpecificsApi();

}
