package me.blvckbytes.bbtweaks.util;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class AmpersandNotationTranslator {

  private final StringBuilder result;

  private boolean isNoneWhiteColorActive;
  private final Set<String> activeFormats;

  private AmpersandNotationTranslator(String input) {
    this.result = new StringBuilder();
    this.activeFormats = new HashSet<>();

    LegacyColorUtil.tokenize(
      input, true,
      result::append,
      colorChar -> {
        if (colorChar == 'r') {
          onReset();
          return;
        }

        String tagName;

        if ((tagName = colorTagNameFromSectionChar(colorChar)) != null) {
          onColorBegin(tagName);
          return;
        }

        if ((tagName = formatTagNameFromSectionChar(colorChar)) != null) {
          onFormatBegin(tagName);
          return;
        }

        result.append(colorChar);
      },
      (r1, r2, g1, g2, b1, b2) -> onColorBegin("#" + r1 + r2 + g1 + g2 + b1 + b2)
    );
  }

  private void onColorBegin(String equivalentTagName) {
    isNoneWhiteColorActive = !equivalentTagName.equals("white");

    resetActiveFormats();

    result.append("<").append(equivalentTagName).append(">");
  }

  private void onFormatBegin(String equivalentTagName) {
    activeFormats.add(equivalentTagName);

    result.append("<").append(equivalentTagName).append(">");
  }

  private void resetActiveFormats() {
    for (var activeFormat : activeFormats)
      result.append("</").append(activeFormat).append(">");

    activeFormats.clear();
  }

  private void onReset() {
    resetActiveFormats();

    if (isNoneWhiteColorActive) {
      isNoneWhiteColorActive = false;
      result.append("<white>");
    }
  }

  public static String translateToTagNotation(String input) {
    return new AmpersandNotationTranslator(input).result.toString();
  }

  private static @Nullable String formatTagNameFromSectionChar(char sectionChar) {
    return switch (sectionChar) {
      case 'k' -> "obf";
      case 'l' -> "bold";
      case 'm' -> "st";
      case 'n' -> "u";
      case 'o' -> "italic";
      default -> null;
    };
  }

  private static @Nullable String colorTagNameFromSectionChar(char sectionChar) {
    return switch (sectionChar) {
      case 'a' -> "green";
      case 'b' -> "aqua";
      case 'c' -> "red";
      case 'd' -> "light_purple";
      case 'e' -> "yellow";
      case 'f' -> "white";
      case '0' -> "black";
      case '1' -> "dark_blue";
      case '2' -> "dark_green";
      case '3' -> "dark_aqua";
      case '4' -> "dark_red";
      case '5' -> "dark_purple";
      case '6' -> "gold";
      case '7' -> "gray";
      case '8' -> "dark_gray";
      case '9' -> "blue";
      default -> null;
    };
  }
}
