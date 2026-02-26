package me.blvckbytes.bbtweaks.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {

  public static List<String> getTokens(String input) {
    var result = new ArrayList<String>();
    var tokenBeginIndex = -1;

    for (var charIndex = 0; charIndex < input.length(); ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var isWhitespace = Character.isWhitespace(currentChar);

      if (isWhitespace) {
        if (tokenBeginIndex < 0)
          continue;

        result.add(input.substring(tokenBeginIndex, charIndex));
        tokenBeginIndex = -1;
        continue;
      }

      if (tokenBeginIndex < 0)
        tokenBeginIndex = charIndex;
    }

    if (tokenBeginIndex >= 0)
      result.add(input.substring(tokenBeginIndex));

    return result;
  }
}
