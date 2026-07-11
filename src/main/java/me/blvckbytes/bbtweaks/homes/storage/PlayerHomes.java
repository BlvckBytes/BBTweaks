package me.blvckbytes.bbtweaks.homes.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerHomes {

  private final List<HomePoint> homes;

  private String lastKnownName;

  private boolean dirty;

  public PlayerHomes(String lastKnownName) {
    this.homes = new ArrayList<>();

    this.lastKnownName = lastKnownName;
  }

  public List<HomePoint> getHomes() {
    return Collections.unmodifiableList(homes);
  }

  public int getHomeCount() {
    return homes.size();
  }

  public void markDirty() {
    dirty = true;
  }

  public boolean isDirty() {
    if (dirty)
      return true;

    return homes.stream().anyMatch(HomePoint::isDirty);
  }

  public void clearDirty() {
    dirty = false;

    for (var home : homes)
      home.clearDirty();
  }

  public String getLastKnownName() {
    return lastKnownName;
  }

  public void setLastKnownName(String lastKnownName) {
    dirty = true;
    this.lastKnownName = lastKnownName;
  }

  public Set<String> getHomeNames() {
    var result = new HashSet<String>();

    for (var home : homes)
      result.add(home.getHomeName());

    return result;
  }

  public @Nullable HomePoint getHomeByName(String name) {
    for (var home : homes) {
      if (home.getHomeName().equalsIgnoreCase(name))
        return home;
    }

    return null;
  }

  public @Nullable HomePoint setHomeAndGetPriorIfAny(HomePoint homePoint) {
    dirty = true;
    var priorHome = deleteHomeIfExists(homePoint.getHomeName());
    homes.add(homePoint);
    return priorHome;
  }

  public @Nullable HomePoint deleteHomeIfExists(String name) {
    for (var iterator = homes.iterator(); iterator.hasNext();) {
      var homePoint = iterator.next();

      if (homePoint.getHomeName().equalsIgnoreCase(name)) {
        iterator.remove();
        dirty = true;
        return homePoint;
      }
    }

    return null;
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    result.addProperty("lastKnownName", lastKnownName);

    var homesArray = new JsonArray();
    result.add("homes", homesArray);

    for (var home : homes)
      homesArray.add(home.toJson());

    return result;
  }

  public static PlayerHomes fromJson(JsonObject jsonObject, File homeFile, Logger logger) {
    if (!(jsonObject.get("lastKnownName") instanceof JsonPrimitive namePrimitive))
      throw new IllegalStateException("Expected \"lastKnownName\" to be present");

    var result = new PlayerHomes(namePrimitive.getAsString());

    if (!(jsonObject.get("homes") instanceof JsonArray homesArray))
      throw new IllegalStateException("Expected \"homes\" to be a json-array");

    for (var index = 0; index < homesArray.size(); ++index) {
      var homeJson = homesArray.get(index);

      if (!(homeJson instanceof JsonObject homeObject)) {
        logger.warning("Ignoring non-object item #" + (index + 1) + "in home-file " + homeFile);
        continue;
      }

      HomePoint homePoint;

      try {
        homePoint = HomePoint.fromJson(homeObject);
      } catch (Throwable e) {
        logger.log(Level.WARNING, "Could not load item #" + (index + 1) + " in home-file " + homeFile, e);
        continue;
      }

      var priorHome = result.setHomeAndGetPriorIfAny(homePoint);

      if (priorHome != null)
        logger.log(Level.WARNING, "Encountered duplicate home at #" + (index + 1) + " in home-file " + homeFile);
    }

    return result;
  }
}
