// https://searchcode.com/api/result/122323314/

/*
 * Copyright 2011-2013 Kevin A. Burton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package peregrine.reduce.sorter;

import org.junit.Test;
import peregrine.config.Config;
import peregrine.io.buffer.region.ByteRegionAllocator;
import peregrine.sort.DefaultSortComparator;
import peregrine.io.buffer.region.ByteRegionAllocationTracker;
import peregrine.util.Longs;

/**
 *
 */
public class TestCoreSortLookup extends BaseTestForKeyLookupSort {

    @Override
    protected KeyLookup createLookup(int nrRecords) {

        Config config = getConfig();

        ByteRegionAllocationTracker globalTracker = new ByteRegionAllocationTracker( getConfig() );
        ByteRegionAllocationTracker localTracker = new ByteRegionAllocationTracker( getConfig() );
        ByteRegionAllocator allocator = new ByteRegionAllocator( config, globalTracker, localTracker );

        return new SortLookup( allocator, nrRecords, 100000 );
    }

    @Test
    /**
     * Test a large sort (which is almost a micro benchmark).  This sorts 16MB
     * in 11 seconds which is much slower (by about 5x) than doing it within the
     * JVM but I suspect the new Unsafe work in Netty 4.x should fix it.
     */
    public void testLargeSort() throws Exception {

        Config config = getConfig();

        ByteRegionAllocationTracker globalTracker = new ByteRegionAllocationTracker( getConfig() );
        ByteRegionAllocationTracker localTracker = new ByteRegionAllocationTracker( getConfig() );
        ByteRegionAllocator allocator = new ByteRegionAllocator( config, globalTracker, localTracker );

        DefaultSortComparator comparator = new DefaultSortComparator();

        int dataSize = 16000000;

        int max = dataSize / (( Longs.LENGTH * 2) + 2);

        KeyLookup lookup;

        System.out.printf( "Sorting with: SortLookup\n" );
        lookup = new SortLookup( allocator, max, dataSize * 2 );
        testSort( createDescendingKeyLookup( lookup, max ), comparator );

    }

}

