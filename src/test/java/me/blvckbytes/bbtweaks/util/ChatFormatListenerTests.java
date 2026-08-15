package me.blvckbytes.bbtweaks.util;

import me.blvckbytes.bbtweaks.chat_format.ChatFormatListener;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatFormatListenerTests {

  private static final Map<String, String> VARIABLE_MAP = Map.of(
    "variable_1", "value one",
    "variable_2", "value two",
    "variable_3", "value three"
  );

  @Test
  public void shouldPassThroughStringsDevoidOfVariables() {
    makeReplaceVariablesCase(
      "This string does not contain any variables",
      "This string does not contain any variables"
    );
  }

  @Test
  public void shouldReplaceVariableOnlyContent() {
    makeReplaceVariablesCase(
      "{variable_1}",
      "value one"
    );

    makeReplaceVariablesCase(
      "{unknown}",
      "{unknown}"
    );
  }

  @Test
  public void shouldReplaceVariablesAtTheVeryBeginning() {
    makeReplaceVariablesCase(
      "{variable_1} continued text",
      "value one continued text"
    );

    makeReplaceVariablesCase(
      "{variable_1}continued text",
      "value onecontinued text"
    );

    makeReplaceVariablesCase(
      "{unknown} continued text",
      "{unknown} continued text"
    );

    makeReplaceVariablesCase(
      "{unknown}continued text",
      "{unknown}continued text"
    );
  }

  @Test
  public void shouldReplaceVariablesAtTheVeryEnd() {
    makeReplaceVariablesCase(
      "prior text {variable_1}",
      "prior text value one"
    );

    makeReplaceVariablesCase(
      "prior text{variable_1}",
      "prior textvalue one"
    );

    makeReplaceVariablesCase(
      "prior text {unknown}",
      "prior text {unknown}"
    );

    makeReplaceVariablesCase(
      "prior text{unknown}",
      "prior text{unknown}"
    );
  }

  @Test
  public void shouldReplaceMultipleVariables() {
    makeReplaceVariablesCase(
      "prior text {variable_1} mid text {variable_2} end text",
      "prior text value one mid text value two end text"
    );

    makeReplaceVariablesCase(
      "prior text{variable_1} mid text{variable_2} end text{variable_3}",
      "prior textvalue one mid textvalue two end textvalue three"
    );
  }

  private static void makeReplaceVariablesCase(String input, String expectedOutput) {
    assertEquals(expectedOutput, ChatFormatListener.replaceVariables(input, VARIABLE_MAP::get));
  }

  @Test
  public void shouldSqueezeSpaces() {
    assertEquals(" ", ChatFormatListener.squeezeSpaces("    "));
    assertEquals("[A] [B] C", ChatFormatListener.squeezeSpaces("[A]  [B]  C"));
    assertEquals(" first second third ", ChatFormatListener.squeezeSpaces("     first      second                third      "));
  }
}
