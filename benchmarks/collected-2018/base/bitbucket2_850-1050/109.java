// https://searchcode.com/api/result/132812293/

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.util.streams;

import java.util.*;
import java.util.functions.BiBlock;
import java.util.functions.Factory;
import java.util.functions.UnaryOperator;

/**
 * Streams
 *
 * @author Brian Goetz
 */
public class Streams {
    private Streams() {
        throw new Error("no instances");
    }

    // MapStream

    public static<K,V> MapStream<K,V> stream(SortedMap<K,V> source) {
        return new MapPipeline<>(new MapTraversableMapStreamAccessor<>(source, true, true, source.size()));
    }

    public static<K,V> MapStream<K,V> stream(Map<K,V> source) {
        return new MapPipeline<>(new MapTraversableMapStreamAccessor<>(source, true, false, source.size()));
    }

    public static<K,V> MapStream<K,V> stream(MapTraversable<K,V> source) {
        return new MapPipeline<>(new MapTraversableMapStreamAccessor<>(source));
    }

    public static<K,V> MapStream<K,V> stream(MapTraversable<K,V> source, int sizeOrUnknown) {
        return new MapPipeline<>(new MapTraversableMapStreamAccessor<>(source, false, false, sizeOrUnknown));
    }

    // Stream

    public static<T> Stream<T> stream(SortedSet<T> source) {
        return new ValuePipeline<>(new TraversableStreamAccessor<>(source, true, true, source.size()));
    }

    public static<T> Stream<T> stream(Set<T> source) {
        return new ValuePipeline<>(new TraversableStreamAccessor<>(source, true, false, source.size()));
    }

    public static<T> Stream<T> stream(Collection<T> source) {
        return new ValuePipeline<>(new TraversableStreamAccessor<>(source, false, false, source.size()));
    }

    public static<T> Stream<T> stream(Traversable<T> source, int sizeOrUnknown) {
        return new ValuePipeline<>(new TraversableStreamAccessor<>(source, false, false, sizeOrUnknown));
    }

    public static<T> Stream<T> stream(Traversable<T> source) {
        return new ValuePipeline<>(new TraversableStreamAccessor<>(source));
    }

    public static<T> Stream<T> stream(Iterable<T> source, int sizeOrUnknown) {
        return stream(source.iterator(), sizeOrUnknown);
    }

    public static<T> Stream<T> stream(Iterable<T> source) {
        return stream(source.iterator());
    }

    public static<T> Stream<T> stream(Iterator<T> source, int sizeOrUnknown) {
        return new ValuePipeline<>(new IteratorStreamAccessor<>(source, sizeOrUnknown));
    }

    public static<T> Stream<T> stream(Iterator<T> source) {
        return new ValuePipeline<>(new IteratorStreamAccessor<>(source));
    }

    public static <T, L extends RandomAccess & List<T>> Spliterator<T> spliterator(L source) {
        return new RandomAccessListSpliterator<>(source);
    }

    public static <T, L extends RandomAccess & List<T>> Spliterator<T> spliterator(L source, int offset, int length) {
        return new RandomAccessListSpliterator<>(source, offset, length);
    }

    public static <T, L extends RandomAccess & List<T>> Stream<T> stream(L source) {
        return new ValuePipeline<>(new RandomAccessListStreamAccessor<T,L>(source));
    }

    public static <T, L extends RandomAccess & List<T>> Stream<T> stream(L source, int offset, int length) {
        return new ValuePipeline<>(new RandomAccessListStreamAccessor<>(source, offset, length));
    }

    public static <T, L extends RandomAccess & List<T>> Stream<T> parallel(L source) {
        return new ValuePipeline<>(new RandomAccessListParallelStreamAccessor<>(source));
    }

    public static <T, L extends RandomAccess & List<T>> Stream<T> parallel(L source, int offset, int length) {
        return new ValuePipeline<>(new RandomAccessListParallelStreamAccessor<>(source, offset, length));
    }


    public static <T> Spliterator<T> spliterator(T[] source) {
        return new ArraySpliterator<>(source);
    }

    public static <T> Spliterator<T> spliterator(T[] source, int offset, int length) {
        return new ArraySpliterator<>(source, offset, length);
    }

    public static <T> Stream<T> stream(T[] source) {
        return new ValuePipeline<>(new ArrayStreamAccessor<T>(source));
    }

    public static <T> Stream<T> stream(T[] source, int offset, int length) {
        return new ValuePipeline<>(new ArrayStreamAccessor<>(source, offset, length));
    }

    public static <T> Stream<T> parallel(T[] source) {
        return new ValuePipeline<>(new ArrayParallelStreamAccessor<>(source));
    }

