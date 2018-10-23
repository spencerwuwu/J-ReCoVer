// https://searchcode.com/api/result/3886639/

package org.gbif.ecat.utils;

import java.util.Collection;
import java.util.Stack;

public class LimitedStack<E> extends Stack<E> {

  private final int size;

  public LimitedStack(int size) {
    this.size = size;
  }

  private void reduce() {
    if (size() == size) {
      remove(0);
    }
  }

  @Override
  public E push(E item) {
    reduce();
    return super.push(item);
  }

  @Override
  public synchronized void addElement(E obj) {
    reduce();
    super.addElement(obj);
  }

  @Override
  public synchronized void insertElementAt(E obj, int index) {
    reduce();
    super.insertElementAt(obj, index);
  }

  @Override
  public boolean add(E o) {
    reduce();
    return super.add(o);
  }

  @Override
  public void add(int index, E element) {
    reduce();
    super.add(index, element);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

}

