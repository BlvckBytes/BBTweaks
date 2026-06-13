package me.blvckbytes.bbtweaks.sign_copier.settings;

import org.bukkit.entity.Player;

import java.util.EnumSet;

public class SignCopierSettings {

  public final Player player;
  public final EnumSet<SettingFlag> flags;

  public SignCopierSettings(Player player) {
    this.player = player;
    this.flags = EnumSet.noneOf(SettingFlag.class);

    for (var flag : SettingFlag.ALL_VALUES) {
      if (flag.defaultEnabled)
        this.flags.add(flag);
    }
  }
}
