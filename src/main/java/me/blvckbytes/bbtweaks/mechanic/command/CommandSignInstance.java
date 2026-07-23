package me.blvckbytes.bbtweaks.mechanic.command;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;

public class CommandSignInstance extends SISOInstance {

  public final String command;

  public CommandSignInstance(Sign sign, String command) {
    super(sign);

    this.command = command;
  }

  @Override
  public boolean tick(long time) {
    return true;
  }
}
