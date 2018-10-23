// https://searchcode.com/api/result/122323251/

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

import peregrine.io.*;
import peregrine.config.*;
import peregrine.controller.*;

public class TestWorkerFailureDuringJob extends BaseTestWithDaemonBackendAndController {

    @Override
    public void doTest() throws Exception {

        Config config = getConfig();
        
        String path = String.format( "/test/%s/test1.in", getClass().getName() );
        
        ExtractWriter writer = new ExtractWriter( config, path );

        int max = 10000 * getFactor();
        
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

        Controller controller = getController();

        Batch batch = new Batch( getClass() );

        batch.map( Mapper.class,
                   new Input( path ),
                   new Output( "shuffle:default" ) );

        batch.reduce( Reducer.class,
                      new Input( "shuffle:default" ),
                      new Output( "/test/test.out" ) );

        controller.exec( batch );

    }

    public static void main( String[] args ) throws Exception {
        //System.setProperty( "peregrine.test.config", "04:01:32" ); 
        //System.setProperty( "peregrine.test.config", "01:01:1" ); 
        System.setProperty( "peregrine.test.config", "02:01:04" );
        //System.setProperty( "peregrine.test.config", "2:1:3" ); 
        //System.setProperty( "peregrine.test.config", "2:1:3" ); 
        //System.setProperty( "peregrine.test.config", "1:1:1" ); 
        runTests();
        
    }

}

