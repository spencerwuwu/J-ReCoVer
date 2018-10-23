// https://searchcode.com/api/result/3886632/

package org.gbif.ecat.utils;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * A threadsafe implementation of a last recently used cache.
 * The map size is limited to a maximum size with the oldest entries being evicted if needed.
 */
public class LimitedSet<E> extends LinkedHashSet<E> {

  private final int listSize;

  public LimitedSet(int listSize) {
    this.listSize = listSize;
  }

  @Override
  public synchronized boolean add(E o) {
    reduce();
    return super.add(o);
  }

  @Override
  public synchronized boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void clear() {
    super.clear();
  }

  @Override
  public synchronized boolean contains(Object arg0) {
    return super.contains(arg0);
  }

  @Override
  public synchronized boolean isEmpty() {
    return super.isEmpty();
  }

  private void reduce() {
    if (size() == listSize) {
      remove(0);
    }
  }

  @Override
  public synchronized boolean remove(Object arg0) {
    return super.remove(arg0);
  }

  @Override
  public synchronized boolean removeAll(Collection<?> arg0) {
    return super.removeAll(arg0);
  }

  @Override
  public synchronized int size() {
    return super.size();
  }

}

