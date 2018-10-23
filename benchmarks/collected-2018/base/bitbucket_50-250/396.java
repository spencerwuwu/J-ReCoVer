// https://searchcode.com/api/result/125476557/

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

import java.util.*;

import peregrine.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;
import peregrine.io.partition.*;
import peregrine.os.*;
import peregrine.task.*;
import peregrine.util.*;


/**
 * Tests running a reduce but also has some code to benchmark them so that we
 * can look at the total performance.
 */
public class TestLocalReducerPerformance extends BaseTestWithDaemonBackendAndController {

    public static String OUTPUT = "blackhole:";
    
    @Override
    public void setUp() {

        super.setUp();
        
        for( Config config : getConfigs() ) {
            // so we can do post mortem on how much was written.
            config.setPurgeShuffleData( false );
        }
        
    }

    @Override
    public void doTest() throws Exception {

        Config config = getConfig();

        String path = String.format( "/test/%s/test1.in", getClass().getName() );
        
        ExtractWriter writer = new ExtractWriter( config, path );

        int size = 10000000; // 10MB by default.
        int value_size = 1024;
        
        // each write 9 bytes per key, plus 2 bytes plus the the value length.
        int write_width = 9 + 2 + value_size;

        int writes = size / write_width;
        
        int max = writes * getFactor();
        
        for( long i = 0; i < max; ++i ) {
            
            StructReader key = StructReaders.wrap( i );
            StructReader value = StructReaders.wrap( new byte[value_size] );
            writer.write( key, value );
            
        }

        writer.close();
 
        Controller controller = getController();

        /*

          TODO: disabled this test because we need to drop caches between
          the jobs and with the new Batch scheduler we can no longer do
          this.

        controller.map( peregrine.Mapper.class,
                        new Input( path ),
                        new Output( "shuffle:default" ) );

        // drop caches here so that I can benchmark raw IO
        Linux.dropCaches();

        controller.reduce( peregrine.Reducer.class,
                           new Input( "shuffle:default" ),
                           new Output( "blackhole:" ) );

        */

    }

    public static void main( String[] args ) throws Exception {

        Getopt getopt = new Getopt( args );

        System.setProperty( "peregrine.test.factor", getopt.getString( "factor", "50" ) );
        System.setProperty( "peregrine.test.config", getopt.getString( "config", "1:1:1" ) );
        TestLocalReducerPerformance.OUTPUT = getopt.getString( "output", "blackhole:" );

        System.out.printf( "Using: \n" );
        System.out.printf( "  factor: %s\n", System.getProperty( "peregrine.test.factor" ) );
        System.out.printf( "  config: %s\n", System.getProperty( "peregrine.test.config" ) );
        System.out.printf( "  output: %s\n", TestLocalReducerPerformance.OUTPUT );
        
        runTests();

    }

}

