package me.blvckbytes.bbtweaks.util;

import it.unimi.dsi.fastutil.chars.CharConsumer;

public class LegacyColorUtil {

  private static boolean isColorChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= '0' && c <= '9') || (c >= 'k' && c <= 'o') || c == 'r';
  }

  private static boolean isHexChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
  }

  public static String enableColors(String input) {
    var inputLength = input.length();
    var result = new StringBuilder(inputLength);

    tokenize(
      input, false,
      result::append,
      colorChar -> result.append("§").append(colorChar),
      (r1, r2, g1, g2, b1, b2) -> {
        result
          .append('§').append('x')
          .append('§').append(r1)
          .append('§').append(r2)
          .append('§').append(g1)
          .append('§').append(g2)
          .append('§').append(b1)
          .append('§').append(b2);
      }
    );

    return result.toString();
  }

  public static void tokenize(
    String input,
    boolean acknowledgeEscapes,
    CharConsumer contentHandler,
    CharConsumer legacyColorBeginHandler,
    RGBValueConsumer rgbColorBeginHandler
  ) {
    var inputLength = input.length();

    char priorChar = 0;

    for (var charIndex = 0; charIndex < inputLength; ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var _priorChar = priorChar;
      priorChar = currentChar;

      var remainingChars = inputLength - 1 - charIndex;

      if (currentChar != '&' || remainingChars == 0 || (acknowledgeEscapes && _priorChar == '\\')) {
        contentHandler.accept(currentChar);
        continue;
      }

      var nextChar = input.charAt(++charIndex);

      // Possible hex-sequence of format &#RRGGBB
      if (nextChar == '#' && remainingChars >= 6 + 1) {
        var r1 = input.charAt(charIndex + 1);
        var r2 = input.charAt(charIndex + 2);
        var g1 = input.charAt(charIndex + 3);
        var g2 = input.charAt(charIndex + 4);
        var b1 = input.charAt(charIndex + 5);
        var b2 = input.charAt(charIndex + 6);

        if (isHexChar(r1) && isHexChar(r2) && isHexChar(g1) && isHexChar(g2) && isHexChar(b1) && isHexChar(b2)) {
          rgbColorBeginHandler.accept(r1, r2, g1, g2, b1, b2);
          charIndex += 6;
          continue;
        }
      }

      if (isColorChar(nextChar)) {
        legacyColorBeginHandler.accept(nextChar);
        continue;
      }

      // Wasn't a color-sequence
      contentHandler.accept(currentChar);
      contentHandler.accept(nextChar);
    }
  }
}
