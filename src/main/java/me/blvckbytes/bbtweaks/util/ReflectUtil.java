package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReflectUtil {

  private static final Method asNmsCopyMethod;

  static {
    try {
      // We're developing for Paper only, and they no longer add the version-prefixes.
      var craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
      asNmsCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
    } catch (Throwable e) {
      throw new IllegalStateException("Could not locate CraftItemSTack#asNMSCopy", e);
    }
  }

  public static <T extends Member> @Nullable T tryLocateNonStaticMember(Class<?> targetClass, Function<Class<?>, T[]> memberAccessor, Predicate<T> predicate) {
    return tryLocateMember(targetClass, memberAccessor, member -> !Modifier.isStatic(member.getModifiers()) && predicate.test(member));
  }

  public static <T extends Member> @Nullable T tryLocateMember(Class<?> targetClass, Function<Class<?>, T[]> memberAccessor, Predicate<T> predicate) {
    for (var member : memberAccessor.apply(targetClass)) {
      if (predicate.test(member))
        return member;
    }

    var superClass = targetClass.getSuperclass();

    if (superClass != Object.class)
      return tryLocateMember(superClass, memberAccessor, predicate);

    return null;
  }

  public static Object asNMSCopy(ItemStack item) {
    try {
      return asNmsCopyMethod.invoke(null, item);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
