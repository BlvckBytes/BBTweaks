package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.homes.storage.HomesStorage;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HomeCommand extends HomeCommandBase {

  private final IPPIntegration ippIntegration;

  public HomeCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    HomesStorage homesStorage,
    IPPIntegration ippIntegration
  ) {
    super(plugin, config, homesStorage, HomeCommandSection.INITIAL_NAME, section -> section.homeCommand);

    this.ippIntegration = ippIntegration;
  }

  @Override
  protected void onHomeNameParsedCommand(@NotNull Player sender, @NotNull Command command, @NotNull String label, @Nullable HomeParameter homeParameter, @NotNull String @NotNull [] args) {
    if (homeParameter == null) {
      var playerHomes = homesStorage.accessHomes(sender);

      config.rootSection.homes.homeCommand.homeList.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("homes", playerHomes.getHomes().stream().map(home -> home.makeEnvironment(config)).toList())
      );

      return;
    }

    var playerHomes = homesStorage.accessHomes(homeParameter.target());
    var targetHome = playerHomes.getHomeByName(homeParameter.homeName());
    var isSelf = homeParameter.target().playerId().equals(sender.getUniqueId());

    if (targetHome == null) {
      config.rootSection.homes.homeCommand.homeDoesNotExist.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("target", isSelf ? null : homeParameter.target().lastKnownName())
          .withVariable("name", homeParameter.homeName())
      );

      return;
    }

    if (args.length == 0) {
      // TODO: Cancel if destination is unsafe and "unsafe" sentinel parameter is not provided

      if (isSelf)
        targetHome.incrementUsageCount();

      return;
    }

    var normalizedAction = HomeAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null) {
      config.rootSection.homes.homeCommand.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", HomeAction.matcher.createCompletions(null))
      );
      return;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("home", targetHome.makeEnvironment(config));

    switch (normalizedAction.constant) {
      case RENAME -> {
        if (args.length != 2) {
          config.rootSection.homes.homeCommand.renameUsage.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return;
        }

        var newHomeName = args[1];

        if (reportInvalidHomeName(sender, newHomeName))
          return;

        var priorName = targetHome.getHomeName();

        var existingHome = playerHomes.getHomeByName(newHomeName);

        if (existingHome != null) {
          config.rootSection.homes.homeCommand.newHomeNameAlreadyExists.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("home", existingHome.makeEnvironment(config))
          );

          return;
        }

        targetHome.setHomeName(newHomeName);

        config.rootSection.homes.homeCommand.homeRenamed.sendMessage(
          sender,
          environment
            .withVariable("prior_name", priorName)
        );
      }

      case SET_ICON -> {
        if (args.length != 2) {
          config.rootSection.homes.homeCommand.setIconUsage.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );
          return;
        }

        var materialInput = args[1];
        var matchedMaterial = shortestOrNull(matchMaterialsBySyllables(ippIntegration, sender, materialInput));

        if (matchedMaterial == null) {
          config.rootSection.homes.unknownIconMaterial.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("input", materialInput)
          );
          return;
        }

        targetHome.setIcon(matchedMaterial.langKeyed.getWrapped());

        config.rootSection.homes.homeCommand.setIconMaterial.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("home", targetHome.makeEnvironment(config))
            .withVariable("icon", matchedMaterial.normalizedUnPrefixedTranslation)
        );
      }

      case REMOVE_ICON -> {
        if (targetHome.getIcon() == null) {
          config.rootSection.homes.homeCommand.noIconSet.sendMessage(sender, environment);
          return;
        }

        targetHome.setIcon(null);
        config.rootSection.homes.homeCommand.iconRemoved.sendMessage(sender, environment);
      }

      case MARK_FAVORITE -> {
        if (args.length != 2) {
          config.rootSection.homes.homeCommand.markFavoriteUsage.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );
          return;
        }

        int favoriteNumber;

        try {
          favoriteNumber = Integer.parseInt(args[1]);
        } catch (Throwable e) {
          config.rootSection.homes.homeCommand.malformedFavoriteNumber.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("input", args[1])
          );
          return;
        }

        targetHome.setFavoriteNumber(favoriteNumber);
        config.rootSection.homes.homeCommand.setFavoriteNumber.sendMessage(sender, environment);
      }

      case REMOVE_FAVORITE -> {
        if (targetHome.getFavoriteNumber() == null) {
          config.rootSection.homes.homeCommand.notMarkedAsFavorite.sendMessage(sender, environment);
          return;
        }

        targetHome.setFavoriteNumber(null);
        config.rootSection.homes.homeCommand.removedFavoriteNumber.sendMessage(sender, environment);
      }
    }
  }

  @Override
  public @Nullable List<String> onHomeNameParsedTabComplete(@NotNull Player sender, @NotNull Command command, @NotNull String label, @NotNull HomeParameter homeParameter, @NotNull String @NotNull [] args) {
    if (args.length == 1)
      return HomeAction.matcher.createCompletions(args[0]);

    if (args.length == 2) {
      var normalizedAction = HomeAction.matcher.matchFirst(args[0]);

      if (normalizedAction != null && normalizedAction.constant == HomeAction.SET_ICON)
        return buildMaterialsSuggestions(ippIntegration, sender, args[0]);
    }

    return List.of();
  }
}
