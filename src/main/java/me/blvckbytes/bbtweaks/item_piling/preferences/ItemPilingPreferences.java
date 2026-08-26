package me.blvckbytes.bbtweaks.item_piling.preferences;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class ItemPilingPreferences {

  public final Player player;

  public final EnumSet<PreferenceFlag> flags;

  public ItemPilingPreferences(Player player) {
    this.player = player;

    this.flags = EnumSet.noneOf(PreferenceFlag.class);

    for (var flag : PreferenceFlag.ALL_VALUES) {
      if (flag.defaultValue)
        this.flags.add(flag);
    }
  }

  public void toggleFlag(PreferenceFlag flag) {
    if (flags.contains(flag)) {
      flags.remove(flag);
      return;
    }

    flags.add(flag);
  }

  public InterpretationEnvironment makeEnvironment() {
    var result = new InterpretationEnvironment();

    for (var flag : PreferenceFlag.ALL_VALUES)
      result.withVariable(flag.name().toLowerCase(), flags.contains(flag));

    return result;
  }
}
