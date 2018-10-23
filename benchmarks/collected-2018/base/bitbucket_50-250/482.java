// https://searchcode.com/api/result/132848762/

/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.*;
import java.util.functions.*;
import java.util.streams.ops.FilterOp;
import java.util.streams.ops.MapOp;
import java.util.streams.ops.SortedOp;

import static org.openjdk.tests.java.util.LambdaTestHelpers.*;

/**
 * IteratorsNullTest -- tests of proper null handling in Iterators methods
 */
@Test(groups = { "null", "lambda" })
public class IteratorsNullTest extends NullArgsTestCase {
    @Factory(dataProvider = "data")
    public IteratorsNullTest(String name, Block<Object[]> sink, Object[] args) {
        super(name, sink, args);
    }

    @DataProvider(name = "data")
    @SuppressWarnings("unchecked")
    public static Object[][] makeData() {
        List<Integer> list = countTo(10);
        return new Object[][] {
                { "FilterOp.iterator", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        FilterOp.iterator((Iterator<Integer>) args[0], (Predicate<Integer>) args[1]);
                    }
                }, new Object[] { list.iterator(), pTrue }
                },
                { "MapOp.iterator", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        MapOp.iterator((Iterator<Integer>) args[0], (Mapper<Integer, Integer>) args[1]);
                    }
                }, new Object[] { list.iterator(), mDoubler }
                },
                {"Iterators.reduce", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        Iterators.<Integer>reduce((Iterator<Integer>) args[0], 0, (BinaryOperator<Integer>) args[1]);
                    }
                }, new Object[]{list.iterator(), rPlus}
                },
                { "Iterators.mapReduce", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        Iterators.<Integer, Integer>mapReduce((Iterator<Integer>) args[0],
                                                              (Mapper<Integer, Integer>) args[1],
                                                              0, (BinaryOperator<Integer>) args[2]);
                    }
                }, new Object[] { list.iterator(), mDoubler, rPlus }
                },
                { "Iterators.mapReduce(int)", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        Iterators.mapReduce((Iterator<Integer>) args[0],
                                            (IntMapper<Integer>) args[1],
                                            0, (IntBinaryOperator) args[2]);
                    }
                }, new Object[] { list.iterator(), imDoubler, irPlus }
                },
                { "Iterators.mapReduce(long)", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        Iterators.mapReduce((Iterator<Integer>) args[0],
                                            (LongMapper<Integer>) args[1],
                                            0, (LongBinaryOperator) args[2]);
                    }
                }, new Object[] { asLongs(list).iterator(), lmDoubler, lrPlus }
                },
                { "Iterators.mapReduce(double)", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        Iterators.mapReduce((Iterator<Integer>) args[0],
                                            (DoubleMapper<Integer>) args[1],
                                            0, (DoubleBinaryOperator) args[2]);
                    }
                }, new Object[] { asDoubles(list).iterator(), dmDoubler, drPlus }
                },
                { "SortedOp.iterator", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        SortedOp.iterator((Iterator<Integer>) args[0], Comparators.<Integer>naturalOrder());
                    }
                }, new Object[] { list.iterator() }
                },
                { "Iterators.sorted(Comparator)", new Block<Object[]>() {
                    public void apply(Object[] args) {
                        SortedOp.iterator((Iterator<Integer>) args[0], (Comparator) args[1]);
                    }
                }, new Object[] { list.iterator(), cInteger }
                },
        };
    }
}

