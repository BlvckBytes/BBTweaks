package me.blvckbytes.bbtweaks.auto_pickup_container.settings;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemAttemptsKeeper {

  // No matter whether we're trying to decide if an item should be attracted (inv-magnet) or
  // if we're looking to add it to any of the currently carried auto-pickup-containers, checking
  // for possibly dozens of stacks per tick is going to cause in a TPS-drop; to mitigate this, failed
  // attempts are kept and not checked for again until the max-age elapsed.

  // The real expensive part is accessing all shulker-inventories of a player-inventory each tick,
  // especially if they have dozens of them. This is why we introduced the LazyContainer - but that
  // doesn't help much if we check all of them for stacks that won't fit, each tick. So this
  // tracker is supposed to space the trials out a bit.

  // But: if only one stack out of, say 10, succeeds, we still need to see that through all the way
  // to pickup, which means possibly multiple attractions and then one pickup-attempt, moving into
  // the shulker-inventory. So if one succeeded, we need to ignore failures for that same tick. This
  // will certainly reduce the optimization a bit, but only for the few cases where a player actively
  // drops a single stack as to get another, which should be irrelevant in the grand scheme.

  private static final long MAX_AGE_T = 5;

  // There will only be very few entries at a time, so a list is by far outperforming a map.
  private final List<ItemAttempt> failedAttempts;
  private final List<ItemAttempt> successfulAttempts;

  public ItemAttemptsKeeper() {
    failedAttempts = new ArrayList<>();
    successfulAttempts = new ArrayList<>();
  }

  public boolean didFailAttemptAndNotSucceedOnceRecently(ItemStack item, long relativeTime) {
    if (hasMatchingAttempt(failedAttempts, item, relativeTime))
      return !hasMatchingAttempt(successfulAttempts, item, relativeTime);

    return false;
  }

  private boolean hasMatchingAttempt(List<ItemAttempt> attempts, ItemStack item, long relativeTime) {
    for (var index = attempts.size() - 1; index >= 0; --index) {
      var attempt = attempts.get(index);
      var currentAge = relativeTime - attempt.relativeTime();

      if (currentAge > MAX_AGE_T) {
        attempts.remove(index);
        continue;
      }

      if (attempt.item().isSimilar(item))
        return true;
    }

    return false;
  }

  public void cleanupExpiredAttempts(long relativeTime) {
    cleanupExpiredAttempts(successfulAttempts, relativeTime);
    cleanupExpiredAttempts(failedAttempts, relativeTime);
  }

  private void cleanupExpiredAttempts(List<ItemAttempt> attempts, long relativeTime) {
    attempts.removeIf(attempt -> {
      var currentAge = relativeTime - attempt.relativeTime();
      return currentAge > MAX_AGE_T;
    });
  }

  public void submitSuccessfulAttempt(ItemStack item, long relativeTime) {
    successfulAttempts.add(new ItemAttempt(item, relativeTime));
  }

  public void submitFailedAttempt(ItemStack item, long relativeTime) {
    failedAttempts.add(new ItemAttempt(item, relativeTime));
  }
}
