package me.blvckbytes.bbtweaks.get_username;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class GetUsernameCommand implements CommandHandler {

  private static final Gson GSON = new GsonBuilder().create();

  private final PluginCommand command;

  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  public GetUsernameCommand(
    ConfigKeeper<MainSection> config,
    JavaPlugin plugin
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(GetUsernameCommandSection.INITIAL_NAME));

    this.config = config;
    this.plugin = plugin;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.getUsername.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!hasCommandPermission(sender)) {
      config.rootSection.getUsername.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length != 1) {
      config.rootSection.getUsername.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("command_label", label)
      );

      return true;
    }

    UUID targetId;

    try {
      targetId = UUID.fromString(potentiallyFixUpUuidDashes(args[0]));
    } catch (Throwable e) {
      config.rootSection.getUsername.malformedUuid.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("input", args[0])
      );

      return true;
    }

    var knownOfflinePlayer = Bukkit.getOfflinePlayer(targetId);
    var knownName = knownOfflinePlayer.getName();

    if (knownName != null) {
      config.rootSection.getUsername.usernameResult.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("uuid", targetId.toString())
          .withVariable("username", knownName)
      );

      return true;
    }

    if (targetId.getMostSignificantBits() == 0) {
      config.rootSection.getUsername.unknownFloodgateId.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("uuid", targetId.toString())
      );

      return true;
    }

    try (var client = HttpClient.newHttpClient()) {
      var request = HttpRequest.newBuilder(URI.create(
        "https://sessionserver.mojang.com/session/minecraft/profile/" + targetId.toString().replace("-", "")
      )).build();

      var response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        config.rootSection.getUsername.noMojangResult.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("uuid", targetId.toString())
        );

        return true;
      }

      var responseJson = GSON.fromJson(response.body(), JsonObject.class);

      if (!(responseJson.get("name") instanceof JsonPrimitive namePrimitive))
        throw new IllegalStateException("Missing property \"name\" in Mojang's profile-response");

      config.rootSection.getUsername.usernameResult.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("uuid", targetId.toString())
          .withVariable("username", namePrimitive.getAsString())
      );
    } catch (Throwable e) {
      config.rootSection.getUsername.fetchErrorOccurred.sendMessage(sender);
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to resolve a UUID to a name via Mojang's API", e);
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!hasCommandPermission(sender))
      return List.of();

    if (args.length == 1) {
      var typedId = args[0].replace("-", "");

      return Arrays.stream(Bukkit.getOfflinePlayers())
        .map(player -> player.getUniqueId().toString().replace("-", ""))
        .filter(idString -> StringUtils.startsWithIgnoreCase(idString, typedId))
        .limit(15)
        .map(this::potentiallyFixUpUuidDashes)
        .toList();
    }

    return List.of();
  }

  private String potentiallyFixUpUuidDashes(String input) {
    var hexCharacters = input.replace("-", "");

    if (hexCharacters.length() != 32)
      return input;

    return hexCharacters.substring(0, 8) + "-" +
      hexCharacters.substring(8, 12) + "-" +
      hexCharacters.substring(12, 16) + "-" +
      hexCharacters.substring(16, 20) + "-" +
      hexCharacters.substring(20);
  }
}
