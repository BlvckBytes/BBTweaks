package me.blvckbytes.bbtweaks.chat_format;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfileStore;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.LegacyColorUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ChatFormatListener implements Listener {

  private final FloodgateIntegration floodgateIntegration;
  private final LuckPerms luckPerms;
  private final DonorSymbolProfileStore donorSymbolProfileStore;
  private final ConfigKeeper<MainSection> config;

  public ChatFormatListener(
    FloodgateIntegration floodgateIntegration,
    LuckPerms luckPerms,
    DonorSymbolProfileStore donorSymbolProfileStore,
    ConfigKeeper<MainSection> config
  ) {
    this.floodgateIntegration = floodgateIntegration;
    this.luckPerms = luckPerms;
    this.donorSymbolProfileStore = donorSymbolProfileStore;
    this.config = config;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent event) {
    var sender = event.getPlayer();

    var metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(sender);
    var group = metaData.getPrimaryGroup();

    String format;

    if ((format = config.rootSection.chatFormat._groupFormatByGroupNameLower.get("group-formats." + group)) == null)
      format = config.rootSection.chatFormat.defaultFormat;

    if (format == null)
      return;

    format = replaceVariables(format, variableName -> {
      String value;

      switch (variableName) {
        case "prefix":
          value = metaData.getPrefix();
          break;

        case "suffix":
          value = metaData.getSuffix();
          break;

        case "prefixes":
          value = String.join("", metaData.getPrefixes().values());
          break;

        case "suffixes":
          value = String.join("", metaData.getSuffixes().values());
          break;

        case "world":
          value = sender.getWorld().getName();
          break;

        case "name":
          value = sender.getName();
          break;

        case "displayname":
          value = LegacyComponentSerializer.legacySection().serialize(sender.displayName());
          break;

        case "username-color":
          value = metaData.getMetaValue("username-color");
          break;

        case "message-color":
          value = metaData.getMetaValue("message-color");
          break;

        default:
          return null;
      }

      if (value == null)
        return "";

      return value;
    });

    // Enable colors on all placeholders but the message - LPC prefixes contain ampersand-sequences.
    format = LegacyColorUtil.enableColors(format);

    var senderDonorSymbolProfile = donorSymbolProfileStore.accessProfile(sender);

    var javaRecipientFormat = replaceVariables(format, variableName -> {
      if (variableName.equals("donor-symbol"))
        return senderDonorSymbolProfile.renderJavaSymbolOrEmpty();

      return null;
    });

    var bedrockRecipientFormat = replaceVariables(format, variableName -> {
      if (variableName.equals("donor-symbol"))
        return senderDonorSymbolProfile.renderBedrockSymbolOrEmpty();

      return null;
    });

    if (config.rootSection.chatFormat.squeezeSpaces) {
      javaRecipientFormat = squeezeSpaces(javaRecipientFormat);
      bedrockRecipientFormat = squeezeSpaces(bedrockRecipientFormat);
    }

    var legacyMessage = LegacyComponentSerializer.legacySection().serialize(event.message());

    // Message-colors are enabled based on player-permissions
    var allowLegacyColors = sender.hasPermission("bbtweaks.chat-format.legacy-colors");
    var allowHexColors = sender.hasPermission("bbtweaks.chat-format.hex-colors");

    javaRecipientFormat = replaceVariables(javaRecipientFormat, variableName -> {
      if (variableName.equals("message"))
        return LegacyColorUtil.enableColors(legacyMessage, allowLegacyColors, allowHexColors);

      return null;
    });

    bedrockRecipientFormat = replaceVariables(bedrockRecipientFormat, variableName -> {
      if (variableName.equals("message"))
        return LegacyColorUtil.enableColors(legacyMessage, allowLegacyColors, allowHexColors);

      return null;
    });

    var javaRecipientRender = LegacyComponentSerializer.legacySection().deserialize(javaRecipientFormat);
    var bedrockRecipientRender = LegacyComponentSerializer.legacySection().deserialize(bedrockRecipientFormat);

    event.renderer((_, _, _, viewer) -> {
      if (!(viewer instanceof Player viewerPlayer) || !floodgateIntegration.isFloodgatePlayer(viewerPlayer))
        return javaRecipientRender;

      return bedrockRecipientRender;
    });
  }

  // ================================================================================
  // Algorithms (see test-cases)
  // ================================================================================

  public static String squeezeSpaces(String input) {
    StringBuilder result = null;
    var previousSpace = false;

    for (var charIndex = 0; charIndex < input.length(); charIndex++) {
      var currentChar = input.charAt(charIndex);

      if (currentChar == ' ' && previousSpace) {
        if (result == null) {
          result = new StringBuilder(input.length());
          result.append(input, 0, charIndex);
        }
      }

      else if (result != null)
        result.append(currentChar);

      previousSpace = currentChar == ' ';
    }

    return result != null ? result.toString() : input;
  }

  public static String replaceVariables(String input, Function<String, @Nullable String> valueLookup) {
    var result = new StringBuilder(input.length());

    int charIndex;
    int nextPriorSubstringBegin = 0;

    for (charIndex = 0; charIndex < input.length(); ++charIndex) {
      var currentChar = input.charAt(charIndex);

      if (currentChar != '{')
        continue;

      var closingIndex = input.indexOf('}', charIndex + 1);

      if (closingIndex < 0)
        continue;

      var variableName = input.substring(charIndex + 1, closingIndex);
      var variableValue = valueLookup.apply(variableName);

      if (variableValue == null)
        continue;

      result.append(input, nextPriorSubstringBegin, charIndex);
      nextPriorSubstringBegin = closingIndex + 1;

      result.append(variableValue);

      charIndex = closingIndex;
    }

    if (nextPriorSubstringBegin < charIndex)
      result.append(input, nextPriorSubstringBegin, input.length());

    return result.toString();
  }
}
