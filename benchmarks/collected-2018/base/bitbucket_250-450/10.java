// https://searchcode.com/api/result/61250345/

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
package peregrine;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;
import peregrine.io.partition.*;
import peregrine.util.primitive.*;
import peregrine.util.*;
import com.spinn3r.log5j.*;

public class TestMapReduce extends peregrine.BaseTestWithMultipleProcesses {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper {

        @Override
        public void map( StructReader key,
                         StructReader value ) {

            emit( key, value );
            
        }

    }

    public static class Reduce extends Reducer {

        AtomicInteger count = new AtomicInteger();
        
        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            List<Integer> ints = new ArrayList();

            // decode these so we know what they actually mean.
            for( StructReader val : values ) {
                ints.add( val.readInt() );
            }

            if ( values.size() != 2 ) {

                throw new RuntimeException( String.format( "%s does not equal %s (%s) on nth reduce %s" ,
                                                           values.size(), 2, ints, count ) );
            }

            count.getAndIncrement();

        }

        @Override
        public void cleanup() {

            if ( count.get() == 0 )
               throw new RuntimeException( "count is zero" );
            
        }

    }

    @Override
    public void doTest() throws Exception {

        doTest( 2500 * getFactor() );
        
    }

    private void fsck( String path ) throws IOException {

        log.info( "Running fsck of %s", path );

        Config config = getConfig();
        
        Membership membership = config.getMembership();
        
        for( Partition part : config.getMembership().getPartitions() ) {

            List<Host> hosts = config.getMembership().getHosts( part );

            Set state = new HashSet();

            for( Host host : hosts ) {

                try {

                    String relative = String.format( "%s", path );
                    
                    log.info( "Checking %s on %s", relative, host );
                    
                    RemotePartitionWriterDelegate delegate = new RemotePartitionWriterDelegate( config, false );
                    delegate.init( config, part, host, relative );
                    
                    java.util.Map stat = delegate.stat();
                    
                    state.add( stat.get( "X-length" ) );

                } catch ( IOException e ) {
                    throw new IOException( "fsck failed: " , e );
                }
                
            }

            if ( state.size() != 1 )
                throw new IOException( String.format( "fsck failed.  %s is in inconsistent state: %s", part, state ) );
            
        }
        
    }
    
    private void doTest( int max ) throws Exception {

        log.info( "Testing with %,d records." , max );

        Config config = getConfig();

        System.gc();

        Runtime runtime = Runtime.getRuntime();
        
        long before = runtime.totalMemory() - runtime.freeMemory();
        
        String path = String.format( "/test/%s/test1.in", getClass().getName() );

        ExtractWriter writer = new ExtractWriter( config, path );

        for( int i = 0; i < max; ++i ) {

            StructReader key = new StructWriter()
                .writeHashcode(i)
                .toStructReader()
                ;
            
            StructReader value = new StructWriter()
                .writeInt(i)
                .toStructReader()
                 ;
            
            writer.write( key, value );
            
        }

        // write data 2x to verify that sorting works.
        for( int i = 0; i < max; ++i ) {
        	
            StructReader key = new StructWriter()
                .writeHashcode(i)
                .toStructReader()
            ;
        
            StructReader value = new StructWriter()
                .writeInt(i)
                .toStructReader()
             ;

            writer.write( key, value );
        }

        writer.close();

        fsck( path );

        // verify that the right number of items have been written to filesystem.

        Set<Partition> partitions = config.getMembership().getPartitions();

        int count = 0;

        int idx = 0;

        for( Partition part : partitions ) {

            Config part_config = configsByHost.get( config.getMembership().getHosts( part ).get( 0 ) );
            
            LocalPartitionReader reader = new LocalPartitionReader( part_config , part, path );

            int countInPartition = 0;
            
            while( reader.hasNext() ) {

                reader.next();
                
                reader.key();
                reader.value();

                ++countInPartition;
                
            }

            System.out.printf( "Partition %s has entries: %,d \n", part, countInPartition );

            count += countInPartition;

        }

        assertEquals( max * 2, count );

        // the writes worked correctly.
        
        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = new Controller( config );

        try {

            controller.map( Map.class,
                            new Input( path ),
                            new Output( "shuffle:default" ) );

            // make sure the shuffle output worked
            
            controller.reduce( Reduce.class,
                               new Input( "shuffle:default" ),
                               new Output( output ) );

            System.gc();

            long after = runtime.totalMemory() - runtime.freeMemory();

            long used = after - before ;
            
            System.out.printf( "Memory footprint before = %,d bytes, after = %,d bytes, diff = %,d bytes\n",
                               before, after, used );

        } finally {
            controller.shutdown();
        }

    }

    public static void main( String[] args ) throws Exception {

        //System.setProperty( "peregrine.test.config", "1:1:1" ); // 3sec

        setPropertyDefault( "peregrine.test.factor", "1" ); // 
        setPropertyDefault( "peregrine.test.config", "01:01:01" ); // takes 3 seconds

        // 256 partitions... 
        //System.setProperty( "peregrine.test.config", "08:01:32" );  // 1m

        runTests();

    }

}

