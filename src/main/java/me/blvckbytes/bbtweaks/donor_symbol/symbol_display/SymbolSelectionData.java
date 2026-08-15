package me.blvckbytes.bbtweaks.donor_symbol.symbol_display;

import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfile;

public record SymbolSelectionData(
  DonorSymbolProfile profile,
  Runnable backButton
) {}
