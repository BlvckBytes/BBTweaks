package me.blvckbytes.bbtweaks.sidebar.color_display;

import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;
import org.jetbrains.annotations.Nullable;

public interface ColorDisplayCallback {

  /**
   * @param color Null if the back-button has been clicked
   */
  void onColorSelect(@Nullable NamedColor color);

}
