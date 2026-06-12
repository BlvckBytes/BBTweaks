package me.blvckbytes.bbtweaks.auto_wirer;

import java.lang.reflect.Field;

public record PendingLateWire(Object instance, Field field) {}
