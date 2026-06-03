package me.blvckbytes.bbtweaks.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class AmpersandNotationTranslatorTests {

  @Test
  public void shouldTranslateColors() {
    makeCase("&aHello, world!", "<green>Hello, world!");
    makeCase("&bHello, &cworld!", "<aqua>Hello, <red>world!");
    makeCase("&#FF00FFHello, &cworld!", "<#FF00FF>Hello, <red>world!");
    makeCase("&#FF00FFHello, &#00FF00world!", "<#FF00FF>Hello, <#00FF00>world!");
  }

  @Test
  public void shouldTranslateFormats() {
    makeCase("&lThis is bold", "<bold>This is bold");
    makeCase("&l&oThis is bold and italic", "<bold><italic>This is bold and italic");
  }

  @Test
  public void shouldResetAsRequired() {
    makeCase("&rPlain text", "Plain text");
    makeCase("&cRed &rtext", "<red>Red <white>text");
    makeCase("&l&oThis is bold and italic &rend", "<bold><italic>This is bold and italic </bold></italic>end");
  }

  @Test
  public void shouldTranslateColorsAndFormats() {
    makeCase(
      "&c&lThis is red and bold &bthis just aqua &lthis aqua and bold &rand this plain white",
      "<red><bold>This is red and bold </bold><aqua>this just aqua <bold>this aqua and bold </bold><white>and this plain white"
    );
  }

  @Test
  public void shouldAcknowledgeEscapes() {
    makeCase("&cThis is red \\&aand continues to be so", "<red>This is red \\&aand continues to be so");
  }

  private static void makeCase(String input, String expectedOutput) {
    var actualOutput = AmpersandNotationTranslator.translateToTagNotation(input);
    assertLinesMatch(List.of(expectedOutput), List.of(actualOutput));
  }
}
