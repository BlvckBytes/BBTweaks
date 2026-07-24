package me.blvckbytes.bbtweaks.auto_pickup_container.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.UsageInfo;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;

import java.time.Duration;
import java.util.List;

public enum CapacityWarningMode implements MatchableEnum {
  // NOTE: The ordinal of this enum is used as the main identifier!

  SCREEN_CHAT_SOUND {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
      sendTitle(usageInfo, config);
      sendChat(usageInfo, config);
      sendSound(usageInfo, config);
    }
  },

  SCREEN_CHAT {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
      sendTitle(usageInfo, config);
      sendChat(usageInfo, config);
    }
  },

  SCREEN_SOUND {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
      sendTitle(usageInfo, config);
      sendSound(usageInfo, config);
    }
  },

  SCREEN {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
      sendTitle(usageInfo, config);
    }
  },

  CHAT_SOUND {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
      sendChat(usageInfo, config);
      sendSound(usageInfo, config);
    }
  },

  CHAT {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
      sendChat(usageInfo, config);
    }
  },

  OFF {
    @Override
    public void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {}
  },
  ;

  public static final CapacityWarningMode DEFAULT_VALUE = OFF;

  public static final EnumMatcher<CapacityWarningMode> matcher = new EnumMatcher<>(values());
  public static final List<CapacityWarningMode> ALL_VALUES = List.of(values());

  public abstract void display(UsageInfo usageInfo, ConfigKeeper<MainSection> config);

  public static CapacityWarningMode byOrdinalOrDefault(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return DEFAULT_VALUE;

    return ALL_VALUES.get(ordinal);
  }

  private static void sendSound(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
    config.rootSection.autoPickupContainer._capacityWarningSound.play(usageInfo.player);
  }

  private static void sendChat(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
    config.rootSection.autoPickupContainer.capacityWarningChat.sendMessage(
      usageInfo.player,
      usageInfo.lastKnownCounts.makeEnvironment()
    );
  }

  private static void sendTitle(UsageInfo usageInfo, ConfigKeeper<MainSection> config) {
    var environment = usageInfo.lastKnownCounts.makeEnvironment();

    usageInfo.player.sendTitlePart(
      TitlePart.TITLE,
      config.rootSection.autoPickupContainer.capacityWarningTitle
        .interpret(SlotType.SINGLE_LINE_CHAT, environment)
        .getFirst()
    );

    usageInfo.player.sendTitlePart(
      TitlePart.SUBTITLE,
      config.rootSection.autoPickupContainer.capacityWarningSubtitle
        .interpret(SlotType.SINGLE_LINE_CHAT, environment)
        .getFirst()
    );

    usageInfo.player.sendTitlePart(
      TitlePart.TIMES,
      Title.Times.times(
        Duration.ofMillis(100),
        Duration.ofMillis(config.rootSection.autoPickupContainer.capacityWarningTitleStayMs),
        Duration.ofMillis(100)
      )
    );
  }
}
