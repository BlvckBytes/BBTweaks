package me.blvckbytes.bbtweaks.mechanic.command;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class CommandSignInstance extends SISOInstance {

  public final String command;

  public CommandSignInstance(
    Sign sign,
    Side side,
    String command
  ) {
    super(sign, side);

    this.command = command;
  }

  @Override
  public boolean tick(long time) {
    return true;
  }
}
