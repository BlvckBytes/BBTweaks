package me.blvckbytes.bbtweaks.pipes;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class PipePredicateEvent extends BlockEvent {

  private static final HandlerList handlers = new HandlerList();

  private final List<Material> includeFilters;
  private final List<Material> excludeFilters;

  private @Nullable Predicate<ItemStack> predicate;

  public PipePredicateEvent(Block theBlock, List<Material> includeFilters, List<Material> excludeFilters) {
    super(theBlock);

    this.includeFilters = includeFilters;
    this.excludeFilters = excludeFilters;
  }

  public boolean testItem(@Nullable ItemStack item) {
    if (!ItemUtil.isStackValid(item))
      return false;

    if (this.predicate == null)
      return doesItemPassFilters(item);

    return this.predicate.test(item);
  }

  public List<Material> getIncludeFilters() {
    return includeFilters;
  }

  public List<Material> getExcludeFilters() {
    return excludeFilters;
  }

  public void setPredicate(@Nullable Predicate<ItemStack> predicate) {
    this.predicate = predicate;
  }

  public @Nullable Predicate<ItemStack> getPredicate() {
    return predicate;
  }

  @Override
  @NotNull
  public HandlerList getHandlers() {
    return handlers;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return handlers;
  }

  private boolean doesItemPassFilters(ItemStack stack) {
    for (var includeFilter : includeFilters) {
      if (includeFilter.isAir())
        continue;

      if (stack.getType() != includeFilter)
        return false;
    }

    for (var excludeFilter : excludeFilters) {
      if (excludeFilter.isAir())
        continue;

      if (stack.getType() == excludeFilter)
        return false;
    }

    return true;
  }
}
