// https://searchcode.com/api/result/132848824/

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
package org.openjdk.tests.java.util.streams.ops;

import org.openjdk.tests.java.util.streams.StreamTestDataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.streams.ops.FoldOp;
import java.util.streams.ops.MapOp;
import java.util.streams.ops.SeedlessFoldOp;

import static org.openjdk.tests.java.util.LambdaTestHelpers.*;

/**
 * ReduceOpTest
 *
 * @author Brian Goetz
 */
@Test
public class ReduceTest extends StreamOpTestCase {
    public void testReduce() {
        List<Integer> list = countTo(10);

        assertEquals(55, (int) list.stream().reduce(rPlus).get());
        assertEquals(55, (int) list.stream().reduce(0, rPlus));
        assertEquals(10, (int) list.stream().reduce(rMax).get());
        assertEquals(1, (int) list.stream().reduce(rMin).get());

        assertEquals(0, (int) countTo(0).stream().reduce(0, rPlus));
        assertTrue(!countTo(0).stream().reduce(rPlus).isPresent());

        assertEquals(110, (int) list.stream().map(mDoubler).reduce(rPlus).get());
        assertEquals(20, (int) list.stream().map(mDoubler).reduce(rMax).get());
        assertEquals(2, (int) list.stream().map(mDoubler).reduce(rMin).get());
    }

    @Test(dataProvider = "opArrays", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData<Integer> data) {
        Optional<Integer> seedless = exerciseOps(data, new SeedlessFoldOp<>(rPlus));
        Integer folded = exerciseOps(data, new FoldOp<>(() -> 0, rPlus, rPlus));
        assertEquals(folded, seedless.orElse(0));

        seedless = exerciseOps(data, new SeedlessFoldOp<>(rMin));
        folded = exerciseOps(data, new FoldOp<>(() -> Integer.MAX_VALUE, rMin, rMin));
        assertEquals(folded, seedless.orElse(Integer.MAX_VALUE));

        seedless = exerciseOps(data, new SeedlessFoldOp<>(rMax));
        folded = exerciseOps(data, new FoldOp<>(() -> Integer.MIN_VALUE, rMax, rMax));
        assertEquals(folded, seedless.orElse(Integer.MIN_VALUE));

        seedless = exerciseOps(data, new SeedlessFoldOp<>(rPlus), new MapOp<>(mDoubler));
        folded = exerciseOps(data, new FoldOp<>(() -> 0, rPlus, rPlus), new MapOp<>(mDoubler));
        assertEquals(folded, seedless.orElse(0));

        seedless = exerciseOps(data, new SeedlessFoldOp<>(rMin), new MapOp<>(mDoubler));
        folded = exerciseOps(data, new FoldOp<>(() -> Integer.MAX_VALUE, rMin, rMin), new MapOp<>(mDoubler));
        assertEquals(folded, seedless.orElse(Integer.MAX_VALUE));

        seedless = exerciseOps(data, new SeedlessFoldOp<>(rMax), new MapOp<>(mDoubler));
        folded = exerciseOps(data, new FoldOp<>(() -> Integer.MIN_VALUE, rMax, rMax), new MapOp<>(mDoubler));
        assertEquals(folded, seedless.orElse(Integer.MIN_VALUE));
    }
}

