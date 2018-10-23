// https://searchcode.com/api/result/122323230/

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

import com.spinn3r.log5j.*;

public class TestMapReduceWithBroadcastOnClose extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper {

        @Override
        public void map( StructReader key,
                         StructReader value ) {

            emit( key, value );
            
        }

    }

    public static class Reduce extends Reducer {

        protected JobOutput broadcastOutput = null;

        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );

            broadcastOutput = output.get(1);

        }

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {
            // noop.
        }

        @Override
        public void close() throws IOException {
            
            broadcastOutput.emit( StructReaders.wrap( 0L ),
                                  StructReaders.wrap( 0L ) );

        }

    }

    @Override
    public void doTest() throws Exception {

        doTest( getMaxFromFactor( 10, 1250 ) );
        
    }

    private void doTest( int max ) throws Exception {

        log.info( "Testing with %,d records." , max );
        
        String path = String.format( "/test/%s/test1.in", getClass().getName() );

        Config config = getConfig();
        
        ExtractWriter writer = new ExtractWriter( config, path );

        for( long i = 0; i < max; ++i ) {

            StructReader key = StructReaders.hashcode( i );
            StructReader value = StructReaders.wrap( i );

            writer.write( key, value );
            
        }

        writer.close();

        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = getController();

        Batch batch = new Batch( getClass() );

        batch.map( Map.class,
                   new Input( path ),
                   new Output( "shuffle:default" ) );

        // make sure the shuffle output worked

        batch.reduce( Reduce.class,
                      new Input( "shuffle:default" ),
                      new Output( output, "broadcast:test" ) );

        controller.exec( batch );

    }

    public static void main( String[] args ) throws Exception {
        System.setProperty( "peregrine.test.factor", "2" );
        System.setProperty( "peregrine.test.config", "1:1:2" );
        runTests();

    }

}

