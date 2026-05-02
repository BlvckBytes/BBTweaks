package me.blvckbytes.bbtweaks.inv_magnet;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

public class ItemAttractionSession {

  private static final double OLD_VELOCITY_FACTOR = .4;

  private final Vector velocity;
  private final double x, y, z;

  private double lastDistanceSquared = -1;

  public ItemAttractionSession(Item item) {
    velocity = item.getVelocity();

    x = item.getX();
    y = item.getY();
    z = item.getZ();
  }

  public void attractIfClosest(Item item, Location to) {
    var deltaX = to.getX() - x;
    var deltaY = to.getY() - y;
    var deltaZ = to.getZ() - z;

    var distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

    if (lastDistanceSquared >= 0 && distanceSquared >= lastDistanceSquared)
      return;

    lastDistanceSquared = distanceSquared;

    var distance = Math.sqrt(distanceSquared);

    // TODO: Experiment with these parameters
    var speed = Math.min(0.65, 0.12 + (distance * 0.08));

    item.setVelocity(new Vector(
      deltaX / distance * speed + OLD_VELOCITY_FACTOR * velocity.getX(),
      deltaY / distance * speed + OLD_VELOCITY_FACTOR * velocity.getY(),
      deltaZ / distance * speed + OLD_VELOCITY_FACTOR * velocity.getZ()
    ));
  }
}
