// https://searchcode.com/api/result/122323757/

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

import com.google.common.annotations.VisibleForTesting;
import org.jboss.netty.buffer.ChannelBuffer;
import peregrine.Record;
import peregrine.StructReader;
import peregrine.io.buffer.region.ByteRegionReference;
import peregrine.io.buffer.region.ReduceByteRegion;
import peregrine.io.chunk.CompositeChunkReader;
import peregrine.util.Integers;
import peregrine.io.buffer.region.ByteRegionAllocator;

import java.io.Closeable;
import java.io.IOException;


/**
 * Allocate a lookup for N records and a separate direct buffer for storing
 * these records.  We include a method to determine if a key, which is about to
 * be written, would have too much capacity for writes.
 */
public class SortLookup implements KeyLookup, Closeable {

    protected int endInclusive;

    protected int size;

    protected int index = -1;

    protected int startInclusive = -1;

    protected ReduceByteRegion reduceByteRegion;

    protected OffsetIndex offsetIndex;

    protected ByteRegionAllocator allocator;

    // true when this is a temporary / intermediate buffer.
    protected boolean temporary = false;

    private ByteRegionReference byteRegionReference;

    private SortLookup() {}

    public SortLookup( ByteRegionAllocator allocator, CompositeChunkReader reader)
      throws IOException {

        //we over-allocate here based on the raw data of the input file and NOT
        //the underlying raw data.

        this( allocator, reader.count(), computeDataCapacity( reader.count(), reader.length() ) );

        while ( reader.hasNext() ) {

            // advance the reader
            reader.next();

            // advance the lookup
            next();

            set( new Record( reader.key(), reader.value() ) );

        }

        reset();

    }

    public SortLookup( ByteRegionAllocator allocator, int nrRecords, long capacity ) {

        this.allocator = allocator;
        this.offsetIndex = new OffsetIndex( allocator, nrRecords );

        if ( capacity > Integer.MAX_VALUE )
            throw new IllegalArgumentException( "Required sort capacity too large: " + capacity );

        this.byteRegionReference = allocator.allocate( (int) capacity );
        this.reduceByteRegion = byteRegionReference.getByteRegion();

        this.endInclusive = nrRecords - 1;
        this.size = nrRecords;
        this.startInclusive = 0;

    }

    @Override
    public KeyLookup allocate(KeyLookup kl0, KeyLookup kl1 ) {

        SortLookup sl0 = (SortLookup)kl0;
        SortLookup sl1 = (SortLookup)kl1;

        int nrRecords = kl0.size() + kl1.size();
        long capacity = sl0.requiredCapacity() + sl1.requiredCapacity();

        SortLookup result = new SortLookup( allocator, nrRecords, capacity );
        result.temporary = true;

        return result;

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
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void set(Record entry) {

        offsetIndex.set( index, reduceByteRegion.writerIndex() );

        writeEntry( entry.getKey() );
        writeEntry( entry.getValue() );

        if ( index + 1 == size ) {
            offsetIndex.set( size, reduceByteRegion.writerIndex() );
        }

    }

    // write a struct reader and length prefix it.
    private void writeEntry( StructReader sr ) {

        ChannelBuffer cb = sr.getChannelBuffer();

        int len = cb.writerIndex();

        reduceByteRegion.writeInt( len );
        reduceByteRegion.writeBytes( cb );

    }

    @Override
    public Record get() {

        int readerIndex = offsetIndex.get( index );

        if ( readerIndex < 0 )
            throw new RuntimeException( String.format( "Offset %s invalid for index %s", readerIndex, index ) );

        reduceByteRegion.readerIndex( readerIndex );

        StructReader key = readEntry();
        StructReader value = readEntry();

        return new Record( key, value );

    }

    private StructReader readEntry() {

        int len = reduceByteRegion.readInt();

        ChannelBuffer cb = reduceByteRegion.readSlice( len );

        return new StructReader( cb );

    }

    @Override
    public void reset() {
        this.index = startInclusive - 1;
    }

    /**
     * @return The number of bytes required to store this in a new lookup.
     * We use this to determine how much to store in a new allocate() call.
     */
    public long requiredCapacity() {

        int start = offsetIndex.get( startInclusive );
        int end   = offsetIndex.get( endInclusive + 1 );

        return end - start;

    }

    // zero copy slice implementation.
    @Override
    public KeyLookup slice( int sliceStartInclusive, int sliceEndInclusive ) {

        SortLookup slice = new SortLookup();

        slice.offsetIndex = offsetIndex;
        slice.reduceByteRegion = reduceByteRegion;

        slice.size  = (sliceEndInclusive - sliceStartInclusive) + 1;
        slice.startInclusive = startInclusive + sliceStartInclusive;
        slice.index  = slice.startInclusive - 1;
        slice.endInclusive = slice.startInclusive + slice.size - 1;
        slice.byteRegionReference = byteRegionReference;
        slice.allocator = allocator;
        slice.temporary = temporary;

        return slice;

    }

    @Override
    public void close() throws IOException {

        offsetIndex.close();
        byteRegionReference.close();

    }

    public static long computeCapacity( int nrRecords, int dataLength ) {

        // this computes an UPPER bound for the capacity.  We could probably
        // get MUCH better usage of memory but we need to compute the length of
        // raw data ahead of time.
        return computeOffsetIndexCapacity( nrRecords ) +
               computeDataCapacity( nrRecords, dataLength );

    }

    // usage for the offset index
    @VisibleForTesting
    public static long computeOffsetIndexCapacity( int nrRecords ) {
        return ((nrRecords + 1) * (long)Integers.LENGTH);
    }

    @VisibleForTesting
    public static long computeDataCapacity( int nrRecords, int dataLength ) {

        return (nrRecords * (long)Integers.LENGTH ) + /* four bytes for each key    */
               (nrRecords * (long)Integers.LENGTH ) + /* four bytes for each value  */
               (long)dataLength                       /* the data bytes             */
            ;

    }

}


