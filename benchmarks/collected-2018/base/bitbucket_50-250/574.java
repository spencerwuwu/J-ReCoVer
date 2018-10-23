// https://searchcode.com/api/result/122323315/

package peregrine.reduce.sorter;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import peregrine.BaseTestWithConfig;
import peregrine.StructReaders;
import peregrine.config.Config;
import peregrine.io.buffer.region.ByteRegionAllocationTracker;
import peregrine.io.buffer.region.ByteRegionAllocator;
import peregrine.io.chunk.CompositeChunkReader;
import peregrine.io.chunk.DefaultChunkReader;
import peregrine.io.chunk.DefaultChunkWriter;
import peregrine.sort.DefaultSortComparator;
import peregrine.util.Integers;
import peregrine.util.netty.BackedChannelBufferWritable;
import peregrine.util.netty.ChannelBufferWritable;

/**
 *
 */
public class TestCoreSortMemoryAllocation extends BaseTestWithConfig {

    @Test
    public void test1() throws Exception {
        doTest( 1 );
        doTest( 100 );
        doTest( 1000 );
        doTest( 10000 );
    }

    public void doTest( int max ) throws Exception {

        Config config = getConfig();

        ChannelBuffer buffer = ChannelBuffers.buffer( 1000000 );

        ChannelBufferWritable channelBufferWritable = new BackedChannelBufferWritable( buffer );

        DefaultChunkWriter writer = new DefaultChunkWriter( config, channelBufferWritable );
        writer.setMinimal( true );
        // 4 + 8 + 4 + 8 ... 24 bytes for the records in raw data form. + 4 bytes for every offset and
        for (long i = 0; i < max; i++) {
            writer.write( StructReaders.wrap( i ), StructReaders.wrap( i ) );
        }

        writer.close();

        int memory_per_entry = Integers.LENGTH + /* 4 bytes for the length for the record */
                               8 +               /* we're using 8 byte keys */
                               Integers.LENGTH + /* 4 bytes for the length of the value */
                               8 +               /* we're using 8 byte values */
                               Integers.LENGTH   /* the offset lookup memory */
            ;

        System.out.printf( "memory_per_entry: %s\n", memory_per_entry );

        // now we have a buffer we can read from

        DefaultChunkReader defaultChunkReader = new DefaultChunkReader( buffer );

        CompositeChunkReader compositeChunkReader = new CompositeChunkReader( config, defaultChunkReader );

        ByteRegionAllocationTracker globalMemoryAllocationTracker = new ByteRegionAllocationTracker( config );
        globalMemoryAllocationTracker.setState( 1 );

        ByteRegionAllocationTracker localMemoryAllocationTracker = new ByteRegionAllocationTracker( config );
        ByteRegionAllocator allocator = new ByteRegionAllocator( config, globalMemoryAllocationTracker, localMemoryAllocationTracker );

        System.out.printf( "BEFORE sortLookup created memory ceiling: %s\n", globalMemoryAllocationTracker.getCeiling() );

        assertEquals( 0, globalMemoryAllocationTracker.getCeiling() );

        SortLookup sortLookup = new SortLookup( allocator, compositeChunkReader );

        long rawSortLookupUsage = globalMemoryAllocationTracker.getCeiling();

        System.out.printf( "Raw SortLookup usage: %s\n", rawSortLookupUsage );

        // make sure the ceiling ia actually using the RIGHT amount of memory...
        assertEquals( SortLookup.computeCapacity( max, buffer.writerIndex() ), globalMemoryAllocationTracker.getCeiling() );

        BaseChunkSorter baseChunkSorter = new BaseChunkSorter( new DefaultSortComparator() );
        baseChunkSorter.sort( sortLookup );

        long fullSortUsage = globalMemoryAllocationTracker.getCeiling();

        System.out.printf( "Full sort usage: %s\n", fullSortUsage );

        long sortUsage = fullSortUsage - rawSortLookupUsage;

        System.out.printf( "Sort usage: %s\n", sortUsage );
        System.out.printf( "Sort overhead: %s\n", sortUsage / (double)rawSortLookupUsage );

    }

}

