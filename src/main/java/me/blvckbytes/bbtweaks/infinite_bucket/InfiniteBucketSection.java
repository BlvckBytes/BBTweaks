package me.blvckbytes.bbtweaks.infinite_bucket;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class InfiniteBucketSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup name;
  public ComponentMarkup lore;
  public boolean glint;

  public InfiniteBucketSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public void applyToMeta(ItemMeta meta) {
    meta.lore(lore.interpret(SlotType.ITEM_LORE, null));
    meta.displayName(name.interpret(SlotType.ITEM_NAME, null).getFirst());
    meta.setEnchantmentGlintOverride(glint);
  }
}
