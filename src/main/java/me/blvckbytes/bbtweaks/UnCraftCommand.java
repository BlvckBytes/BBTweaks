package me.blvckbytes.bbtweaks;

import me.blvckbytes.bbtweaks.util.TypeNameResolver;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnCraftCommand implements CommandExecutor, TabCompleter {

  private static final Map<Tag<Material>, Material> preferredMaterials;

  private static final Set<Tag<Material>> excludedUnCraftInputTags;
  private static final Set<Material> excludedUnCraftInputMaterials;

  private static final Set<Material> excludedUnCraftOutputMaterials;
  private static final Set<Tag<Material>> excludedUnCraftOutputTags;

  // Sadly, there are no tags for these yet
  private static final boolean excludeInputSmithingTemplates = true;
  private static final boolean excludeInputDyes = true;
  private static final boolean excludeInputWaxedItems = true;
  private static final boolean excludeAllSameToBlock = true;
  private static final boolean excludeAllSameToIngot = true;
  private static final boolean excludeInputStews = true;
  private static final boolean excludeInputMusicDiscs = true;
  private static final boolean excludeOutputWoods = true;
  private static final boolean excludeOutputStripped = true;

  static {
    excludedUnCraftInputTags = new HashSet<>();
    excludedUnCraftInputMaterials = new HashSet<>();

    excludedUnCraftOutputMaterials = new HashSet<>();
    excludedUnCraftOutputTags = new HashSet<>();

    // All armors, tools and weapons are handled by mcMMO
    excludedUnCraftInputTags.add(Tag.ITEMS_HEAD_ARMOR);
    excludedUnCraftInputTags.add(Tag.ITEMS_CHEST_ARMOR);
    excludedUnCraftInputTags.add(Tag.ITEMS_LEG_ARMOR);
    excludedUnCraftInputTags.add(Tag.ITEMS_FOOT_ARMOR);
    excludedUnCraftInputTags.add(Tag.ITEMS_PICKAXES);
    excludedUnCraftInputTags.add(Tag.ITEMS_AXES);
    excludedUnCraftInputTags.add(Tag.ITEMS_SHOVELS);
    excludedUnCraftInputTags.add(Tag.ITEMS_HOES);
    excludedUnCraftInputTags.add(Tag.ITEMS_SWORDS);

    // These are already deconstructions
    excludedUnCraftInputMaterials.add(Material.WHEAT_SEEDS);
    excludedUnCraftInputMaterials.add(Material.MELON_SEEDS);
    excludedUnCraftInputMaterials.add(Material.PUMPKIN_SEEDS);
    excludedUnCraftInputMaterials.add(Material.SUGAR);

    // We need to exclude sticks, due to the following:
    // STICK:
    // 4x -> {OAK_PLANKS=2}
    // 1x -> {BAMBOO=2}
    // Otherwise, one could convert between oak-planks and bamboo
    excludedUnCraftInputMaterials.add(Material.STICK);

    // These are craftable, but would allow to access quartz
    excludedUnCraftInputMaterials.add(Material.DIORITE);
    excludedUnCraftInputMaterials.add(Material.GRANITE);

    // For obvious reasons...
    excludedUnCraftInputMaterials.add(Material.GOLDEN_APPLE);
    excludedUnCraftInputMaterials.add(Material.ENCHANTED_GOLDEN_APPLE);

    for (Material material : Material.values()) {
      var name = material.name();

      if (excludeInputSmithingTemplates && name.endsWith("_SMITHING_TEMPLATE"))
        excludedUnCraftInputMaterials.add(material);

      if (excludeInputDyes && name.endsWith("_DYE"))
        excludedUnCraftInputMaterials.add(material);

      if (excludeInputWaxedItems && name.startsWith("WAXED_"))
        excludedUnCraftInputMaterials.add(material);

      if (excludeInputStews && name.endsWith("_STEW"))
        excludedUnCraftInputMaterials.add(material);

      if (excludeInputMusicDiscs && name.startsWith("MUSIC_DISC_"))
        excludedUnCraftInputMaterials.add(material);

      if (excludeOutputWoods & name.endsWith("_WOOD"))
        excludedUnCraftOutputMaterials.add(material);

      if (excludeOutputStripped & name.startsWith("STRIPPED_"))
        excludedUnCraftOutputMaterials.add(material);
    }

    preferredMaterials = new HashMap<>();

    preferredMaterials.put(Tag.ITEMS_PLANKS, Material.OAK_PLANKS);
    preferredMaterials.put(Tag.ITEMS_SAND, Material.SAND);
    preferredMaterials.put(Tag.ITEMS_LOGS, Material.OAK_LOG);
    preferredMaterials.put(Tag.ITEMS_WOOL, Material.WHITE_WOOL);
    preferredMaterials.put(Tag.ITEMS_STONE_CRAFTING_MATERIALS, Material.COBBLESTONE);

    excludedUnCraftOutputMaterials.add(Material.WAXED_COPPER_BLOCK);
    excludedUnCraftOutputMaterials.add(Material.ENCHANTED_GOLDEN_APPLE);

    // Because that's just odd...
    excludedUnCraftOutputMaterials.add(Material.FILLED_MAP);

    excludedUnCraftOutputTags.add(Tag.ITEMS_SKULLS);
  }

  record UnCraftEntry(
    int inputAmount,
    Map<Material, Integer> results
  ) {
    static UnCraftEntry tryCreateWithScaledSingleUnit(int inputAmount, Map<Material, Integer> results) {
      // All ingredient-counts need to be a multiple of the result-amount for the scaling to succeed with whole numbers
      if (inputAmount == 0 || !results.values().stream().allMatch(resultAmount -> resultAmount % inputAmount == 0))
        return new UnCraftEntry(inputAmount, results);

      for (var resultEntry : results.entrySet())
        resultEntry.setValue(resultEntry.getValue() / inputAmount);

      return new UnCraftEntry(1, results);
    }
  }

  record ItemAndSlot(ItemStack item, int slot) {}

  private final Logger logger;
  private final Map<Material, List<UnCraftEntry>> unCraftBucketByInputType;

  // There are so many items that defining all un-craft recipes from a blank slate is near
  // impossible, at least with my level of patience. The way we go about it is to loop all
  // craftable recipes and simply turn them around - then apply some (more or less) clever
  // filtering as to exclude the obvious entries we dislike; the result is saved to a file.
  // This file then has to be visually inspected by a human, and copied over to the actual
  // input. When updating the server-version, new entries may appear - these must also be
  // merged in manually, as to ensure that no unwanted recipes slide in unknowingly.

  // The recipes-file takes one recipe per line, with blank lines simply being ignored.
  // A line is of the following syntax:
  // <amount> <input-type> -> <amount> <output-type> [, <amount> <output-type>]

  private final BBTweaksPlugin plugin;
  private final TypeNameResolver typeNameResolver;

  private final File unCraftRecipesTemplateFile;
  private final File unCraftRecipesFile;

  public UnCraftCommand(BBTweaksPlugin plugin, TypeNameResolver typeNameResolver) {
    this.plugin = plugin;
    this.typeNameResolver = typeNameResolver;
    this.logger = plugin.getLogger();

    this.unCraftRecipesTemplateFile = createFileIfAbsent(plugin, "uncraft_recipes_template.txt");
    this.unCraftRecipesFile = createFileIfAbsent(plugin, "uncraft_recipes.txt");

    this.unCraftBucketByInputType = new HashMap<>();
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    // Either /uc <index> [all] or /uc [all]
    if (args.length < 3 && !(args.length == 2 && args[0].equalsIgnoreCase("all")))
      return List.of("all");

    return List.of();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(plugin.accessConfigValue("unCraft.chat.inGameOnly"));
      return true;
    }

    if (!player.hasPermission("bbtweaks.uncraft")) {
      sender.sendMessage(plugin.accessConfigValue("unCraft.chat.missingPermission"));
      return true;
    }

    var inventory = player.getInventory();
    var heldItem = inventory.getItemInMainHand();

    if (heldItem.getType().isAir()) {
      sender.sendMessage(plugin.accessConfigValue("unCraft.chat.noItemInMainHand"));
      return true;
    }

    var heldType = tryExtractMaterialFromItem(heldItem);
    List<UnCraftEntry> availableEntries;

    if (heldType == null || (availableEntries = unCraftBucketByInputType.getOrDefault(heldType, Collections.emptyList())).isEmpty()) {
      sender.sendMessage(plugin.accessConfigValue("unCraft.chat.unsupportedItem"));
      return true;
    }

    UnCraftEntry targetEntry;
    int argsOffset;

    if (availableEntries.size() == 1) {
      targetEntry = availableEntries.get(0);
      argsOffset = 0;
    }

    else {
      if (args.length == 0) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.choicesHeadline").replace("{label}", label));

        for (var entryIndex = 0; entryIndex < availableEntries.size(); ++entryIndex) {
          sender.sendMessage(
            plugin.accessConfigValue("unCraft.chat.choicesEntry")
              .replace("{label}", label)
              .replace("{choice_number}", String.valueOf(entryIndex + 1))
              .replace("{results}", generateResultsString(player, availableEntries.get(entryIndex).results))
          );
        }

        return true;
      }

      int targetNumber;

      try {
        targetNumber = Integer.parseInt(args[0]);
      } catch (Throwable e) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.invalidSelection").replace("{selection}", args[0]));
        return true;
      }

      if (targetNumber <= 0 || targetNumber > availableEntries.size()) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.invalidSelection").replace("{selection}", args[0]));
        return true;
      }

      targetEntry = availableEntries.get(targetNumber - 1);
      argsOffset = 1;
    }

    var isInAllMode = false;

    if (args.length > argsOffset)
      isInAllMode = args[argsOffset].equalsIgnoreCase("all");

    var targetItems = new ArrayList<ItemAndSlot>();

    if (!isInAllMode)
      targetItems.add(new ItemAndSlot(heldItem, inventory.getHeldItemSlot()));

    else {
      for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
        var slotContents = inventory.getItem(slotIndex);
        var contentsType = tryExtractMaterialFromItem(slotContents);

        // Only expand into items of the same type
        if (contentsType == null || contentsType != heldType)
          continue;

        targetItems.add(new ItemAndSlot(slotContents, slotIndex));
      }
    }

    var totalResultCounters = new HashMap<Material, Integer>();
    var unCraftCounter = 0;

    // Do not add to the inventory directly - we're (possibly) reducing the slot bit by bit, and
    // if we want the results to go into that same slot in the case that it could be reduced to
    // nothing, we need to postpone adding until the very end; otherwise, items will be added
    // elsewhere and subsequent adds will "magnetically" stack to that, which is undesired.
    var itemsToAdd = new ArrayList<ItemStack>();

    for (var targetItem : targetItems) {
      while (targetItem.item.getAmount() >= targetEntry.inputAmount) {
        ++unCraftCounter;

        int newAmount = targetItem.item.getAmount() - targetEntry.inputAmount;

        targetItem.item.setAmount(newAmount);

        targetEntry.results.forEach((material, amount) -> {
          itemsToAdd.add(new ItemStack(material, amount));
          totalResultCounters.put(material, totalResultCounters.computeIfAbsent(material, k -> 0) + amount);
        });

        if (newAmount <= 0) {
          inventory.setItem(targetItem.slot, null);
          break;
        }
      }
    }

    if (unCraftCounter == 0) {
      sender.sendMessage(
        plugin.accessConfigValue("unCraft.chat.notEnoughItems")
          .replace("{required_amount}", String.valueOf(targetEntry.inputAmount))
          .replace("{result_item}", typeNameResolver.resolve(player, heldType))
      );

      return true;
    }

    var itemsToDrop = new ArrayList<ItemStack>();

    for (var itemToAdd : itemsToAdd)
      itemsToDrop.addAll(inventory.addItem(itemToAdd).values());

    sender.sendMessage(
      plugin.accessConfigValue(targetEntry.inputAmount == 1 ? "unCraft.chat.successfulUnCraftUnitOne" : "unCraft.chat.successfulUnCraftUnitMany")
        .replace("{uncrafted_item}", typeNameResolver.resolve(player, heldType))
        .replace("{uncraft_count}", String.valueOf(unCraftCounter))
        .replace("{results}", generateResultsString(player, totalResultCounters))
        .replace("{uncraft_unit}", String.valueOf(targetEntry.inputAmount))
    );

    if (!itemsToDrop.isEmpty()) {
      for (var remainder : itemsToDrop)
        player.dropItem(remainder);

      sender.sendMessage(plugin.accessConfigValue("unCraft.chat.droppedItems"));
    }

    return true;
  }

  private String generateResultsString(Player player, Map<Material, Integer> resultMap) {
    var results = new StringJoiner(plugin.accessConfigValue("unCraft.chat.resultSeparator"));

    for (var resultEntry : resultMap.entrySet()) {
      results.add(
        plugin.accessConfigValue("unCraft.chat.resultEntry")
          .replace("{result_item}", typeNameResolver.resolve(player, resultEntry.getKey()))
          .replace("{result_amount}", String.valueOf(resultEntry.getValue()))
      );
    }

    return results.toString();
  }

  private boolean doesRepresentTag(Collection<Material> materials, Tag<Material> tag) {
    // I don't think there are any downsides to allowing there to be more items in the
    // tag than in the collection, so let's let that pass for now.
    return materials.stream().allMatch(tag::isTagged);
  }

  private @Nullable Material decideChoiceMaterial(Collection<Material> materialChoices) {
    materialChoices = materialChoices.stream()
      .filter(material -> !isExcluded(material, false))
      .toList();

    // If there is only a single valid choice, we're talking about specific recipes here, like
    // a block of a certain wood-type, or an item of a certain dye; keep these.
    if (materialChoices.size() > 1) {
      for (var preferredMaterialsEntry : preferredMaterials.entrySet()) {
        var tag = preferredMaterialsEntry.getKey();

        if (doesRepresentTag(materialChoices, tag))
          return preferredMaterialsEntry.getValue();
      }
    }

    Material result = null;

    for (var material : materialChoices) {
      if (result == null || material.ordinal() < result.ordinal())
        result = material;
    }

    return result;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private @Nullable Material addChoiceToUnCraftResults(RecipeChoice choice, Map<Material, Integer> unCraftResults) {
    if (choice == null) {
      logger.warning("Encountered null-valued choice while scanning recipes for the uncraft command");
      return null;
    }

    if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
      var materialChoices = materialChoice.getChoices();

      var material = decideChoiceMaterial(materialChoices);

      if (material == null)
        return null;

      unCraftResults.put(material, unCraftResults.computeIfAbsent(material, k -> 0) + 1);

      return material;
    }

    logger.warning("Skipping recipe with choice-type " + choice.getClass().getSimpleName() + " for the uncraft command");
    return null;
  }

  private boolean isExcluded(Material material, boolean isUnCraftInput) {
    if (isUnCraftInput)
      return excludedUnCraftInputMaterials.contains(material) || excludedUnCraftInputTags.stream().anyMatch(tag -> tag.isTagged(material));

    return excludedUnCraftOutputMaterials.contains(material) || excludedUnCraftOutputTags.stream().anyMatch(tag -> tag.isTagged(material));
  }

  private List<String> tokenizeRecipeLine(String line) {
    var result = new ArrayList<String>();

    // Yes - this is a bit crafty, but it gets the job done and performance is basically irrelevant.

    var firstLevelTokens = line.split(",");

    for (var index = 0; index < firstLevelTokens.length; ++index) {
      if (index != 0)
        result.add(",");

      var firstLevelToken = firstLevelTokens[index];

      var secondLevelTokens = firstLevelToken.split(" ");

      for (String levelToken : secondLevelTokens) {
        var secondLevelToken = levelToken.trim();

        if (!secondLevelToken.isBlank())
          result.add(secondLevelToken);
      }
    }

    return result;
  }

  private void tryLoadRecipeLine(String line) {
    var tokens = tokenizeRecipeLine(line);

    if (tokens.isEmpty())
      return;

    if (tokens.size() < 5)
      throw new IllegalStateException("Requiring at least five tokens per line: <amount> <input-type> -> <amount> <output-type>");

    int inputAmount;

    try {
      inputAmount = Integer.parseInt(tokens.get(0));
    } catch (Throwable e) {
      throw new IllegalStateException("Malformed input-amount: " + tokens.get(0));
    }

    if (inputAmount <= 0)
      throw new IllegalStateException("Input-amount cannot be less than or equal to zero");

    Material inputMaterial;

    try {
      inputMaterial = Material.valueOf(tokens.get(1));
    } catch (Throwable e) {
      throw new IllegalStateException("Malformed input-type: " + tokens.get(1));
    }

    if (!"->".equals(tokens.get(2)))
      throw new IllegalStateException("Expected arrow-operator after input-description: ->");

    var results = new HashMap<Material, Integer>();

    for (var tokenIndex = 3; tokenIndex < tokens.size(); ++tokenIndex) {
      if (tokenIndex != 3) {
        var comma = tokens.get(tokenIndex);

        if (!",".equals(comma))
          throw new IllegalStateException("Expecting output-entries to be comma-separated");

        ++tokenIndex;
      }

      int outputAmount;

      try {
        outputAmount = Integer.parseInt(tokens.get(tokenIndex));
      } catch (Throwable e) {
        throw new IllegalStateException("Malformed output-amount: " + tokens.get(tokenIndex));
      }

      if (outputAmount <= 0)
        throw new IllegalStateException("Output-amount cannot be less than or equal to zero");

      if (tokenIndex == tokens.size() - 1)
        throw new IllegalStateException("Line cannot end with an amount - missing corresponding type");

      Material outputMaterial;

      try {
        outputMaterial = Material.valueOf(tokens.get(tokenIndex + 1));
      } catch (Throwable e) {
        throw new IllegalStateException("Malformed output-type: " + tokens.get(tokenIndex + 1));
      }

      if (results.put(outputMaterial, outputAmount) != null)
        throw new IllegalStateException("Duplicate output-material: " + outputMaterial);

      ++tokenIndex;
    }

    // This check is of utmost importance. As an example, one can use WHITE_DYE with any wool-color
    // as to restore it back to white; the preferred materials mapping then collapses this wildcard
    // to white, as is desired for many other recipes, which then produces: 1 WHITE_WOOL -> 1 WHITE_DYE, 1 WHITE_WOOL.
    // Obviously, that's nonsensical and stems from the fact that this entire recipe is not
    // fit for uncrafting at all; simply throw and let the user remove such edge-cases manually.

    // The above case has been fixed and is automatically filtered out now, but just to be safe, the check will stay.

    for (var resultEntry : results.entrySet()) {
      if (resultEntry.getKey() != inputMaterial)
        continue;

      if (resultEntry.getValue() < inputAmount)
        continue;

      throw new IllegalStateException("Recipe has item " + inputMaterial + " in outputs, with amount >= the input-amount of " + inputAmount);
    }

    var typeBucket = unCraftBucketByInputType.computeIfAbsent(inputMaterial, k -> new ArrayList<>());

    typeBucket.add(UnCraftEntry.tryCreateWithScaledSingleUnit(inputAmount, results));
  }

  public void loadRecipesFromFile() {
    this.unCraftBucketByInputType.clear();

    var loadCounter = 0;

    try (
      var fileReader = new FileReader(unCraftRecipesFile);
      var scanner = new Scanner(fileReader)
    ) {
      int lineNumber = 0;
      while (scanner.hasNextLine()) {
        ++lineNumber;

        try {
          tryLoadRecipeLine(scanner.nextLine());
          ++loadCounter;
        } catch (Throwable e) {
          logger.log(Level.WARNING, "Could not load line " + lineNumber + " of file " + unCraftRecipesFile, e);
        }
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to load " + unCraftRecipesFile, e);
    }

    logger.info("Loaded " + loadCounter + " uncraft-recipes from the file!");
  }

  public void discoverRecipesAndCreateTemplateFile() {
    var localBuckets = new HashMap<Material, List<UnCraftEntry>>();

    recipeLoop: for (var iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
      var recipe = iterator.next();
      var result = recipe.getResult();

      var recipeResultType = tryExtractMaterialFromItem(recipe.getResult());

      if (recipeResultType == null || isExcluded(recipeResultType, true) || recipeResultType.isAir())
        continue;

      var resultAmount = result.getAmount();

      if (resultAmount <= 0)
        continue;

      var unCraftResults = new HashMap<Material, Integer>();

      Material choiceMaterial;

      if (recipe instanceof ShapedRecipe shapedRecipe) {
        var choiceMap = shapedRecipe.getChoiceMap();

        for (var shapeRow : shapedRecipe.getShape()) {
          for (var ingredientIndex = 0; ingredientIndex < shapeRow.length(); ++ingredientIndex) {
            var ingredientChar = shapeRow.charAt(ingredientIndex);
            var choice = choiceMap.get(ingredientChar);

            // Empty placeholder char - ignore
            if (choice == null)
              continue;

            if ((choiceMaterial = addChoiceToUnCraftResults(choice, unCraftResults)) == null || isExcluded(choiceMaterial, false))
              continue recipeLoop;
          }
        }
      }

      else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
        for (var choice : shapelessRecipe.getChoiceList()) {
          if ((choiceMaterial = addChoiceToUnCraftResults(choice, unCraftResults)) == null || isExcluded(choiceMaterial, false))
            continue recipeLoop;
        }
      }

      // These include coloring shulker-boxes... Why did they do it for them, but not for all other
      // coloring-processes? Unbelievable! I was looking all over the place to find this!
      else if (recipe instanceof TransmuteRecipe transmuteRecipe) {
        // The thing undergoing change
        if ((choiceMaterial = addChoiceToUnCraftResults(transmuteRecipe.getInput(), unCraftResults)) == null || isExcluded(choiceMaterial, false))
          continue;

        // The "catalyst" (in the case of shulker-boxes, the dye)
        if ((choiceMaterial = addChoiceToUnCraftResults(transmuteRecipe.getMaterial(), unCraftResults)) == null || isExcluded(choiceMaterial, false))
          continue;
      }

      else
        continue;

      if (resultAmount == 9 && unCraftResults.size() == 1) {
        if (excludeAllSameToBlock) {
          // Things like crafting 1x DIAMOND_BLOCK -> 9x DIAMOND; these are already deconstructions themselves
          for (var entry : unCraftResults.entrySet()) {
            if (entry.getKey().name().endsWith("_BLOCK") && entry.getValue() == 1)
              continue recipeLoop;
          }
        }

        if (excludeAllSameToIngot) {
          // Things like crafting 1x IRON_INGOT -> 9x IRON_NUGGET; these are already deconstructions themselves
          for (var entry : unCraftResults.entrySet()) {
            if (entry.getKey().name().endsWith("_INGOT") && entry.getValue() == 1)
              continue recipeLoop;
          }
        }
      }

      if (isRecoloringRecipe(recipeResultType, unCraftResults.keySet())) {
        var patchedSuccessfully = false;

        Integer patchedAmount;

        // Yet another hack - yay! Shulker-boxes cannot be immediately crafted with color,
        // so there is no non-recoloring recipe that one could deconstruct colored boxes into.
        // Thus, let's patch this recipe to represent exactly that - an initial coloring.
        if (Tag.ITEMS_SHULKER_BOXES.isTagged(recipeResultType)) {
          if ((patchedAmount = unCraftResults.remove(Material.BLACK_SHULKER_BOX)) != null) {
            if (unCraftResults.put(Material.SHULKER_BOX, patchedAmount) == null)
              patchedSuccessfully = true;
          }
        }

        if (!patchedSuccessfully)
          continue;
      }

      localBuckets.computeIfAbsent(recipeResultType, k -> new ArrayList<>()).add(new UnCraftEntry(resultAmount, unCraftResults));
    }

    try (var writer = new FileWriter(unCraftRecipesTemplateFile)) {
      var entries = new ArrayList<>(localBuckets.entrySet());

      // Important: ensure a well-defined order on the file-contents, as to be able to
      // easily diff on version-upgrades, without everything being scrambled again.

      entries.sort(Comparator.comparing(a -> a.getKey().name()));

      for (var entry : entries) {
        var unCraftEntries = new ArrayList<>(entry.getValue());

        unCraftEntries.sort((a, b) -> {
          int ret;

          if ((ret = Integer.compare(a.inputAmount, b.inputAmount)) != 0)
            return ret;

          return Integer.compare(a.results.size(), b.results.size());
        });

        for (var unCraftEntry : unCraftEntries) {
          var result = new StringJoiner(", ");

          var resultItemEntries = new ArrayList<>(unCraftEntry.results.entrySet());

          resultItemEntries.sort(Comparator.comparing(a -> a.getKey().name()));

          for (var resultEntry : resultItemEntries)
            result.add(resultEntry.getValue() + " " + resultEntry.getKey());

          writer.write(unCraftEntry.inputAmount + " " + entry.getKey() + " -> " + result + "\n");
        }
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not generate the uncraft-recipes template-file", e);
    }

    logger.info("Created uncraft-recipes template-file.");
  }

  private @Nullable Material tryExtractMaterialFromItem(@Nullable ItemStack item) {
    if (item == null || item.getType().isAir())
      return null;

    var meta = item.getItemMeta();

    if (meta == null)
      return item.getType();

    if (meta instanceof Damageable d && d.hasDamage())
      return null;

    if (meta.hasDisplayName())
      return null;

    if (meta.hasLore())
      return null;

    if (meta.hasEnchants())
      return null;

    if (meta.hasAttributeModifiers())
      return null;

    var pdc = meta.getPersistentDataContainer();

    if (!pdc.getKeys().isEmpty())
      return null;

    var innerItems = getInnerItems(meta);

    // Cannot uncraft items which contain other items, as they would be lost.
    if (innerItems != null && innerItems.stream().anyMatch(inner -> inner != null && !inner.getType().isAir()))
      return null;

    return item.getType();
  }

   private List<ItemStack> getInnerItems(ItemMeta meta) {
    if (meta instanceof BlockStateMeta blockStateMeta) {
      if (blockStateMeta.getBlockState() instanceof Container container)
        return Arrays.asList(container.getInventory().getContents());

      return null;
    }

    if (meta instanceof BundleMeta bundleMeta) {
      var bundleItems = bundleMeta.getItems();
      return bundleItems.isEmpty() ? Collections.emptyList() : bundleItems;
    }

    return null;
  }

  private static File createFileIfAbsent(Plugin plugin, String name) {
    var file = new File(plugin.getDataFolder(), name);

    if (!file.getParentFile().isDirectory()) {
      if (!file.getParentFile().mkdirs())
        throw new IllegalStateException("Could not create parent-dir of " + file);
    }

    if (!file.isFile()) {
      try {
        if (!file.createNewFile())
          throw new IllegalStateException("Operation returned non-successful status-code");
      } catch (Throwable e) {
        throw new IllegalStateException("Could not create file " + file);
      }
    }

    return file;
  }

  private boolean isRecoloringRecipe(Material recipeResultType, Collection<Material> recipeIngredients) {
    var resultDye = getProbableDyeColor(recipeResultType);

    if (resultDye == null)
      return false;

    var hasDyeIngredient = false;

    // Check whether there's a dye ahead of time, as to be able to early-return in the second loop
    for (var recipeIngredient : recipeIngredients) {
      if (recipeIngredient.name().endsWith("_DYE")) {
        hasDyeIngredient = true;
        break;
      }
    }

    for (var recipeIngredient : recipeIngredients) {
      if (recipeIngredient.name().endsWith("_DYE"))
        continue;

      var ingredientColor = getProbableDyeColor(recipeIngredient);

      if (ingredientColor == null)
        return false;

      // Dying a white or colorless item is not considered a re-color
      if (ingredientColor == DyeColor.WHITE) {
        // But if both are white, let's exclude that - a "recoloring" to the same color.
        return hasDyeIngredient && resultDye == DyeColor.WHITE;
      }
    }

    // Result has a color, ingredients contains dye and only dyed items - is highly likely a recoloring recipe.
    return hasDyeIngredient;
  }

  private @Nullable DyeColor getProbableDyeColor(Material material) {
    // Yes, this is quite the hack - but Bukkit doesn't provide us with anything whatsoever...
    for (var color : DyeColor.values()) {
      if (material.name().startsWith(color.name() + "_"))
        return color;
    }

    return null;
  }
}
