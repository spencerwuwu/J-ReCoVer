// https://searchcode.com/api/result/13740165/

/*
 * Code for blog.techhead.biz
 * Distributed under BSD-style license
 */

package biz.techhead.funcy;

import java.util.Iterator;
import static biz.techhead.funcy.Tuples.T;
import biz.techhead.funcy.Tuples.T2;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import static java.lang.Math.max;

/**
 * Iterables is a utility class for building and working with Iterators,
 * Iterables and Collections.
 * 
 * <p>It was inspired by the Groovy programming language and Common Lisp (as is
 * almost all of this library).  Much of the effort here is duplicated by the
 * fantastic Google Collections API.  I probably would not have bothered to
 * write this class had I looked into Google Collections first. However, I have
 * since retrofitted it to use Google Collections (when available) for the
 * immutable Sets/Lists.
 *
 * <code><pre>&nbsp;{@literal
 * import static biz.techhead.funcy.Iterables.*;
 * import static biz.techhead.funcy.SugarFunc.*;
 * 
 * final Set<Integer> digits = Set(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).immutable();
 *
 * FuncySet<Integer> first20 = map(digits,
 *     new Mapper<Integer,Integer>() { void plus10() { out = in + 10; } },
 *     copySet(digits) );
 *
 * Set<Integer> first10odd = first20.subset(
 *     new Predicate<Integer>() { void odd() { out = ((in % 2) != 0); } } );
 *
 * int sumOf1st10Odd = reduce(first10odd,
 *     new Reducer<Integer>() { void sum() { out = in._1 + in._2; } } );
 *
 * FuncyList<String> dwarfs = List("Happy", "Grumpy", "Sleepy", "Sneezy",
 *                                 "Dopey", "Bashful");
 * dwarfs.add("Doc");
 * dwarfs = dwarfs.map(
 *     new Mapper<String,String>() { void up() { out = in.toUpperCase(); } } );
 * }</pre></code>
 *
 * @see Maps
 * @see SugarFunc
 *
 * @author Jonathan Hawkes <jhawkes at techhead.biz>
 */
public class Iterables {

    // prevent instantiation
    private Iterables() {}

    /**
     * Constructs a new {@link Iterables.FuncySet FuncySet}.  This Set will
     * allow null values and {@code iterator} will iterate over the values in
     * the order that they were added.
     */
    public static <T> FuncySet<T> Set() {
        return new FuncyLinkedHashSet<T>();
    }
    
    /**
     * Constructs a new {@link Iterables.FuncySet FuncySet} initialized with the
     * given elements. This Set will allow null values and {@code iterator} will
     * iterate over them in the order that they were added.
     */
    public static <T> FuncySet<T> Set(T... elements) {
        FuncySet<T> set = new FuncyLinkedHashSet<T>(elements.length);
        for (T element : elements) {
            set.add(element);
        }
        return set;
    }

    /**
     * Copies the given Collection into a new
     * {@link Iterables.FuncySet FuncySet}.
     */
    public static <T> FuncySet<T> copySet(Collection<? extends T> col) {
        return new FuncyLinkedHashSet<T>(col);
    }

    /**
     * Copies all elements of the given Iterable into a new
     * {@link Iterables.FuncySet FuncySet}.  If the Iterable is infinite,
     * this method will never return (until {@code OutOfMemoryError}).
     */
    public static <T> FuncySet<T> copySet(Iterable<? extends T> it) {
        FuncySet<T> set = Set();
        return addAll(set, it);
    }

    /**
     * Returns a new {@link Iterables.FuncyList FuncyList}.  Allows null values.
     */
    public static <T> FuncyList<T> List() {
        return new FuncyArrayList<T>();
    }

    /**
     * Returns a new {@link Iterables.FuncyList FuncyList} initialized with the
     * given elements.  Allows null values.
     */
    public static <T> FuncyList<T> List(T... elements) {
        FuncyList<T> list = new FuncyArrayList<T>(elements.length);
        for (T element : elements) {
            list.add(element);
        }
        return list;
    }

