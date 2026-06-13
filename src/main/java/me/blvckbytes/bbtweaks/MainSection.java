package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.ab_sleep.ABSleepSection;
import me.blvckbytes.bbtweaks.additional_recipes.AdditionalRecipesSection;
import me.blvckbytes.bbtweaks.auto_fly.AutoFlySection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerSection;
import me.blvckbytes.bbtweaks.auto_tool.AutoToolSection;
import me.blvckbytes.bbtweaks.back.BackOverrideSection;
import me.blvckbytes.bbtweaks.bottlexp.BottleXpSection;
import me.blvckbytes.bbtweaks.command_items.CommandItemsSection;
import me.blvckbytes.bbtweaks.custom_commands.CustomCommandsSection;
import me.blvckbytes.bbtweaks.durability_warnings.config.DurabilityWarningsSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelSection;
import me.blvckbytes.bbtweaks.get_exp.GetExpSection;
import me.blvckbytes.bbtweaks.get_uuid.GetUuidSection;
import me.blvckbytes.bbtweaks.integration.discord.DiscordSection;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterSection;
import me.blvckbytes.bbtweaks.inv_magnet.config.InvMagnetSection;
import me.blvckbytes.bbtweaks.main_command.MainCommandSection;
import me.blvckbytes.bbtweaks.markers_menu.MarkersMenuSection;
import me.blvckbytes.bbtweaks.mechanic.MechanicSection;
import me.blvckbytes.bbtweaks.multi_break.config.MultiBreakSection;
import me.blvckbytes.bbtweaks.newbie_announce.NewbieAnnounceSection;
import me.blvckbytes.bbtweaks.newbie_teleport.NewbieTeleportSection;
import me.blvckbytes.bbtweaks.ping.PingSection;
import me.blvckbytes.bbtweaks.rd_breaker.RDBreakerSection;
import me.blvckbytes.bbtweaks.seed.SeedOverrideSection;
import me.blvckbytes.bbtweaks.shulker_accessor.config.ShulkerAccessorSection;
import me.blvckbytes.bbtweaks.sidebar.config.SidebarSection;
import me.blvckbytes.bbtweaks.sign_copier.SignCopierSection;
import me.blvckbytes.bbtweaks.un_craft.config.UnCraftSection;

@CSAlways
public class MainSection extends ConfigSection {

  public MainCommandSection mainCommand;
  public ABSleepSection abSleep;
  public RDBreakerSection rdBreaker;
  public BackOverrideSection backOverride;
  public FurnaceLevelSection furnaceLevel;
  public GetUuidSection getUuid;
  public PingSection ping;
  public AdditionalRecipesSection additionalRecipes;
  public UnCraftSection unCraft;
  public MechanicSection mechanic;
  public SeedOverrideSection seedOverride;
  public CustomCommandsSection customCommands;
  public InvFilterSection invFilter;
  public MarkersMenuSection markersMenu;
  public AutoFlySection autoFly;
  public NewbieTeleportSection newbieTeleport;
  public DiscordSection discord;
  public NewbieAnnounceSection newbieAnnounce;
  public MultiBreakSection multiBreak;
  public AutoToolSection autoTool;
  public InvMagnetSection invMagnet;
  public ShulkerAccessorSection shulkerAccessor;
  public AutoPickupContainerSection autoPickupContainer;
  public GetExpSection getExp;
  public CommandItemsSection commandItems;
  public DurabilityWarningsSection durabilityWarnings;
  public SidebarSection sidebar;
  public SignCopierSection signCopier;
  public BottleXpSection bottleXp;

  public MainSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
