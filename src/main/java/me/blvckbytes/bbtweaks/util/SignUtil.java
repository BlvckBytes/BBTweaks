package me.blvckbytes.bbtweaks.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class SignUtil {

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