    public static <T> Stream<T> parallel(T[] source, int offset, int length) {
        return new ValuePipeline<>(new ArrayParallelStreamAccessor<>(source, offset, length));
    }

    public static<T> Stream<T> parallel(Spliterator<T> source, int sizeOrUnknown) {
        return new ValuePipeline<>(new SpliteratorStreamAccessor<>(source, sizeOrUnknown));
    }

    public static<T> Stream<T> parallel(Spliterator<T> source) {
        return new ValuePipeline<>(new SpliteratorStreamAccessor<>(source));
    }

    // Infinite streams

    public static<T> Stream<T> iterate(final T seed, final UnaryOperator<T> f) {
        Objects.requireNonNull(f);

        final InfiniteIterator<T> iterate = new InfiniteIterator<T>() {
            T t = null;

            @Override
            public T next() {
                return t = (t == null) ? seed : f.operate(t);
            }
        };

        return new ValuePipeline<>(new InfiniteIteratorStreamAccessor<>(iterate));
    }

    public static<T> Stream<T> repeat(T t) {
        return repeat(-1, t);
    }

    public static<T> Stream<T> repeat(final int n, final T t) {
        if (n < 0) {
            final InfiniteIterator<T> iterate = new InfiniteIterator<T>() {
                @Override
                public T next() {
                    return t;
                }
            };

            return new ValuePipeline<>(new InfiniteIteratorStreamAccessor<>(iterate));
        }
        else {
            final Iterator<T> repeat = new Iterator<T>() {
                int c = n;

                @Override
                public boolean hasNext() {
                    return c > 0;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    c--;
                    return t;
                }
            };

            return new ValuePipeline<>(new IteratorStreamAccessor<>(repeat));
        }
    }

    public static<T> Stream<T> repeatedly(Factory<T> f) {
        return repeatedly(-1, f);
    }

    public static<T> Stream<T> repeatedly(final int n, final Factory<T> f) {
        Objects.requireNonNull(f);

        if (n < 0) {
            final InfiniteIterator<T> repeatedly = () -> f.make();

            return new ValuePipeline<>(new InfiniteIteratorStreamAccessor<>(repeatedly));
        }
        else {
            final Iterator<T> repeatedly = new Iterator<T>() {
                int c = n;

                @Override
                public boolean hasNext() {
                    return c > 0;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    c--;
                    return f.make();
                }
            };

            return new ValuePipeline<>(new IteratorStreamAccessor<>(repeatedly));
        }
    }

    public static<T> Stream<T> cycle(final Iterable<T> source) {
        Objects.requireNonNull(source);

        // Check if the source is empty
        if (!source.iterator().hasNext()) {
            throw new IllegalArgumentException("Source Iterable to cycle elements from must not be empty");
        }

        final InfiniteIterator<T> cycle = new InfiniteIterator<T>() {
            Iterator<T> i = source.iterator();

            @Override
            public T next() {
                if (!i.hasNext()) {
                    i = source.iterator();
                }

                return i.next();
            }
        };

        return new ValuePipeline<>(new InfiniteIteratorStreamAccessor<>(cycle));
    }

    // @@@ Need from(StreamAccessor) methods


    private static final Spliterator<?> EMPTY_SPLITERATOR = new Spliterator<Object>() {
            @Override
            public int getNaturalSplits() {
                return 0;
            }

            @Override
            public Spliterator<Object> split() {
                return emptySpliterator();
            }

            @Override
            public Iterator<Object> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public void into(Sink<Object, ?, ?> sink) {
                sink.begin(0);
                sink.end();
            }

            @Override
            public int getSizeIfKnown() {
                return 0;
            }

            @Override
            public boolean isPredictableSplits() {
                return true;
            }
        };

    public static<T> Spliterator<T> emptySpliterator() {
        return (Spliterator<T>) EMPTY_SPLITERATOR;
    }

