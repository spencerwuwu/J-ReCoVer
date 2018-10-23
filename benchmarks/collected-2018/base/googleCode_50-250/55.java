// https://searchcode.com/api/result/3886628/

package org.gbif.ecat.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * * A threadsafe implementation of a last recently used cache.
 * The map size is limited to a maximum size with the oldest entries being evicted if needed.
 */
public class LimitedList<E> extends ArrayList<E> {

  private final int listSize;

  public LimitedList(int listSize) {
    this.listSize = listSize;
  }

  @Override
  public synchronized boolean add(E o) {
    reduce();
    return super.add(o);
  }

  @Override
  public synchronized void add(int index, E element) {
    reduce();
    super.add(index, element);
  }

  @Override
  public synchronized boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized boolean addAll(int index, Collection<? extends E> c) {
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
  public synchronized E get(int arg0) {
    return super.get(arg0);
  }

  @Override
  public synchronized int indexOf(Object arg0) {
    return super.indexOf(arg0);
  }

  @Override
  public synchronized boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public synchronized int lastIndexOf(Object arg0) {
    return super.lastIndexOf(arg0);
  }

  private void reduce() {
    if (size() == listSize) {
      remove(0);
    }
  }

  @Override
  public synchronized E remove(int arg0) {
    return super.remove(arg0);
  }

  @Override
  public synchronized boolean remove(Object arg0) {
    return super.remove(arg0);
  }

  @Override
  public synchronized E set(int arg0, E arg1) {
    return super.set(arg0, arg1);
  }

  @Override
  public synchronized int size() {
    return super.size();
  }

  @Override
  public synchronized List<E> subList(int arg0, int arg1) {
    return super.subList(arg0, arg1);
  }

  @Override
  public synchronized Object[] toArray() {
    return super.toArray();
  }

  @Override
  public synchronized <T> T[] toArray(T[] arg0) {
    return super.toArray(arg0);
  }

}

