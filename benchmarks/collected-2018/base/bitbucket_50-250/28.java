// https://searchcode.com/api/result/61250344/

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

public class TestMapReduceWithMergeFactor extends peregrine.BaseTestWithMultipleProcesses {

    private static final Logger log = Logger.getLogger();

    private static final int NR_VALUES_PER_KEY = 5;
    
    public static class Map extends Mapper {

    }

    public static class Reduce extends Reducer {

        public void reduce( StructReader key, List<StructReader> values ) {

            if ( values.size() != NR_VALUES_PER_KEY ) {
                throw new RuntimeException( String.format( "%s != %s", values.size(), NR_VALUES_PER_KEY ) );
            }
            
        }
        
    }
    
    public void doTest() throws Exception {

        for( Config config : configs ) {
            config.setShuffleSegmentMergeParallelism( 10 );
        }

        // change the shuffle buffer so we have lots of smaller files.
        for( Config config : configs ) {
            config.setShuffleBufferSize( 2000 );
            config.setSortBufferSize( 100000 );
        }
        
        doTest( 100000 * getFactor() );
        
    }

    private void doTest( int max ) throws Exception {

        Config config = getConfig();
        
        String path = String.format( "/test/%s/test1.in", getClass().getName() );
        
        ExtractWriter writer = new ExtractWriter( configs.get(0), path );

        for( int j = 0; j < NR_VALUES_PER_KEY; ++j ) {
        
            for( long i = 0; i < max; ++i ) {

            	StructReader key = StructReaders.hashcode( i );
            	StructReader value = StructReaders.wrap( i );

                writer.write( key, value );
                
            }
            
        }

        writer.close();

        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = new Controller( config );

        try {
            
            controller.map( Map.class,
                            new Input( path ),
                            new Output( "shuffle:default" ) );

            int nr_shuffles = new File( "/tmp/peregrine-fs-11112/localhost/11112/tmp/shuffle/default" ).list().length;
            
            controller.reduce( Reduce.class,
                               new Input( "shuffle:default" ),
                               new Output( output ) );

            //FIXME: add a feature to make sure that the strategy we wanted was
            //actually working.
            
        } finally {
            controller.shutdown();
        }
        
    }

    public static void main( String[] args ) throws Exception {

        //System.setProperty( "peregrine.test.config", "1:1:1" ); // 3sec

        System.setProperty( "peregrine.test.config", "02:01:04" ); // takes 3 seconds
        System.setProperty( "peregrine.test.factor", "1" ); // 1m
        //System.setProperty( "peregrine.test.config", "01:01:1" ); // takes 3 seconds

        // concurrency=2, replicas=1, hosts=4
        
        // 256 partitions... 
        //System.setProperty( "peregrine.test.config", "08:01:32" );  // 1m
        System.setProperty( "peregrine.test.config", "02:01:04" );  // 1m

        runTests();

    }

}