    private static class IteratorStreamAccessor<T>
            implements StreamAccessor.ForSequential<T>, Iterator<T> {
        private final Iterator<T> it;
        private final int sizeOrUnknown;

        public IteratorStreamAccessor(Iterator<T> it) {
            this.it = it;
            this.sizeOrUnknown = -1;
        }

        public IteratorStreamAccessor(Iterator<T> it, int sizeOrUnknown) {
            this.it = it;
            this.sizeOrUnknown = sizeOrUnknown;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            sink.begin(getSizeIfKnown());
            while (it.hasNext())
                sink.accept(it.next());
            sink.end();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Iterator<T> iterator() {
            return it;
        }

        @Override
        public int getStreamFlags() {
            return (sizeOrUnknown >= 0) ? Stream.FLAG_SIZED : 0;
        }

        @Override
        public int getSizeIfKnown() {
            return sizeOrUnknown;
        }
    }

    private static interface InfiniteIterator<T> extends Iterator<T> {
        @Override
        public boolean hasNext() default {
            return true;
        }
    }

    // @@@ Limited to sequential access, thus parallel evaluation cannot occur
    //     Change later to implement StreamAccessor.spliterator
    private static class InfiniteIteratorStreamAccessor<T>
            implements StreamAccessor.ForSequential<T>, InfiniteIterator<T> {
        private final InfiniteIterator<T> it;

        public InfiniteIteratorStreamAccessor(InfiniteIterator<T> it) {
            this.it = it;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            // Implementing this method would result in a infinite loop
            // @@@ No mechanism for ops to short circuit push loop
            // @@@ Spliterator to the rescue? arity of 1, n elements for lhs, rhs is always infinite
            throw new UnsupportedOperationException("Infinite streams cannot push to a sink");
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public Iterator<T> iterator() {
            return it;
        }

        @Override
        public int getStreamFlags() {
            return Stream.FLAG_INFINITE;
        }

        @Override
        public int getSizeIfKnown() {
            return -1;
        }
    }

    static class SpliteratorStreamAccessor<T> implements StreamAccessor<T> {
        private final Spliterator<T> spliterator;
        private final int sizeOrUnknown;

        public SpliteratorStreamAccessor(Spliterator<T> spliterator) {
            this.spliterator = spliterator;
            this.sizeOrUnknown = -1;
        }

        public SpliteratorStreamAccessor(Spliterator<T> spliterator, int sizeOrUnknown) {
            this.spliterator = spliterator;
            this.sizeOrUnknown = sizeOrUnknown;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            spliterator.into(sink);
        }

        @Override
        public int getStreamFlags() {
            int flags = 0;
            flags |= (sizeOrUnknown >= 0) ? Stream.FLAG_SIZED : 0;
            return flags;
        }

        @Override
        public int getSizeIfKnown() {
            return sizeOrUnknown;
        }

        @Override
        public Iterator<T> iterator() {
            return spliterator.iterator();
        }

        @Override
        public Spliterator<T> spliterator() {
            return spliterator;
        }

        @Override
        public StreamShape getShape() {
            return StreamShape.VALUE;
        }

        @Override
        public boolean isParallel() {
            return true;
        }
    }

    private static class TraversableStreamAccessor<T>
            implements StreamAccessor.ForSequential<T>, Iterator<T> {
        private final Traversable<T> traversable;
        private final int flags;
        private final int sizeOrUnknown;
        Iterator<T> iterator = null;

        TraversableStreamAccessor(Traversable<T> traversable) {
            this(traversable, false, false, -1);
        }

        TraversableStreamAccessor(Traversable<T> traversable, boolean distinct, boolean sorted, int sizeOrUnknown) {
            this.traversable = traversable;
            this.flags = (distinct ? Stream.FLAG_DISTINCT : 0 ) |
                        (sorted ? Stream.FLAG_SORTED : 0 ) |
                        (sizeOrUnknown >= 0 ? Stream.FLAG_SIZED : 0);
            this.sizeOrUnknown = sizeOrUnknown;
        }

        public Iterator<T> iterator() {
            if (iterator == null)
                iterator = traversable.iterator();
            return iterator;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            sink.begin(getSizeIfKnown());
            if (iterator == null) {
                traversable.forEach(sink);
                iterator = Collections.emptyIterator();
            }
            else {
                while (iterator.hasNext())
                    sink.accept(iterator.next());
            }
            sink.end();
        }

        @Override
        public T next() {
            return iterator().next();
        }

        @Override
        public boolean hasNext() {
            return iterator().hasNext();
        }

        @Override
        public int getStreamFlags() {
            return flags;
        }

        @Override
        public int getSizeIfKnown() {
            return sizeOrUnknown;
        }
    }

    private static class MapTraversableMapStreamAccessor<K,V>
            implements MapStreamAccessor.ForSequential<K,V> {
        private final MapTraversable<K,V> traversable;
        private final int flags;
        private final int sizeOrUnknown;
        MapIterator<K,V> iterator = null;

        MapTraversableMapStreamAccessor(MapTraversable<K,V> traversable) {
            this(traversable, false, false, -1);
        }

        MapTraversableMapStreamAccessor(MapTraversable<K,V> traversable, boolean distinct, boolean sorted, int sizeOrUnknown) {
            this.traversable = traversable;
            this.flags = (distinct ? Stream.FLAG_DISTINCT : 0 ) |
                        (sorted ? Stream.FLAG_SORTED : 0 ) |
                        (sizeOrUnknown >= 0 ? Stream.FLAG_SIZED : 0);
            this.sizeOrUnknown = sizeOrUnknown;
        }

        @Override
        public MapIterator<K,V> iterator() {
            if (iterator == null) {
                iterator = traversable.iterator();
            }
            return iterator;
        }

        @Override
        public void into(final Sink<Mapping<K,V>, ?, ?> sink) {
            Sink<Mapping<K, V>, K, V> castSink = (Sink<Mapping<K, V>, K, V>) sink;
            if (iterator == null) {
                traversable.forEach(castSink);
                // @@@ Collections.emptyMapIterator ?
                iterator = MapIterator.IteratorAdapter.adapt(Collections.<Mapping<K,V>>emptyIterator());
            }
            else {
                while (iterator.hasNext()) {
                    K k = iterator.nextKey();
                    V v = iterator.curValue();
                    castSink.accept(k, v);
                }
            }
        }

        @Override
        public void forEach(final BiBlock<? super K,? super V> block) {
            if (iterator == null) {
                traversable.forEach(block);
                // @@@ Collections.emptyMapIterator ?
                iterator = MapIterator.IteratorAdapter.adapt(Collections.<Mapping<K,V>>emptyIterator());
            } else {
                while (iterator.hasNext()) {
                    block.apply(iterator.nextKey(), iterator.curValue());
                }
            }
        }

        @Override
        public Mapping<K,V> next() {
            return iterator().next();
        }

        @Override
        public boolean hasNext() {
            return iterator().hasNext();
        }

        @Override
        public int getStreamFlags() {
            return flags;
        }

        @Override
        public int getSizeIfKnown() {
            return sizeOrUnknown;
        }

        @Override
        public K nextKey() {
            return iterator().nextKey();
        }

        @Override
        public V nextValue() {
            return iterator().nextValue();
        }

        @Override
        public K curKey() {
            return iterator().curKey();
        }

        @Override
        public V curValue() {
            return iterator().curValue();
        }
    }

    private abstract static class RandomAccessIterator<T> implements Iterator<T> {
        protected final int endOffset;
        protected int offset;

        protected RandomAccessIterator(int startOffset, int len) {
            this.endOffset = startOffset + len;
            this.offset = startOffset;

            assert offset >= 0 : "offset not positive";
            assert endOffset >= offset : "end lower than start";
        }

        @Override
        public final boolean hasNext() {
            return offset < endOffset;
        }

        @Override
        public abstract T next();
    }

    private static class ArrayIterator<T> extends RandomAccessIterator<T> {
        protected final T[] elements;

        private ArrayIterator(T[] elements, int startOffset, int len) {
            super(startOffset, len);
            this.elements = Objects.requireNonNull(elements);

            assert (offset < elements.length) || (0 == len && 0 == offset) : "offset not in array";
            assert endOffset <= elements.length : "end not in array";
        }

        @Override
        public T next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            return elements[offset++];
        }
    }

    private static class RandomAccessListIterator<T, L extends RandomAccess & List<T>>  extends RandomAccessIterator<T> {
        protected final L elements;

        private RandomAccessListIterator(L elements, int startOffset, int len) {
            super(startOffset, len);

            assert (startOffset < elements.size()) || (0 == len && 0 == startOffset) : "offset not in list";
            assert endOffset <= elements.size() : "end not in list";

            this.elements = Objects.requireNonNull(elements);
        }

        @Override
        public T next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            return elements.get(offset++);
        }
    }

