// https://searchcode.com/api/result/61250350/

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
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;
import peregrine.util.*;
import peregrine.util.primitive.IntBytes;
import peregrine.io.partition.*;

public class TestBroadcastMapReduce extends peregrine.BaseTestWithMultipleProcesses {
    
    public static class Map extends Mapper {

        private int count = 0;

        private JobOutput countBroadcast = null;
        
        @Override
        public void init( List<JobOutput> output ) {

            super.init( output );
            countBroadcast = output.get(1);
            
        }

        @Override
        public void map( StructReader key,
        		         StructReader value ) {
            ++count;
        }

        @Override
        public void cleanup() {
            
            if ( count == 0 ) {
                throw new RuntimeException( "no results" );
            }

            System.out.printf( "Writing count: %,d to %s\n", count, countBroadcast );

            StructReader key = StructReaders.hashcode( "id" );
            StructReader value = StructReaders.wrap( count );
            
            countBroadcast.emit( key, value );
            
        }

    }

    public static class Reduce extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            int count = 0;
            
            for( StructReader val : values ) {
                count += val.readInt();
            }
            
            emit( key, StructReaders.wrap( count ) );
            
        }

    }

    /**
     * 
     */
     public void doTest() throws Exception {

         String path = String.format( "/test/%s/test1.in", getClass().getName() );

         Config config = getConfig();
         
         ExtractWriter writer = new ExtractWriter( config, path );

         int max = 1000;
         
         for( long i = 0; i < max; ++i ) {
             
             StructReader key = StructReaders.hashcode( i );
             StructReader value = key;
             writer.write( key, value );
             
         }
         
         writer.close();

         String count_out = String.format( "/test/%s/test1.count", getClass().getName() );
             
         Controller controller = new Controller( config );

         try {
             
             controller.map( Map.class,
                             new Input( path ),
                             new Output( "shuffle:default",
                                         "broadcast:count" ) );

             System.out.printf( "job done.. now going to assert the values.\n" );
             
             controller.reduce( Reduce.class,
                                new Input( "shuffle:count" ),
                                new Output( count_out ) );

         } finally {
             controller.shutdown();
         }

         // // now read all partition values...
         
         assertValueOnAllPartitions( config.getMembership() , count_out, max );
         
         System.out.printf( "WIN\n" );

    }

    public void assertValueOnAllPartitions( Membership membership, String path, int value ) throws Exception {

        for( Partition part : membership.getPartitions() ) {

            for( Host host : membership.getHosts( part ) ) {

                LocalPartitionReader reader = new LocalPartitionReader( configsByHost.get( host ), part, path );

                String source = String.format( "path=%s , part=%s (%s)", path, part, reader );

                System.out.printf( "Reading from: %s\n", source );
                
                if ( reader.hasNext() == false )
                    throw new Exception( "No values in: " + source );

                reader.next();
                
                StructReader _key   = reader.key();
                StructReader _value = reader.value();
                
                int count = _value.readInt();

                if ( count != value )
                    throw new Exception( "Invalid value: " + count );

                if ( count == 0 )
                    throw new Exception( "zero" );

                System.out.printf( "count: %,d\n", count );
                
                if ( reader.hasNext() )
                    throw new IOException( "too many values" );
                
            }

        }

    }

    public static void main( String[] args ) throws Exception {

        System.setProperty( "peregrine.test.config", "1:1:1" ); 

        //System.setProperty( "peregrine.test.config", "4:1:16" ); 
        //System.setProperty( "peregrine.test.config", "01:01:1" ); 
        //System.setProperty( "peregrine.test.config", "8:1:32" ); 
        //System.setProperty( "peregrine.test.config", "8:1:16" ); 
        runTests();

    }

}