    /**
     * Copies the contents of the given Collection into a new
     * {@link Iterables.FuncyList FuncyList}.
     */
    public static <T> FuncyList<T> copyList(Collection<? extends T> col) {
        return new FuncyArrayList<T>(col);
    }

    /**
     * Copies all elements of the given Iterable into a new
     * {@link Iterables.FuncyList FuncyList}.  If the Iterable is infinite,
     * this method will never return (until {@code OutOfMemoryError}).
     */
    public static <T> FuncyList<T> copyList(Iterable<? extends T> it) {
        FuncyList<T> list = List();
        return addAll(list, it);
    }

    /**
     * Copies all elements of the given Iterable into the given Collection.
     * If the Iterable is infinite, this method will never return (until
     * {@code OutOfMemoryError}).
     * @return The Collection copied into
     */
    public static <T,C extends Collection<? super T>>
            C addAll(C col, Iterable<? extends T> it) {
        for (T item : it) {
            col.add(item);
        }
        return col;
    }

    /**
     * Copies all elements of the given Iterable into a new Set which cannot
     * be modified.  If the Google Collections API is available in the
     * classpath, then its {@code ImmutableSet} will be used (providing that no
     * element is null).  If the Iterable is infact an instance of
     * {@code ImmutableSet} then no copy will be made.  If the Iterable is
     * infinite, this method will never return (until {@code OutOfMemoryError}).
     * The iteration order of the Set will be the same as the order of the given
     * Iterable.
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> immutableSet(Iterable<? extends T> it) {
        return ( Set<T> ) toImmutableSet.call(it);
    }

    /**
     * Copies all elements of the given Iterable into a new List which cannot
     * be modified.  If the Google Collections API is available in the
     * classpath, then its {@code ImmutableList} will be used (providing that no
     * element is null).  If the Iterable is infact an instance of
     * {@code ImmutableList} then no copy will be made.  If the Iterable is
     * infinite, this method will never return (until {@code OutOfMemoryError}).
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> immutableList(Iterable<? extends T> it) {
        return ( List<T> ) toImmutableList.call(it);
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Set<T> makeImmutableSet(Iterable<? extends T> it) {
        Set<T> copy = (it instanceof Collection) ?
            new LinkedHashSet<T>( ( Collection<? extends T> ) it ) :
            addAll( new LinkedHashSet<T>(), it );
        return Collections.unmodifiableSet(copy);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> makeImmutableList(Iterable<? extends T> it) {
        List<T> copy = (it instanceof Collection) ?
            new ArrayList<T>( ( Collection<? extends T> ) it ) :
            addAll( new ArrayList<T>(), it );
        return Collections.unmodifiableList(copy);
    }

    /**
     * Takes an Iterator and a function and returns a new
     * {@link Iterables.FuncyIterator FuncyIterator} that applies
     * the given function to each element as it is requested.
     */
    public static <I,O,E extends RuntimeException>
            FuncyIterator<O> map(final Iterator<I> it,
                                 final FuncE<O,? super I,E> f) {
        return new AbstractFuncyIterator<O>() {

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public O next() {
                return f.call( it.next() );
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    /**
     * Takes an Iterable and a function and returns a new
     * {@link Iterables.FuncyIterable FuncyIterable} that applies
     * the given function to each element as it is requested.
     */
    public static <I,O,E extends RuntimeException>
            FuncyIterable<O> map(final Iterable<I> col,
                                 final FuncE<O,? super I,E> f) {
        return new AbstractFuncyIterable<O>() {

            @Override
            public FuncyIterator<O> iterator() {
                return Iterables.map(col.iterator(), f);
            }
        };
    }

    /**
     * Applies the given function to every element of the given Iterable
     * and adds the result to the given Collection.  If the Iterable is
     * infinite, this method will never return (until an exception is thrown
     * or {@code OutOfMemoryError}).
     *
     * @return the Collection
     * @throws E if the function does
     */
    public static <C extends Collection<? super O>,I,O,E extends Throwable>
            C map(Iterable<I> it, FuncE<O,? super I,E> f, C out) throws E {
        for (I item : it) {
            out.add( f.call(item) );
        }
        return out;
    }

    /**
     * Applies the given function to every element of the given Collection
     * and adds the result to a newly constructed
     * {@link Iterables.FuncyList FuncyList}.
     *
     * @return the new {@code FuncyList}
     * @throws E if the function does
     */
    public static <I,O,E extends Throwable>
            FuncyList<O> map(Collection<I> col, FuncE<O,? super I,E> f)
            throws E {
        return map(col, f, new FuncyArrayList<O>(col.size()));
    }

    /**
     * Like {@code reduce} found in Common Lisp, this method takes the first
     * two elements from the Iterator and passes them into the reducing
     * function.  The result of the function, along with the next element are
     * then again fed into the reducing function and so on, etc.  If the
     * Iterator does not terminate at some point then neither will this method.
     *
     * @return a single reduced value
     * @throws E if the function does
     */
    public static <T,E extends Throwable>
            T reduce(Iterator<? extends T> it, FuncE<T,T2<T,T>,E> f) throws E {
        T reduced = it.next();
        while (it.hasNext()) {
            reduced = f.call( T(reduced, it.next()) );
        }
        return reduced;
    }
    
    /**
     * Like {@code reduce} found in Common Lisp, this method takes the first
     * two elements from the Iterable and passes them into the reducing
     * function.  The result of the function, along with the next element are
     * then again fed into the reducing function and so on, etc.  If the
     * Iterable does not terminate at some point then neither will this method.
     *
     * @return a single reduced value
     * @throws E if the function does
     */
    public static <T,E extends Throwable>
            T reduce(Iterable<? extends T> it, FuncE<T,T2<T,T>,E> f) throws E {
        return reduce(it.iterator(), f);
    }

    /**
     * Returns a {@link Iterables.FuncyIterator FuncyIterator} which iterates
     * over only a subset of the elements of the given Iterator. (Those selected
     * by the given predicate function.)
     * 
     * <p>There is the possibility of the Iterator returned by this method
     * <em>getting stuck</em> if the source Iterator is infinite (or very large)
     * and the given predicate selects none or very few of the elements.
     */
    public static <T,E extends RuntimeException>
            FuncyIterator<T> subset(final Iterator<T> it,
                                    final FuncE<Boolean,? super T,E> f) {
        return new AbstractFuncyIterator<T>() {

            private T next = null;
            private boolean foundNext = false;
            private boolean cursorsInSync = false;

            @Override
            public boolean hasNext() {
                if (foundNext) return true;
                while ( it.hasNext() ) {
                    next = it.next();
                    cursorsInSync = false;
                    if ( f.call(next) ) {
                        foundNext = true;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                if ( hasNext() ) {
                    cursorsInSync = true;
                    foundNext = false;
                    return next;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                if (cursorsInSync) {
                    it.remove();
                }
                throw new IllegalStateException("Cannot call remove" +
                        " after hasNext but before next");
            }
        };
    }
    
    /**
     * Returns a {@link Iterables.FuncyIterable FuncyIterable} which iterates
     * over only a subset of the elements of the given Iterable. (Those selected
     * by the given predicate function.)
     * 
     * <p>There is the possibility of the Iterable returned by this method
     * <em>getting stuck</em> if the source Iterable is infinite (or very large)
     * and the given predicate selects none or very few of the elements.
     */
    public static <T,E extends RuntimeException>
            FuncyIterable<T> subset(final Iterable<T> it,
                                    final FuncE<Boolean,? super T,E> f) {
        return new AbstractFuncyIterable<T>() {

            @Override
            public FuncyIterator<T> iterator() {
                return Iterables.subset(it.iterator(), f);
            }
        };
    }

    /**
     * Iterates over the given Iterable and adds those elements selected by
     * the given predicate to the given Collection.
     *
     * <p>If the Iterable is infinite, this method will never return (unless
     * an exception is thrown or an {@code OutOfMemoryError}).
     *
     * @return the Collection
     * @throws E if the predicate throws it
     */
    public static <C extends Collection<? super T>,T,E extends Throwable>
            C subset(Iterable<T> it, FuncE<Boolean,? super T,E> f, C out)
            throws E {
        for (T item : it) {
            if ( f.call(item) ) {
                out.add(item);
            }
        }
        return out;
    }

    /**
     * Checks each element of the Collection against the given predicate
     * and returns the selected elements in a new
     * {@link Iterables.FuncyList FuncyList}.
     * 
     * @return the {@code FuncyList}
     * @throws E if the predicate throws it
     */
    public static <T,E extends Throwable>
            FuncyList<T> subset(Collection<T> it, FuncE<Boolean,? super T,E> f)
            throws E {
        FuncyList<T> subset = List();
        return subset(it, f, subset);
    }
            
    /**
     * Checks each element of the Set against the given predicate
     * and returns the selected elements in a new
     * {@link Iterables.FuncySet FuncySet}.
     * 
     * @return the {@code FuncySet}
     * @throws E if the predicate throws it
     */
    public static <T,E extends Throwable>
            FuncySet<T> subset(Set<T> it, FuncE<Boolean,? super T,E> f)
            throws E {
        FuncySet<T> subset = Set();
        return subset(it, f, subset);
    }

    /**
     * Executes the given function on each element of the Iterator.  If the
     * Iterator is infinite, this method will not return (unless the Iterator
     * or function throws an exception).
     *
     * @throws E if the function throws it
     */
    public static <T,E extends Throwable>
            void each(Iterator<T> i, FuncE<?,? super T,E> f) throws E {
        while ( i.hasNext() ) {
            f.call( i.next() );
        }
    }

    /**
     * Executes the given function on each element of the Iterable.  If the
     * Iterable is infinite, this method will not return (unless the Iterable
     * or function throws an exception).
     *
     * @throws E if the function throws it
     */
    public static <T,E extends Throwable>
            void each(Iterable<T> seq, FuncE<?,? super T,E> f) throws E {
        for (T item : seq) {
            f.call(item);
        }
    }
            
    /**
     * Returns {@code true} if the given predicate also returns true for every
     * element returned by the Iterator.  If the Iterator does not have a
     * finite number of elements, this method may not return.
     * 
     * @throws E if the predicate does
     */
    public static <T,E extends Throwable>
            boolean every(Iterator<T> i, FuncE<Boolean,? super T,E> f)
            throws E {
        while ( i.hasNext() ) {
            if ( !f.call( i.next() ) ) return false;
        }
        return true;
    }
            
    /**
     * Returns {@code true} if the given predicate also returns true for every
     * element returned by the Iterable.  If the Iterable does not have a
     * finite number of elements, this method may not return.
     * 
     * @throws E if the predicate does
     */
    public static <T,E extends Throwable>
            boolean every(Iterable<T> seq, FuncE<Boolean,? super T,E> f)
            throws E {
        for (T item : seq) {
            if ( !f.call(item) ) return false;
        }
        return true;
    }
            
    /**
     * Returns {@code true} if the given predicate also returns true for any
     * element returned by the Iterator.  If the Iterator does not have a
     * finite number of elements, this method may not return.
     * 
     * @throws E if the predicate does
     */
    public static <T,E extends Throwable>
            boolean any(Iterator<T> i, FuncE<Boolean,? super T,E> f) throws E {
        while ( i.hasNext() ) {
            if ( f.call( i.next() ) ) return true;
        }
        return false;
    }
    
    /**
     * Returns {@code true} if the given predicate also returns true for any
     * element returned by the Iterable.  If the Iterable does not have a
     * finite number of elements, this method may not return.
     * 
     * @throws E if the predicate does
     */
    public static <T,E extends Throwable>
            boolean any(Iterable<T> seq, FuncE<Boolean,? super T,E> f)
            throws E {
        for (T item : seq) {
            if ( f.call(item) ) return true;
        }
        return false;
    }

    /**
     * An {@code Iterator} with some convenience methods for the operations
     * available in {@link Iterables}.
     */
    public static interface FuncyIterator<T> extends Iterator<T> {

        /**
         * Same as {@link Iterables#map(Iterator,FuncE) Iterables.map(this, f)}
         */
        <O,E extends RuntimeException>
                FuncyIterator<O> map(FuncE<O,? super T,E> f);

        /**
         * Same as {@link Iterables#subset(Iterator,FuncE)
         * Iterables.subset(this, f)}
         */
        <E extends RuntimeException>
                FuncyIterator<T> subset(FuncE<Boolean,? super T,E> f);

        /**
         * Same as {@link Iterables#each(Iterator,FuncE)
         * Iterables.each(this, f)}
         */
        <E extends Throwable>
                void each(FuncE<?,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#every(Iterator,FuncE)
         * Iterables.every(this, f)}
         */
        <E extends Throwable>
                boolean every(FuncE<Boolean,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#any(Iterator,FuncE) Iterables.any(this, f)}
         */
        <E extends Throwable>
                boolean any(FuncE<Boolean,? super T,E> f) throws E;
    }

    /**
     * An {@code Iterable} with some convenience methods for the operations
     * available in {@link Iterables}.
     */
    public static interface FuncyIterable<T> extends Iterable<T> {

        /**
         * Applies the given function to every element of this {@code Iterable}.
         *
         * <p>There are two distinct implementation strategies.  If this
         * Iterable is also a {@code Collection}, it is expected that the
         * implementation will apply the mapping function to all the elements at
         * up-front cost and then return a new Collection. Otherwise, it is
         * expected that the function will be applied to elements only as they
         * are asked for.
         *
         * @see Iterables#map(Collection,FuncE)
         * @see Iterables#map(Iterable,FuncE)
         *
         * @throws E if implementation tries to map this entire {@code Iterable}
         *           at up-front cost and the mapping function throws E
         * @throws CheckedBaggageException if the implementation does a lazy
         *           mapping of this {@code Iterable} and the mapping function
         *           throws a checked exception when applied to an element
         */
        <O,E extends Throwable>
                FuncyIterable<O> map(FuncE<O,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#map(Iterable,FuncE,Collection)
         * Iterables.map(this, f, out)}
         */
        <O,C extends Collection<? super O>,E extends Throwable>
                C map(FuncE<O,? super T,E> f, C out) throws E;

        //Could not figure out how to get this as a properly type-safe method
        //The common-sense try below did not work.
        // public <D super T,E extends Throwable>
        //    D reduce(FuncE<D,T2<D,D>> f) throws E;

        /**
         * Returns another {@code FuncyIterable} that contains only a subset of
         * the elements found in this one.
         *
         * <p>There are two distinct implementation strategies.  If this
         * Iterable is also a {@code Collection}, it is expected that the
         * implementation will apply the predicate function to all the elements
         * at up-front cost and then return a new Collection consisting only
         * of the selected elements. Otherwise, it is expected that the function
         * will be applied to elements only as they are asked for and will
         * continue to apply the predicate to all elements of the source
         * Iterable until one is selected or there are no more elements.
         *
         * @see Iterables#subset(Collection,FuncE)
         * @see Iterables#subset(Iterable,FuncE)
         *
         * @throws E if implementation tries to find the subset of this entire
         *           {@code Iterable} at up-front cost and the predicate
         *           function throws E
         * @throws CheckedBaggageException if the implementation finds a lazy
         *           subset of this {@code Iterable} and the predicate function
         *           throws a checked exception when applied to an element
         */
        <E extends Throwable>
                FuncyIterable<T> subset(FuncE<Boolean,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#subset(Iterable,FuncE,Collection)
         * Iterables.subset(this, f, out)}
         */
        <C extends Collection<? super T>,E extends Throwable>
                C subset(FuncE<Boolean,? super T,E> f, C out) throws E;

        /**
         * Same as {@link Iterables#each(Iterable,FuncE)
         * Iterables.each(this, f)}
         */
        <E extends Throwable>
                void each(FuncE<?,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#every(Iterable,FuncE)
         * Iterables.every(this, f)}
         */
        <E extends Throwable>
                boolean every(FuncE<Boolean,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#any(Iterable,FuncE) Iterables.any(this, f)}
         */
        <E extends Throwable>
                boolean any(FuncE<Boolean,? super T,E> f) throws E;

        @Override
        FuncyIterator<T> iterator();
    }

    /**
     * A {@code Collection} with some convenience methods for the operations
     * available in {@link Iterables}.
     */
    public static interface FuncyCollection<T>
            extends FuncyIterable<T>, Collection<T> {

        /**
         * Same as {@link Iterables#map(Collection,FuncE)
         * Iterables.map(this, f)}
         */
        @Override
        <O,E extends Throwable>
                FuncyList<O> map(FuncE<O,? super T,E> f) throws E;
        
        @Override
        <E extends Throwable>
                FuncyCollection<T> subset(FuncE<Boolean,? super T,E> f)
                throws E;
        
        /**
         * Returns an immutable copy of this Collection.
         */
        Collection<T> immutable();
    }

    /**
     * A {@code Set} with some convenience methods for the operations
     * available in {@link Iterables}.
     */
    public static interface FuncySet<T>
            extends FuncyCollection<T>, Set<T> {

        /**
         * Same as {@link Iterables#subset(Set,FuncE) Iterables.subset(this, f)}
         */
        @Override
        <E extends Throwable>
                FuncySet<T> subset(FuncE<Boolean,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#immutableSet(Iterable)
         * Iterables.immutableSet(this)}
         */
        @Override
        Set<T> immutable();
    }

    /**
     * A {@code List} with some convenience methods for the operations
     * available in {@link Iterables}.
     */
    public static interface FuncyList<T>
            extends FuncyCollection<T>, List<T> {

        /**
         * Same as {@link Iterables#subset(Collection,FuncE)
         * Iterables.subset(this, f)}
         */
        @Override
        <E extends Throwable>
                FuncyList<T> subset(FuncE<Boolean,? super T,E> f) throws E;

        /**
         * Same as {@link Iterables#immutableList(Iterable)
         * Iterables.immutableList(this)}
         */
        @Override
        List<T> immutable();
    }

    static abstract class AbstractFuncyIterator<T>
            implements FuncyIterator<T> {

        @Override
        public <O,E extends RuntimeException>
                FuncyIterator<O> map(FuncE<O,? super T,E> f) {
            return Iterables.map(this, f);
        }

        @Override
        public <E extends RuntimeException>
                FuncyIterator<T> subset(FuncE<Boolean,? super T,E> f) {
            return Iterables.subset(this, f);
        }

        @Override
        public <E extends Throwable>
                void each(FuncE<?,? super T,E> f) throws E {
            Iterables.each(this, f);
        }

        @Override
        public <E extends Throwable>
                boolean every(FuncE<Boolean,? super T,E> f) throws E {
            return Iterables.every(this, f);
        }

        @Override
        public <E extends Throwable>
                boolean any(FuncE<Boolean,? super T,E> f) throws E {
            return Iterables.any(this, f);
        }
    }

    /**
     * Decorates an {@code Iterator} as a {@code FuncyIterator}.
     */
    private static class DelegatingFuncyIterator<T>
            extends AbstractFuncyIterator<T> {

        DelegatingFuncyIterator(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public void remove() {
            it.remove();
        }

        private Iterator<T> it;
    }

    /**
     * The simplest implementation of a {@code FuncyIterable}.
     */
    static abstract class AbstractFuncyIterable<T> implements FuncyIterable<T> {

        @Override
        public <O, E extends Throwable>
                FuncyIterable<O> map(FuncE<O, ? super T, E> f) {
            return Iterables.map(this, Funcs.asFunc(f));
        }

        @Override
        public <O, C extends Collection<? super O>, E extends Throwable>
                C map(FuncE<O, ? super T, E> f, C out) throws E {
            return Iterables.map(this, f, out);
        }

        @Override
        public <E extends Throwable>
                FuncyIterable<T> subset(FuncE<Boolean, ? super T, E> f) {
            return Iterables.subset(this, Funcs.asFunc(f));
        }

        @Override
        public <C extends Collection<? super T>, E extends Throwable>
                C subset(FuncE<Boolean, ? super T, E> f, C out) throws E {
            return Iterables.subset(this, f, out);
        }

        @Override
        public <E extends Throwable>
                void each(FuncE<?, ? super T, E> f) throws E {
            Iterables.each(this, f);
        }

        @Override
        public <E extends Throwable>
                boolean every(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.every(this, f);
        }

        @Override
        public <E extends Throwable>
                boolean any(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.any(this, f);
        }
    }

    /**
     * A {@code LinkedHashSet} that implements {@code FuncySet} for convenience.
     */
    private static class FuncyLinkedHashSet<T> extends LinkedHashSet<T>
            implements FuncySet<T> {

        FuncyLinkedHashSet() {
            super();
        }

        FuncyLinkedHashSet(int capacity) {
            super( max( (int)(capacity/.75f)+1, 16 ), .75f );
        }

        FuncyLinkedHashSet(Collection<? extends T> col) {
            super(col);
        }

        public <O, E extends Throwable>
                FuncyList<O> map(FuncE<O, ? super T, E> f) throws E {
            return Iterables.map(this, f);
        }

        public <O, C extends Collection<? super O>, E extends Throwable>
                C map(FuncE<O, ? super T, E> f, C out) throws E {
            return Iterables.map(this, f, out);
        }

        public <E extends Throwable>
                FuncySet<T> subset(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.subset(this, f);
        }

        public <C extends Collection<? super T>, E extends Throwable>
                C subset(FuncE<Boolean, ? super T, E> f, C out) throws E {
            return Iterables.subset(this, f, out);
        }

        public <E extends Throwable>
                void each(FuncE<?, ? super T, E> f) throws E {
            Iterables.each(this, f);
        }

        public <E extends Throwable>
                boolean every(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.every(this, f);
        }

        public <E extends Throwable>
                boolean any(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.any(this, f);
        }

        public FuncyIterator<T> iterator() {
            return new DelegatingFuncyIterator<T>( super.iterator() );
        }

        public Set<T> immutable() {
            return Iterables.immutableSet(this);
        }
    }

    /**
     * An {@code ArrayList} that implements {@code FuncyList} for convenience.
     */
    static class FuncyArrayList<T> extends ArrayList<T>
            implements FuncyList<T> {

        FuncyArrayList() {
            super();
        }

        FuncyArrayList(int capacity) {
            super(capacity);
        }

        FuncyArrayList(Collection<? extends T> col) {
            super(col);
        }

        public <O, E extends Throwable>
                FuncyList<O> map(FuncE<O, ? super T, E> f) throws E {
            return Iterables.map(this, f);
        }

        public <O, C extends Collection<? super O>, E extends Throwable>
                C map(FuncE<O, ? super T, E> f, C out) throws E {
            return Iterables.map(this, f, out);
        }

        public <E extends Throwable>
                FuncyList<T> subset(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.subset(this, f);
        }

        public <C extends Collection<? super T>, E extends Throwable>
                C subset(FuncE<Boolean, ? super T, E> f, C out) throws E {
            return Iterables.subset(this, f, out);
        }

        public <E extends Throwable>
                void each(FuncE<?, ? super T, E> f) throws E {
            Iterables.each(this, f);
        }

        public <E extends Throwable>
                boolean every(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.every(this, f);
        }

        public <E extends Throwable>
                boolean any(FuncE<Boolean, ? super T, E> f) throws E {
            return Iterables.any(this, f);
        }

        public FuncyIterator<T> iterator() {
            return new DelegatingFuncyIterator<T>( super.iterator() );
        }

        public List<T> immutable() {
            return Iterables.immutableList(this);
        }
    }

    /**
     * This function is used to transform a given Iterable into an immutable
     * Set. If the Google Collections API is available, it will use that.
     */
    private static final Func<Set,Iterable> toImmutableSet;

    /**
     * This function is used to transform a given Iterable into an immutable
     * List. If the Google Collections API is available, it will use that.
     */
    private static final Func<List,Iterable> toImmutableList;

    /*
     * This ugly little bit of code attempts to use the Google Collections API
     * without directly depending on it.
     *
     * If Google Collections is found in the classpath, then immutable Sets
     * and Lists will be constructed using
     * com.google.common.collect.ImmutableSet.copyOf and
     * com.google.common.collect.ImmutableList.copyOf respectively.
     */

    static {
        Func<Set,Iterable> temp;
        try {
            final Method m =
                    Class.forName("com.google.common.collect.ImmutableSet")
                        .getMethod("copyOf");
            temp = new Func<Set,Iterable>() {
                @SuppressWarnings("unchecked")
                public Set call(Iterable in) {
                    try {
                        return (Set) m.invoke(null, in);
                    } catch (IllegalAccessException iae) {
                        // The method IS accessible
                        throw new AssertionError();
                    } catch (InvocationTargetException ite) {
                        Throwable cause = ite.getCause();
                        if (cause instanceof NullPointerException) {
                            // We had a null value.
                            // Just do it the old-fashioned way.
                            return makeImmutableSet(in);
                        }
                        throw new AssertionError("Google Collections API" +
                                " changed - unexpected Exception from" +
                                " ImmutableSet.copyOf");
                    }
                }
            };
        } catch (Exception ex) {
               //ClassNotFoundException
               //NoSuchMethodException
            temp = new Func<Set,Iterable>() {
                @SuppressWarnings("unchecked")
                public Set call(Iterable in) {
                    return makeImmutableSet(in);
                }  
            };
        }
        toImmutableSet = temp;
    }

    /*
     * And copy-and-paste job 2
     */

    static {
        Func<List,Iterable> temp;
        try {
            final Method m =
                    Class.forName("com.google.common.collect.ImmutableList")
                        .getMethod("copyOf");
            temp = new Func<List,Iterable>() {
                @SuppressWarnings("unchecked")
                public List call(Iterable in) {
                    try {
                        return (List) m.invoke(null, in);
                    } catch (IllegalAccessException iae) {
                        // The method IS accessible
                        throw new AssertionError();
                    } catch (InvocationTargetException ite) {
                        Throwable cause = ite.getCause();
                        if (cause instanceof NullPointerException) {
                            // We had a null value.
                            // Just do it the old-fashioned way.
                            return makeImmutableList(in);
                        }
                        throw new AssertionError("Google Collections API" +
                                " changed - unexpected Exception from" +
                                " ImmutableList.copyOf");
                    }
                }
            };
        } catch (Exception ex) {
               //ClassNotFoundException
               //NoSuchMethodException
            temp = new Func<List,Iterable>() {
                @SuppressWarnings("unchecked")
                public List call(Iterable in) {
                    return makeImmutableList(in);
                }  
            };
        }
        toImmutableList = temp;
    }
}

