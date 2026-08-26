package me.blvckbytes.bbtweaks.item_piling.preferences;

import java.util.Arrays;
import java.util.List;

public enum PreferenceFlag {
  // NOTE: The ordinal of this enum is used as the main identifier!
  SHOW_ITEM_COUNT_FOR_PILED_STACKS(true),
  SHOW_ITEM_COUNT_FOR_VANILLA_STACKS(false),
  SHOW_ITEM_COUNT_FOR_UNIT_STACKS(false),

  SHOW_ITEM_MATERIAL_FOR_PILED_STACKS(false),
  SHOW_ITEM_MATERIAL_FOR_VANILLA_STACKS(false),
  SHOW_ITEM_MATERIAL_FOR_UNIT_STACKS(false),

  FORMAT_ITEM_COUNT_TO_STACKS(true),
  IMMEDIATELY_STACK_BLOCK_BREAK_ITEMS(false),
  ;

  public static final List<PreferenceFlag> ALL_VALUES = Arrays.asList(values());

  public final boolean defaultValue;

  PreferenceFlag(boolean defaultValue) {
    this.defaultValue = defaultValue;
  }
}
