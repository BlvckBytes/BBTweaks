package me.blvckbytes.bbtweaks.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ComponentUtil {

  public static String[] getTrimmedLines(Sign sign, Side side) {
    var lineComponents = sign.getSide(side).lines();
    var lineTexts = new String[lineComponents.size()];

    for (var index = 0; index < lineTexts.length; ++index)
      lineTexts[index] = asTrimmedText(lineComponents.get(index));

    return lineTexts;
  }

  public static String asTrimmedText(@Nullable Component component) {
    var result = new StringBuilder();
    forEachTextOfComponent(component, result::append);
    return result.toString().trim();
  }

  public static void forEachTextOfComponent(@Nullable Component component, Consumer<String> textHandler) {
    if (component == null)
      return;

    if (component instanceof TextComponent textComponent)
      textHandler.accept(textComponent.content());

    for (var child : component.children())
      forEachTextOfComponent(child, textHandler);
  }
}
