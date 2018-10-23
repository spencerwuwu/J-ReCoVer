// https://searchcode.com/api/result/122323249/

/*
 * Copyright 2011-2013 Kevin A. Burton
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

import com.spinn3r.log5j.Logger;
import peregrine.config.Config;
import peregrine.config.Host;
import peregrine.config.Membership;
import peregrine.config.Partition;
import peregrine.controller.Controller;
import peregrine.controller.client.ControllerClient;
import peregrine.io.ExtractWriter;
import peregrine.io.Input;
import peregrine.io.Output;
import peregrine.io.partition.LocalPartitionReader;
import peregrine.io.partition.RemotePartitionWriterDelegate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class TestJobSubmission extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    @Override
    public void doTest() throws Exception {

        doTest( 100 );

    }

    private void doTest( int max ) throws Exception {

        log.info( "Testing with %,d records." , max );

        Config config = getConfig();

        String path = String.format( "/test/%s/test1.in", getClass().getName() );

        ExtractWriter writer = new ExtractWriter( config, path );

        for( int i = 0; i < max; ++i ) {

            StructReader key = StructReaders.hashcode( i );
            StructReader value = StructReaders.hashcode( i );

            writer.write( key, value );

        }

        writer.close();

        // the writes worked correctly.

        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Batch batch = new Batch(getClass());

        batch.map( TestMapReduce.Map.class,
                   new Input( path ),
                   new Output( "shuffle:default" ) );

        // make sure the shuffle output worked

        batch.reduce( TestMapReduce.Reduce.class,
                      new Input( "shuffle:default" ),
                      new Output( output ) );

        batch = ControllerClient.submit( getConfig(), batch );

        assertTrue( "No batch ID", batch.getIdentifier() > 0 );

        ControllerClient.waitForBatch( config, batch.getIdentifier(), 30000L );

    }

    public static void main( String[] args ) throws Exception {

        //System.setProperty( "peregrine.test.config", "1:1:1" ); // 3sec

        //setPropertyDefault( "peregrine.test.factor", "100" ); //
        //setPropertyDefault( "peregrine.test.config", "1:1:1" ); // takes 3 seconds

        // 256 partitions...
        //System.setProperty( "peregrine.test.config", "08:01:32" );  // 1m

        System.setProperty( "peregrine.test.config", "1:1:1" );  // 1m
        runTests();

    }

}

