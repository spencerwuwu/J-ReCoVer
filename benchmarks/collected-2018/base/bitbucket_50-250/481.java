// https://searchcode.com/api/result/132832092/

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

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.functions.*;
import java.util.streams.ops.*;

/**
 * ValuePipeline
 *
 * @param <T> Type of elements in the upstream source.
 * @param <U> Type of elements in produced by this stage.
 *
 * @author Brian Goetz
 */
public class ValuePipeline<T, U> extends AbstractPipeline<T,U> implements Stream<U>  {

    public<S> ValuePipeline(StreamAccessor<S> source) {
        super(source);
        assert source.getShape() == StreamShape.VALUE;
    }

    public ValuePipeline(AbstractPipeline<?, T> upstream, IntermediateOp<T, U> op) {
        super(upstream, op);
    }

    public<V> Stream<V> pipeline(IntermediateOp<U, V> op) {
        // @@@ delegate to shape to do instantiation
        return new ValuePipeline<>(this, op);
    }

    @Override
    public Stream<U> filter(Predicate<? super U> predicate) {
        return chainValue(new FilterOp<>(predicate));
    }

    @Override
    public <R> Stream<R> map(Mapper<? super U, ? extends R> mapper) {
        return chainValue(new MapOp<>(mapper));
    }

    @Override
    public <R> Stream<R> flatMap(FlatMapper<? super U, R> mapper) {
        return chainValue(new FlatMapOp<>(mapper));
    }

    @Override
    public Stream<U> uniqueElements() {
        return chainValue(UniqOp.<U>singleton());
    }

    @Override
    public Stream<U> sorted(Comparator<? super U> comparator) {
        return chainValue(new SortedOp<>(comparator));
    }

    @Override
    public Stream<U> cumulate(BinaryOperator<U> operator) {
        return chainValue(new CumulateOp<>(operator));
    }

    @Override
    public void forEach(Block<? super U> block) {
        pipeline(ForEachOp.make(block));
    }

    @Override
    public Stream<U> tee(Block<? super U> block) {
        return chainValue(new TeeOp<>(block));
    }

    @Override
    public Stream<U> limit(int limit) {
        return chainValue(new LimitOp<U>(limit));
    }

    @Override
    public Stream<U> skip(int n) {
        return chainValue(new SkipOp<U>(n));
    }

    @Override
    public Stream<U> concat(Stream<? extends U> other) {
        return chainValue(ConcatOp.<U>make(other));
    }

    @Override
    public <A extends Destination<? super U>> A into(A target) {
        target.addAll(this);
        return target;
    }

    @Override
    public <K> Map<K,Collection<U>> groupBy(Mapper<? super U, ? extends K> classifier) {
        return pipeline(new GroupByOp<>(classifier));
    }

    @Override
    public <K, W> Map<K, W> reduceBy(Mapper<? super U, ? extends K> classifier,
                                     Factory<W> baseFactory,
                                     Combiner<W, U, W> reducer) {
        return pipeline(new ReduceByOp<>(classifier, baseFactory, reducer));
    }

    @Override
    public Object[] toArray() {
        return pipeline(ToArrayOp.<U>singleton());
    }

    @Override
    public boolean anyMatch(Predicate<? super U> predicate) {
        return pipeline(MatchOp.make(predicate, MatchOp.MatchKind.ANY));
    }

    @Override
    public boolean allMatch(Predicate<? super U> predicate) {
        return pipeline(MatchOp.make(predicate, MatchOp.MatchKind.ALL));
    }

    @Override
    public boolean noneMatch(Predicate<? super U> predicate) {
        return pipeline(MatchOp.make(predicate, MatchOp.MatchKind.NONE));
    }

    @Override
    public Optional<U> findFirst() {
        return pipeline(FindFirstOp.<U>singleton());
    }

    @Override
    public Optional<U> findAny() {
        return pipeline(FindAnyOp.<U>singleton());
    }

    @Override
    public Stream<U> sequential() {
        if (!isParallel()) {
            return this;
        }
        else {
            TreeUtils.Node<U> collected = evaluate(TreeUtils.CollectorOp.<U>singleton());
            return Streams.stream(collected, collected.size());
        }
    }

    @Override
    public <R> MapStream<U, R> mapped(Mapper<? super U, ? extends R> mapper) {
        return chainMap(new MappedOp<>(mapper));
    }

    @Override
    public U reduce(final U seed, final BinaryOperator<U> op) {
        return pipeline(new FoldOp<>(seed, op, op));
    }

    @Override
    public Optional<U> reduce(BinaryOperator<U> op) {
        return pipeline(new SeedlessFoldOp<>(op));
    }

    @Override
    public <V> V fold(Factory<V> baseFactory, Combiner<V, U, V> reducer, BinaryOperator<V> combiner) {
        return pipeline(new FoldOp<>(baseFactory, reducer, combiner));
    }
}

