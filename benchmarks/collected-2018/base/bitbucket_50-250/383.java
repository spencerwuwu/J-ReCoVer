// https://searchcode.com/api/result/61250366/

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
package peregrine.pfsd;

import java.io.*;
import java.util.*;

import peregrine.*;
import peregrine.util.*;
import peregrine.util.primitive.LongBytes;
import peregrine.reduce.*;
import peregrine.config.*;
import peregrine.io.*;
import peregrine.io.chunk.*;
import peregrine.shuffle.sender.*;
import peregrine.reduce.sorter.*;
import peregrine.http.*;

/**
 * Test the FULL shuffle path, not just pats of it...including running with two
 * daemons, writing a LOT of data, and then reading it back in correctly as if
 * we were a reducer.
 */
public class TestFanoutWritePerformance extends BaseTestWithMultipleProcesses {

    List<HttpClient> clients = new ArrayList();

    byte[] value = new byte[16384];

    int max_emits;
    
    public void init() throws Exception {

        Config config = getConfig();
        
        max_emits = ((10000000 * getFactor() ) / value.length) / config.getHosts().size();

        System.out.printf( "max_emits: %,d\n" , max_emits );
        
        long before = System.currentTimeMillis();

        int idx = 0;
        for( Host host : config.getHosts() ) {

            HttpClient client = new HttpClient( String.format( "http://%s/test-%s", host, idx++ ) );
            clients.add( client );
            
        }

        long after = System.currentTimeMillis();

        long duration = after-before;

        System.out.printf( "init duration: %,d ms\n", duration );

    }

    public void close() throws Exception {
        close3();
    }

    public void close3() throws Exception {

        long before = System.currentTimeMillis();

        while( true ) {

            List nextClients = new ArrayList();
            
            for( HttpClient client : clients ) {
                
                client.close( false );
                
                if ( client.getResult() == null ) {
                    nextClients.add( client );
                }
                
            }

            clients = nextClients;
            
            if ( clients.size() == 0 )
                break;

            Thread.sleep( 10L );
            System.out.printf( "sleeping.\n" );

        } 

        long after = System.currentTimeMillis();

        long duration = after-before;

        System.out.printf( "close3 duration: %,d ms\n", duration );

    }

    public void close2() throws Exception {

        long before = System.currentTimeMillis();

        for( HttpClient client : clients ) {
            client.shutdown();
        }

        for( HttpClient client : clients ) {
            client.close();
        }

        long after = System.currentTimeMillis();

        long duration = after-before;

        System.out.printf( "close2 duration: %,d ms\n", duration );

    }

    public void close1() throws Exception {

        System.out.printf( "basic non-parallel close\n" );
        
        long before = System.currentTimeMillis();

        for( HttpClient client : clients ) {
            client.close();
        }

        long after = System.currentTimeMillis();

        long duration = after-before;

        System.out.printf( "close1 duration: %,d ms\n", duration );

    }

    public void write() throws Exception {

        long before = System.currentTimeMillis();

        for ( int i = 0; i < max_emits; ++i ) {

            for( HttpClient client : clients ) {
                client.write( value );
            }

        }

        long after = System.currentTimeMillis();

        long duration = after-before;

        System.out.printf( "write duration: %,d ms\n", duration );

    }

    public void doTest() throws Exception {

        Config config = getConfig();
        
        // create the writers.

        long before = System.currentTimeMillis();

        init();

        write();

        long written = max_emits * value.length * clients.size();

        System.out.printf( "Running with %,d hosts.\n", config.getHosts().size() );

        close();

        long after = System.currentTimeMillis();

        long duration = (after-before);

        long throughput = -1;

        try {
            throughput = (long)((written / duration) * 1000 );
        } catch ( Throwable t ) {}

        System.out.printf( "Wrote %,d bytes with duration %,d ms with throughput %,d b/s\n", written, duration, throughput );

    }

    public static void main( String[] args ) throws Exception {

        //System.setProperty( "peregrine.test.factor", "35" ); 
        System.setProperty( "peregrine.test.factor", "20" ); 
        //System.setProperty( "peregrine.test.config", "01:01:1" ); 
        System.setProperty( "peregrine.test.config", "01:01:256" ); 

        runTests();

    }

}

