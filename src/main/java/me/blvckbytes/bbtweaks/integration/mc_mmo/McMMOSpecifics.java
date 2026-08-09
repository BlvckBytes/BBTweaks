package me.blvckbytes.bbtweaks.integration.mc_mmo;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.PerksUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;

public class McMMOSpecifics {

  private final EnumMap<PrimarySkillType, String> cachedSkillNameByType = new EnumMap<>(PrimarySkillType.class);

  public @Nullable Integer getSignEncodedAbilityDurationSeconds(Player player, SuperAbilityType abilityType) {
    var mmoPlayer = UserManager.getPlayer(player);

    if (mmoPlayer == null)
      return null;

    // = System.currentTimeMillis() / 1000 + durationSeconds, at the time of starting the ability.
    var deactivationSecondsStamp = mmoPlayer.getProfile().getAbilityDATS(abilityType);
    var durationSeconds = getAbilityDurationSeconds(mmoPlayer, abilityType);
    var startSecondsStamp = deactivationSecondsStamp - durationSeconds;

    var currentSecondsStamp = System.currentTimeMillis() / 1000;
    var elapsedSeconds = currentSecondsStamp - startSecondsStamp;

    if (elapsedSeconds <= durationSeconds)
      return (int) (durationSeconds - elapsedSeconds);

    var elapsedCooldownSeconds = elapsedSeconds - durationSeconds;
    var cooldownSeconds = getAbilityCooldownSeconds(mmoPlayer, abilityType);

    if (elapsedCooldownSeconds <= cooldownSeconds)
      return -1 * (int) (cooldownSeconds - elapsedCooldownSeconds);

    return null;
  }

  public @Nullable Integer getSkillLevel(Player player, PrimarySkillType skillType) {
    var mmoPlayer = UserManager.getPlayer(player);

    if (mmoPlayer == null)
      return null;

    return mmoPlayer.getSkillLevel(skillType);
  }

  public @Nullable Integer getSkillExp(Player player, PrimarySkillType skillType) {
    var mmoPlayer = UserManager.getPlayer(player);

    if (mmoPlayer == null)
      return null;

    return mmoPlayer.getSkillXpLevel(skillType);
  }

  public @Nullable Integer getSkillExpToLevel(Player player, PrimarySkillType skillType) {
    var mmoPlayer = UserManager.getPlayer(player);

    if (mmoPlayer == null)
      return null;

    return mmoPlayer.getXpToLevel(skillType);
  }

  public String getSkillName(PrimarySkillType skillType) {
    return cachedSkillNameByType.computeIfAbsent(skillType, mcMMO.p.getSkillTools()::getLocalizedSkillName);
  }

  public @Nullable Integer getPowerLevel(Player player) {
    var mmoPlayer = UserManager.getPlayer(player);

    if (mmoPlayer == null)
      return null;

    return mmoPlayer.getPowerLevel();
  }

  public EnumSet<SuperAbilityType> getAbilitiesOfSkill(PrimarySkillType skillType) {
    var abilities = EnumSet.noneOf(SuperAbilityType.class);

    switch (skillType) {
      case ARCHERY -> abilities.add(SuperAbilityType.EXPLOSIVE_SHOT);
      case AXES -> abilities.add(SuperAbilityType.SKULL_SPLITTER);
      case CROSSBOWS -> abilities.add(SuperAbilityType.SUPER_SHOTGUN);
      case EXCAVATION -> abilities.add(SuperAbilityType.GIGA_DRILL_BREAKER);
      case HERBALISM -> abilities.add(SuperAbilityType.GREEN_TERRA);
      case MACES -> abilities.add(SuperAbilityType.MACES_SUPER_ABILITY);
      case MINING -> {
        abilities.add(SuperAbilityType.SUPER_BREAKER);
        abilities.add(SuperAbilityType.BLAST_MINING);
      }
      case SPEARS -> abilities.add(SuperAbilityType.SPEARS_SUPER_ABILITY);
      case SWORDS -> abilities.add(SuperAbilityType.SERRATED_STRIKES);
      case TRIDENTS -> abilities.add(SuperAbilityType.TRIDENTS_SUPER_ABILITY);
      case UNARMED -> abilities.add(SuperAbilityType.BERSERK);
      case WOODCUTTING -> abilities.add(SuperAbilityType.TREE_FELLER);
    }

    return abilities;
  }

  private int getAbilityCooldownSeconds(McMMOPlayer mmoPlayer, SuperAbilityType superAbilityType) {
    return PerksUtils.handleCooldownPerks(mmoPlayer.getPlayer(), superAbilityType.getCooldown());
  }

  private int getAbilityDurationSeconds(McMMOPlayer mmoPlayer, SuperAbilityType superAbilityType) {
    int abilityLengthVar = mcMMO.p.getAdvancedConfig().getAbilityLength();
    int abilityLengthCap = mcMMO.p.getAdvancedConfig().getAbilityLengthCap();

    var primarySkillType = mcMMO.p.getSkillTools().getPrimarySkillBySuperAbility(superAbilityType);
    var primarySkillLevel = mmoPlayer.getSkillLevel(primarySkillType);

    if (abilityLengthCap > 0)
      primarySkillLevel = Math.min(abilityLengthCap, primarySkillLevel);

    return PerksUtils.handleActivationPerks(
      mmoPlayer.getPlayer(),
      2 + (primarySkillLevel / abilityLengthVar),
      superAbilityType.getMaxLength()
    );
  }
}
