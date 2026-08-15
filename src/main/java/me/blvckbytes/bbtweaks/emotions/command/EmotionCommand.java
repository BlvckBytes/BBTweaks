package me.blvckbytes.bbtweaks.emotions.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.custom_commands.RegisterAdditionalCustomCommandsEvent;
import me.blvckbytes.bbtweaks.emotions.DisplayedMessagesSection;
import me.blvckbytes.bbtweaks.emotions.EmotionSection;
import me.blvckbytes.bbtweaks.emotions.NotificationPart;
import me.blvckbytes.bbtweaks.emotions.NotificationOrigin;
import me.blvckbytes.bbtweaks.emotions.user_profile.EmotionUserProfileStore;
import me.blvckbytes.bbtweaks.integration.discord.DiscordIntegration;
import me.blvckbytes.bbtweaks.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public class EmotionCommand implements CommandHandler, Listener {

  private final PluginCommand command;

  private final JavaPlugin plugin;
  private final EmotionUserProfileStore profileStore;
  private final DiscordIntegration discordIntegration;
  private final ConfigKeeper<MainSection> config;

  public EmotionCommand(
    JavaPlugin plugin,
    EmotionUserProfileStore profileStore,
    DiscordIntegration discordIntegration,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(EmotionCommandSection.INITIAL_NAME));

    this.plugin = plugin;
    this.profileStore = profileStore;
    this.discordIntegration = discordIntegration;
    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.emotion.mainCommand;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.emotion.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.emotion.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      displayOverviewScreen(player, label, 1);
      return true;
    }

    var identifier = args[0];
    Integer overviewPage;

    // While this is not necessarily the cleanest solution, I prefer making fully numeric
    // emotion-identifiers without arguments inaccessible above further increasing the command's complexity.
    if (args.length == 1 && (overviewPage = tryParseInteger(identifier)) != null) {
      displayOverviewScreen(player, label, overviewPage);
      return true;
    }

    var identifierLower = identifier.toLowerCase();
    var emotion = config.rootSection.emotion.emotionByIdentifierLower.get(identifierLower);

    if (emotion == null) {
      config.rootSection.emotion.unknownEmotion.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("input", identifier)
      );

      return true;
    }

    if (!emotion.hasUsePermission(player)) {
      config.rootSection.emotion.missingEmotionPermission.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("emotion_identifier", identifier)
      );

      return true;
    }

    var userProfile = profileStore.accessUserProfile(player);
    var remainingCooldownSeconds = userProfile.getRemainingCooldownSeconds(emotion);

    if (remainingCooldownSeconds > 0) {
      config.rootSection.emotion.awaitRemainingCooldown.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("remaining_time", remainingCooldownSeconds)
          .withVariable("emotion_identifier", identifier)
      );

      return true;
    }

    var isImplicitAllTarget = emotion.doesNoTargetEqualsAll && args.length == 1;

    if (args.length >= 2 || isImplicitAllTarget) {
      var firstEmotionTarget = isImplicitAllTarget ? config.rootSection.emotion.mainCommand.getMainAllSentinel() : args[1];

      if (config.rootSection.emotion.mainCommand.isAllSentinel(firstEmotionTarget)) {
        if (!emotion.supportsAll) {
          config.rootSection.emotion.unsupportedAllTarget.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("emotion_identifier", identifier)
          );

          return true;
        }

        if (args.length > 2) {
          config.rootSection.emotion.cannotCombineAllSentinelWithNames.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("all_sentinel", config.rootSection.emotion.mainCommand.getMainAllSentinel())
          );

          return true;
        }

        if (!playEmotionAll(player, emotion)) {
          config.rootSection.emotion.noReceivingPlayersOnline.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("emotion_identifier", identifier)
          );
          return true;
        }

        userProfile.touchCooldown(emotion);
        return true;
      }

      if (!emotion.supportsOthers) {
        config.rootSection.emotion.unsupportedOtherTarget.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("emotion_identifier", identifier)
        );

        return true;
      }

      var targetPlayers = new HashSet<Player>();

      for (var argsIndex = 1; argsIndex < args.length; ++argsIndex) {
        var targetName = args[argsIndex];

        if (config.rootSection.emotion.mainCommand.isAllSentinel(targetName)) {
          config.rootSection.emotion.cannotCombineAllSentinelWithNames.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("all_sentinel", config.rootSection.emotion.mainCommand.getMainAllSentinel())
          );

          return true;
        }

        var targetPlayer = PlayerUtil.getPlayerByName(targetName);

        if (targetPlayer != null) {
          if (!canPlayEmotionAt(player, targetPlayer))
            targetPlayer = null;
        }

        if (targetPlayer == null || !targetPlayer.isOnline()) {
          config.rootSection.emotion.receivingPlayerNotOnline.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("target_player", targetName)
          );

          return true;
        }

        if (targetPlayer.equals(player)) {
          config.rootSection.emotion.receiverCannotBeSelf.sendMessage(player);
          return true;
        }

        if (!targetPlayers.add(targetPlayer)) {
          config.rootSection.emotion.receivingPlayerDuplicate.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("target_player", targetName)
          );

          return true;
        }
      }

      if (targetPlayers.size() == 1) {
        playEmotionOther(player, targetPlayers.iterator().next(), emotion);

        userProfile.touchCooldown(emotion);
        return true;
      }

      if (targetPlayers.size() > emotion.maximumNumberOfTargets) {
        config.rootSection.emotion.maximumNumberOfTargetsExceeded.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("emotion_identifier", identifier)
            .withVariable("maximum_count", emotion.maximumNumberOfTargets)
        );

        return true;
      }

      playEmotionMany(player, targetPlayers, emotion);

      userProfile.touchCooldown(emotion);
      return true;
    }

    if (!emotion.supportsSelf) {
      config.rootSection.emotion.unsupportedPlayingOnSelf.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("emotion_identifier", args[0])
      );

      return true;
    }

    playEmotionSelf(player, emotion);

    userProfile.touchCooldown(emotion);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return List.of();

    if (!command.testPermission(player))
      return List.of();

    if (args.length == 1) {
      return config.rootSection.emotion.emotionByIdentifier.keySet()
        .stream()
        .filter(it -> StringUtils.containsIgnoreCase(it, args[0]))
        .toList();
    }

    var identifierLower = args[0].toLowerCase();
    var emotion = config.rootSection.emotion.emotionByIdentifierLower.get(identifierLower);

    if (emotion == null)
      return List.of();

    if (!emotion.hasUsePermission(player))
      return List.of();

    if (!(emotion.supportsOthers || emotion.supportsAll))
      return List.of();

    if (args.length - 1 > emotion.maximumNumberOfTargets)
      return List.of();

    // The all-sentinel may not be followed up by any additional names, as that would be illogical
    if (config.rootSection.emotion.mainCommand.isAllSentinel(args[1]))
      return List.of();

    var selectedPlayerIds = new HashSet<UUID>();

    for (var index = 1; index < args.length; ++index) {
      var target = PlayerUtil.getPlayerByName(args[index]);

      if (target != null)
        selectedPlayerIds.add(target.getUniqueId());
    }

    var lastArg = args[args.length - 1];

    var suggestedNames = PlayerUtil.suggestPlayerNames(lastArg, candidate -> {
      if (candidate == player)
        return false;

      if (!canPlayEmotionAt(player, candidate))
        return false;

      return !selectedPlayerIds.contains(candidate.getUniqueId());
    });

    // Only suggest the all-sentinels on the first target-name
    if (emotion.supportsAll && args.length == 2)
      config.rootSection.emotion.mainCommand.addAllSentinelSuggestions(args[1], suggestedNames);

    return suggestedNames;
  }

  @EventHandler
  public void onRegisterAdditionalCommands(RegisterAdditionalCustomCommandsEvent event) {
    for (var emotionEntry : config.rootSection.emotion.emotionByIdentifier.entrySet()) {
      var emotion = emotionEntry.getValue();

      if (!emotion.tryRegisterDirectly)
        continue;

      var emotionIdentifier = emotionEntry.getKey();

      var commandNames = new HashSet<>(emotion.directAliases);
      commandNames.add(emotionIdentifier);

      for (var commandName : commandNames)
        event.addCommand(new DirectEmotionCommand(commandName, emotionIdentifier, this));
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean canPlayEmotionAt(Player sender, Player other) {
    return sender.canSee(other) || sender.hasPermission("bbtweaks.emotion.bypass-hidden");
  }

  private void displayOverviewScreen(Player player, String commandLabel, int page) {
    List<InterpretationEnvironment> helpScreenEnvironments = new ArrayList<>();
    var mismatchedPermission = false;

    for (var emotionEntry : config.rootSection.emotion.emotionByIdentifier.entrySet()) {
      var emotion = emotionEntry.getValue();
      var emotionIdentifier = emotionEntry.getKey();

      if (!emotion.hasUsePermission(player)) {
        mismatchedPermission = true;
        continue;
      }

      var aliases = new ArrayList<>(emotion.directAliases);

      if (emotion.tryRegisterDirectly)
        aliases.add(emotionIdentifier.toLowerCase());

      helpScreenEnvironments.add(new HelpScreenEntry(
        emotionIdentifier,
        emotion.description.interpret(SlotType.SINGLE_LINE_CHAT, null).getFirst(),
        aliases,
        emotion.supportsSelf,
        emotion.supportsOthers,
        emotion.supportsAll
      ).makeEnvironment());
    }

    if (mismatchedPermission && helpScreenEnvironments.isEmpty()) {
      config.rootSection.emotion.noAccessToAnyEmotion.sendMessage(player);
      return;
    }

    int pageSize = config.rootSection.emotion.mainCommand.paginationSize;
    int numberOfPages = (helpScreenEnvironments.size() + (pageSize - 1)) / pageSize;

    if (page > numberOfPages)
      page = numberOfPages;

    if (page <= 0)
      page = 1;

    if (helpScreenEnvironments.size() > pageSize) {
      var firstIndex = (page - 1) * pageSize;
      var lastIndex = Math.min(helpScreenEnvironments.size(), firstIndex + pageSize);
      helpScreenEnvironments = helpScreenEnvironments.subList(firstIndex, lastIndex);
    }

    config.rootSection.emotion.commandEmotionHelpScreen.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("number_of_pages", numberOfPages)
        .withVariable("current_page", page)
        .withVariable("page_size", pageSize)
        .withVariable("label", commandLabel)
        .withVariable("all_sentinel", config.rootSection.emotion.mainCommand.getMainAllSentinel())
        .withVariable("emotions", helpScreenEnvironments)
    );
  }

  private @Nullable Integer tryParseInteger(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private boolean playEmotionAll(Player sender, EmotionSection emotion) {
    if (Bukkit.getOnlinePlayers().size() == 1)
      return false;

    var messageEnvironment = makeMessageEnvironment(sender, true);
    var messages = emotion.accessAtOthersMessages();

    for (var receiver : Bukkit.getOnlinePlayers()) {
      var isSender = receiver.equals(sender);

      addReceiverVariables(receiver, messageEnvironment);

      if (messages.asBroadcast != null) {
        var origin = isSender ? NotificationOrigin.IS_SENDER : NotificationOrigin.BROADCAST;
        displayMessages(receiver, origin, messageEnvironment, messages.asBroadcast);
      }

      if (isSender)
        continue;

      playEmotionSound(receiver, NotificationOrigin.TARGETED_VIA_ALL, emotion);

      for (var effect : emotion.effects)
        effect.playEffect(receiver, NotificationOrigin.TARGETED_VIA_ALL, profileStore, plugin);

      if (messages.toReceiver != null)
        displayMessages(receiver, NotificationOrigin.TARGETED_VIA_ALL, messageEnvironment, messages.toReceiver);
    }

    if (messages.asBroadcast != null)
      possiblyBroadcastToConsole(emotion, messages.asBroadcast, messageEnvironment);

    for (var effect : emotion.effects)
      effect.playEffect(sender, NotificationOrigin.IS_SENDER, profileStore, plugin);

    playEmotionSound(sender, NotificationOrigin.IS_SENDER, emotion);

    if (messages.toSender != null)
      displayMessages(sender, NotificationOrigin.IS_SENDER, messageEnvironment, messages.toSender);

    if (messages.toDiscord != null)
      discordIntegration.sendMessage(messages.toDiscord.asPlainString(messageEnvironment));

    return true;
  }

  private void playEmotionMany(Player sender, Collection<Player> receivers, EmotionSection emotion) {
    var receiverNames = new ArrayList<String>(receivers.size());
    var receiverDisplayNames = new ArrayList<Component>(receivers.size());
    var messages = emotion.accessAtOthersMessages();

    for (var receiver : receivers) {
      receiverNames.add(receiver.getName());
      receiverDisplayNames.add(receiver.displayName());
    }

    var messageEnvironment = makeMessageEnvironment(sender, false)
      .withVariable("receivers_names", receiverNames)
      .withVariable("receivers_display_names", receiverDisplayNames);

    if (messages.asBroadcast != null) {
      for (var broadcastReceiver : Bukkit.getOnlinePlayers()) {
        var origin = NotificationOrigin.BROADCAST;

        if (sender.equals(broadcastReceiver))
          origin = NotificationOrigin.IS_SENDER;
        else if (receivers.contains(broadcastReceiver))
          origin = NotificationOrigin.TARGETED_DIRECTLY;

        displayMessages(broadcastReceiver, origin, messageEnvironment, messages.asBroadcast);
      }

      possiblyBroadcastToConsole(emotion, messages.asBroadcast, messageEnvironment);
    }

    for (var receiver : receivers) {
      addReceiverVariables(receiver, messageEnvironment);

      playEmotionSound(receiver, NotificationOrigin.TARGETED_DIRECTLY, emotion);

      for (var effect : emotion.effects)
        effect.playEffect(receiver, NotificationOrigin.TARGETED_DIRECTLY, profileStore, plugin);

      if (messages.toReceiver != null)
        displayMessages(receiver, NotificationOrigin.TARGETED_DIRECTLY, messageEnvironment, messages.toReceiver);
    }

    playEmotionSound(sender, NotificationOrigin.IS_SENDER, emotion);

    for (var effect : emotion.effects)
      effect.playEffect(sender, NotificationOrigin.IS_SENDER, profileStore, plugin);

    if (messages.toSender != null)
      displayMessages(sender, NotificationOrigin.IS_SENDER, messageEnvironment, messages.toSender);

    if (messages.toDiscord != null)
      discordIntegration.sendMessage(messages.toDiscord.asPlainString(messageEnvironment));
  }

  private void addReceiverVariables(Player receiver, InterpretationEnvironment environment) {
    environment
      .withVariable("receiver_name", receiver.getName())
      .withVariable("receiver_display_name", receiver.displayName());
  }

  private InterpretationEnvironment makeMessageEnvironment(Player sender, boolean isAtAll) {
    return new InterpretationEnvironment()
      .withVariable("is_at_all", isAtAll)
      .withVariable("sender_name", sender.getName())
      .withVariable("sender_display_name", sender.displayName());
  }

  private void displayMessages(
    Player receiver,
    NotificationOrigin origin,
    InterpretationEnvironment messageEnvironment,
    DisplayedMessagesSection messages
  ) {
    var userProfile = profileStore.accessUserProfile(receiver);

    if (messages.actionBarMessage != null && userProfile.doesReceive(NotificationPart.ACTION_BAR, origin))
      messages.actionBarMessage.sendActionBar(receiver, messageEnvironment);

    if (messages.chatMessage != null && userProfile.doesReceive(NotificationPart.CHAT, origin))
      messages.chatMessage.sendMessage(receiver, messageEnvironment);

    if (userProfile.doesReceive(NotificationPart.TITLE, origin)) {
      if (messages.titleMessage != null || messages.subTitleMessage != null) {
        if (messages.titleMessage != null) {
          receiver.sendTitlePart(
            TitlePart.TITLE,
            messages.titleMessage.interpret(SlotType.SINGLE_LINE_CHAT, messageEnvironment).getFirst()
          );
        }

        if (messages.subTitleMessage != null) {
          receiver.sendTitlePart(
            TitlePart.SUBTITLE,
            messages.subTitleMessage.interpret(SlotType.SINGLE_LINE_CHAT, messageEnvironment).getFirst()
          );
        }

        receiver.sendTitlePart(
          TitlePart.TIMES,
          Title.Times.times(
            Duration.ofMillis(messages.titleFadeIn * 50L),
            Duration.ofMillis(messages.titleStay * 50L),
            Duration.ofMillis(messages.titleFadeOut * 50L)
          )
        );
      }
    }
  }

  private void playEmotionSound(
    Player receiver,
    NotificationOrigin origin,
    EmotionSection emotion
  ) {
    if (emotion._sound == null)
      return;

    var userProfile = profileStore.accessUserProfile(receiver);

    if (userProfile.doesReceive(NotificationPart.SOUND, origin))
      emotion._sound.play(receiver, (float) emotion.soundVolume, (float) emotion.soundPitch);
  }

  private void playEmotionOther(Player sender, Player receiver, EmotionSection emotion) {
    var messageEnvironment = makeMessageEnvironment(sender, false);
    addReceiverVariables(receiver, messageEnvironment);
    var messages = emotion.accessAtOthersMessages();

    if (messages.asBroadcast != null) {
      for (var broadcastReceiver : Bukkit.getOnlinePlayers()) {
        var origin = NotificationOrigin.BROADCAST;

        if (sender.equals(broadcastReceiver))
          origin = NotificationOrigin.IS_SENDER;
        else if (receiver.equals(broadcastReceiver))
          origin = NotificationOrigin.TARGETED_DIRECTLY;

        displayMessages(broadcastReceiver, origin, messageEnvironment, messages.asBroadcast);
      }

      possiblyBroadcastToConsole(emotion, messages.asBroadcast, messageEnvironment);
    }

    playEmotionSound(receiver, NotificationOrigin.TARGETED_DIRECTLY, emotion);

    if (messages.toReceiver != null)
      displayMessages(receiver, NotificationOrigin.TARGETED_DIRECTLY, messageEnvironment, messages.toReceiver);

    for (var effect : emotion.effects) {
      effect.playEffect(sender, NotificationOrigin.IS_SENDER, profileStore, plugin);
      effect.playEffect(receiver, NotificationOrigin.TARGETED_DIRECTLY, profileStore, plugin);
    }

    playEmotionSound(sender, NotificationOrigin.IS_SENDER, emotion);

    if (messages.toSender != null)
      displayMessages(sender, NotificationOrigin.IS_SENDER, messageEnvironment, messages.toSender);

    if (messages.toDiscord != null)
      discordIntegration.sendMessage(messages.toDiscord.asPlainString(messageEnvironment));
  }

  private void playEmotionSelf(Player sender, EmotionSection emotion) {
    var messageEnvironment = makeMessageEnvironment(sender, false);
    var messages = emotion.accessAtSelfMessages();

    if (messages.asBroadcast != null) {
      for (var broadcastReceiver : Bukkit.getOnlinePlayers()) {
        var origin = NotificationOrigin.BROADCAST;

        if (sender.equals(broadcastReceiver))
          origin = NotificationOrigin.IS_SENDER;

        displayMessages(broadcastReceiver, origin, messageEnvironment, messages.asBroadcast);
      }

      possiblyBroadcastToConsole(emotion, messages.asBroadcast, messageEnvironment);
    }

    for (var effect : emotion.effects)
      effect.playEffect(sender, NotificationOrigin.IS_SENDER, profileStore, plugin);

    playEmotionSound(sender, NotificationOrigin.IS_SENDER, emotion);

    if (messages.toSender != null)
      displayMessages(sender, NotificationOrigin.IS_SENDER, messageEnvironment, messages.toSender);

    if (messages.toDiscord != null)
      discordIntegration.sendMessage(messages.toDiscord.asPlainString(messageEnvironment));
  }

  private void possiblyBroadcastToConsole(
    EmotionSection emotion,
    @Nullable DisplayedMessagesSection broadcastMessages,
    InterpretationEnvironment messageEnvironment
  ) {
    if (broadcastMessages == null || !emotion.broadcastToConsole)
      return;

    if (broadcastMessages.chatMessage == null)
      return;

    broadcastMessages.chatMessage.sendMessage(Bukkit.getConsoleSender(), messageEnvironment);
  }
}
