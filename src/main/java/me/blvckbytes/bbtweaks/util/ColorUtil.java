package me.blvckbytes.bbtweaks.util;

public class ColorUtil {

  public static String enableColors(String input) {
    var inputLength = input.length();
    var result = new StringBuilder(inputLength);

    for (var charIndex = 0; charIndex < inputLength; ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var remainingChars = inputLength - 1 - charIndex;

      if (currentChar != '&' || remainingChars == 0) {
        result.append(currentChar);
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

        if (
          isHexChar(r1) && isHexChar(r2)
            && isHexChar(g1) && isHexChar(g2)
            && isHexChar(b1) && isHexChar(b2)
        ) {
          result
            .append('§').append('x')
            .append('§').append(r1)
            .append('§').append(r2)
            .append('§').append(g1)
            .append('§').append(g2)
            .append('§').append(b1)
            .append('§').append(b2);

          charIndex += 6;
          continue;
        }
      }

      // Vanilla color-sequence
      if (isColorChar(nextChar)) {
        result.append('§').append(nextChar);
        continue;
      }

      // Wasn't a color-sequence, store as-is
      result.append(currentChar).append(nextChar);
    }

    return result.toString();
  }

  private static boolean isColorChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= '0' && c <= '9') || (c >= 'k' && c <= 'o') || c == 'r';
  }

  private static boolean isHexChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
  }
}
