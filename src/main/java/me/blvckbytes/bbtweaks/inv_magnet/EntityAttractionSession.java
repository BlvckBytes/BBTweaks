package me.blvckbytes.bbtweaks.inv_magnet;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityAttractionSession {

  private static final double OLD_VELOCITY_FACTOR = .4;

  private final Vector velocity;
  private final double x, y, z;

  private double lastDistanceSquared = -1;

  public EntityAttractionSession(Entity entity) {
    velocity = entity.getVelocity();

    x = entity.getX();
    y = entity.getY();
    z = entity.getZ();
  }

  public void attractIfClosest(Entity entity, Location to) {
    var deltaX = to.getX() - x;
    var deltaY = to.getY() - y;
    var deltaZ = to.getZ() - z;

    var distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

    if (lastDistanceSquared >= 0 && distanceSquared >= lastDistanceSquared)
      return;

    lastDistanceSquared = distanceSquared;

    var distance = Math.sqrt(distanceSquared);

    var speed = Math.min(0.65, 0.12 + (distance * 0.08));

    entity.setVelocity(new Vector(
      deltaX / distance * speed + OLD_VELOCITY_FACTOR * velocity.getX(),
      deltaY / distance * speed + OLD_VELOCITY_FACTOR * velocity.getY(),
      deltaZ / distance * speed + OLD_VELOCITY_FACTOR * velocity.getZ()
    ));
  }
}
