package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SingleBottleXpCommand extends BaseBottleXpCommand implements Listener {

  private final PluginCommand command;

  private final NamespacedKey keyExperienceValue;

  public SingleBottleXpCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    super(config, () -> config.rootSection.bottleXp.singleBottleCommand.experienceOverview);

    this.command = Objects.requireNonNull(plugin.getCommand(SingleBottleXpCommandSection.INITIAL_NAME));

    this.keyExperienceValue = new NamespacedKey(plugin, "single-bottle-xp-value");
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.bottleXp.singleBottleCommand;
  }

  @Override
  protected void handleBottling(
    Player player,
    String label,
    String[] args,
    int maximumExperience,
    int availableExperience,
    InterpretationEnvironment environment
  ) {
    if (args.length > 1) {
      config.rootSection.bottleXp.singleBottleCommand.commandUsage.sendMessage(
        player,
        environment
          .withVariable("label", label)
      );

      return;
    }

    var bottleItem = makeBottleItem(maximumExperience);

    if (!player.getInventory().addItem(bottleItem).isEmpty()) {
      config.rootSection.bottleXp.singleBottleCommand.cannotHoldBottle.sendMessage(player);
      return;
    }

    var levelBefore = player.getLevel();
    setExperience(player, availableExperience - maximumExperience);
    var levelAfter = player.getLevel();

    config.rootSection.bottleXp.singleBottleCommand.afterBottling.sendMessage(
      player,
      environment
        .withVariable("bottled_experience", maximumExperience)
        .withVariable("level_before", levelBefore)
        .withVariable("level_after", levelAfter)
    );
  }

  @Override
  protected List<String> handleRemainingTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onExpBottle(ExpBottleEvent event) {
    var bottle = event.getEntity();
    var bottlePdc = bottle.getItem().getPersistentDataContainer();
    var experienceValue = bottlePdc.get(keyExperienceValue, PersistentDataType.INTEGER);

    if (experienceValue == null)
      return;

    event.setCancelled(true);
    event.setExperience(0);

    bottle.getWorld().spawn(
      bottle.getLocation(),
      ExperienceOrb.class,
      orb -> orb.setExperience(experienceValue)
    );
  }

  private ItemStack makeBottleItem(int experience) {
    var item = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
    var meta = Objects.requireNonNull(item.getItemMeta());

    meta.lore(
      config.rootSection.bottleXp.singleBottleCommand.bottleLore.interpret(
        SlotType.ITEM_LORE,
        new InterpretationEnvironment()
          .withVariable("experience", experience)
      )
    );

    meta.getPersistentDataContainer().set(keyExperienceValue, PersistentDataType.INTEGER, experience);

    item.setItemMeta(meta);

    return item;
  }
}
