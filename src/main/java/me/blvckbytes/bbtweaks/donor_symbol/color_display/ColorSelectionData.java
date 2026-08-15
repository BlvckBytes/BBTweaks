package me.blvckbytes.bbtweaks.donor_symbol.color_display;

import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfile;

public record ColorSelectionData(
  DonorSymbolProfile profile,
  Runnable backButton
) {}