    private static class ArraySpliterator<T> extends ArrayIterator<T> implements Spliterator<T> {
        boolean traversing = false;

        ArraySpliterator(T[] elements) {
            this(elements, 0, elements.length);
        }

        ArraySpliterator(T[] elements, int offset, int length) {
            super(elements, offset, length);
        }

        @Override
        public int getSizeIfKnown() {
            return endOffset - offset;
        }

        @Override
        public int estimateSize() {
            return getSizeIfKnown();
        }

        @Override
        public boolean isPredictableSplits() {
            return true;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            traversing = true;
            sink.begin(getSizeIfKnown());
            for (int i=offset; i<endOffset; i++) {
                sink.accept(elements[i]);
            }
            // update only once; reduce heap write traffic
            offset = endOffset;
            sink.end();
        }

        @Override
        public int getNaturalSplits() {
            return (endOffset - offset > 1) ? 1 : 0;
        }

        @Override
        public Spliterator<T> split() {
            if (traversing) {
                throw new IllegalStateException("split after starting traversal");
            }
            int t = (endOffset - offset) / 2;
            Spliterator<T> ret = new ArraySpliterator<>(elements, offset, t);
            offset += t;
            return ret;
        }

        @Override
        public Iterator<T> iterator() {
            traversing = true;
            return this;
        }
    }

