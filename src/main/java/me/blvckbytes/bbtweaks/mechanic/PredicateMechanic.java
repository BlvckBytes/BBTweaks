package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.sign_copier.event.SignCopierExtractAdditionalAttributesEvent;
import me.blvckbytes.item_predicate_parser.event.*;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class PredicateMechanic<InstanceType extends MechanicInstance> extends BaseMechanic<InstanceType> implements Listener {

  protected static final Component COMPONENT_PREDICATE_MODE_ON = Component.text("Predicate Mode").color(NamedTextColor.GREEN);
  protected static final Component COMPONENT_PREDICATE_MODE_OFF = Component.empty();

  protected final IPPIntegration ippIntegration;

  protected final NamespacedKey predicateKey;
  protected final NamespacedKey predicateLanguageKey;

  public PredicateMechanic(
    Plugin plugin, ConfigKeeper<MainSection> config, IPPIntegration ippIntegration,
    NamespacedKey predicateKey, NamespacedKey predicateLanguageKey
  ) {
    super(plugin, config);

    this.ippIntegration = ippIntegration;
    this.predicateKey = predicateKey;
    this.predicateLanguageKey = predicateLanguageKey;
  }

  @EventHandler
  public void onExtractAdditionalAttributes(SignCopierExtractAdditionalAttributesEvent event) {
    event.copyFromSignPdcAndAddIfSet(predicateKey, PersistentDataType.STRING);
    event.copyFromSignPdcAndAddIfSet(predicateLanguageKey, PersistentDataType.STRING);
  }

  @EventHandler
  public void onPredicateGet(PredicateGetEvent event) {
    var sign = getEditableSignFromPredicateEventAndAcknowledge(event);

    if (sign == null)
      return;

    event.setResult(loadPredicateFromSign(sign));
  }

  @EventHandler
  public void onPredicateSet(PredicateSetEvent event) {
    var sign = getEditableSignFromPredicateEventAndAcknowledge(event);

    if (sign == null)
      return;

    setPredicateToSign(sign, event.getValue());
    reloadInstanceBySign(sign);
  }

  @EventHandler
  public void onPredicateRemove(PredicateRemoveEvent event) {
    var sign = getEditableSignFromPredicateEventAndAcknowledge(event);

    if (sign == null)
      return;

    var currentPredicate = loadPredicateFromSign(sign);

    event.setRemovedPredicate(currentPredicate);

    if (currentPredicate != null) {
      setPredicateToSign(sign, null);
      reloadInstanceBySign(sign);
    }
  }

  protected abstract @Nullable Sign tryGetSignByAuxiliaryBlock(Block block);

  protected @Nullable PredicateAndLanguage loadPredicateFromSign(Sign sign) {
    var pdc = sign.getPersistentDataContainer();

    var languageString = pdc.get(predicateLanguageKey, PersistentDataType.STRING);
    TranslationLanguage language;

    try {
      language = TranslationLanguage.valueOf(languageString);
    } catch (Throwable e) {
      return null;
    }

    var predicateString = pdc.get(predicateKey, PersistentDataType.STRING);
    ItemPredicate predicate;

    try {
      var tokens = ippIntegration.predicateHelper.parseTokens(predicateString);
      predicate = ippIntegration.predicateHelper.parsePredicate(language, tokens);
    } catch (Throwable e) {
      return null;
    }

    if (predicate == null)
      return null;

    return new PredicateAndLanguage(predicate, language);
  }

  private void setPredicateToSign(Sign sign, @Nullable PredicateAndLanguage predicateAndLanguage) {
    var pdc = sign.getPersistentDataContainer();

    if (predicateAndLanguage != null) {
      pdc.set(predicateKey, PersistentDataType.STRING, predicateAndLanguage.getTokenPredicateString());
      pdc.set(predicateLanguageKey, PersistentDataType.STRING, predicateAndLanguage.language.name());
    }

    else {
      pdc.remove(predicateKey);
      pdc.remove(predicateLanguageKey);
    }

    sign.update(true, false);
  }

  private @Nullable Sign getEditableSignFromPredicateEventAndAcknowledge(PredicateEvent event) {
    var block = event.getBlock();
    var instance = instanceBySignPosition.get(block.getWorld(), block.getX(), block.getY(), block.getZ());
    var sign = instance != null ? instance.getSign() : tryGetSignByAuxiliaryBlock(block);

    if (sign != null) {
      event.acknowledge();
      event.setDataHoldingBlock(sign.getBlock());

      if (!canEditSign(event.getPlayer(), sign)) {
        event.setDeniedAccessBlock(sign.getBlock());
        return null;
      }
    }

    return sign;
  }
}
