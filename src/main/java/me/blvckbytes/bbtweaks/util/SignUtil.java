package me.blvckbytes.bbtweaks.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.DoubleChestInventory;

import java.util.ArrayList;
import java.util.function.Predicate;

public class SignUtil {

  private static final BlockFace[] SIGN_FACES = new BlockFace[] {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  public static boolean checkIfAnyContainerSignMatches(Container container, Predicate<Sign> predicate) {
    var containerBlocks = new ArrayList<Block>(2);

    if (container.getInventory() instanceof DoubleChestInventory doubleInventory) {
      if (doubleInventory.getRightSide().getHolder() instanceof Container rightContainer)
        containerBlocks.add(rightContainer.getBlock());

      if (doubleInventory.getLeftSide().getHolder() instanceof Container leftContainer)
        containerBlocks.add(leftContainer.getBlock());
    }

    else
      containerBlocks.add(container.getBlock());

    for (var containerBlock : containerBlocks) {
      for (var signFace : SIGN_FACES) {
        var possibleSignBlock = containerBlock.getRelative(signFace);

        if (!(Tag.WALL_SIGNS.isTagged(possibleSignBlock.getType())))
          continue;

        if (predicate.test((Sign) possibleSignBlock.getState()))
          return true;
      }
    }

    return false;
  }

  public static void setPlainTextLine(Sign sign, int lineIndex, String value, boolean update) {
    var front = sign.getSide(Side.FRONT);
    front.line(lineIndex, Component.text(value));

    if (update)
      sign.update(true, false);
  }

  public static String getPlainTextLine(Sign sign, int lineIndex) {
    var frontLines = sign.getSide(Side.FRONT).lines();

    if (frontLines.size() <= lineIndex)
      return "";

    var targetLine = frontLines.get(lineIndex);

    if (targetLine == null)
      return "";

    var result = new StringBuilder();

    componentToPlainText(targetLine, result);

    return result.toString().trim();
  }

  private static void componentToPlainText(Component component, StringBuilder output) {
    if (component instanceof TextComponent textComponent)
      output.append(textComponent.content());

    for (var child : component.children())
      componentToPlainText(child, output);
  }
}
