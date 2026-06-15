package me.blvckbytes.bbtweaks.sign_copier;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.auto_wirer.LateWired;
import me.blvckbytes.bbtweaks.sign_copier.settings.SettingFlag;
import me.blvckbytes.bbtweaks.sign_copier.settings.SignCopierSettings;
import me.blvckbytes.bbtweaks.sign_copier.settings.SignCopierSettingsStore;
import me.blvckbytes.bbtweaks.sign_copier.settings_display.SignCopierSettingsDisplayHandler;
import me.blvckbytes.bbtweaks.util.AmpersandNotationTranslator;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class SignCopyCommand implements CommandHandler, Listener {

  public static final int NUMBER_OF_LINES = 4;

  private final SignCopierSettingsStore settingsStore;
  private final SignCopierSettingsDisplayHandler settingsDisplayHandler;
  private final Plugin plugin;
  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey[] keysLineContents, keysLineIsPlain;
  private final NamespacedKey keyIsGlowing, keySignColor;

  private @LateWired SignEditCommand signEditCommand;

  public SignCopyCommand(
    SignCopierSettingsStore settingsStore,
    SignCopierSettingsDisplayHandler settingsDisplayHandler,
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.settingsStore = settingsStore;
    this.settingsDisplayHandler = settingsDisplayHandler;
    this.plugin = plugin;
    this.command = Objects.requireNonNull(plugin.getCommand(SignCopyCommandSection.INITIAL_NAME));

    this.config = config;

    this.keysLineContents = new NamespacedKey[NUMBER_OF_LINES];
    this.keysLineIsPlain = new NamespacedKey[NUMBER_OF_LINES];

    for (var lineIndex = 0; lineIndex < NUMBER_OF_LINES; ++lineIndex) {
      this.keysLineContents[lineIndex] = new NamespacedKey(plugin, "sign-copier-line-contents-" + lineIndex);
      this.keysLineIsPlain[lineIndex] = new NamespacedKey(plugin, "sign-copier-line-is-plain-" + lineIndex);
    }

    this.keyIsGlowing = new NamespacedKey(plugin, "sign-copier-is-glowing");
    this.keySignColor = new NamespacedKey(plugin, "sign-copier-sign-color");
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.signCopier.signCopyCommand;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.signCopier.playersOnly.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> action;

    if (args.length == 0 || (action = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.signCopier.helpScreen.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("sign_edit_command", signEditCommand.getCommand().getName())
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    var pdc = player.getPersistentDataContainer();

    if (action.constant == CommandAction.CLEAR) {
      if (!pdc.has(keysLineContents[0])) {
        config.rootSection.signCopier.noSignCopied.sendMessage(player);
        return true;
      }

      for (var lineIndex = 0; lineIndex < NUMBER_OF_LINES; ++lineIndex) {
        if (pdc.has(keysLineContents[lineIndex]))
          pdc.remove(keysLineContents[lineIndex]);

        if (pdc.has(keysLineIsPlain[lineIndex]))
          pdc.remove(keysLineIsPlain[lineIndex]);
      }

      config.rootSection.signCopier.copiedSignRemoved.sendMessage(player);
      return true;
    }

    if (action.constant == CommandAction.EDIT || action.constant == CommandAction.EDIT_PLAIN) {
      if (args.length < 2) {
        config.rootSection.signCopier.actionEditUsage.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("label", label)
            .withVariable("action", action.getNormalizedName())
        );

        return true;
      }

      handleEditAction(player, action.constant == CommandAction.EDIT_PLAIN, args, 1);

      return true;
    }

    if (action.constant == CommandAction.PREVIEW) {
      handlePreviewAction(player);
      return true;
    }

    if (action.constant == CommandAction.SETTINGS) {
      settingsDisplayHandler.show(player, settingsStore.accessSettings(player));
      return true;
    }

    if (action.constant == CommandAction.COPY || action.constant == CommandAction.PASTE) {
      var hitResult = player.getWorld().rayTraceBlocks(
        player.getEyeLocation(),
        player.getEyeLocation().getDirection(),
        5, FluidCollisionMode.NEVER, false
      );

      Block block;

      if (hitResult == null || (block = hitResult.getHitBlock()) == null || !(block.getState() instanceof Sign sign)) {
        config.rootSection.signCopier.notLookingAtSign.sendMessage(player);
        return true;
      }

      handleCopyPasteAction(player, sign, action.constant == CommandAction.COPY);
      return true;
    }

    throw new IllegalArgumentException("Unimplemented action: " + action.constant);
  }

  public void handlePreviewAction(Player player) {
    var copiedLines = getCopiedLines(player.getPersistentDataContainer());

    if (copiedLines == null) {
      config.rootSection.signCopier.noSignCopied.sendMessage(player);
      return;
    }

    config.rootSection.signCopier.previewScreen.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("lines", copiedLines)
    );
  }

  public void handleEditAction(Player player, boolean isPlain, String[] args, int argsOffset) {
    var pdc = player.getPersistentDataContainer();

    if (!pdc.has(keysLineContents[0])) {
      config.rootSection.signCopier.noSignCopied.sendMessage(player);
      return;
    }

    int lineNumber;

    try {
      lineNumber = Integer.parseInt(args[argsOffset]);

      if (lineNumber <= 0)
        throw new IllegalArgumentException();
    } catch (Throwable e) {
      config.rootSection.signCopier.malformedLineNumber.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("input", args[argsOffset])
      );

      return;
    }

    if (lineNumber > NUMBER_OF_LINES) {
      config.rootSection.signCopier.outOfBoundsLineNumber.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("line_number", lineNumber)
          .withVariable("max_line_number", NUMBER_OF_LINES)
      );

      return;
    }

    if (args.length == argsOffset + 1) {
      config.rootSection.signCopier.setLineToBlank.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("line_number", lineNumber)
      );

      pdc.set(keysLineContents[lineNumber - 1], PersistentDataType.STRING, "");
      return;
    }

    var contentsJoiner = new StringJoiner(" ");

    for (var argIndex = argsOffset + 1; argIndex < args.length; ++argIndex)
      contentsJoiner.add(args[argIndex]);

    var contents = contentsJoiner.toString();

    pdc.set(keysLineContents[lineNumber - 1], PersistentDataType.STRING, contents);
    pdc.set(keysLineIsPlain[lineNumber - 1], PersistentDataType.BOOLEAN, isPlain);

    var renderedContents = isPlain ? Component.text(contents) : renderLine(contents);

    config.rootSection.signCopier.setLineToContents.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("line_number", lineNumber)
        .withVariable("contents", renderedContents)
    );
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    if (args.length == 2) {
      if (normalizedAction.constant == CommandAction.EDIT || normalizedAction.constant == CommandAction.EDIT_PLAIN)
        return IntStream.range(1, NUMBER_OF_LINES + 1)
          .mapToObj(String::valueOf)
          .filter(it -> it.startsWith(args[1]))
          .toList();
    }

    return List.of();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInteract(PlayerInteractEvent event) {
    if (event.useInteractedBlock() == Event.Result.DENY && !config.rootSection.signCopier.alsoHandleCancelledInteractions)
      return;

    var action = event.getAction();

    if (!action.isRightClick() && !action.isLeftClick())
      return;

    if (event.getItem() == null || event.getItem().getType() != Material.INK_SAC)
      return;

    var block = event.getClickedBlock();

    if (block == null || !Tag.ALL_SIGNS.isTagged(block.getType()))
      return;

    var settings = settingsStore.accessSettings(event.getPlayer());

    if (!settings.flags.contains(SettingFlag.INK_SAC_AS_SHORTCUT))
      return;

    if (!(block.getState() instanceof Sign sign))
      return;

    event.setCancelled(true);

    handleCopyPasteAction(event.getPlayer(), sign, event.getAction().isRightClick());
  }

  private void handleCopyPasteAction(Player player, Sign sign, boolean copy) {
    var settings = settingsStore.accessSettings(player);

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    if (copy) {
      copySign(player, sign);

      if (settings.flags.contains(SettingFlag.SEND_COPIED_MESSAGE))
        config.rootSection.signCopier.signCopied.sendMessage(player, environment);

      return;
    }

    var pasteError = pasteSign(player, settings, sign);

    if (pasteError == null) {
      sign.update(true, false);

      if (settings.flags.contains(SettingFlag.SEND_PASTED_MESSAGE))
        config.rootSection.signCopier.signPasted.sendMessage(player, environment);

      return;
    }

    (switch (pasteError) {
      case NO_LINES_AVAILABLE -> config.rootSection.signCopier.noSignCopied;
      case CHANGE_WAS_CANCELLED -> config.rootSection.signCopier.signPasteWasCancelled;
    }).sendMessage(player, environment);
  }

  private @Nullable PasteSignError pasteSign(Player player, SignCopierSettings settings, Sign sign) {
    var pdc = player.getPersistentDataContainer();

    var copiedLines = getCopiedLines(pdc);

    if (copiedLines == null)
      return PasteSignError.NO_LINES_AVAILABLE;

    var renderedLines = new ArrayList<Component>(copiedLines.size());

    for (var copiedLine : copiedLines)
      renderedLines.add(copiedLine.renderedLine());

    var signSide = sign.getTargetSide(player);

    // The SignSide is unaware of its Sade and getting the target-side is rather involved,
    // so I'd rather use this little "hack" (they're accessed directly, by reference).
    var side = signSide == sign.getSide(Side.FRONT) ? Side.FRONT : Side.BACK;

    //noinspection UnstableApiUsage
    var changeEvent = new SignChangeEvent(sign.getBlock(), player, renderedLines, side);

    callChangeEventWithExclusions(changeEvent);

    if (changeEvent.isCancelled())
      return PasteSignError.CHANGE_WAS_CANCELLED;

    var finalLines = changeEvent.lines();

    for (var lineIndex = 0; lineIndex < finalLines.size(); ++lineIndex)
      signSide.line(lineIndex, finalLines.get(lineIndex));

    if (settings.flags.contains(SettingFlag.PASTE_SIGN_GLOWING)) {
      var isGlowingValue = pdc.get(keyIsGlowing, PersistentDataType.BOOLEAN);

      if (isGlowingValue != null)
        signSide.setGlowingText(isGlowingValue);
    }

    if (settings.flags.contains(SettingFlag.PASTE_SIGN_COLOR))
      signSide.setColor(getDyeColor(pdc));

    return null;
  }

  private @Nullable DyeColor getDyeColor(PersistentDataContainer pdc) {
    var signColorValue = pdc.get(keySignColor, PersistentDataType.STRING);

    if (signColorValue == null)
      return null;

    try {
      return DyeColor.valueOf(signColorValue);
    } catch (Throwable e) {
      return null;
    }
  }

  private void callChangeEventWithExclusions(SignChangeEvent event) {
    var skippedHandlers = config.rootSection.signCopier.skippedChangeEventHandlers;

    for (var listener : event.getHandlers().getRegisteredListeners()) {
      var className = listener.getListener().getClass().getName();

      if (skippedHandlers.stream().anyMatch(it -> StringUtils.equalsIgnoreCase(it, className)))
        continue;

      try {
        listener.callEvent(event);
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + listener.getPlugin().getName(), e);
      }
    }
  }

  // NOTE: These do not account for \& by design, as to give the player a way to escape whenever needed.

  private String escapeAmpersands(String input) {
    return input.replace("&", "\\&");
  }

  private String unescapeAmpersands(String input) {
    return input.replace("\\&", "&");
  }

  private void copySign(Player player, Sign sign) {
    var pdc = player.getPersistentDataContainer();

    var signSide = sign.getTargetSide(player);
    var lines = signSide.lines();

    for (var lineIndex = 0; lineIndex < NUMBER_OF_LINES; ++lineIndex) {
      if (lineIndex >= lines.size())
        break;

      var contents = escapeAmpersands(MiniMessage.miniMessage().serialize(lines.get(lineIndex)));

      pdc.set(keysLineContents[lineIndex], PersistentDataType.STRING, contents);
      pdc.set(keysLineIsPlain[lineIndex], PersistentDataType.BOOLEAN, false);
    }

    pdc.set(keyIsGlowing, PersistentDataType.BOOLEAN, signSide.isGlowingText());

    var signColor = signSide.getColor();

    if (signColor == null)
      pdc.remove(keySignColor);
    else
      pdc.set(keySignColor, PersistentDataType.STRING, signColor.name());
  }

  private Component renderLine(String contents) {
    contents = AmpersandNotationTranslator.translateToTagNotation(contents);
    contents = unescapeAmpersands(contents);
    return MiniMessage.miniMessage().deserialize(contents);
  }

  private @Nullable List<CopiedLine> getCopiedLines(PersistentDataContainer pdc) {
    if (!pdc.has(keysLineContents[0]))
      return null;

    var copiedLines = new ArrayList<CopiedLine>(NUMBER_OF_LINES);

    for (var lineIndex = 0; lineIndex < NUMBER_OF_LINES; ++lineIndex) {
      var lineContents = pdc.get(keysLineContents[lineIndex], PersistentDataType.STRING);

      if (lineContents == null)
        lineContents = "";

      var lineIsPlain = pdc.get(keysLineIsPlain[lineIndex], PersistentDataType.BOOLEAN);

      Component render;
      CommandAction editAction;

      if (lineIsPlain != null && lineIsPlain) {
        render = Component.text(lineContents);
        editAction = CommandAction.EDIT_PLAIN;
      } else {
        render = renderLine(lineContents);
        editAction = CommandAction.EDIT;
      }

      var editCommand = "/" + command.getName()
        + " " + CommandAction.matcher.getNormalizedName(editAction) +
        " " + (lineIndex + 1) +
        " " + lineContents;

      copiedLines.add(new CopiedLine(render, editCommand));
    }

    return copiedLines;
  }
}
