package me.blvckbytes.bbtweaks.pipes.search;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.pipes.search.display.SearchDisplayEntry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ItemCollectionEntry implements SearchDisplayEntry {

  private final List<ItemAndSlot> members;

  private final ItemStack type;
  private final Material material;
  private final int stackSize;

  private ItemCollectionEntry(List<ItemAndSlot> members, ItemStack type) {
    this.members = members;

    // Let's create an independent copy for the representative at this point, as the first member may be
    // taken out during the screen-session by somebody else and thereby become unusable for further rendering.
    this.type = new ItemStack(type);
    this.type.setAmount(1);

    this.stackSize = this.type.getMaxStackSize() > 0 ? this.type.getMaxStackSize() : 1;
    this.material = this.type.getType();
  }

  public Material getMaterial() {
    return material;
  }

  public int getStackSize() {
    return stackSize;
  }

  public boolean isEmpty() {
    return members.isEmpty();
  }

  public @Nullable ItemAndSlot getFirstMember() {
    if (members.isEmpty())
      return null;

    return members.getFirst();
  }

  public void removeMember(ItemAndSlot member) {
    members.remove(member);
  }

  public final List<ItemStackEntry> getMembersAsEntries() {
    var result = new ArrayList<ItemStackEntry>(members.size());

    for (var member : members)
      result.add(new ItemStackEntry(member));

    return result;
  }

  @Override
  public ItemStack makeRepresentative(InterpretationEnvironment baseEnvironment, ConfigKeeper<MainSection> config) {
    var representativeItem = new ItemStack(type);

    var totalAmount = 0;

    for (var member : members) {
      var amount = member.item().getAmount();

      if (amount > 0)
        totalAmount += amount;
    }

    var numberStacks = totalAmount / stackSize;
    var singleItems = totalAmount % stackSize;
    var numberDoubleChests = (double) numberStacks / (6 * 9);

    config.rootSection.pipes.search.display.items.collectionRepresentativePatch.patch(
      representativeItem,
      baseEnvironment.copy()
        .withVariable("number_stacks", numberStacks)
        .withVariable("number_double_chests", numberDoubleChests)
        .withVariable("single_items", singleItems)
        .withVariable("stack_size", stackSize)
    );

    return representativeItem;
  }

  public static List<ItemCollectionEntry> collectEntries(List<ItemAndSlot> items) {
    var results = new ArrayList<ItemCollectionEntry>();

    for (var item : items) {
      var bucket = findBucketFor(item.item(), results);

      if (bucket == null) {
        bucket = new ItemCollectionEntry(new ArrayList<>(), item.item());
        results.add(bucket);
      }

      bucket.members.add(item);
    }

    return results;
  }

  private static @Nullable ItemCollectionEntry findBucketFor(ItemStack item, List<ItemCollectionEntry> existingCollections) {
    for (var collection : existingCollections) {
      if (item.isSimilar(collection.type))
        return collection;
    }

    return null;
  }
}
