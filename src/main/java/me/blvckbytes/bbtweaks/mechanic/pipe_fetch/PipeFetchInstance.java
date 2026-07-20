package me.blvckbytes.bbtweaks.mechanic.pipe_fetch;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;

public class PipeFetchInstance extends SISOInstance {

  /*
    Show current state on sign, lines 3 and 4 (4 used when more details are needed):
    - Idle
      - <&7>Idle
    - Warmup
      - <&6>Input/Output Warmup
      - <&6>{x}P {y}T
    - Moving
      - <&a>Moving Items
      - <&a>{done_slots}/{total_slots}
   */

  // TODO: For debug-purposes, maybe keep a 10-line ring-buffer for the last invocations,
  //       displayed when shift+right-clicking on the instance-sign.

  public PipeFetchInstance(Sign sign) {
    super(sign);
  }

  @Override
  public boolean tick(long time) {
    if (time % 2 != 0)
      return true;

    var blockData = mountBlock.getBlockData();

    if (blockData.getMaterial() != Material.PISTON || !(blockData instanceof Directional directional))
      return false;

    var pistonFacing = directional.getFacing();

    var inputBlock = mountBlock.getRelative(pistonFacing.getOppositeFace());
    var outputBlock = mountBlock.getRelative(pistonFacing);

    // TODO: Keep a visited-set for walking both ways (avoid duplication) - walk output first and stop if there are no destination-containers.
    // TODO: Add located containers from output to visited-set
    // TODO: Once all SearchedInventory instances are gathered, go async for deciding what goes where; work fully
    //       with AddOnlyInventories and collect a list of transactions to perform later on (from, to, count).

    // TODO: A low-to-high transition initializes a fetch, which blocks
    //       further calls until completion.

    // new PipeSearchSession().start() - :^)

    return true;
  }
}
