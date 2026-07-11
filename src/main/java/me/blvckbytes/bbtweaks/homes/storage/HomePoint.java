package me.blvckbytes.bbtweaks.homes.storage;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class HomePoint {

  private String homeName;

  private final String worldName;
  private final UUID worldId;
  private final double x, y, z;
  private final float yaw, pitch;

  private final long createdAt;

  private long lastUsedAt;
  private @Nullable Material icon;
  private @Nullable Integer favoriteNumber;
  private int usageCount;

  private boolean dirty;

  private HomePoint(String homeName, String worldName, UUID worldId, double x, double y, double z, float yaw, float pitch, long createdAt) {
    this.homeName = homeName;
    this.worldName = worldName;
    this.worldId = worldId;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.createdAt = createdAt;
  }

  public void incrementUsageCount() {
    ++usageCount;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void clearDirty() {
    dirty = false;
  }

  public String getHomeName() {
    return homeName;
  }

  public void setHomeName(String homeName) {
    dirty = true;
    this.homeName = homeName;
  }

  public @Nullable Material getIcon() {
    return icon;
  }

  public void setIcon(@Nullable Material icon) {
    dirty = true;
    this.icon = icon;
  }

  public @Nullable Integer getFavoriteNumber() {
    return favoriteNumber;
  }

  public void setFavoriteNumber(@Nullable Integer favoriteNumber) {
    dirty = true;
    this.favoriteNumber = favoriteNumber;
  }

  public InterpretationEnvironment makeEnvironment(ConfigKeeper<MainSection> config) {
    var worldDisplayName = config.rootSection.homes._worldDisplayNameByNameLower.get(worldName.toLowerCase());

    return new InterpretationEnvironment()
      .withVariable("name", homeName)
      .withVariable("created_at", createdAt)
      .withVariable("last_used_at", lastUsedAt)
      .withVariable("usage_count", usageCount)
      .withVariable("x", x)
      .withVariable("y", y)
      .withVariable("z", z)
      .withVariable("world", worldDisplayName == null ? worldName : worldDisplayName);
  }

  public static HomePoint makeNewWithCurrentStamp(String name, Location location) {
    var world = Objects.requireNonNull(location.getWorld());

    return new HomePoint(
      name,
      world.getName(),
      world.getUID(),
      location.getX(),
      location.getY(),
      location.getZ(),
      location.getYaw(),
      location.getPitch(),
      System.currentTimeMillis()
    );
  }

  public static HomePoint fromJson(JsonObject json) {
    var result = new HomePoint(
      jsonString(json, "homeName"),
      jsonString(json, "worldName"),
      UUID.fromString(jsonString(json, "worldId")),
      jsonNumber(json, "x").doubleValue(),
      jsonNumber(json, "y").doubleValue(),
      jsonNumber(json, "z").doubleValue(),
      jsonNumber(json, "yaw").floatValue(),
      jsonNumber(json, "pitch").floatValue(),
      jsonNumber(json, "createdAt").longValue()
    );

    Number number;

    if ((number = optionalJsonNumber(json, "lastUsedAt")) != null)
      result.lastUsedAt = number.longValue();

    if ((number = optionalJsonNumber(json, "favoriteNumber")) != null)
      result.favoriteNumber = number.intValue();

    if ((number = optionalJsonNumber(json, "usageCount")) != null)
      result.usageCount = number.intValue();

    String string;

    if ((string = optionalJsonString(json, "icon")) != null)
      result.icon = MaterialMatcher.tryMatch(string);

    return result;
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    result.addProperty("homeName", homeName);
    result.addProperty("worldName", worldName);
    result.addProperty("worldId", worldId.toString());
    result.addProperty("x", x);
    result.addProperty("y", y);
    result.addProperty("z", z);
    result.addProperty("yaw", yaw);
    result.addProperty("pitch", pitch);

    result.addProperty("createdAt", createdAt);
    result.addProperty("lastUsedAt", lastUsedAt);

    if (icon != null)
      result.addProperty("icon", icon.name());

    if (favoriteNumber != null)
      result.addProperty("favoriteNumber", favoriteNumber);

    result.addProperty("usageCount", usageCount);

    return result;
  }

  private static @Nullable String optionalJsonString(JsonObject json, String key) {
    var jsonElement = json.get(key);

    if (jsonElement instanceof JsonNull)
      return null;

    if (!(jsonElement instanceof JsonPrimitive primitive) || !primitive.isString())
      throw new IllegalStateException("Expected \"" + key + "\" to be a json string");

    return primitive.getAsString();
  }

  private static @Nullable Number optionalJsonNumber(JsonObject json, String key) {
    var jsonElement = json.get(key);

    if (jsonElement instanceof JsonNull)
      return null;

    if (!(jsonElement instanceof JsonPrimitive primitive) || !primitive.isNumber())
      throw new IllegalStateException("Expected \"" + key + "\" to be a json number");

    return primitive.getAsNumber();
  }

  private static String jsonString(JsonObject json, String key) {
    return Objects.requireNonNull(optionalJsonString(json, key), "Missing json-key \"" + key + "\"");
  }

  private static Number jsonNumber(JsonObject json, String key) {
    return Objects.requireNonNull(optionalJsonNumber(json, key), "Missing json-key \"" + key + "\"");
  }
}
