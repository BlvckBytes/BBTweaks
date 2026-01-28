package me.blvckbytes.bbtweaks.mechanic.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ComparableMap<K extends Comparable<K>, V> extends AbstractMap<K, V> {

  private final Class<K> keyType;
  private final List<K> keys;
  private final Set<K> keySetView;

  private final List<V> values;
  private final Collection<V> valuesView;

  private final Set<Entry<K, V>> entrySetView;

  private int modCount;

  public ComparableMap(Class<K> keyType) {
    this.keyType = keyType;

    this.keys = new ArrayList<>();
    this.values = new ArrayList<>();

    this.keySetView = new AbstractSet<>() {

      @Override
      public @NotNull Iterator<K> iterator() {
        return listIterator(keys, values);
      }

      @Override
      public int size() {
        return ComparableMap.this.size();
      }

      @Override
      public boolean remove(Object object) {
        return ComparableMap.this.remove(object) != null;
      }
    };

    this.entrySetView = new AbstractSet<>() {

      @Override
      public @NotNull Iterator<Entry<K, V>> iterator() {
        return new Iterator<>() {
          int lastIndex = -1;
          int expectedModCount = modCount;

          @Override
          public boolean hasNext() {
            if (modCount != expectedModCount)
              throw new ConcurrentModificationException();

            return lastIndex + 1 < keys.size();
          }

          @Override
          public Entry<K, V> next() {
            if (modCount != expectedModCount)
              throw new ConcurrentModificationException();

            int currentIndex = ++lastIndex;

            return new Entry<>() {
              @Override
              public K getKey() {
                return keys.get(currentIndex);
              }

              @Override
              public V getValue() {
                return values.get(currentIndex);
              }

              @Override
              public V setValue(V v) {
                return values.set(currentIndex, v);
              }
            };
          }

          @Override
          public void remove() {
            if (lastIndex < 0)
              throw new IllegalStateException();

            ++expectedModCount;
            ++ComparableMap.this.modCount;

            keys.remove(lastIndex);
            values.remove(lastIndex);
            --lastIndex;
          }
        };
      }

      @Override
      public int size() {
        return keys.size();
      }
    };

    this.valuesView = new AbstractCollection<>() {

      @Override
      public @NotNull Iterator<V> iterator() {
        return listIterator(values, keys);
      }

      @Override
      public int size() {
        return values.size();
      }
    };
  }

  private int indexOfKey(K key) {
    return Collections.binarySearch(keys, key);
  }

  @Override
  public int size() {
    return keys.size();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    if (!keyType.isInstance(key))
      return false;

    return indexOfKey(keyType.cast(key)) >= 0;
  }

  @Override
  public boolean containsValue(Object value) {
    for (var containedValue : values) {
      if (Objects.equals(containedValue, value))
        return true;
    }

    return false;
  }

  @Override
  public V get(Object key) {
    if (!keyType.isInstance(key))
      return null;

    var keyIndex = indexOfKey(keyType.cast(key));

    if (keyIndex < 0)
      return null;

    return values.get(keyIndex);
  }

  @Override
  public @Nullable V put(K key, V value) {
    int existingPosition = indexOfKey(key);

    if (existingPosition >= 0)
      return values.set(existingPosition, value);

    ++modCount;

    int insertionPosition = -existingPosition - 1;

    keys.add(insertionPosition, key);
    values.add(insertionPosition, value);

    return null;
  }

  @Override
  public V remove(Object key) {
    if (!keyType.isInstance(key))
      return null;

    var keyIndex = indexOfKey(keyType.cast(key));

    if (keyIndex < 0)
      return null;

    ++modCount;

    keys.remove(keyIndex);
    return values.remove(keyIndex);
  }

  @Override
  public void clear() {
    ++modCount;
    keys.clear();
    values.clear();
  }

  @Override
  public @NotNull Set<K> keySet() {
    return keySetView;
  }

  @Override
  public @NotNull Collection<V> values() {
    return valuesView;
  }

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    return entrySetView;
  }

  private <T> Iterator<T> listIterator(List<T> list, List<?> other) {
    return new Iterator<>() {
      int lastIndex = -1;
      int expectedModCount = modCount;

      @Override
      public boolean hasNext() {
        if (modCount != expectedModCount)
          throw new ConcurrentModificationException();

        return lastIndex + 1 < list.size();
      }

      @Override
      public T next() {
        if (modCount != expectedModCount)
          throw new ConcurrentModificationException();

        return list.get(++lastIndex);
      }

      @Override
      public void remove() {
        if (modCount != expectedModCount)
          throw new ConcurrentModificationException();

        if (lastIndex < 0)
          throw new IllegalStateException();

        ++ComparableMap.this.modCount;
        ++expectedModCount;

        list.remove(lastIndex);
        other.remove(lastIndex);
        --lastIndex;
      }
    };
  }
}
