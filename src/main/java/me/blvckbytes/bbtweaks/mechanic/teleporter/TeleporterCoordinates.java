package me.blvckbytes.bbtweaks.mechanic.teleporter;

import org.jetbrains.annotations.Nullable;

public record TeleporterCoordinates(double x, double y, double z) {

  public static @Nullable TeleporterCoordinates tryParse(String input) {
    String[] parts;

    if (input.indexOf(':') >= 0)
      parts = input.split(":");
    else
      parts = input.trim().split(" ");

    if (parts.length != 3)
      return null;

    try {
      return new TeleporterCoordinates(
        Double.parseDouble(parts[0].replace(',', '.')),
        Double.parseDouble(parts[1].replace(',', '.')),
        Double.parseDouble(parts[2].replace(',', '.'))
      );
    } catch (Throwable _) {
      return null;
    }
  }
}
