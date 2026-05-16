package me.blvckbytes.bbtweaks.command_items;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class CommandItemListener implements Listener {

  private final ConfigKeeper<MainSection> config;
  private final Map<UUID, Map<String, Long>> lastUseStampByNamerByPlayerId;

  public CommandItemListener(ConfigKeeper<MainSection> config) {
    this.config = config;

    this.lastUseStampByNamerByPlayerId = new HashMap<>();
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (!event.getAction().isRightClick())
      return;

    var heldItem = event.getItem();

    if (heldItem == null || heldItem.getType().isAir())
      return;

    var heldMeta = heldItem.getItemMeta();

    if (heldMeta == null)
      return;

    var displayName = heldMeta.displayName();

    if (displayName == null)
      return;

    var nameBuilder = new StringBuilder();

    forEachTextOfComponent(displayName, nameBuilder::append);

    var nameLower = nameBuilder.toString().trim().toLowerCase();
    var commandItem = config.rootSection.commandItems.commandItemByNameLower.get(nameLower);

    if (commandItem == null)
      return;

    if (commandItem._itemType != null && commandItem._itemType != heldItem.getType())
      return;

    var player = event.getPlayer();

    var lastUseStampByName = lastUseStampByNamerByPlayerId.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
    var lastUseStamp = lastUseStampByName.get(commandItem.itemName);
    var now = System.currentTimeMillis();

    if (lastUseStamp != null && now - lastUseStamp < config.rootSection.commandItems.useCooldownMs)
      return;

    lastUseStampByName.put(commandItem.itemName, now);

    player.performCommand(
      commandItem.command.asPlainString(
        new InterpretationEnvironment()
          .withVariable("player", player.getName())
      )
    );
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lastUseStampByNamerByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private static void forEachTextOfComponent(Component component, Consumer<String> textHandler) {
    if (component instanceof TextComponent textComponent)
      textHandler.accept(textComponent.content());

    for (var child : component.children())
      forEachTextOfComponent(child, textHandler);
  }
}
