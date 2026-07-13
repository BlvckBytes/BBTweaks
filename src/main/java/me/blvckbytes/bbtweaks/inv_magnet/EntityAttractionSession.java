package me.blvckbytes.bbtweaks.inv_magnet;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityAttractionSession {

  private static final double OLD_VELOCITY_FACTOR = .4;

  private final Vector originalVelocity;
  private final double originalX, originalY, originalZ;

  private double lastDistanceSquared = -1;

  public EntityAttractionSession(Entity entity) {
    originalVelocity = entity.getVelocity();

    originalX = entity.getX();
    originalY = entity.getY();
    originalZ = entity.getZ();
  }

  public void attractOrClearIfClosest(Entity attractedEntity, Location to, boolean clearIfClosest) {
    var deltaX = to.getX() - originalX;
    var deltaY = to.getY() - originalY;
    var deltaZ = to.getZ() - originalZ;

    var distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

    if (lastDistanceSquared >= 0 && distanceSquared >= lastDistanceSquared)
      return;

    lastDistanceSquared = distanceSquared;

    if (clearIfClosest) {
      attractedEntity.setVelocity(originalVelocity);
      return;
    }

    var distance = Math.sqrt(distanceSquared);

    var speed = Math.min(0.65, 0.12 + (distance * 0.08));

    attractedEntity.setVelocity(new Vector(
      deltaX / distance * speed + OLD_VELOCITY_FACTOR * originalVelocity.getX(),
      deltaY / distance * speed + OLD_VELOCITY_FACTOR * originalVelocity.getY(),
      deltaZ / distance * speed + OLD_VELOCITY_FACTOR * originalVelocity.getZ()
    ));
  }
}