    private static class RandomAccessListSpliterator<T, L extends RandomAccess & List<T>> extends RandomAccessListIterator<T,L> implements Spliterator<T> {
        boolean traversing = false;

        RandomAccessListSpliterator(L elements) {
            this(elements, 0, elements.size());
        }

        RandomAccessListSpliterator(L elements, int offset, int length) {
            super(elements, offset, length);
        }

        @Override
        public int getSizeIfKnown() {
            return endOffset - offset;
        }

        @Override
        public int estimateSize() {
            return getSizeIfKnown();
        }

        @Override
        public boolean isPredictableSplits() {
            return true;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            traversing = true;
            sink.begin(getSizeIfKnown());
            for (int i=offset; i<endOffset; i++) {
                sink.accept(elements.get(i));
            }
            // update only once; reduce heap write traffic
            offset = endOffset;
            sink.end();
        }

        @Override
        public int getNaturalSplits() {
            return (endOffset - offset > 1) ? 1 : 0;
        }

        @Override
        public Spliterator<T> split() {
            if (traversing) {
                throw new IllegalStateException("split after starting traversal");
            }
            int t = (endOffset - offset) / 2;
            Spliterator<T> ret = new RandomAccessListSpliterator<>(elements, offset, t);
            offset += t;
            return ret;
        }

        @Override
        public Iterator<T> iterator() {
            traversing = true;
            return this;
        }
    }

    private static class ArrayStreamAccessor<T>
            extends ArrayIterator<T> implements StreamAccessor<T> {

        ArrayStreamAccessor(T[] elements) {
            this(elements, 0, elements.length);
        }

        ArrayStreamAccessor(T[] elements, int offset, int length) {
            super(elements, offset, length);
        }

        @Override
        public int getStreamFlags() {
            return Stream.FLAG_SIZED;
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            sink.begin(getSizeIfKnown());
            for (int i=offset; i<endOffset; i++) {
                sink.accept(elements[i]);
            }
            // update only once; reduce heap write traffic
            offset = endOffset;
            sink.end();
        }

        @Override
        public boolean isParallel() {
            return false;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public Spliterator<T> spliterator() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ArrayParallelStreamAccessor<T>
            extends ArraySpliterator<T> implements StreamAccessor<T> {

        ArrayParallelStreamAccessor(T[] elements) {
            this(elements, 0, elements.length);
        }

        ArrayParallelStreamAccessor(T[] elements, int offset, int length) {
            super(elements, offset, length);
        }

        @Override
        public int getStreamFlags() {
            return Stream.FLAG_SIZED;
        }

        @Override
        public boolean isParallel() {
            return true;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public Spliterator<T> spliterator() {
            return this;
        }
    }

    private static class RandomAccessListStreamAccessor<T,L extends RandomAccess & List<T>>
            extends RandomAccessListIterator<T,L> implements StreamAccessor<T> {

        RandomAccessListStreamAccessor(L elements) {
            this(elements, 0, elements.size());
        }

        RandomAccessListStreamAccessor(L elements, int offset, int length) {
            super(elements, offset, length);
        }

        @Override
        public void into(Sink<T, ?, ?> sink) {
            sink.begin(getSizeIfKnown());
            for (int i=offset; i<endOffset; i++) {
                sink.accept(elements.get(i));
            }
            // update only once; reduce heap write traffic
            offset = endOffset;
            sink.end();
        }

        @Override
        public int getStreamFlags() {
            return Stream.FLAG_SIZED;
        }

        @Override
        public boolean isParallel() {
            return false;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public Spliterator<T> spliterator() {
            throw new UnsupportedOperationException();
        }
    }

    private static class RandomAccessListParallelStreamAccessor<T,L extends RandomAccess & List<T>>
            extends RandomAccessListSpliterator<T,L> implements StreamAccessor<T> {

        RandomAccessListParallelStreamAccessor(L elements) {
            this(elements, 0, elements.size());
        }

        RandomAccessListParallelStreamAccessor(L elements, int offset, int length) {
            super(elements, offset, length);
        }

        @Override
        public int getStreamFlags() {
            return Stream.FLAG_SIZED;
        }

        @Override
        public boolean isParallel() {
            return true;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public Spliterator<T> spliterator() {
            return this;
        }
    }
}

