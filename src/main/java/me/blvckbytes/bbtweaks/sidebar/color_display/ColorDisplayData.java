package me.blvckbytes.bbtweaks.sidebar.color_display;

import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;

public record ColorDisplayData(NamedColor initialSelection, ColorDisplayCallback callback) {}