// https://searchcode.com/api/result/122323255/

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

public class TestChunkWriterCloseOnReferenceCount extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    private static final int COUNT = 150;
    private static final int DUPLICATES = 40;
    
    public static class Merge extends Merger {

        private void handle( StructReader wrapped ) {

            for ( StructReader val : StructReaders.unsplice( wrapped ) ) {
                val.readInt();
            }
            
        }
        
        @Override
        public void merge( StructReader key,
                           List<StructReader> values ) {

            handle( values.get( 0 ) );
            handle( values.get( 1 ) );

        }

    }

    private void write( String path ) throws Exception {
        
        Config config = getConfig();

        ExtractWriter writer = new ExtractWriter( config, path );

        int count = COUNT;
        int duplicates = DUPLICATES;

        if ( getFactor() == 1 ) {
            count = 100;
            duplicates = 10;
        }

        for( int i = 0; i < count; ++i ) {
        
            for( int j = 0; j < duplicates; ++j ) {
                
                writer.write( StructReaders.hashcode( i ),
                              StructReaders.wrap( 1 ) );
                
            }

        }

        writer.close();

    }

    private void sort( Controller controller, String in, String out ) throws Exception {

        Batch batch = new Batch( getClass() );

        batch.map( Mapper.class,
                   new Input( in ),
                   new Output( "shuffle:default" ) );
        
        batch.reduce( Reducer.class,
                      new Input( "shuffle:default" ),
                      new Output( out ) );

        controller.exec( batch );
        
    }
    
    @Override
    public void doTest() throws Exception {

        Config config = getConfig();

        String test1_in = String.format( "/test/%s/test1.in", getClass().getName() );
        String test2_in = String.format( "/test/%s/test2.in", getClass().getName() );

        String test1_sorted = String.format( "/test/%s/test1.sorted", getClass().getName() );
        String test2_sorted = String.format( "/test/%s/test2.sorted", getClass().getName() );

        write( test1_in );
        write( test2_in );
        
        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = getController();

        sort( controller, test1_in, test1_sorted );
        sort( controller, test2_in, test2_sorted );

        Batch batch = new Batch( getClass() );

        batch.merge( Merge.class,
                     new Input( test1_sorted, test2_sorted ),
                     new Output( String.format( "/test/%s.out", getClass().getName() ) ) );

        controller.exec( batch );

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


