// https://searchcode.com/api/result/133843949/

package com.humaorie.dollar;

/*
 * Dollar, http://bitbucket.org/dfa/dollar
 * (c) 2010, 2011 Davide Angelocola <davide.angelocola@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import com.humaorie.dollar.ArrayWrapper.BooleanArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.ByteArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.CharArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.DoubleArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.FloatArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.IntegerArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.LongArrayWrapper;
import com.humaorie.dollar.ArrayWrapper.ShortArrayWrapper;
import com.humaorie.dollar.Dollar.ArrayWrapper;
import com.humaorie.dollar.Dollar.Function;
import com.humaorie.dollar.Dollar.Function2;
import com.humaorie.dollar.Dollar.Function3;
import com.humaorie.dollar.Dollar.Wrapper;
import static com.humaorie.dollar.Dollar.not;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

public abstract class AbstractWrapper<T> implements Dollar.Wrapper<T> {

    // the minimum interface that subclasses *must* implement
    @Override
    public abstract Wrapper<T> copy();

    @Override
    public abstract int size();

    @Override
    public Wrapper<T> concat(Wrapper<T> wrapper) {
        return new ConcatWrapper<T>().concat(this).concat(wrapper);
    }

    @Override
    public Wrapper<T> concat(T... items) {
        return new ConcatWrapper<T>().concat(this).concat(items);
    }

    @Override
    public Wrapper<T> concat(Collection<T> items) {
        return new ConcatWrapper<T>().concat(this).concat(items);
    }

    @Override
    public Wrapper<T> repeat(int n) {
        return new RepeatWrapper<T>(this, n);
    }

    @Override
    public Wrapper<T> fill(T object) {
        return new FillWrapper<T>(object, size());
    }

    @Override
    public Wrapper<T> slice(int j) {
        return slice(0, j);
    }

    @Override
    public Wrapper<T> slice(int i, int j) {
        return new ListWrapper<T>(toList()).slice(i, j);
    }

    @Override
    public Wrapper<T> shuffle() {
        return shuffle(new Random());
    }

    @Override
    public Wrapper<T> shuffle(Random random) {
        Preconditions.require(random != null, "random must be non-null");
        return new ListWrapper<T>(this).shuffle(random);
    }

    @Override
    public Wrapper<T> sort() {
        return new ListWrapper<T>(this).sort();
    }

    @Override
    public Wrapper<T> sort(Comparator<T> comparator) {
        return new ListWrapper<T>(this).sort(comparator);
    }

    @Override
    public Iterator<T> iterator() {
        return new ListWrapper<T>(this).iterator();
    }

    @Override
    public Wrapper<T> reverse() {
        return new ListWrapper<T>(this).reverse();
    }

    @Override
    public String join() {
        return join("");
    }

    @Override
    public String join(String separator) {
        Preconditions.require(separator != null, "separator must be non-null");
        Separator sep = new Separator(separator);
        StringBuilder sb = new StringBuilder();

        for (T item : this) {
            sb.append(sep).append(item);
        }

        return sb.toString();
    }

    protected static class Separator {

        private final String separator;
        private boolean wasCalled;

        public Separator(String separator) {
            this.separator = separator;
            this.wasCalled = false;
        }

        @Override
        public String toString() {
            if (!wasCalled) {
                wasCalled = true;
                return "";
            } else {
                return separator;
            }
        }
    }

    @Override
    public List<T> toList() {
        return toList(LinkedList.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<T> toList(Class<? extends List> concreteListClass) {
        List<T> list = createList(concreteListClass);
        addToCollection(list, this);
        return list;
    }

    @Override
    public Set<T> toSet() {
        return toSet(HashSet.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<T> toSet(Class<? extends Set> concreteSetClass) {
        Set<T> set = createSet(concreteSetClass);
        addToCollection(set, this);
        return set;
    }

    @Override
    public T[] toArray() {
        return iterableToArray(this, size());
    }

    @Override
    public T[] toArray(Class<T> concreteClass) {
        return iterableToArray(this, concreteClass, size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayWrapper<T> convert() {
        T[] array = toArray();
        Class<?> componentType = array.getClass().getComponentType();
        if (componentType.equals(Byte.class)) {
            return (ArrayWrapper<T>) new ByteArrayWrapper((Byte[]) array);
        } else if (componentType.equals(Short.class)) {
            return (ArrayWrapper<T>) new ShortArrayWrapper((Short[]) array);
        } else if (componentType.equals(Integer.class)) {
            return (ArrayWrapper<T>) new IntegerArrayWrapper((Integer[]) array);
        } else if (componentType.equals(Long.class)) {
            return (ArrayWrapper<T>) new LongArrayWrapper((Long[]) array);
        } else if (componentType.equals(Character.class)) {
            return (ArrayWrapper<T>) new CharArrayWrapper((Character[]) array);
        } else if (componentType.equals(Boolean.class)) {
            return (ArrayWrapper<T>) new BooleanArrayWrapper((Boolean[]) array);
        } else if (componentType.equals(Float.class)) {
            return (ArrayWrapper<T>) new FloatArrayWrapper((Float[]) array);
        } else if (componentType.equals(Double.class)) {
            return (ArrayWrapper<T>) new DoubleArrayWrapper((Double[]) array);
        } else {
            throw new IllegalArgumentException("cannot convert() array of "
                    + componentType.toString());
        }
    }

    /**
     * Lazy implementation. Function not called until necessary.
     */
    @Override
    public Wrapper<T> filter(final Function<Boolean, T> predicate) {
        return new LazyFilterWrapper<T>(this, predicate);
    }

    @Override
    public Wrapper<T> reject(final Function<Boolean, T> predicate) {
        return new LazyFilterWrapper<T>(this, not(predicate));
    }

    @Override
    public boolean all(Function<Boolean, T> predicate) {
        return findIt(not(predicate)) == null;
    }

    @Override
    public boolean any(Function<Boolean, T> predicate) {
        return findIt(predicate) != null;
    }

    @Override
    public T find(Function<Boolean, T> predicate) {
        T t = findIt(predicate);
        if (t == null) {
            throw new NoSuchElementException();
        } else {
            return t;
        }
    }

    private T findIt(Function<Boolean, T> predicate) {
        for (T t : this) {
            if (predicate.call(t)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public int indexOf(Function<Boolean, T> predicate) {
        int i = 0;
        for (T t : this) {
            if (predicate.call(t)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * Lazy implementation. Function not called until necessary.
     */
    @Override
    public <R> Wrapper<R> map(final Function<R, T> function) {
        return new LazyMappedWrapper<R, T>(this, function);
    }

    @Override
    public Wrapper<T> each(Function<T, T> function) {
        for (T item : this) {
            function.call(item);
        }
        return this;
    }

    @Override
    public <R> R reduce(R initial, Function2<R, T, R> function) {
        R value = initial;
        for (T item : this) {
            value = function.call(item, value);
        }
        return value;
    }

    @Override
    public <R, V> R reduce(R initial, Function3<R, T, R, V> function, V context) {
        R value = initial;
        for (T item : this) {
            value = function.call(item, value, context);
        }
        return value;
    }

    // helpers
    static <T> T[] iterableToArray(Iterable<T> iterable, int size) {
        Class<?> concreteClass = null;

        for (T item : iterable) {
            if (item != null) {
                concreteClass = item.getClass();
                break;
            }
        }

        return iterableToArray(iterable, concreteClass, size);
    }

    @SuppressWarnings("unchecked")
    static <T> T[] iterableToArray(Iterable<T> iterable,
            Class<?> concreteClass, int size) {
        if (concreteClass == null) {
            return null;
        }

        T[] array = (T[]) Array.newInstance(concreteClass, size);
        int i = 0;

        for (T item : iterable) {
            array[i++] = item;
        }

        return array;
    }

    // extend the interface of Collection.addAll() for arrays and iterable;
    static <T> void addToCollection(Collection<T> collection, T... array) {
        addToCollection(collection, new com.humaorie.dollar.ArrayWrapper<T>(array));
    }

    static <T> void addToCollection(Collection<T> collection, Iterable<T> iterable) {
        for (T item : iterable) {
            collection.add(item);
        }
    }

    // no-arg constructor
    static IllegalArgumentException illegalConcreteClass(Class<?> concreteClass) {
        return new IllegalArgumentException(String.format(
                "class '%s' must have a public, no-args constructor",
                concreteClass.getName()));
    }

    static void assertNoArgsConstructor(Class<?> concreteClass) {
        try {
            Constructor<?> noArgsConstructor = concreteClass.getConstructor();

            if (noArgsConstructor == null) {
                throw illegalConcreteClass(concreteClass);
            }
        } catch (NoSuchMethodException ex) {
            throw illegalConcreteClass(concreteClass);
        } catch (SecurityException ex) {
            throw illegalConcreteClass(concreteClass);
        }
    }

    static Object newInstanceOf(Class<?> concreteClass) {
        try {
            return concreteClass.newInstance();
        } catch (InstantiationException ex) {
            throw illegalConcreteClass(concreteClass);
        } catch (IllegalAccessException ex) {
            throw illegalConcreteClass(concreteClass);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T> Set<T> createSet(Class<? extends Set> concreteSetClass) {
        assertNoArgsConstructor(concreteSetClass);
        return (Set<T>) newInstanceOf(concreteSetClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T> List<T> createList(Class<? extends List> concreteListClass) {
        assertNoArgsConstructor(concreteListClass);
        return (List<T>) newInstanceOf(concreteListClass);
    }
}

