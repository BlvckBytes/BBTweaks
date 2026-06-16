package me.blvckbytes.bbtweaks.auto_pickup_container.settings;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemAttemptsKeeper {

  // No matter whether we're trying to decide if an item should be attracted (inv-magnet) or
  // if we're looking to add it to any of the currently carried auto-pickup-containers, checking
  // for possibly dozens of stacks per tick is going to cause in a TPS-drop; to mitigate this, failed
  // attempts are kept and not checked for again until the max-age elapsed.

  private static final long MAX_AGE_T = 2;

  // There will only be very few entries at a time, so a list is by far outperforming a map.
  private final List<ItemAttempt> attempts;

  public ItemAttemptsKeeper() {
    attempts = new ArrayList<>();
  }

  public boolean didFailAttemptRecently(ItemStack item, long relativeTime) {
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

  public void submitFailedAttempt(ItemStack item, long relativeTime) {
    attempts.add(new ItemAttempt(item, relativeTime));
  }
}
