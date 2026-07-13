package me.blvckbytes.bbtweaks.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddOnlyInventoryTests {

  // TODO: This could certainly use more test-cases, but at least I got started with setting everything up...

  @BeforeAll
  public static void setUp() {
    MockBukkit.mock();
  }

  @Test
  public void shouldTrackSlotAndCallAdditions() {
    var itemToAdd = new ItemStack(Material.DIRT, 64);

    makeCase(
      inventory -> {
        inventory.setItem(2, new ItemStack(Material.DIRT, 12));
        inventory.setItem(5, new ItemStack(Material.DIAMOND, 21));
        inventory.setItem(8, new ItemStack(Material.DIRT, 5));
      },
      addOnlyInventory -> addAndAssertFits(addOnlyInventory, itemToAdd),
      List.of(
        new SlotItemAddition(2, false, itemToAdd, 64 - 12),
        new SlotItemAddition(8, false, itemToAdd, 12)
      ),
      List.of(
        new CallItemAddition(itemToAdd, itemToAdd.getAmount())
      )
    );

    makeCase(
      inventory -> {
        inventory.setItem(2, new ItemStack(Material.DIRT, 50));
        inventory.setItem(5, new ItemStack(Material.DIAMOND, 21));
        inventory.setItem(8, new ItemStack(Material.DIRT, 55));
      },
      addOnlyInventory -> addAndAssertFits(addOnlyInventory, itemToAdd),
      List.of(
        new SlotItemAddition(2, false, itemToAdd, 64 - 50),
        new SlotItemAddition(8, false, itemToAdd, 64 - 55),
        new SlotItemAddition(0, true, itemToAdd, itemToAdd.getAmount() - (64 - 50 + 64 - 55))
      ),
      List.of(
        new CallItemAddition(itemToAdd, itemToAdd.getAmount())
      )
    );
  }

  @AfterAll
  public static void tearDown() {
    MockBukkit.unmock();
  }

  private static void addAndAssertFits(AddOnlyInventory addOnlyInventory, ItemStack itemToAdd) {
    var addedAmount = addOnlyInventory.addItemAndGetAddedAmount(itemToAdd, itemToAdd.getAmount());
    assertEquals(itemToAdd.getAmount(), addedAmount);
  }

  private static void makeCase(
    Consumer<Inventory> inventoryPreparer,
    Consumer<AddOnlyInventory> itemAdder,
    List<SlotItemAddition> expectedSlotItemAdditions,
    List<CallItemAddition> expectedCallItemAdditions
  ) {
    var slotAdditions = new ArrayList<SlotItemAddition>();
    var callAdditions = new ArrayList<CallItemAddition>();

    var inventory = Bukkit.createInventory(null, 9 * 6);

    inventoryPreparer.accept(inventory);

    var addOnlyInventory = new SimulatingAddOnlyInventory(
      inventory,
      (slot, wasVacant, addedItem, addedAmount, stackSizeOverride) -> slotAdditions.add(new SlotItemAddition(slot, wasVacant, addedItem, addedAmount, stackSizeOverride)),
      (addedItem, addedAmount, stackSizeOverride) -> callAdditions.add(new CallItemAddition(addedItem, addedAmount, stackSizeOverride))
    );

    itemAdder.accept(addOnlyInventory);

    assertListsEqual(expectedSlotItemAdditions, slotAdditions, SlotItemAddition::isSimilarWithEqualParameters);
    assertListsEqual(expectedCallItemAdditions, callAdditions, CallItemAddition::isSimilarWithEqualParameters);
  }

  private static <T> void assertListsEqual(List<T> expected, List<T> actual, BiPredicate<T, T> equalityFunction) {
    assertEquals(expected.size(), actual.size());

    for (var index = 0; index < expected.size(); ++index) {
      var expectedItem = expected.get(index);
      var actualItem = actual.get(index);

      assertTrue(equalityFunction.test(expectedItem, actualItem), "Item-inequality at index " + index);
    }
  }
}
