// https://searchcode.com/api/result/122323313/

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

import peregrine.Record;

import java.io.IOException;

/**
 * A KeyLookup that acts as the "gold standard" to compare our new sorting
 * algorithm against.  This just functions out of memory and doesn't require
 * integer decoding.
 */
public class HeapKeyLookup implements KeyLookup {

    protected Record[] entries;
    protected int endInclusive;
    protected int size;

    protected int index = -1;

    protected int startInclusive = -1;

    public HeapKeyLookup() { }

    public HeapKeyLookup(int nr_entries) {
        this( new Record[ nr_entries ] );
    }

    public HeapKeyLookup(Record[] entries) {
        this.entries = entries;
        this.endInclusive = entries.length - 1;
        this.size = entries.length;
        this.startInclusive = 0;
    }

    @Override
    public boolean hasNext() {
        return index < endInclusive;
    }

    @Override
    public void next() {
        ++index;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void set( Record entry ) {
        entries[index] = entry;
    }

    @Override
    public Record get() {
        Record result = entries[index];

        return result;
    }

    @Override
    public void reset() {
        this.index = startInclusive - 1;
    }

    // zero copy slice implementation.
    @Override
    public KeyLookup slice( int sliceStartInclusive, int sliceEndInclusive ) {

        HeapKeyLookup slice = new HeapKeyLookup();

        slice.entries = entries;
        slice.size    = (sliceEndInclusive - sliceStartInclusive) + 1;
        slice.startInclusive = this.startInclusive + sliceStartInclusive;
        slice.index   = slice.startInclusive - 1;
        slice.endInclusive = slice.startInclusive + slice.size - 1;

        return slice;

    }

    @Override
    public KeyLookup allocate( KeyLookup kl0, KeyLookup kl1 ) {

        int size = kl0.size() + kl1.size();

        return new HeapKeyLookup( new Record[ size ] );

    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

}

