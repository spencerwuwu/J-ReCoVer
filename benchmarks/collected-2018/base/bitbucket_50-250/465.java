// https://searchcode.com/api/result/132832106/

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
import java.util.functions.*;

/**
 * A potentially infinite sequence of elements. A stream is a consumable data
 * structure. The elements of the stream are available for consumption by either
 * iteration or an operation. Once consumed the elements are no longer available
 * from the stream.
 *
 * @param <T> Type of elements.
 *
 * @author Brian Goetz
 */
public interface Stream<T> extends BaseStream<T, Iterator<T>> {

    /**
     * Stream elements are distinct. No two elements contained in the stream
     * are equivalent via {@code equals()} or equality ({@code ==}) operator.
     */
    public static final int FLAG_DISTINCT = 1 << 0;

    /**
     * Stream elements are sorted. Elements are {@code Comparable} and each
     * element is greater or equal to the element which preceed it (if any) and
     * less than or equal to the element which follows it (if any).
     */
    public static final int FLAG_SORTED = 1 << 1;

    /**
     * Stream size can be calculated in less than {@code O(n)} time.
     */
    public static final int FLAG_SIZED = 1 << 2;

    /**
     * Stream size is known to be infinite. Mutually exclusive to {@link #FLAG_SIZED}.
     */
    public static final int FLAG_INFINITE = 1 << 3;

    /**
     * Mask of state bits for defined states.
     */
    public static final int FLAG_MASK = (1 << 4) - 1;

    /**
     * Mask of undefined state bits.
     */
    public static final int FLAG_UNKNOWN_MASK_V1 = ~FLAG_MASK;


    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Mapper<? super T, ? extends R> mapper);

    <R> Stream<R> flatMap(FlatMapper<? super T, R> mapper);

    Stream<T> uniqueElements();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> cumulate(BinaryOperator<T> operator);

    /**
     * Each element of this stream is processed by the provided block.
     *
     * @param block the Block via which all elements will be processed.
     */
    void forEach(Block<? super T> block);

    Stream<T> tee(Block<? super T> block);

    /**
     * Limit this stream to at most {@code n} elements. The stream will not be affected
     * if it contains less than or equal to {@code n} elements.
     *
     * @param n the number elements the stream should be limited to.
     * @return the limited stream.
     */
    Stream<T> limit(int n);

    /**
     * Skip at most {@code n} elements.
     *
     * @param n the number of elements to be skipped.
     * @return the skipped stream.
     */
    Stream<T> skip(int n);

    /**
     * Concatenate to the end of this stream.
     *
     * @param other the stream to concatenate.
     * @return the concatenated stream.
     */
    Stream<T> concat(Stream<? extends T> other);

    <A extends Destination<? super T>> A into(A target);

    Object[] toArray();

    <U> Map<U, Collection<T>> groupBy(Mapper<? super T, ? extends U> classifier);

    <U, W> Map<U, W> reduceBy(Mapper<? super T, ? extends U> classifier,
                              Factory<W> baseFactory,
                              Combiner<W, T, W> reducer);

    T reduce(T base, BinaryOperator<T> op);

    Optional<T> reduce(BinaryOperator<T> op);

    <U> U fold(Factory<U> baseFactory,
               Combiner<U, T, U> reducer,
               BinaryOperator<U> combiner);

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    Optional<T> findFirst();

    Optional<T> findAny();

    Stream<T> sequential();

    <U> MapStream<T, U> mapped(Mapper<? super T, ? extends U> mapper);


    /**
     * An aggregate that supports an {@code addAll(Stream)} operation.
     *
     * @param <T> Type of aggregate elements.
     */
    interface Destination<T> {
        public void addAll(Stream<? extends T> stream);
    }
}

