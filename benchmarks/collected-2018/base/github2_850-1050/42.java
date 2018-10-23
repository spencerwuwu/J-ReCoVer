// https://searchcode.com/api/result/101299595/

/*
 * Copyright 2014 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.harp.util;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.booleans.BooleanArrays;
import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.longs.AbstractLong2ObjectMap;
import it.unimi.dsi.fastutil.longs.AbstractLongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.AbstractObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;

/**
 * A type-specific hash map with a fast, small-footprint implementation. Modifed
 * from Long2ObjectOpenHashMap in fastutil-6.5.7
 * 
 * @author zhangbj
 *
 */
public class Long2ObjectOpenHashMap<V> extends AbstractLong2ObjectMap<V> implements
  Hash {
  private static final long serialVersionUID = 0L;
  private static final boolean ASSERTS = false;
  /** The array of keys. */
  protected transient long key[];
  /** The array of values. */
  protected transient V value[];
  /** The array telling whether a position is used. */
  protected transient boolean used[];
  /** The acceptable load factor. */
  protected final float f;
  /** The current table size. */
  protected transient int n;
  /**
   * Threshold after which we rehash. It must be the table size times {@link #f}
   * .
   */
  protected transient int maxFill;
  /** The mask for wrapping a position counter. */
  protected transient int mask;
  /** Number of entries in the set. */
  protected int size;
  /** Cached set of entries. */
  protected transient volatile FastEntrySet<V> entries;
  /** Cached set of keys. */
  protected transient volatile LongSet keys;
  /** Cached collection of values. */
  protected transient volatile ObjectCollection<V> values;
  /** */
  protected final Class<V> vClass;

  /**
   * Creates a new hash map.
   * 
   * <p>
   * The actual table size will be the least power of two greater than
   * <code>expected</code>/<code>f</code>.
   * 
   * @param expected the expected number of elements in the hash set.
   * @param f the load factor.
   */
  @SuppressWarnings("unchecked")
  public Long2ObjectOpenHashMap(final int expected, final float f, Class<V> vClass) {
    if (f <= 0 || f > 1)
      throw new IllegalArgumentException(
        "Load factor must be greater than 0 and smaller than or equal to 1");
    if (expected < 0)
      throw new IllegalArgumentException(
        "The expected number of elements must be nonnegative");
    this.f = f;
    n = arraySize(expected, f);
    mask = n - 1;
    maxFill = maxFill(n, f);
    key = new long[n];
    value = (V[]) Array.newInstance(vClass, n);
    used = new boolean[n];
    this.vClass = vClass;
  }

  /**
   * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load
   * factor.
   * 
   * @param expected the expected number of elements in the hash map.
   */
  public Long2ObjectOpenHashMap(final int expected, Class<V> vClass) {
    this(expected, DEFAULT_LOAD_FACTOR, vClass);
  }

  /**
   * Creates a new hash map with initial expected
   * {@link Hash#DEFAULT_INITIAL_SIZE} entries and
   * {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
   */
  public Long2ObjectOpenHashMap(Class<V> vClass) {
    this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR, vClass);
  }

  /**
   * Creates a new hash map copying a given one.
   * 
   * @param m a {@link Map} to be copied into the new hash map.
   * @param f the load factor
   * @param vClass Value class
   */
  public Long2ObjectOpenHashMap(final Map<? extends Long, ? extends V> m,
    final float f, Class<V> vClass) {
    this(m.size(), f, vClass);
    putAll(m);
  }

  /**
   * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor
   * copying a given one.
   * 
   * @param m a {@link Map} to be copied into the new hash map.
   */
  public Long2ObjectOpenHashMap(final Map<? extends Long, ? extends V> m,
    Class<V> vClass) {
    this(m, DEFAULT_LOAD_FACTOR, vClass);
  }

  /**
   * Creates a new hash map copying a given type-specific one.
   * 
   * @param m a type-specific map to be copied into the new hash map.
   * @param f the load factor.
   */
  public Long2ObjectOpenHashMap(final Long2ObjectMap<V> m, final float f,
    Class<V> vClass) {
    this(m.size(), f, vClass);
    putAll(m);
  }

  /**
   * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor
   * copying a given type-specific one.
   * 
   * @param m a type-specific map to be copied into the new hash map.
   */
  public Long2ObjectOpenHashMap(final Long2ObjectMap<V> m, Class<V> vClass) {
    this(m, DEFAULT_LOAD_FACTOR, vClass);
  }

  /**
   * Creates a new hash map using the elements of two parallel arrays.
   * 
   * @param k the array of keys of the new hash map.
   * @param v the array of corresponding values in the new hash map.
   * @param f the load factor.
   * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have
   *           different lengths.
   */
  public Long2ObjectOpenHashMap(final long[] k, final V v[], final float f,
    Class<V> vClass) {
    this(k.length, f, vClass);
    if (k.length != v.length)
      throw new IllegalArgumentException(
        "The key array and the value array have different lengths (" + k.length
          + " and " + v.length + ")");
    for (int i = 0; i < k.length; i++)
      this.put(k[i], v[i]);
  }

  /**
   * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor
   * using the elements of two parallel arrays.
   * 
   * @param k the array of keys of the new hash map.
   * @param v the array of corresponding values in the new hash map.
   * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have
   *           different lengths.
   */
  public Long2ObjectOpenHashMap(final long[] k, final V v[], Class<V> vClass) {
    this(k, v, DEFAULT_LOAD_FACTOR, vClass);
  }

  /*
   * The following methods implements some basic building blocks used by all
   * accessors. They are (and should be maintained) identical to those used in
   * OpenHashSet.drv.
   */
  public V put(final long k, final V v) {
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k))) {
        final V oldValue = value[pos];
        value[pos] = v;
        return oldValue;
      }
      pos = (pos + 1) & mask;
    }
    used[pos] = true;
    key[pos] = k;
    value[pos] = v;
    if (++size >= maxFill)
      rehash(arraySize(size + 1, f));
    if (ASSERTS)
      checkTable();
    return defRetValue;
  }

  public V put(final Long ok, final V ov) {
    final V v = (ov);
    final long k = ((ok).longValue());
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k))) {
        final V oldValue = (value[pos]);
        value[pos] = v;
        return oldValue;
      }
      pos = (pos + 1) & mask;
    }
    used[pos] = true;
    key[pos] = k;
    value[pos] = v;
    if (++size >= maxFill)
      rehash(arraySize(size + 1, f));
    if (ASSERTS)
      checkTable();
    return (this.defRetValue);
  }

  /**
   * Shifts left entries with the specified hash code, starting at the specified
   * position, and empties the resulting free entry.
   * 
   * @param pos a starting position.
   * @return the position cleared by the shifting process.
   */
  protected final int shiftKeys(int pos) {
    // Shift entries with the same hash.
    int last, slot;
    for (;;) {
      pos = ((last = pos) + 1) & mask;
      while (used[pos]) {
        slot = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((key[pos])
          ^ mask)
          & mask;
        if (last <= pos ? last >= slot || slot > pos : last >= slot
          && slot > pos)
          break;
        pos = (pos + 1) & mask;
      }
      if (!used[pos])
        break;
      key[last] = key[pos];
      value[last] = value[pos];
    }
    used[last] = false;
    value[last] = null;
    return last;
  }

  public V remove(final long k) {
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k))) {
        size--;
        final V v = value[pos];
        shiftKeys(pos);
        return v;
      }
      pos = (pos + 1) & mask;
    }
    return defRetValue;
  }

  public V remove(final Object ok) {
    final long k = ((((Long) (ok)).longValue()));
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k))) {
        size--;
        final V v = value[pos];
        shiftKeys(pos);
        return (v);
      }
      pos = (pos + 1) & mask;
    }
    return (this.defRetValue);
  }

  public V get(final Long ok) {
    final long k = ((ok).longValue());
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k)))
        return (value[pos]);
      pos = (pos + 1) & mask;
    }
    return (this.defRetValue);
  }

  public V get(final long k) {
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k)))
        return value[pos];
      pos = (pos + 1) & mask;
    }
    return defRetValue;
  }

  public boolean containsKey(final long k) {
    // The starting point.
    int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
      & mask;
    // There's always an unused entry.
    while (used[pos]) {
      if (((key[pos]) == (k)))
        return true;
      pos = (pos + 1) & mask;
    }
    return false;
  }

  public boolean containsValue(final Object v) {
    final V value[] = this.value;
    final boolean used[] = this.used;
    for (int i = n; i-- != 0;)
      if (used[i] && ((value[i]) == null ? (v) == null : (value[i]).equals(v)))
        return true;
    return false;
  }

  /*
   * Removes all elements from this map.
   * 
   * <P>To increase object reuse, this method does not change the table size. If
   * you want to reduce the table size, you must use {@link #trim()}.
   */
  public void clear() {
    if (size == 0)
      return;
    size = 0;
    BooleanArrays.fill(used, false);
    // We null all object entries so that the garbage collector can do its work.
    ObjectArrays.fill(value, null);
  }
  
  /**
   * Clean, object is still leaved there, to enable reuse, not repeated memory
   * allocation
   */
  public int clean() {
    if (size == 0)
      return 0;
    size = 0;
    BooleanArrays.fill(used, false);
    return used.length;
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * The entry class for a hash map does not record key and value, but rather
   * the position in the hash table of the corresponding entry. This is
   * necessary so that calls to {@link java.util.Map.Entry#setValue(Object)} are
   * reflected in the map
   */
  private final class MapEntry implements Long2ObjectMap.Entry<V>,
    Map.Entry<Long, V> {
    // The table index this entry refers to, or -1 if this entry has been
    // deleted.
    private int index;

    MapEntry(final int index) {
      this.index = index;
    }

    public Long getKey() {
      return (Long.valueOf(key[index]));
    }

    public long getLongKey() {
      return key[index];
    }

    public V getValue() {
      return (value[index]);
    }

    public V setValue(final V v) {
      final V oldValue = value[index];
      value[index] = v;
      return oldValue;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(final Object o) {
      if (!(o instanceof Map.Entry))
        return false;
      Map.Entry<Long, V> e = (Map.Entry<Long, V>) o;
      return ((key[index]) == (((e.getKey()).longValue())))
        && ((value[index]) == null ? ((e.getValue())) == null : (value[index])
          .equals((e.getValue())));
    }

    public int hashCode() {
      return it.unimi.dsi.fastutil.HashCommon.long2int(key[index])
        ^ ((value[index]) == null ? 0 : (value[index]).hashCode());
    }

    public String toString() {
      return key[index] + "=>" + value[index];
    }
  }

  /** An iterator over a hash map. */
  private class MapIterator {
    /**
     * The index of the next entry to be returned, if positive or zero. If
     * negative, the next entry to be returned, if any, is that of index -pos -2
     * from the {@link #wrapped} list.
     */
    int pos = Long2ObjectOpenHashMap.this.n;
    /**
     * The index of the last entry that has been returned. It is -1 if either we
     * did not return an entry yet, or the last returned entry has been removed.
     */
    int last = -1;
    /** A downward counter measuring how many entries must still be returned. */
    int c = size;
    /**
     * A lazily allocated list containing the keys of elements that have wrapped
     * around the table because of removals; such elements would not be
     * enumerated (other elements would be usually enumerated twice in their
     * place).
     */
    LongArrayList wrapped;
    {
      final boolean used[] = Long2ObjectOpenHashMap.this.used;
      if (c != 0)
        while (!used[--pos])
          ;
    }

    public boolean hasNext() {
      return c != 0;
    }

    public int nextEntry() {
      if (!hasNext())
        throw new NoSuchElementException();
      c--;
      // We are just enumerating elements from the wrapped list.
      if (pos < 0) {
        final long k = wrapped.getLong(-(last = --pos) - 2);
        // The starting point.
        int pos = (int) it.unimi.dsi.fastutil.HashCommon
          .murmurHash3((k) ^ mask) & mask;
        // There's always an unused entry.
        while (used[pos]) {
          if (((key[pos]) == (k)))
            return pos;
          pos = (pos + 1) & mask;
        }
      }
      last = pos;
      // System.err.println( "Count: " + c );
      if (c != 0) {
        final boolean used[] = Long2ObjectOpenHashMap.this.used;
        while (pos-- != 0 && !used[pos])
          ;
        // When here pos < 0 there are no more elements to be enumerated by
        // scanning, but wrapped might be nonempty.
      }
      return last;
    }

    /**
     * Shifts left entries with the specified hash code, starting at the
     * specified position, and empties the resulting free entry. If any entry
     * wraps around the table, instantiates lazily {@link #wrapped} and stores
     * the entry key.
     * 
     * @param pos a starting position.
     * @return the position cleared by the shifting process.
     */
    protected final int shiftKeys(int pos) {
      // Shift entries with the same hash.
      int last, slot;
      for (;;) {
        pos = ((last = pos) + 1) & mask;
        while (used[pos]) {
          slot = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((key[pos])
            ^ mask)
            & mask;
          if (last <= pos ? last >= slot || slot > pos : last >= slot
            && slot > pos)
            break;
          pos = (pos + 1) & mask;
        }
        if (!used[pos])
          break;
        if (pos < last) {
          // Wrapped entry.
          if (wrapped == null)
            wrapped = new LongArrayList();
          wrapped.add(key[pos]);
        }
        key[last] = key[pos];
        value[last] = value[pos];
      }
      used[last] = false;
      value[last] = null;
      return last;
    }

    public void remove() {
      if (last == -1)
        throw new IllegalStateException();
      if (pos < -1) {
        // We're removing wrapped entries.
        Long2ObjectOpenHashMap.this.remove(wrapped.getLong(-pos - 2));
        last = -1;
        return;
      }
      size--;
      if (shiftKeys(last) == pos && c > 0) {
        c++;
        nextEntry();
      }
      last = -1; // You can no longer remove this entry.
      if (ASSERTS)
        checkTable();
    }

    public int skip(final int n) {
      int i = n;
      while (i-- != 0 && hasNext())
        nextEntry();
      return n - i - 1;
    }
  }

  private class EntryIterator extends MapIterator implements
    ObjectIterator<Long2ObjectMap.Entry<V>> {
    private MapEntry entry;

    public Long2ObjectMap.Entry<V> next() {
      return entry = new MapEntry(nextEntry());
    }

    @Override
    public void remove() {
      super.remove();
      entry.index = -1; // You cannot use a deleted entry.
    }
  }

  private class FastEntryIterator extends MapIterator implements
    ObjectIterator<Long2ObjectMap.Entry<V>> {
    final edu.iu.harp.util.BasicEntry<V> entry = new edu.iu.harp.util.BasicEntry<V>(
      ((long) 0), (null));

    public edu.iu.harp.util.BasicEntry<V> next() {
      final int e = nextEntry();
      entry.key = key[e];
      entry.value = value[e];
      return entry;
    }
  }

  private final class MapEntrySet extends
    AbstractObjectSet<Long2ObjectMap.Entry<V>> implements FastEntrySet<V> {
    public ObjectIterator<Long2ObjectMap.Entry<V>> iterator() {
      return new EntryIterator();
    }

    public ObjectIterator<Long2ObjectMap.Entry<V>> fastIterator() {
      return new FastEntryIterator();
    }

    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
      if (!(o instanceof Map.Entry))
        return false;
      final Map.Entry<Long, V> e = (Map.Entry<Long, V>) o;
      final long k = ((e.getKey()).longValue());
      // The starting point.
      int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
        & mask;
      // There's always an unused entry.
      while (used[pos]) {
        if (((key[pos]) == (k)))
          return ((value[pos]) == null ? ((e.getValue())) == null
            : (value[pos]).equals((e.getValue())));
        pos = (pos + 1) & mask;
      }
      return false;
    }

    @SuppressWarnings("unchecked")
    public boolean remove(final Object o) {
      if (!(o instanceof Map.Entry))
        return false;
      final Map.Entry<Long, V> e = (Map.Entry<Long, V>) o;
      final long k = ((e.getKey()).longValue());
      // The starting point.
      int pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
        & mask;
      // There's always an unused entry.
      while (used[pos]) {
        if (((key[pos]) == (k))) {
          Long2ObjectOpenHashMap.this.remove(e.getKey());
          return true;
        }
        pos = (pos + 1) & mask;
      }
      return false;
    }

    public int size() {
      return size;
    }

    public void clear() {
      Long2ObjectOpenHashMap.this.clear();
    }
  }

  public FastEntrySet<V> long2ObjectEntrySet() {
    if (entries == null)
      entries = new MapEntrySet();
    return entries;
  }

  /**
   * An iterator on keys.
   * 
   * <P>
   * We simply override the {@link java.util.ListIterator#next()}/
   * {@link java.util.ListIterator#previous()} methods (and possibly their
   * type-specific counterparts) so that they return keys instead of entries.
   */
  private final class KeyIterator extends MapIterator implements LongIterator {
    public KeyIterator() {
      super();
    }

    public long nextLong() {
      return key[nextEntry()];
    }

    public Long next() {
      return (Long.valueOf(key[nextEntry()]));
    }
  }

  private final class KeySet extends AbstractLongSet {
    public LongIterator iterator() {
      return new KeyIterator();
    }

    public int size() {
      return size;
    }

    public boolean contains(long k) {
      return containsKey(k);
    }

    public boolean remove(long k) {
      final int oldSize = size;
      Long2ObjectOpenHashMap.this.remove(k);
      return size != oldSize;
    }

    public void clear() {
      Long2ObjectOpenHashMap.this.clear();
    }
  }

  public LongSet keySet() {
    if (keys == null)
      keys = new KeySet();
    return keys;
  }

  /**
   * An iterator on values.
   * 
   * <P>
   * We simply override the {@link java.util.ListIterator#next()}/
   * {@link java.util.ListIterator#previous()} methods (and possibly their
   * type-specific counterparts) so that they return values instead of entries.
   */
  private final class ValueIterator extends MapIterator implements
    ObjectIterator<V> {
    public ValueIterator() {
      super();
    }

    public V next() {
      return value[nextEntry()];
    }
  }

  public ObjectCollection<V> values() {
    if (values == null)
      values = new AbstractObjectCollection<V>() {
        public ObjectIterator<V> iterator() {
          return new ValueIterator();
        }

        public int size() {
          return size;
        }

        public boolean contains(Object v) {
          return containsValue(v);
        }

        public void clear() {
          Long2ObjectOpenHashMap.this.clear();
        }
      };
    return values;
  }

  /**
   * Rehashes the map, making the table as small as possible.
   * 
   * <P>
   * This method rehashes the table to the smallest size satisfying the load
   * factor. It can be used when the set will not be changed anymore, so to
   * optimize access speed and size.
   * 
   * <P>
   * If the table size is already the minimum possible, this method does
   * nothing.
   * 
   * @return true if there was enough memory to trim the map.
   * @see #trim(int)
   */
  public boolean trim() {
    final int l = arraySize(size, f);
    if (l >= n)
      return true;
    try {
      rehash(l);
    } catch (OutOfMemoryError cantDoIt) {
      return false;
    }
    return true;
  }

  /**
   * Rehashes this map if the table is too large.
   * 
   * <P>
   * Let <var>N</var> be the smallest table size that can hold
   * <code>max(n,{@link #size()})</code> entries, still satisfying the load
   * factor. If the current table size is smaller than or equal to <var>N</var>,
   * this method does nothing. Otherwise, it rehashes this map in a table of
   * size <var>N</var>.
   * 
   * <P>
   * This method is useful when reusing maps. {@linkplain #clear() Clearing a
   * map} leaves the table size untouched. If you are reusing a map many times,
   * you can call this method with a typical size to avoid keeping around a very
   * large table just because of a few large transient maps.
   * 
   * @param n the threshold for the trimming.
   * @return true if there was enough memory to trim the map.
   * @see #trim()
   */
  public boolean trim(final int n) {
    final int l = HashCommon.nextPowerOfTwo((int) Math.ceil(n / f));
    if (this.n <= l)
      return true;
    try {
      rehash(l);
    } catch (OutOfMemoryError cantDoIt) {
      return false;
    }
    return true;
  }

  /**
   * Rehashes the map.
   * 
   * <P>
   * This method implements the basic rehashing strategy, and may be overriden
   * by subclasses implementing different rehashing strategies (e.g., disk-based
   * rehashing). However, you should not override this method unless you
   * understand the internal workings of this class.
   * 
   * @param newN the new size
   */
  @SuppressWarnings("unchecked")
  protected void rehash(final int newN) {
    int i = 0, pos;
    final boolean used[] = this.used;
    long k;
    final long key[] = this.key;
    final V value[] = this.value;
    final int mask = newN - 1; // Note that this is used by the hashing macro
    final long newKey[] = new long[newN];
    final V newValue[] = (V[]) new Object[newN];
    final boolean newUsed[] = new boolean[newN];
    for (int j = size; j-- != 0;) {
      while (!used[i])
        i++;
      k = key[i];
      pos = (int) it.unimi.dsi.fastutil.HashCommon.murmurHash3((k) ^ mask)
        & mask;
      while (newUsed[pos])
        pos = (pos + 1) & mask;
      newUsed[pos] = true;
      newKey[pos] = k;
      newValue[pos] = value[i];
      i++;
    }
    n = newN;
    this.mask = mask;
    maxFill = maxFill(n, f);
    this.key = newKey;
    this.value = newValue;
    this.used = newUsed;
  }

  /**
   * Returns a hash code for this map.
   * 
   * This method overrides the generic method provided by the superclass. Since
   * <code>equals()</code> is not overriden, it is important that the value
   * returned by this method is the same value as the one returned by the
   * overriden method.
   * 
   * @return a hash code for this map.
   */
  public int hashCode() {
    int h = 0;
    for (int j = size, i = 0, t = 0; j-- != 0;) {
      while (!used[i])
        i++;
      t = it.unimi.dsi.fastutil.HashCommon.long2int(key[i]);
      if (this != value[i])
        t ^= ((value[i]) == null ? 0 : (value[i]).hashCode());
      h += t;
      i++;
    }
    return h;
  }

  private void checkTable() {
  }
}

