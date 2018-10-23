// https://searchcode.com/api/result/122323756/

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

import peregrine.io.buffer.region.ByteRegionAllocator;
import peregrine.io.buffer.region.ByteRegionReference;
import peregrine.io.buffer.region.ReduceByteRegion;
import peregrine.util.Integers;

import java.io.Closeable;
import java.io.IOException;

/**
 * An index to keep a mapping from the ordinal position in a list of records to the
 * raw byte offset within an underlying buffer.
 */
public class OffsetIndex implements Closeable {

    private ReduceByteRegion byteRegion;

    private ByteRegionReference byteRegionReference;

    private int length;

    public OffsetIndex(ByteRegionAllocator allocator, int nrRecords ) {
        length = (nrRecords + 1) * Integers.LENGTH;

        byteRegionReference = allocator.allocate( length );
        byteRegion = byteRegionReference.getByteRegion();
    }

    public void set( int index , int offset ) {
        byteRegion.setInt( index * Integers.LENGTH , offset );
    }

    public int get( int index ) {

        int ptr = index * Integers.LENGTH;

        if ( ptr >= length )
            return -1;

        return byteRegion.getInt( ptr );

    }

    public int capacity() {
        return byteRegion.capacity();
    }

    @Override
    public void close() throws IOException {
        byteRegionReference.close();
    }

}

