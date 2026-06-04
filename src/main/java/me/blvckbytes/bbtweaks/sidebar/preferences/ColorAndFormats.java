package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class ColorAndFormats implements DirectFieldAccess {

  public NamedColor color;
  public EnumSet<Format> formats;

  public ColorAndFormats(NamedColor color, EnumSet<Format> formats) {
    this.color = color;
    this.formats = formats;
  }

  public ColorAndFormats(ColorAndFormats other) {
    this.color = other.color;
    this.formats = EnumSet.copyOf(other.formats);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ColorAndFormats other))
      return false;

    return color == other.color && formats.equals(other.formats);
  }

  @Override
  public @Nullable Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "color" -> color;
      case "bold" -> formats.contains(Format.BOLD);
      case "underlined" -> formats.contains(Format.UNDERLINED);
      case "italic" -> formats.contains(Format.ITALIC);
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public @Nullable Set<String> getAvailableFields() {
    return Set.of("color", "bold", "underline", "italic");
  }
}
