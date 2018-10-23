// https://searchcode.com/api/result/125476558/

/*
 * Copyright 2011 Kevin A. Burton
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
package peregrine.reduce;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import peregrine.*;
import peregrine.config.*;
import peregrine.io.JobOutput;
import peregrine.io.SequenceReader;
import peregrine.io.buffer.region.ByteRegionAllocator;
import peregrine.io.chunk.*;

import peregrine.io.partition.DefaultPartitionWriter;
import peregrine.io.partition.LocalPartition;
import peregrine.reduce.sorter.*;
import peregrine.sort.DefaultSortComparator;
import peregrine.io.buffer.region.ByteRegionAllocationTracker;
import peregrine.util.Duration;

/**
 * Tests running a reduce but also has some code to benchmark them so that we
 * can look at the total performance.
 */
public class TestCoreChunkSorterWithCustomComparator extends BaseTestWithConfig {

    // allow us to specify max from the command line for benchmarks.
    private static int MAX = -1;

    @Test
    public void test1() throws Exception {

        Config config = getConfigs().get( 0 );

        // ******** write the chunk file

        File dir = new File( config.getBasedir() );
        String path = "/tmp/test.sort";

        System.out.printf( "Writing data... \n" );

        Partition partition = new Partition( 0 );
        DefaultPartitionWriter writer = new DefaultPartitionWriter( config, partition, path );
        writer.setEnableLocalDelegate( true );
        writer.init();

        int max = MAX;

        if ( max == -1 ) {
            max = getMaxFromFactor( 100, 1000 );
        }

        System.out.printf( "Working with max=%,d\n", max );

        for( int i = 0; i < max; ++i ) {

            StructReader key   = StructReaders.hashcode( i );
            StructReader value = StructReaders.wrap( i );
            
            writer.write( key, value );
            
        }
        
        writer.close();

        System.out.printf( "Writing data... done\n" );

        // ******** now create a keylookup for that file.

        //TODO: use multiple chunk files.

        List<DefaultChunkReader> chunkReaders = LocalPartition.getChunkReaders( config, partition, path );

        for( DefaultChunkReader reader : chunkReaders ) {

            File file = reader.getMappedFile().getFile();

            System.out.printf( "    Wrote %,d bytes to %s\n", file.length(), file.getName() );


        }

        // OMG generics are ugly.
        List<ChunkReader> readers =(List<ChunkReader>)(List)chunkReaders;

        CompositeChunkReader composite = new CompositeChunkReader( config, readers );

        ByteRegionAllocationTracker globalTracker = new ByteRegionAllocationTracker( getConfig() );
        ByteRegionAllocationTracker localTracker = new ByteRegionAllocationTracker( getConfig() );
        ByteRegionAllocator allocator = new ByteRegionAllocator( config, globalTracker, localTracker );

        SortLookup lookup = new SortLookup( allocator, composite );

        //now look through it printing out the key and value pairs.

        System.out.printf( "Testing read of data...\n" );

        while( lookup.hasNext() ) {

            lookup.next();

            Record entry = lookup.get();

        }

        composite.close();

        System.out.printf( "Testing read of data...done\n" );

        List<DefaultChunkReader> inputChunkReaders = LocalPartition.getChunkReaders( config, partition, path );
        List<ChunkReader> input = (List<ChunkReader>)(List)inputChunkReaders;

        File output = new File( "/tmp/TestCoreChunkSorterWithCustomComparator.out" );
        List<JobOutput> jobOutput = new ArrayList<JobOutput>();

        Duration duration = new Duration();

        ChunkSorter sorter = new ChunkSorter( config , partition, new Job(), new DefaultSortComparator() );
        SequenceReader result = sorter.sort( input, output, jobOutput );

        System.out.printf( "Sort took: %s\n", duration );

    }

    public static void main( String[] args ) throws Exception {

        if ( args.length == 1 ) {
            MAX=Integer.parseInt( args[0] );
        }

        TestCoreChunkSorterWithCustomComparator test = new TestCoreChunkSorterWithCustomComparator();
        test.setUp();
        test.test1();
        test.tearDown();

        //runTests();

    }

}

