package me.blvckbytes.bbtweaks.auto_wirer;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public record DependencyInstance(
  Class<?> type,
  Object instance,
  @Nullable Type fieldType
) {

  public boolean matches(Field field) {
    return matches(field.getGenericType());
  }

  public boolean matches(Parameter parameter) {
    return matches(parameter.getParameterizedType());
  }

  private boolean matches(Type requiredType) {
    if (requiredType instanceof Class<?> requiredClassType) {
      if (fieldType == null)
        return requiredClassType.isAssignableFrom(this.type);

      if (!(fieldType instanceof Class<?> fieldClassType))
        return false;

      return requiredClassType.isAssignableFrom(fieldClassType);
    }

    if (requiredType instanceof ParameterizedType requiredParameterizedType) {
      if (!(fieldType instanceof ParameterizedType fieldParameterizedType))
        return false;

      if (!(requiredParameterizedType.getRawType() instanceof Class<?> requiredRawClassType))
        return false;

      if (!(fieldParameterizedType.getRawType() instanceof Class<?> fieldRawClassType))
        return false;

      if (!requiredRawClassType.isAssignableFrom(fieldRawClassType))
        return false;

      return doGenericsMatch(requiredParameterizedType.getActualTypeArguments(), fieldParameterizedType.getActualTypeArguments());
    }

    return false;
  }

  private boolean doGenericsMatch(Type[] requiredTypes, Type[] fieldTypes) {
    if (requiredTypes.length != fieldTypes.length)
      return false;

    for (var index = 0; index < requiredTypes.length; ++index) {
      if (!(requiredTypes[index] instanceof Class<?> requiredClass))
        return false;

      if (!(fieldTypes[index] instanceof Class<?> fieldClass))
        return false;

      if (!(requiredClass.isAssignableFrom(fieldClass)))
        return false;
    }

    return true;
  }
}
