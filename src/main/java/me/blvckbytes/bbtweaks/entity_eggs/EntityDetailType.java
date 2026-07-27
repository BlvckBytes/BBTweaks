package me.blvckbytes.bbtweaks.entity_eggs;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.node.MarkupNode;
import at.blvckbytes.component_markup.markup.interpreter.CaptureNode;
import me.blvckbytes.bbtweaks.MainSection;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum EntityDetailType {
  HEALTH,
  HELD_ITEM,
  FIRE_TICKS,
  CUSTOM_NAME,
  IS_BABY,
  VILLAGER_PROFESSION,
  VILLAGER_LEVEL,
  VILLAGER_RECIPES,
  ;

  public static List<MarkupNode> captureDetails(LivingEntity entity, ConfigKeeper<MainSection> config) {
    var result = new ArrayList<MarkupNode>();

    result.add(makeDetailCapture(HEALTH, entity.getHealth() / 2.0, config));

    var activeItem = entity.getActiveItem();

    if (!activeItem.getType().isAir())
      result.add(makeDetailCapture(HELD_ITEM, activeItem.getType().translationKey(), config));

    if (entity.getFireTicks() > 0)
      result.add(makeDetailCapture(FIRE_TICKS, entity.getFireTicks(), config));

    if (entity.customName() != null)
      result.add(makeDetailCapture(CUSTOM_NAME, entity.customName(), config));

    if (entity instanceof Ageable ageable)
      result.add(makeDetailCapture(IS_BABY, !ageable.isAdult(), config));

    if (entity instanceof Villager villager) {
      result.add(makeDetailCapture(VILLAGER_PROFESSION, villager.getProfession().translationKey(), config));
      result.add(makeDetailCapture(VILLAGER_LEVEL, villager.getVillagerLevel(), config));

      var recipeResults = new ArrayList<InterpretationEnvironment>();

      for (var recipe : villager.getRecipes())
        recipeResults.add(itemToEnvironment(recipe.getResult()));

      if (!recipeResults.isEmpty())
        result.add(makeDetailCapture(VILLAGER_RECIPES, recipeResults, config));
    }

    return result;
  }

  private static InterpretationEnvironment itemToEnvironment(ItemStack item) {
    var result = new InterpretationEnvironment()
      .withVariable("type", item.getType().translationKey())
      .withVariable("amount", item.getAmount());

    Map<Enchantment, Integer> enchants = null;

    if (item.getType() == Material.ENCHANTED_BOOK) {
      if (item.getItemMeta() instanceof EnchantmentStorageMeta enchantmentStorageMeta)
        enchants = enchantmentStorageMeta.getStoredEnchants();
    } else {
      var meta = item.getItemMeta();

      if (meta != null)
        enchants = meta.getEnchants();
    }

    var enchantComponents = new ArrayList<Component>();

    if (enchants != null) {
      for (var entry : enchants.entrySet()) {
        var level = entry.getValue();

        enchantComponents.add(
          Component.text()
            .append(entry.getKey().description())
            .append(Component.space())
            .append(
              level > 0 && level < 10
                ? Component.translatable("enchantment.level." + level)
                : Component.text(level)
            )
            .build()
        );
      }
    }

    result.withVariable("enchants", enchantComponents);

    return result;
  }

  private static MarkupNode makeDetailCapture(EntityDetailType detailType, Object value, ConfigKeeper<MainSection> config) {
    return CaptureNode.createVariableCapture(
      config.rootSection.entityEggs.detailLineRenderer.markupNode,
      new InterpretationEnvironment()
        .withVariable("type", detailType.name())
        .withVariable("value", value)
    );
  }
}
