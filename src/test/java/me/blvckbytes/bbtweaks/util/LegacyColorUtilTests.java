package me.blvckbytes.bbtweaks.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LegacyColorUtilTests {

  @Test
  public void shouldEnableVanillaColors() {
    makeEnableColorsCase(
      "&cHello, &bworld! &o:)", true, false,
      "§cHello, §bworld! §o:)"
    );
  }

  @Test
  public void shouldNotEnableVanillaColors() {
    makeEnableColorsCase(
      "&cHello, &bworld! &o:)", false, false,
      "&cHello, &bworld! &o:)"
    );
  }

  @Test
  public void shouldEnableHexColors() {
    makeEnableColorsCase(
      "&#FF0000Hello, &#00FF00world! &#0000FF:)", false, true,
      "§x§F§F§0§0§0§0Hello, §x§0§0§F§F§0§0world! §x§0§0§0§0§F§F:)"
    );
  }

  @Test
  public void shouldNotEnableHexColors() {
    makeEnableColorsCase(
      "&#FF0000Hello, &#00FF00world! &#0000FF:)", false, false,
      "&#FF0000Hello, &#00FF00world! &#0000FF:)"
    );
  }

  @Test
  public void shouldEnableAnyColors() {
    makeEnableColorsCase(
      "&#FF0000Hello, &cworld! &#0000FF:)", true, true,
      "§x§F§F§0§0§0§0Hello, §cworld! §x§0§0§0§0§F§F:)"
    );
  }

  @Test
  public void shouldNotEnableAnyColors() {
    makeEnableColorsCase(
      "&#FF0000Hello, &cworld! &#0000FF:)", false, false,
      "&#FF0000Hello, &cworld! &#0000FF:)"
    );
  }

  private static void makeEnableColorsCase(String input, boolean allowVanilla, boolean allowHex, String expectedOutput) {
    assertEquals(expectedOutput, LegacyColorUtil.enableColors(input, allowVanilla, allowHex));
  }
}
