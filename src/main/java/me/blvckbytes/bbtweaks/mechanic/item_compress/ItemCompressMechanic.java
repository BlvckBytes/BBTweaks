package me.blvckbytes.bbtweaks.mechanic.item_compress;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ItemCompressMechanic extends BaseMechanic<ItemCompressInstance> implements ItemCompressApi {

  /*
    TODO: Handle usages of compressed items properly

    - Deny crafting in:
      - Vanilla workbench
      - AutoCrafter
      - PoolCrafter
    - Reduce count when
      - placed
      - eaten
      - used as furnace-fuel
   */

  private final NamespacedKey keyCompressedAmount;
  private final NamespacedKey keyPrependedLoreLineCount;
  private final NamespacedKey keyUniqueId;

  public ItemCompressMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, config);

    this.keyCompressedAmount = new NamespacedKey(plugin, "item-compress-amount");
    this.keyPrependedLoreLineCount = new NamespacedKey(plugin, "item-compress-prepended-lore-line-count");
    this.keyUniqueId = new NamespacedKey(plugin, "item-compress-unique-id");
  }

  @Override
  public boolean onInstanceClick(Player player, ItemCompressInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("ItemCompress");
  }

  @Override
  public @Nullable ItemCompressInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.item-compress")) {
      config.rootSection.mechanic.itemCompress.noPermission.sendMessage(creator);
      return null;
    }

    var instance = new ItemCompressInstance(sign, side, this);
    var mountBlock = instance.getMountBlock();

    if (!(mountBlock.getState(false) instanceof Chest chest)) {
      if (creator != null)
        config.rootSection.mechanic.itemCompress.noChest.sendMessage(creator, getSignEnvironment(sign));

      return null;
    }

    if (checkIfAnyContainerSignMatches(chest, this::isSignRegistered)) {
      if (creator != null) {
        config.rootSection.mechanic.itemCompress.existingSign.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("x", mountBlock.getX())
            .withVariable("y", mountBlock.getY())
            .withVariable("z", mountBlock.getZ())
        );
      }

      return null;
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.itemCompress.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }

  @Override
  public void compressItemsInInventory(Inventory inventory) {
    var contents = inventory.getStorageContents();

    var compressedItemsAndSlots = new ArrayList<ItemAndSlot>();
    var encounteredNonCompressedStacks = false;

    // First, we locate all compressed stacks.

    for (var slotIndex = 0; slotIndex < contents.length; ++slotIndex) {
      var currentItem = contents[slotIndex];

      if (!ItemUtil.isStackValid(currentItem))
        continue;

      if (!isCompressedStack(currentItem)) {
        encounteredNonCompressedStacks = true;
        continue;
      }

      compressedItemsAndSlots.add(new ItemAndSlot(currentItem, slotIndex));
    }

    // Then, we try to combine compressed stacks to minimize the total footprint.

    var compressedIndicesToRemove = new IntArrayList();

    for (var currentCompressedIndex = 1; currentCompressedIndex < compressedItemsAndSlots.size(); ++currentCompressedIndex) {
      var currentCompressedItemAndSlot = compressedItemsAndSlots.get(currentCompressedIndex);

      for (var previousCompressedIndex = 0; previousCompressedIndex < currentCompressedIndex; ++previousCompressedIndex) {
        if (compressedIndicesToRemove.contains(previousCompressedIndex))
          continue;

        var previousCompressedItemAndSlot = compressedItemsAndSlots.get(previousCompressedIndex);

        if (tryCombineCompressedStacksAndGetIfFitWhole(currentCompressedItemAndSlot.item(), previousCompressedItemAndSlot.item())) {
          compressedIndicesToRemove.add(currentCompressedIndex);
          break;
        }
      }
    }

    for (var compressedIndexToRemoveIndex = compressedIndicesToRemove.size() - 1; compressedIndexToRemoveIndex >= 0; --compressedIndexToRemoveIndex) {
      var compressedIndexToRemove = compressedIndicesToRemove.getInt(compressedIndexToRemoveIndex);
      var compressedItemAndSlot = compressedItemsAndSlots.remove(compressedIndexToRemove);
      inventory.setItem(compressedItemAndSlot.slot(), null);
    }

    // Then, we add all remaining, uncompressed items onto them or create new compressed stacks for remainders.
    if (encounteredNonCompressedStacks) {
      for (var slotIndex = 0; slotIndex < contents.length; ++slotIndex) {
        var currentItem = contents[slotIndex];

        if (!ItemUtil.isStackValid(currentItem) || isCompressedStack(currentItem))
          continue;

        var remainingAmount = currentItem.getAmount();

        for (var compressedItemAndSlot : compressedItemsAndSlots) {
          if (!isCompressedStackSimilarTo(compressedItemAndSlot.item(), currentItem))
            continue;

          remainingAmount = addAmountToCompressedStackAndGetRemaining(compressedItemAndSlot.item(), remainingAmount);
        }

        if (remainingAmount <= 0) {
          inventory.setItem(slotIndex, null);
          continue;
        }

        currentItem.setAmount(remainingAmount);

        var compressedStack = createCompressedStackFrom(currentItem);

        compressedItemsAndSlots.add(new ItemAndSlot(compressedStack, slotIndex));

        inventory.setItem(slotIndex, compressedStack);
      }
    }

    // TODO: Add dirty-flag to not needlessly write back.
    compressedItemsAndSlots.forEach(it -> inventory.setItem(it.slot(), it.item()));
  }

  private boolean tryCombineCompressedStacksAndGetIfFitWhole(ItemStack source, ItemStack destination) {
    var totalSourceAmount = getCompressedStackTotalAmount(source);

    if (totalSourceAmount == null)
      return false;

    var totalDestinationAmount = getCompressedStackTotalAmount(destination);

    if (totalDestinationAmount == null)
      return false;

    var originalSourceStack = getOriginalStack(source);

    if (originalSourceStack == null)
      return false;

    var originalDestinationStack = getOriginalStack(destination);

    if (originalDestinationStack == null)
      return false;

    if (!originalSourceStack.isSimilar(originalDestinationStack))
      return false;

    var remainingSourceAmount = addAmountToCompressedStackAndGetRemaining(destination, totalSourceAmount);

    if (remainingSourceAmount >= totalSourceAmount)
      return false;

    if (remainingSourceAmount == 0)
      return true;

    setAmountOfCompressedStack(source, remainingSourceAmount);
    return false;
  }

  @Override
  public boolean isCompressedStack(ItemStack item) {
    return item.getPersistentDataContainer().has(keyCompressedAmount);
  }

  @Override
  public boolean isCompressedStackSimilarTo(ItemStack compressedStack, ItemStack other) {
    var originalStack = getOriginalStack(compressedStack);
    return originalStack != null && originalStack.isSimilar(other);
  }

  private @Nullable ItemStack getOriginalStack(ItemStack compressedStack) {
    if (!isCompressedStack(compressedStack))
      return null;

    var compressedMeta = compressedStack.getItemMeta();

    if (compressedMeta == null)
      return null;

    var compressedStackLore = compressedMeta.lore();

    if (compressedStackLore == null || compressedStackLore.isEmpty())
      return null;

    var prependedLoreLineCount = compressedMeta.getPersistentDataContainer().get(keyPrependedLoreLineCount, PersistentDataType.INTEGER);

    if (prependedLoreLineCount == null || prependedLoreLineCount > compressedStackLore.size())
      return null;

    var originalItem = new ItemStack(compressedStack);

    // Strip off the prepended lore as for the comparison to occur against the original lines.
    // Let's re-use the compressed-meta, as it is not used beyond this scope anyway.
    var originalLore = compressedStackLore.subList(prependedLoreLineCount, compressedStackLore.size());
    compressedMeta.lore(originalLore);

    // Also strip off the PDC-keys used to track metrics.
    var pdc = compressedMeta.getPersistentDataContainer();
    pdc.remove(keyCompressedAmount);
    pdc.remove(keyPrependedLoreLineCount);
    pdc.remove(keyUniqueId);

    originalItem.setItemMeta(compressedMeta);

    return originalItem;
  }

  @Override
  public int addAmountToCompressedStackAndGetRemaining(ItemStack compressedStack, int amountToAdd) {
    if (amountToAdd <= 0)
      return 0;

    var compressedAmount = getCompressedStackTotalAmount(compressedStack);

    if (compressedAmount == null)
      return amountToAdd;

    var remainingSpace = config.rootSection.mechanic.itemCompress.maxCompressedAmount - compressedAmount;

    if (remainingSpace <= 0)
      return amountToAdd;

    var addedAmount = Math.min(remainingSpace, amountToAdd);

    setAmountOfCompressedStack(compressedStack, compressedAmount + addedAmount);

    return amountToAdd - addedAmount;
  }

  private void setAmountOfCompressedStack(ItemStack compressedStack, int compressedAmount) {
    var compressedMeta = Objects.requireNonNull(compressedStack.getItemMeta());

    compressedMeta.getPersistentDataContainer().set(keyCompressedAmount, PersistentDataType.INTEGER, compressedAmount);

    setCompressedStackLore(compressedMeta, compressedAmount, compressedStack.getMaxStackSize());

    compressedStack.setItemMeta(compressedMeta);
  }

  @Override
  public ItemStack createCompressedStackFrom(ItemStack item) {
    var compressedStack = new ItemStack(item);
    compressedStack.setAmount(1);

    var compressedAmount = item.getAmount();
    var compressedMeta = Objects.requireNonNull(compressedStack.getItemMeta());

    setCompressedStackLore(compressedMeta, compressedAmount, item.getMaxStackSize());

    var pdc = compressedMeta.getPersistentDataContainer();

    pdc.set(keyCompressedAmount, PersistentDataType.INTEGER, compressedAmount);
    pdc.set(keyUniqueId, PersistentDataType.BYTE_ARRAY, makeUniqueId());

    compressedStack.setItemMeta(compressedMeta);

    return compressedStack;
  }

  // Compressed items must never stack; two, say, stones with the same count will have the same
  // lore and would stack together, which would not necessarily destroy any items, but be
  // rather confusing to players. Compressed items represent unique containers.
  private byte[] makeUniqueId() {
    var uuid = UUID.randomUUID();
    var idBytes = new byte[16];

    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();

    for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
      idBytes[byteIndex] = (byte) (msb >>> (56 - byteIndex * 8));
      idBytes[byteIndex + 8] = (byte) (lsb >>> (56 - byteIndex * 8));
    }

    return idBytes;
  }

  @Override
  public @Nullable Integer getCompressedStackTotalAmount(ItemStack compressedStack) {
    return compressedStack.getPersistentDataContainer().get(keyCompressedAmount, PersistentDataType.INTEGER);
  }

  private void setCompressedStackLore(ItemMeta compressedMeta, int totalAmount, int stackSize) {
    var pdc = compressedMeta.getPersistentDataContainer();

    var compressedLore = compressedMeta.lore();
    var originalLore = compressedLore;

    if (compressedLore != null) {
      var prependedLoreLineCount = pdc.get(keyPrependedLoreLineCount, PersistentDataType.INTEGER);

       if (prependedLoreLineCount != null && prependedLoreLineCount > 0 && prependedLoreLineCount <= compressedLore.size())
        originalLore = compressedLore.subList(prependedLoreLineCount, compressedLore.size());
    }

    var compressedStackLore = buildCompressedStackLore(totalAmount, stackSize);

    int prependedLineCount;

    if (originalLore == null || originalLore.isEmpty()) {
      prependedLineCount = compressedStackLore.size();
      compressedMeta.lore(compressedStackLore);
    }

    else {
      // Separating blank-line for when the original lore does not start with one.
      if (!ComponentUtil.asTrimmedText(originalLore.getFirst()).isBlank())
        compressedStackLore.add(Component.empty());

      var finalLore = new ArrayList<Component>(originalLore.size() + compressedStackLore.size());

      finalLore.addAll(compressedStackLore);
      finalLore.addAll(originalLore);

      prependedLineCount = compressedStackLore.size();
      compressedMeta.lore(finalLore);
    }

    pdc.set(keyPrependedLoreLineCount, PersistentDataType.INTEGER, prependedLineCount);
  }

  private List<Component> buildCompressedStackLore(int totalAmount, int stackSize) {
    var numberStacks = totalAmount / stackSize;
    var singleItems = totalAmount % stackSize;
    var numberDoubleChests = (double) numberStacks / (6 * 9);

    return config.rootSection.mechanic.itemCompress.compressedStackLore.interpret(
      SlotType.ITEM_LORE,
      new InterpretationEnvironment()
        .withVariable("number_stacks", numberStacks)
        .withVariable("number_double_chests", numberDoubleChests)
        .withVariable("stack_size", stackSize)
        .withVariable("single_items", singleItems)
        .withVariable("total_amount", totalAmount)
    );
  }
}
