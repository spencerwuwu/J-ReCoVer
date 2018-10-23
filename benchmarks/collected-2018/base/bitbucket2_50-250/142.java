// https://searchcode.com/api/result/125476562/

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
import peregrine.BaseTestWithConfig;
import peregrine.Record;
import peregrine.StructReader;
import peregrine.StructReaders;
import peregrine.sort.DefaultSortComparator;
import peregrine.sort.SortComparator;
import peregrine.util.Duration;
import peregrine.util.Hex;
import peregrine.util.Integers;

import java.util.List;
import java.util.Random;

/**
 *
 */
public abstract class BaseTestForKeyLookupSort extends BaseTestWithConfig {

    protected abstract KeyLookup createLookup( int nrRecords );

    @Test
    public void testWriteAndRead() throws Exception {

        int max = 10;
        assertNotNull( getConfig() );

        KeyLookup lookup = createLookup( max );

        for (int i = 0; i < max; i++) {

            long val = (long)i;

            lookup.next();
            lookup.set( new Record( StructReaders.wrap( val ),
                                    StructReaders.wrap( val ) ) );


        }

        lookup.reset();

        long idx = 0;
        while( lookup.hasNext() ) {

            lookup.next();
            Record record = lookup.get();

            StructReader val = StructReaders.wrap( idx );
            assertEquals( val, record.getKey() );
            assertEquals( val, record.getValue() );

            ++idx;

        }

        assertEquals( max, idx );

        System.out.printf( "Worked\n" );

    }


    @Test
    public void testBasicSortFunctionality() throws Exception {

        DefaultSortComparator comparator = new DefaultSortComparator();

        List<Integer> tests = Integers.toList( 2, 4, 8, 16, 32, 64, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 0, 1 );

        for( int max : tests ) {

            assertNotNull( getConfig() );

            KeyLookup lookup;

            lookup = createLookup( max );

            System.out.printf( "Testing descending sort for %,d entries.\n" , max );

            testSort( createDescendingKeyLookup( lookup, max ), comparator );

            lookup = createLookup( max );

            System.out.printf( "Testing sequential sort for %,d entries.\n" , max );
            testSort( createSequentialKeyLookup( lookup, max ), comparator );

            lookup = createLookup( max );

            System.out.printf( "Testing random sort for %,d entries.\n" , max );
            testSort( createRandomKeyLookup( lookup, max ), comparator );

        }

    }


    protected void dump( KeyLookup lookup, SortComparator comparator ) {

        while( lookup.hasNext() ) {
            lookup.next();
            Record current = lookup.get();

            System.out.printf( "  %s\n", Hex.encode( comparator.getSortKey( current.getKey(), current.getValue() ) ) );

        }

        lookup.reset();

    }

    protected void testSort( KeyLookup lookup, SortComparator comparator ) throws Exception {

        System.out.printf( "Stating sort...\n" );

        Duration duration = new Duration();

        BaseChunkSorter sorter = new BaseChunkSorter( comparator );
        KeyLookup result = sorter.sort( lookup );

        //dump( result, comparator );

        assertIteration( result, lookup.size() );
        assertOrder( result, comparator );

        System.out.printf( "Sort duration: %s\n", duration );

    }


    // assert that we can properly iterate through the result.
    protected void assertIteration(KeyLookup lookup, int expected) {

        int count = 0;

        while( lookup.hasNext() ) {
            lookup.next();
            Record entry = lookup.get();
            assertNotNull( entry );
            ++count;
        }

        assertEquals( expected , count );
        lookup.reset();

    }

    // assert that the given result is in the right order
    protected void assertOrder( KeyLookup lookup, SortComparator comparator ) {

        if ( lookup.size() <= 1 )
            return;

        lookup.next();
        Record last = lookup.get();

        while( lookup.hasNext() ) {
            lookup.next();
            Record current = lookup.get();

            int cmp = comparator.compare( last, current );

            assertTrue( String.format( "Invalid order: last=%s vs current=%s",
                                       Hex.encode( comparator.getSortKey( last.getKey(), last.getValue() ) ),
                                       Hex.encode( comparator.getSortKey( current.getKey(), current.getValue() ) ) ),
                        cmp <= 0 );

            last = current;

        }

        lookup.reset();

    }

    protected KeyLookup createSequentialKeyLookup( KeyLookup lookup, int count ) {

        for (long i = 0; i < count; i++) {
            lookup.next();
            lookup.set( new Record( StructReaders.wrap( i ), StructReaders.wrap( i ) ) );
        }

        lookup.reset();
        return lookup;

    }

    protected KeyLookup createDescendingKeyLookup( KeyLookup lookup, int count ) {

        for (long i = 0; i < count; i++) {
            lookup.next();
            lookup.set( new Record( StructReaders.wrap( count-i ), StructReaders.wrap( count-i ) ) );
        }

        lookup.reset();
        return lookup;

    }

    protected KeyLookup createRandomKeyLookup( KeyLookup lookup, int count ) {

        Random r = new Random();

        for (long i = 0; i < count; i++) {

            long rand = r.nextLong();

            lookup.next();
            lookup.set( new Record( StructReaders.wrap( rand ), StructReaders.wrap( rand ) ) );

        }

        lookup.reset();
        return lookup;

    }

}

