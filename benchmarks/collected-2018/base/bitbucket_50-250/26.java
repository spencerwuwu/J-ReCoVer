// https://searchcode.com/api/result/125476496/

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

import java.util.*;

import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;

import com.spinn3r.log5j.*;

public class TestCombinerWithinReduce extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper {

    }

    public static class Reduce extends Reducer {

        public void reduce( StructReader key, List<StructReader> values ) {

            emit( key, StructReaders.wrap( true ) );

        }
        
    }

    @Override
    public void setUp() {

        addExtraWorkerArgument("shuffleBufferSize", "2M");
        addExtraWorkerArgument("sortBufferSize", "16M");
        addExtraWorkerArgument("shuffleSegmentMergeParallelism", "4");

        super.setUp();

    }
        
    public void doTest() throws Exception {

        doTest( 400000 * getFactor() );
        
    }

    private void doTest( int max ) throws Exception {

        Config config = getConfig();
        
        String path = String.format( "/test/%s/test1.in", getClass().getName() );
        
        ExtractWriter writer = new ExtractWriter( getConfigs().get(0), path );
        
        for( long i = 0; i < max; ++i ) {
            
            StructReader key = StructReaders.hashcode( "id" );
            StructReader value = StructReaders.wrap( true );
            
            writer.write( key, value );
            
        }

        writer.close();

        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = getController();

        Batch batch = new Batch( getClass() );

        batch.map( Map.class,
                   new Input( path ),
                   new Output( "shuffle:default" ) );

        batch.reduce( new Job().setDelegate( Reduce.class )
                               .setCombiner( Reduce.class )
                               .setInput( "shuffle:default" )
                               .setOutput( output ) );

        controller.exec( batch );

    }

    public static void main( String[] args ) throws Exception {

        System.setProperty( "peregrine.test.factor", "3" ); // 3sec
        System.setProperty( "peregrine.test.config", "2:1:4" ); // 3sec

        runTests();

    }

}

