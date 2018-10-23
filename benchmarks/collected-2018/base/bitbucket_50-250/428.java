// https://searchcode.com/api/result/125476544/

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
package peregrine.shuffle;

import java.io.*;
import peregrine.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;
import peregrine.io.chunk.*;
import peregrine.io.driver.shuffle.*;
import peregrine.reduce.*;
import peregrine.reduce.sorter.*;
import peregrine.shuffle.sender.*;
import peregrine.util.*;
import peregrine.task.*;


/**
 * Test the FULL shuffle path, not just pats of it...including running with two
 * daemons, writing a LOT of data, and then reading it back in correctly as if
 * we were a reducer.
 */
public class TestFullShufflePath extends BaseTestWithDaemonBackendAndController {

    private void doTestIter( ShuffleJobOutput output, int max_emits ) throws Exception {

        for ( long i = 0; i < max_emits; ++i ) {

            byte[] value = new byte[] { (byte)'x', (byte)'x', (byte)'x', (byte)'x', (byte)'x', (byte)'x', (byte)'x' };

            output.emit( StructReaders.wrap( i ), StructReaders.wrap( value ) );
        }

    }

    public void doTest( int iterations, int max_emits ) throws Exception {

        Config config = getConfig();

        System.out.printf( "Running with %,d hosts.\n", config.getHosts().size() );

        Controller controller = getController();

        Job job = new Job();
        
        job.getPartitionerInstance().init( config );


        ShuffleJobOutput output = new ShuffleJobOutput( config, job, new Partition( 0 ), new Report() );

        for( int i = 0; i < iterations; ++i ) {

            ChunkReference chunkRef = new ChunkReference( new Partition( 0 ) );
            chunkRef.local = i;

            // we need to call onChunk to startup the shuffle job output.
            output.onChunk( chunkRef );

            doTestIter( output, max_emits );

            output.onChunkEnd( chunkRef );

        }

        output.close();

        controller.flushAllShufflers();

        // TODO: this test is no longer possible becasue we can't read from just
        // ONE of the partitions without the other one blocking.  However it
        // would be nice to add it back in at some point.
        
        // now the data should be on disk... try to read it back out wth a ShuffleInputChunkReader

        // int count = 0;
        
        // count += readShuffle( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000000.tmp", 0 );
        // count += readShuffle( "/tmp/peregrine-fs/localhost/11113/tmp/shuffle/default/0000000000.tmp", 1 );

        // assertEquals( max_emits * iterations, count );

        // ShuffleInputChunkReader reader = new ShuffleInputChunkReader( configs.get( 0 ),
        //                                                               new Partition( 0 ),
        //                                                               "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000000.tmp" );

        // while( reader.hasNext() ) {
        //     reader.next();
        //     //System.out.printf( "key: %s, value: %s\n", Hex.encode( reader.key() ), Hex.encode( reader.value() ) );
        // }
        // reader.close();
        // ChunkSorter sorter = new ChunkSorter( configs.get( 0 ) , new Partition( 0 ), new ShuffleInputReference( "default" ) );

        // File file_input = new File( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000000.tmp" );
        // File file_output = new File( "/tmp/test.out" );

        // System.out.printf( "===============================\n" );
        
        // ChunkReader result = sorter.sort( file_input, file_output );

        // assertResults( result, max_emits );

    }

    public static void assertResults( SequenceReader reader, int max ) throws Exception {

    	StructReader last = null;

        FullKeyComparator comparator = new FullKeyComparator();

        int count = 0;
        while( reader.hasNext() ) {
            
        	reader.next();
        	
        	StructReader key   = reader.key();
        	StructReader value = reader.value();

            System.out.printf( "%s\n", Hex.encode( key ) );
            
            if ( last != null && comparator.compare( last.toByteArray(), value.toByteArray() ) > 0 )
                throw new RuntimeException( "value is NOT less than last value" );

            Longs.toByteArray( count );

            /*
            if ( last != null && comparator.compare( last, new Tuple( correct, correct ) ) == 0 ) {

                String message = "value is NOT correct";
                
                throw new RuntimeException( message );
            }
            */
            
            last = value;
            ++count;
            
        }

        assertTrue( count > 0 );
        //assertEquals( reader.size(), count );
        
        //assertEquals( max, count );

    }

    private int readShuffle( String path, int partition ) throws IOException {

        ShuffleInputChunkReader reader = new ShuffleInputChunkReader( getConfigs().get( partition ), new Partition( partition ), path );

        assertTrue( reader.count() > 0 );

        int count = 0;
        while( reader.hasNext() ) {

            reader.next();
            //System.out.printf( "readShuffle: partition: %s, key: %s, value: %s\n",
            //                   partition, Hex.encode( reader.key() ), Hex.encode( reader.value() ) );

            ++count;

        }

        assertEquals( reader.count(), count );

        System.out.printf( "Read count: %,d\n", count );

        return count;
        
    }

    @Override
    public void doTest() throws Exception {

        doTest( 2, 100000 );

        // FIXME sort BOTH partitions and make sure the counts are right.
        // 
        
        //doTest( 2, 0 );  FIXME: this doesn't work.
        //doTest( 2, 1 );
        //doTest( 2, 2 );
        // doTest( 2, 3 );
        // doTest( 2, 10 );
        // doTest( 2, 100 );
        // doTest( 2, 1000 );
        // doTest( 2, 10000 );
        // doTest( 2, 10000000 );

        //doTest( 3, 100 );

        //doTest( 2, 5000000 );

        //doTest( 10, 3 );
        //doTest( 3, 100 );

    }

    public static void main( String[] args ) throws Exception {
        runTests();
    }

}

