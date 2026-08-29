package me.blvckbytes.bbtweaks.item_piling;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.item_piling.preferences.ItemPilingPreferencesStore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemPilingEntityNamePatcher extends PacketAdapter implements Disableable {

  private final ItemPilingPreferencesStore preferencesStore;
  private final PileEntityMetadataKeeper metadataKeeper;
  private final ProtocolManager protocolManager;
  private final ConfigKeeper<MainSection> config;

  public ItemPilingEntityNamePatcher(
    ItemPilingPreferencesStore preferencesStore,
    PileEntityMetadataKeeper metadataKeeper,
    ProtocolManager protocolManager,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA);

    this.preferencesStore = preferencesStore;
    this.metadataKeeper = metadataKeeper;
    this.protocolManager = protocolManager;
    this.config = config;

    protocolManager.addPacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    var packet = event.getPacket();

    if (packet.getType() != PacketType.Play.Server.ENTITY_METADATA)
      return;

    int entityId = packet.getIntegers().read(0);
    var dataValues = packet.getDataValueCollectionModifier().read(0);

    if (!modifyDataValuesIfHasMetadata(entityId, event.getPlayer(), dataValues))
      return;

    packet.getDataValueCollectionModifier().write(0, dataValues);
  }

  public void possiblyUpdateEntityIfHasMetadata(int entityId, Player player) {
    var metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

    metadataPacket.getIntegers().write(0, entityId);

    var dataValues = new ArrayList<WrappedDataValue>();

    if (!modifyDataValuesIfHasMetadata(entityId, player, dataValues))
      return;

    metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

    protocolManager.sendServerPacket(player, metadataPacket);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean modifyDataValuesIfHasMetadata(int entityId, Player player, List<WrappedDataValue> dataValues) {
    var pileMetadata = metadataKeeper.getPileMetadata(entityId);

    if (pileMetadata == null)
      return false;

    var entityName = renderItemEntityName(pileMetadata, player);

    modifyOrAppendDataValue(
      dataValues, 2,
      WrappedDataWatcher.Registry.getChatComponentSerializer(true),
      entityName == null
        ? Optional.empty()
        : Optional.of(AdventureComponentConverter.fromComponent(entityName))
    );

    modifyOrAppendDataValue(
      dataValues, 3,
      WrappedDataWatcher.Registry.get((Type) Boolean.class),
      entityName != null
    );

    return true;
  }

  private void modifyOrAppendDataValue(List<WrappedDataValue> values, int index, WrappedDataWatcher.Serializer serializer, Object possiblyWrappedValue) {
    for (var dataValue : values) {
      var dataValueIndex = dataValue.getIndex();

      if (dataValueIndex == index) {
        dataValue.setValue(possiblyWrappedValue);
        return;
      }
    }

    values.add(WrappedDataValue.fromWrappedValue(index, serializer, possiblyWrappedValue));
  }

  @Override
  public void onPacketReceiving(PacketEvent event) {}

  @Override
  public void disable() {
    protocolManager.removePacketListener(this);
  }

  private @Nullable Component renderItemEntityName(AmountAndType amountAndType, Player player) {
    var stackSize = amountAndType.type().getMaxStackSize();
    var stackCount = amountAndType.totalAmount() / stackSize;
    var pieceCount = amountAndType.totalAmount() % stackSize;

    var name = config.rootSection.itemPiling.itemEntityName.interpret(
      SlotType.SINGLE_LINE_CHAT,
      preferencesStore.accessPreferences(player).makeEnvironment()
        .withVariable("total_amount", amountAndType.totalAmount())
        .withVariable("stack_size", stackSize)
        .withVariable("stack_count", stackCount)
        .withVariable("piece_count", pieceCount)
        .withVariable("type_key", amountAndType.type().translationKey())
    ).getFirst();

    if (name.equals(Component.empty()))
      return null;

    return name;
  }
}
