package me.blvckbytes.bbtweaks.un_craft;

import me.blvckbytes.bbtweaks.BBTweaksPlugin;
import me.blvckbytes.bbtweaks.util.MutableInt;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UnCraftCommand implements CommandExecutor, TabCompleter {

  record MaterialExtractionResult(@Nullable Material material, String absenceReason) {}
  record ItemAndSlot(ItemStack item, int slot) {}

  private static final String REASON_MARKER = "Reason:";

  private final Logger logger;
  private final Map<Material, List<UnCraftEntry>> unCraftBucketByInputType;

  // There are so many items that defining all un-craft recipes from a blank slate is near
  // impossible, at least with my level of patience. The way we go about it is to loop all
  // craftable recipes and simply turn them around - then apply some (more or less) clever
  // filtering as to exclude the obvious entries we dislike; the result is saved to a file.
  // This file then has to be visually inspected by a human, and copied over to the actual
  // input. When updating the server-version, new entries may appear - these must also be
  // merged in manually, as to ensure that no unwanted recipes slide in unknowingly.

  private final BBTweaksPlugin plugin;
  private final TypeNameResolver typeNameResolver;

  private final File unCraftRecipesTemplateFile;
  private final File unCraftRecipesFile;

  private final List<TypeExclusionRule> typeExclusionRules;
  private final List<TypeInclusionRule> typeInclusionRules;
  private final List<RecipeExclusionRule> recipeExclusionRules;
  private final List<PreferredMaterial> preferredMaterials;
  private final List<ParsedRecipe> additionalRecipes;

  public UnCraftCommand(BBTweaksPlugin plugin, TypeNameResolver typeNameResolver) {
    this.plugin = plugin;
    this.typeNameResolver = typeNameResolver;
    this.logger = plugin.getLogger();

    this.unCraftRecipesTemplateFile = createFileIfAbsent(plugin, "uncraft_recipes_template.txt");
    this.unCraftRecipesFile = createFileIfAbsent(plugin, "uncraft_recipes.txt");

    this.unCraftBucketByInputType = new HashMap<>();
    this.typeExclusionRules = new ArrayList<>();
    this.typeInclusionRules = new ArrayList<>();
    this.recipeExclusionRules = new ArrayList<>();
    this.preferredMaterials = new ArrayList<>();
    this.additionalRecipes = new ArrayList<>();

    // Give it some delay as for other plugins to register their recipes
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      loadConfig();
      discoverRecipesAndCreateTemplateFile();
      loadRecipesFromFile();
    }, 10);

    plugin.registerConfigReloadListener(() -> {
      loadConfig();
      discoverRecipesAndCreateTemplateFile();
      loadRecipesFromFile();
    });
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    String lastArg;

    if (args.length != 0 && !(lastArg = args[args.length - 1]).isBlank()) {
      var firstChar = lastArg.charAt(0);

      // Do not suggest any flags if the user's entering a number (choice-index)
      if (firstChar >= '0' && firstChar <= '9')
        return List.of();

      // Do not suggest flags if the user already selected (at least) one that's available
      if (firstChar == '-') {
        for (int charIndex = 1; charIndex < lastArg.length(); ++charIndex) {
          if (CommandFlag.getFlagByChar(lastArg.charAt(charIndex)) != null)
            return List.of();
        }
      }
    }

    var argsAndFlags = CommandFlag.parseFlags(args);
    var suggestedFlags = new ArrayList<String>();

    for (var commandFlag : CommandFlag.values()) {
      if (!argsAndFlags.flags().contains(commandFlag))
        suggestedFlags.add(commandFlag.representation);
    }

    return suggestedFlags;
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

    var extractionResult = tryExtractMaterialFromItem(heldItem);

    if (extractionResult.material == null) {
      sender.sendMessage(
        plugin.accessConfigValue("unCraft.chat.unsupportedItem")
          .replace("{reason}", extractionResult.absenceReason)
      );

      return true;
    }

    var heldType = extractionResult.material;
    var availableEntries = unCraftBucketByInputType.getOrDefault(heldType, Collections.emptyList());

    if (availableEntries.isEmpty()) {
      sender.sendMessage(
        plugin.accessConfigValue("unCraft.chat.unsupportedItem")
          .replace("{reason}", plugin.accessConfigValue("unCraft.additionalReasons.noEntryFound"))
      );

      return true;
    }

    var exclusionReasons = new HashSet<String>();
    var permittedEntries = new ArrayList<UnCraftEntry>();

    for (var availableEntry : availableEntries) {
      if (!availableEntry.exclusionReasons.isEmpty()) {
        exclusionReasons.addAll(availableEntry.exclusionReasons);
        continue;
      }

      permittedEntries.add(availableEntry);
    }

    if (permittedEntries.isEmpty()) {
      String reason;

      if (exclusionReasons.isEmpty())
        reason = plugin.accessConfigValue("unCraft.additionalReasons.noReasonGiven");
      else
        reason = String.join("; ", exclusionReasons);

      sender.sendMessage(
        plugin.accessConfigValue("unCraft.chat.unsupportedItem")
          .replace("{reason}", reason)
      );

      return true;
    }

    var argsAndFlags = CommandFlag.parseFlags(args);

    UnCraftEntry targetEntry;

    if (permittedEntries.size() == 1)
      targetEntry = permittedEntries.get(0);

    else {
      if (argsAndFlags.args().isEmpty()) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.choicesHeadline").replace("{label}", label));

        for (var entryIndex = 0; entryIndex < permittedEntries.size(); ++entryIndex) {
          var entryResults = permittedEntries.get(entryIndex).results;

          sender.sendMessage(
            plugin.accessConfigValue("unCraft.chat.choicesEntry")
              .replace("{label}", label)
              .replace("{choice_number}", String.valueOf(entryIndex + 1))
              .replace("{results}", generateResultsString(player, entryResults.keySet(), entryResults::get))
          );
        }

        return true;
      }

      var targetString = argsAndFlags.args().get(0);
      int targetNumber;

      try {
        targetNumber = Integer.parseInt(targetString);
      } catch (Throwable e) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.invalidSelection").replace("{selection}", targetString));
        return true;
      }

      if (targetNumber <= 0 || targetNumber > permittedEntries.size()) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.invalidSelection").replace("{selection}", targetString));
        return true;
      }

      targetEntry = permittedEntries.get(targetNumber - 1);
    }

    var targetItems = new ArrayList<ItemAndSlot>();

    if (argsAndFlags.flags().contains(CommandFlag.ALL_MODE)) {
      if (!player.hasPermission("bbtweaks.uncraft.all")) {
        sender.sendMessage(plugin.accessConfigValue("unCraft.chat.missingPermissionAllMode"));
        return true;
      }

      for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
        var slotContents = inventory.getItem(slotIndex);

        if (slotContents == null || slotContents.getType().isAir())
          continue;

        var contentsExtractionResult = tryExtractMaterialFromItem(slotContents);

        // Only expand into items of the same type
        if (contentsExtractionResult.material != heldType)
          continue;

        targetItems.add(new ItemAndSlot(slotContents, slotIndex));
      }
    }

    else
      targetItems.add(new ItemAndSlot(heldItem, inventory.getHeldItemSlot()));

    var wholeUnitsUnCraftCounter = 0;
    var reducedUnitsUnCraftAmounts = new ArrayList<Integer>();

    // Do not add to the inventory directly - we're (possibly) reducing the slot bit by bit, and
    // if we want the results to go into that same slot in the case that it could be reduced to
    // nothing, we need to postpone adding until the very end; otherwise, items will be added
    // elsewhere and subsequent adds will "magnetically" stack to that, which is undesired.

    var itemsToAdd = new HashMap<Material, MutableInt>();
    var acceptReduced = argsAndFlags.flags().contains(CommandFlag.ACCEPT_REDUCED);

    for (var targetItem : targetItems) {
      int remainingAmount;

      while ((remainingAmount = targetItem.item.getAmount()) > 0) {
        int newAmount;

        // Need to scale down the result-amounts, if applicable
        if (remainingAmount < targetEntry.inputAmount) {
          if (!acceptReduced)
            break;

          var scalingFactor = (double) remainingAmount / targetEntry.inputAmount;

          if (remainingAmount < targetEntry.minRequiredAmount)
            break;

          for (var resultEntry : targetEntry.results.entrySet()) {
            var resultAmount = (int) Math.floor(resultEntry.getValue() * scalingFactor);

            if (resultAmount > 0)
              itemsToAdd.computeIfAbsent(resultEntry.getKey(), k -> new MutableInt()).value += resultAmount;
          }

          newAmount = 0;
          reducedUnitsUnCraftAmounts.add(remainingAmount);
        }

        // Can still reduce by whole units
        else {
          for (var resultEntry : targetEntry.results.entrySet())
            itemsToAdd.computeIfAbsent(resultEntry.getKey(), k -> new MutableInt()).value += resultEntry.getValue();

          newAmount = remainingAmount - targetEntry.inputAmount;
          ++wholeUnitsUnCraftCounter;
        }

        if (newAmount <= 0) {
          targetItem.item.setAmount(0);
          inventory.setItem(targetItem.slot, null);
          continue;
        }

        targetItem.item.setAmount(newAmount);
      }
    }

    if (wholeUnitsUnCraftCounter == 0 && reducedUnitsUnCraftAmounts.isEmpty()) {
      var requiredAmount = acceptReduced ? targetEntry.minRequiredAmount : targetEntry.inputAmount;

      sender.sendMessage(
        plugin.accessConfigValue("unCraft.chat.notEnoughItems")
          .replace("{required_amount}", String.valueOf(requiredAmount))
          .replace("{result_item}", typeNameResolver.resolve(player, heldType))
      );

      return true;
    }

    String message;

    if (targetEntry.inputAmount == 1)
      message = plugin.accessConfigValue("unCraft.chat.successfulUnCraftUnitOne");
    else {
      if (wholeUnitsUnCraftCounter == 0)
        message = plugin.accessConfigValue("unCraft.chat.successfulUnCraftUnitManyNoWholeAndReduced");
      else if (reducedUnitsUnCraftAmounts.isEmpty())
        message = plugin.accessConfigValue("unCraft.chat.successfulUnCraftUnitManyWholeAndNoReduced");
      else
        message = plugin.accessConfigValue("unCraft.chat.successfulUnCraftUnitManyWholeAndReduced");
    }

    sender.sendMessage(
      message
        .replace("{uncrafted_item}", typeNameResolver.resolve(player, heldType))
        .replace("{whole_units_uncraft_count}", String.valueOf(wholeUnitsUnCraftCounter))
        .replace("{reduced_units_uncraft_count}", String.valueOf(reducedUnitsUnCraftAmounts.size()))
        .replace("{results}", generateResultsString(player, itemsToAdd.keySet(), k -> itemsToAdd.get(k).value))
        .replace("{uncraft_unit}", String.valueOf(targetEntry.inputAmount))
        .replace(
          "{reduced_amounts}",
          reducedUnitsUnCraftAmounts.stream()
            .map(amount -> plugin.accessConfigValue("unCraft.chat.reducedAmountsEntry").replace("{reduced_amount}", String.valueOf(amount)))
            .collect(Collectors.joining(plugin.accessConfigValue("unCraft.chat.reducedAmountsSeparator")))
        )
    );

    var itemsToDrop = new HashMap<Material, MutableInt>();

    forEachStackOfTypeCountMap(itemsToAdd, item -> {
      for (var remainder : inventory.addItem(item).values())
        itemsToDrop.computeIfAbsent(item.getType(), k -> new MutableInt()).value += remainder.getAmount();
    });

    if (!itemsToDrop.isEmpty()) {
      sender.sendMessage(
        plugin.accessConfigValue("unCraft.chat.droppedItems")
          .replace("{items}", generateResultsString(player, itemsToDrop.keySet(), k -> itemsToDrop.get(k).value))
      );

      forEachStackOfTypeCountMap(itemsToDrop, player::dropItem);
    }

    return true;
  }

  private void forEachStackOfTypeCountMap(Map<Material, MutableInt> typeCountMap, Consumer<ItemStack> itemHandler) {
    for (var typeEntry : typeCountMap.entrySet()) {
      var itemType = typeEntry.getKey();
      var remainingAmount = typeEntry.getValue();

      while (remainingAmount.value > 0) {
        var stackSize = Math.min(itemType.getMaxStackSize(), remainingAmount.value);
        remainingAmount.value -= stackSize;

        itemHandler.accept(new ItemStack(itemType, stackSize));
      }
    }
  }

  private String generateResultsString(Player player, Collection<Material> materials, ToIntFunction<Material> amountAccessor) {
    var results = new StringJoiner(plugin.accessConfigValue("unCraft.chat.resultSeparator"));

    for (var material : materials) {
      results.add(
        plugin.accessConfigValue("unCraft.chat.resultEntry")
          .replace("{result_item}", typeNameResolver.resolve(player, material))
          .replace("{result_amount}", String.valueOf(amountAccessor.applyAsInt(material)))
      );
    }

    return results.toString();
  }

  private @NotNull Material decideChoiceMaterial(RecipeChoice.MaterialChoice materialChoice, Set<String> exclusionReasonsOutput) {
    var availableChoices = materialChoice.getChoices();

    if (availableChoices.isEmpty())
      throw new InvalidRecipeException("Encountered an empty material-choice");

    var permittedChoices = new ArrayList<Material>();

    var exclusionReasons = new HashSet<String>();

    for (var material : availableChoices) {
      var exclusionRules = getApplyingExclusionRules(material, MaterialType.UNCRAFT_RESULT);

      if (exclusionRules.isEmpty()) {
        permittedChoices.add(material);
        continue;
      }

      exclusionRules.forEach(rule -> exclusionReasons.add(rule.reason()));
    }

    // If there is only a single valid choice, we're talking about specific recipes here, like
    // a block of a certain wood-type, or an item of a certain dye; keep these.
    if (permittedChoices.size() > 1) {
      for (var preferredMaterial : preferredMaterials) {
        if (preferredMaterial.matches(permittedChoices))
          return preferredMaterial.preferredMaterial();
      }
    }

    Material result = null;

    for (var material : permittedChoices) {
      if (result == null || material.ordinal() < result.ordinal())
        result = material;
    }

    if (result == null) {
      exclusionReasonsOutput.addAll(exclusionReasons);
      // If we're already excluding the recipe, we might as well have the ingredient noted down, for further information
      result = availableChoices.iterator().next();
    }

    return result;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private void addChoiceToUnCraftResults(@NotNull RecipeChoice choice, Map<Material, Integer> unCraftResults, Set<String> exclusionReasonsOutput) {
    if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
      var material = decideChoiceMaterial(materialChoice, exclusionReasonsOutput);
      unCraftResults.put(material, unCraftResults.computeIfAbsent(material, k -> 0) + 1);
      return;
    }

    // As far as I know, exact choices represent choices which work via ItemStack#isSimilar and thus
    // may require name, lore, enchantments and other fancy properties; we will never support those.
    if (choice instanceof RecipeChoice.ExactChoice)
      throw new SkipRecipeException();

    throw new InvalidRecipeException("Encountered choice-type of " + choice.getClass());
  }

  private List<TypeExclusionRule> getApplyingExclusionRules(Material material, MaterialType materialType) {
    return typeExclusionRules.stream().filter(rule -> rule.matches(material, materialType)).toList();
  }

  public void loadRecipesFromFile() {
    this.unCraftBucketByInputType.clear();

    var loadedCounter = 0;
    var excludedCounter = 0;

    String lastExclusionReason = null;
    int lastExclusionReasonLine = 0;

    try (
      var fileReader = new FileReader(unCraftRecipesFile);
      var scanner = new Scanner(fileReader)
    ) {
      int lineNumber = 0;
      while (scanner.hasNextLine()) {
        ++lineNumber;

        var lineContents = scanner.nextLine().trim();

        if (lineContents.isEmpty())
          continue;

        var wasCommentedOut = false;
        var isExclusionReason = false;

        while (lineContents.startsWith("#")) {
          wasCommentedOut = true;
          lineContents = lineContents.substring(1).trim();

          if (lineContents.startsWith(REASON_MARKER)) {
            isExclusionReason = true;
            lastExclusionReason = lineContents.substring(REASON_MARKER.length()).trim();
            lastExclusionReasonLine = lineNumber;
            break;
          }
        }

        String exclusionReason = null;

        if (wasCommentedOut) {
          if (isExclusionReason)
            continue;

          if (lastExclusionReasonLine == lineNumber - 1)
            exclusionReason = lastExclusionReason;
          else
            exclusionReason = plugin.accessConfigValue("unCraft.additionalReasons.noReasonGiven");
        }

        try {
          var parsedRecipe = RecipeSyntax.tryParseRecipe(lineContents);

          var entry = UnCraftEntry.tryCreateWithScaledSingleUnit(
            parsedRecipe.uncraftedItemAmount(),
            parsedRecipe.uncraftResults(),
            exclusionReason == null ? Collections.emptySet() : Set.of(exclusionReason)
          );

          if (exclusionReason == null) {
            // This check is of utmost importance. As an example, one can use WHITE_DYE with any wool-color
            // as to restore it back to white; the preferred materials mapping then collapses this wildcard
            // to white, as is desired for many other recipes, which then produces: 1 WHITE_WOOL -> 1 WHITE_DYE, 1 WHITE_WOOL.
            // Obviously, that's nonsensical and stems from the fact that this entire recipe is not
            // fit for uncrafting at all; simply throw and let the user remove such edge-cases manually.

            // The above case has been fixed and is automatically filtered out now, but just to be safe, the check will stay.

            for (var resultEntry : parsedRecipe.uncraftResults().entrySet()) {
              if (resultEntry.getKey() != parsedRecipe.uncraftedItemType())
                continue;

              if (resultEntry.getValue() < parsedRecipe.uncraftedItemAmount())
                continue;

              throw new IllegalStateException("Recipe has item " + resultEntry.getKey() + " in outputs, with amount >= the input-amount of " + parsedRecipe.uncraftedItemAmount());
            }
          }

          var unCraftBucket = unCraftBucketByInputType.computeIfAbsent(parsedRecipe.uncraftedItemType(), k -> new ArrayList<>());

          unCraftBucket.add(entry);

          if (!entry.exclusionReasons.isEmpty())
            ++excludedCounter;

           ++loadedCounter;
        } catch (Throwable e) {
          logger.log(Level.WARNING, "Could not load line " + lineNumber + " of file " + unCraftRecipesFile, e);
        }
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to load " + unCraftRecipesFile, e);
    }

    logger.info("Loaded " + loadedCounter + " uncraft-recipes, of which " + excludedCounter + " were excluded (making for " + (loadedCounter - excludedCounter) + " active recipes)");
  }

  private void handleRecipe(@NotNull Recipe recipe, Map<Material, List<UnCraftEntry>> outputBuckets) {
    var exclusionReasons = new HashSet<String>();
    var unCraftResults = new HashMap<Material, Integer>();

    if (recipe instanceof ShapedRecipe shapedRecipe) {
      var choiceMap = shapedRecipe.getChoiceMap();

      for (var shapeRow : shapedRecipe.getShape()) {
        for (var ingredientIndex = 0; ingredientIndex < shapeRow.length(); ++ingredientIndex) {
          var ingredientChar = shapeRow.charAt(ingredientIndex);
          var choice = choiceMap.get(ingredientChar);

          // Empty placeholder char - ignore
          if (choice == null)
            continue;

          addChoiceToUnCraftResults(choice, unCraftResults, exclusionReasons);
        }
      }
    }

    else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
      for (var choice : shapelessRecipe.getChoiceList())
        addChoiceToUnCraftResults(choice, unCraftResults, exclusionReasons);
    }

    // These include coloring shulker-boxes... Why did they do it for them, but not for all other
    // coloring-processes? Unbelievable! I was looking all over the place to find this!
    else if (recipe instanceof TransmuteRecipe transmuteRecipe) {
      // The thing undergoing change
      addChoiceToUnCraftResults(transmuteRecipe.getInput(), unCraftResults, exclusionReasons);

      // The "catalyst" (in the case of shulker-boxes, the dye)
      addChoiceToUnCraftResults(transmuteRecipe.getMaterial(), unCraftResults, exclusionReasons);
    }

    else {
      // BlastingRecipe, SmithingTrimRecipe, SmokerRecipe, FurnaceRecipe, StonecuttingRecipe, etc.
      return;
    }

    var result = recipe.getResult();

    if (result.getType().isAir())
      throw new InvalidRecipeException("Result is air");

    var extractionResult = tryExtractMaterialFromItem(result);
    var recipeResultType = extractionResult.material;

    // Ignored result, probably due to some fancy recipe which hands out complex items
    if (recipeResultType == null)
      return;

    var resultAmount = result.getAmount();

    if (resultAmount <= 0)
      throw new InvalidRecipeException("Amount of result less than or equal to zero");

    for (var exclusionRule : getApplyingExclusionRules(recipeResultType, MaterialType.UNCRAFTED_ITEM))
      exclusionReasons.add(exclusionRule.reason());

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
        exclusionReasons.add(plugin.accessConfigValue("unCraft.additionalReasons.recoloringRecipe"));
    }

    for (var recipeExclusionRule : recipeExclusionRules) {
      if (recipeExclusionRule.matches(recipeResultType, resultAmount, unCraftResults))
        exclusionReasons.add(recipeExclusionRule.reason());
    }

    if (isRecipeIncluded(recipeResultType, unCraftResults.keySet()))
      exclusionReasons.clear();

    var entry = UnCraftEntry.tryCreateWithScaledSingleUnit(resultAmount, unCraftResults, exclusionReasons);

    outputBuckets.computeIfAbsent(recipeResultType, k -> new ArrayList<>()).add(entry);
  }

  private boolean isRecipeIncluded(Material uncraftedItem, Set<Material> unCraftResults) {
    for (var typeInclusionRule : typeInclusionRules) {
      if (typeInclusionRule.matches(uncraftedItem, MaterialType.UNCRAFTED_ITEM))
        return true;

      for (var resultType : unCraftResults) {
        if (typeInclusionRule.matches(resultType, MaterialType.UNCRAFT_RESULT))
          return true;
      }
    }

    return false;
  }

  public void discoverRecipesAndCreateTemplateFile() {
    var localBuckets = new HashMap<Material, List<UnCraftEntry>>();

    for (var iterator = Bukkit.recipeIterator(); iterator.hasNext();) {
      var recipe = iterator.next();

      try {
        handleRecipe(recipe, localBuckets);
      } catch (InvalidRecipeException e) {
        logger.warning("Skipping invalid recipe, reason: " + e.reason + ", recipe: " + recipe);
      } catch (SkipRecipeException ignored) {}
    }

    for (var additionalRecipe : additionalRecipes) {
      var bucket = localBuckets.computeIfAbsent(additionalRecipe.uncraftedItemType(), k -> new ArrayList<>());

      bucket.add(UnCraftEntry.tryCreateWithScaledSingleUnit(
        additionalRecipe.uncraftedItemAmount(),
        additionalRecipe.uncraftResults(),
        Collections.emptySet()
      ));
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

          var recipeLine = unCraftEntry.inputAmount + " " + entry.getKey() + " -> " + result;

          if (!unCraftEntry.exclusionReasons.isEmpty()) {
            writer.write("# " + REASON_MARKER + " " + String.join("; ", unCraftEntry.exclusionReasons) + "\n");
            writer.write("# " + recipeLine + "\n");
            continue;
          }

          writer.write(recipeLine + "\n");
        }
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not generate the uncraft-recipes template-file", e);
    }

    logger.info("Created uncraft-recipes template-file");
  }

  private MaterialExtractionResult tryExtractMaterialFromItem(ItemStack item) {
    var meta = item.getItemMeta();

    if (meta == null)
      return new MaterialExtractionResult(item.getType(), "");

    if (meta instanceof Damageable d && d.hasDamage())
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasDamage"));

    if (meta.hasDisplayName())
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasName"));

    if (meta.hasLore())
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasLore"));

    if (meta.hasEnchants())
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasEnchants"));

    if (meta.hasAttributeModifiers())
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasAttributeModifiers"));

    var pdc = meta.getPersistentDataContainer();

    if (!pdc.getKeys().isEmpty())
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasPdcKeys"));

    var innerItems = getInnerItems(meta);

    // Cannot uncraft items which contain other items, as they would be lost.
    if (innerItems != null && innerItems.stream().anyMatch(inner -> inner != null && !inner.getType().isAir()))
      return new MaterialExtractionResult(null, plugin.accessConfigValue("unCraft.additionalReasons.hasInnerItems"));

    return new MaterialExtractionResult(item.getType(), null);
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

  private void loadConfig() {
    typeExclusionRules.clear();
    loadMapLists("unCraft.typeExclusionRules", TypeExclusionRule::fromConfig, typeExclusionRules);
    logger.info("Loaded " + typeExclusionRules.size() + " uncraft type-exclusion-rules");

    typeInclusionRules.clear();
    loadMapLists("unCraft.typeInclusionRules", TypeInclusionRule::fromConfig, typeInclusionRules);
    logger.info("Loaded " + typeInclusionRules.size() + " uncraft type-inclusion-rules");

    recipeExclusionRules.clear();
    loadMapLists("unCraft.recipeExclusionRules", RecipeExclusionRule::fromConfig, recipeExclusionRules);
    logger.info("Loaded " + recipeExclusionRules.size() + " uncraft recipe-exclusion-rules");

    preferredMaterials.clear();
    loadMapLists("unCraft.preferredMaterials", PreferredMaterial::fromConfig, preferredMaterials);
    logger.info("Loaded " + preferredMaterials.size() + " uncraft preferred materials");

    additionalRecipes.clear();
    for (var recipeString : plugin.getConfiguration().getStringList("unCraft.additionalRecipes")) {
      try {
        additionalRecipes.add(RecipeSyntax.tryParseRecipe(recipeString));
      } catch (Throwable e) {
        logger.warning("Could not parse additional recipe-syntax \"" + recipeString + "\": " + e.getMessage());
      }
    }
    logger.info("Loaded " + additionalRecipes.size() + " additional uncraft-recipes");
  }

  private <T> void loadMapLists(String path, Function<ConfigurationSection, T> mapper, List<T> output) {
    var temporaryConfig = new YamlConfiguration();

    int entryNumber = 0;

    for (Map<?, ?> exclusionRule : plugin.getConfiguration().getMapList(path)) {
      var temporarySection = temporaryConfig.createSection("root", exclusionRule);
      ++entryNumber;

      try {
        output.add(mapper.apply(temporarySection));
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Could not parse the #" + entryNumber + " entry of " + path, e);
      }
    }
  }
}
