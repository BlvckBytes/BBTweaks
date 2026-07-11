package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.homes.storage.HomePoint;
import me.blvckbytes.bbtweaks.homes.storage.HomesStorage;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.item_predicate_parser.translation.TranslatedLangKeyed;
import me.blvckbytes.item_predicate_parser.translation.keyed.LangKeyedItemMaterial;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SetHomeCommand extends HomeCommandBase {

  private final IPPIntegration ippIntegration;

  public SetHomeCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    HomesStorage homesStorage,
    IPPIntegration ippIntegration
  ) {
    super(plugin, config, homesStorage, SetHomeCommandSection.INITIAL_NAME, section -> section.setHomeCommand);

    this.ippIntegration = ippIntegration;
  }

  @Override
  protected void onHomeNameParsedCommand(@NotNull Player sender, @NotNull Command command, @NotNull String label, @Nullable HomeParameter homeParameter, @NotNull String @NotNull [] args) {
    if (homeParameter == null) {
      config.rootSection.homes.setHomeCommand.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return;
    }

    var playerHomes = homesStorage.accessHomes(homeParameter.target());

    var location = sender.getLocation();

    // TODO: Check if location is unsafe

    var homePoint = HomePoint.makeNewWithCurrentStamp(homeParameter.homeName(), location);
    TranslatedLangKeyed<LangKeyedItemMaterial> iconMaterial = null;

    if (args.length == 1) {
      var materialInput = args[0];
      iconMaterial = shortestOrNull(matchMaterialsBySyllables(ippIntegration, sender, materialInput));

      if (iconMaterial == null) {
        config.rootSection.homes.unknownIconMaterial.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("input", materialInput)
        );

        return;
      }

      homePoint.setIcon(iconMaterial.langKeyed.getWrapped());
    }

    var priorHome = playerHomes.setHomeAndGetPriorIfAny(homePoint);

    var environment = new InterpretationEnvironment()
      .withVariable("icon", iconMaterial == null ? null : iconMaterial.normalizedUnPrefixedTranslation)
      .withVariable("home", homePoint.makeEnvironment(config));

    if (priorHome == null) {
      config.rootSection.homes.setHomeCommand.homeSet.sendMessage(sender, environment);
      return;
    }

    config.rootSection.homes.setHomeCommand.homeOverwritten.sendMessage(
      sender,
      environment
        .withVariable("prior_home", priorHome.makeEnvironment(config))
        .withVariable("target", homeParameter.target().playerId().equals(sender.getUniqueId()) ? null : homeParameter.target().lastKnownName())
    );
  }

  @Override
  public @Nullable List<String> onHomeNameParsedTabComplete(@NotNull Player sender, @NotNull Command command, @NotNull String label, @NotNull HomeParameter homeParameter, @NotNull String @NotNull [] args) {
    if (args.length == 1)
      return buildMaterialsSuggestions(ippIntegration, sender, args[0]);

    return List.of();
  }
}
