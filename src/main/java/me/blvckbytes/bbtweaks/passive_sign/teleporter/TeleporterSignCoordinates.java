package me.blvckbytes.bbtweaks.passive_sign.teleporter;

import org.jetbrains.annotations.Nullable;

public record TeleporterSignCoordinates(double x, double y, double z) {

  public static @Nullable TeleporterSignCoordinates tryParse(String input) {
    String[] parts;

    if (input.indexOf(':') >= 0)
      parts = input.split(":");
    else
      parts = input.trim().split(" ");

    if (parts.length != 3)
      return null;

    try {
      return new TeleporterSignCoordinates(
        Double.parseDouble(parts[0].replace(',', '.')),
        Double.parseDouble(parts[1].replace(',', '.')),
        Double.parseDouble(parts[2].replace(',', '.'))
      );
    } catch (Throwable _) {
      return null;
    }
  }
}
