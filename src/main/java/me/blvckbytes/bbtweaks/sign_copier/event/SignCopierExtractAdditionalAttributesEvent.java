package me.blvckbytes.bbtweaks.sign_copier.event;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class SignCopierExtractAdditionalAttributesEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final Sign sign;

  public final PersistentDataContainer signPdc;

  private final PersistentDataContainer additionalAttributesPdc;

  public SignCopierExtractAdditionalAttributesEvent(Sign sign, PersistentDataContainer additionalAttributesPdc) {
    this.sign = sign;
    this.signPdc = sign.getPersistentDataContainer();
    this.additionalAttributesPdc = additionalAttributesPdc;
  }

  public <P, C> void copyFromSignPdcAndAddIfSet(NamespacedKey key, PersistentDataType<P, C> type) {
    C value = signPdc.get(key, type);

    if (value != null)
      add(key, type, value);
  }

  public <P, C> void add(NamespacedKey key, PersistentDataType<P, C> type, @NotNull C value) {
    if (additionalAttributesPdc.has(key))
      throw new IllegalStateException("Key " + key + " has already been added to this event");

    additionalAttributesPdc.set(key, type, value);
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  public static @NotNull HandlerList getHandlerList() {
    return handlers;
  }
}
