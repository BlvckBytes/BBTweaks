package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.event.*;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class PredicateMechanic<InstanceType extends MechanicInstance> extends BaseMechanic<InstanceType> implements Listener {

  protected final PredicateHelper predicateHelper;

  protected final NamespacedKey predicateKey;
  protected final NamespacedKey predicateLanguageKey;

  public PredicateMechanic(
    Plugin plugin, ConfigKeeper<MainSection> config, PredicateHelper predicateHelper,
    NamespacedKey predicateKey, NamespacedKey predicateLanguageKey
  ) {
    super(plugin, config);

    this.predicateHelper = predicateHelper;
    this.predicateKey = predicateKey;
    this.predicateLanguageKey = predicateLanguageKey;
  }

  @EventHandler
  public void onPredicateGet(PredicateGetEvent event) {
    var sign = getSignFromPredicateEvent(event);

    if (sign == null)
      return;

    event.acknowledge();
    event.setResult(loadPredicateFromSign(sign));
  }

  @EventHandler
  public void onPredicateSet(PredicateSetEvent event) {
    var sign = getSignFromPredicateEvent(event);

    if (sign == null)
      return;

    event.acknowledge();
    setPredicateToSign(sign, event.getValue());
    reloadInstanceBySign(sign);
  }

  @EventHandler
  public void onPredicateRemove(PredicateRemoveEvent event) {
    var sign = getSignFromPredicateEvent(event);

    if (sign == null)
      return;

    var currentPredicate = loadPredicateFromSign(sign);

    event.acknowledge();
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
      var tokens = predicateHelper.parseTokens(predicateString);
      predicate = predicateHelper.parsePredicate(language, tokens);
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

  private @Nullable Sign getSignFromPredicateEvent(PredicateEvent predicateEvent) {
    var block = predicateEvent.getBlock();

    var instance = instanceBySignPosition.get(block.getWorld(), block.getX(), block.getY(), block.getZ());

    if (instance != null)
      return instance.getSign();

    return tryGetSignByAuxiliaryBlock(block);
  }
}
