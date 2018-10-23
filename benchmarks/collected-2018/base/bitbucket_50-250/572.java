// https://searchcode.com/api/result/122323220/

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

import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;

import com.spinn3r.log5j.*;

public class TestDataImbalance extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    @Override
    public void doTest() throws Exception {

        Config config = getConfig();
        
        String path = String.format( "/test/%s/test1.in", getClass().getName() );
        
        ExtractWriter writer = new ExtractWriter( config, path );

        int max = 10;
        
        for( long i = 0; i < max; ++i ) {

            // NOTE: this MUST use writeLong and NOT writeHashcode as this will
            // enable data imbalance which is what we're testing for
            StructReader key = new StructWriter()
                .writeLong(i)
                .toStructReader()
                ;

            writer.write( key, key );
            
        }

        writer.close();
        
        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = getController();

        Batch batch = new Batch( getClass() );

        batch.map( Mapper.class,
                   new Input( path ),
                   new Output( "shuffle:default" ) );

        controller.exec( batch );

        /*

        controller.reduce( Reducer.class,
                           new Input( "shuffle:default" ),
                           new Output( output ) );
        */

    }

    public static void main( String[] args ) throws Exception {

        //System.setProperty( "peregrine.test.config", "1:1:1" ); // 3sec

        //System.setProperty( "peregrine.test.factor", "10" ); // 1m
        System.setProperty( "peregrine.test.config", "01:01:1" ); // takes 3 seconds

        // 256 partitions... 
        //System.setProperty( "peregrine.test.config", "08:01:32" );  // 1m

        runTests();

    }

}

