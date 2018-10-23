// https://searchcode.com/api/result/61250347/

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
import java.net.*;

import peregrine.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.http.*;
import peregrine.io.*;
import peregrine.io.driver.*;
import peregrine.io.driver.example.*;
import peregrine.worker.*;

public class TestExampleDriver extends peregrine.BaseTestWithTwoDaemons {

    public static class Map extends Mapper {

        @Override
        public void init( List<JobOutput> output ) {

            //register the driver we want to use for jobs.
            IODriverRegistry.register( new ExampleIODriver() );

            super.init( output );

        }

    }

    public static class Reduce extends Reducer {

    }
        
    public void doTest() throws Exception {
        
        //register the driver
        IODriverRegistry.register( new ExampleIODriver() );

        Controller controller = new Controller( config );

        try {

            controller.map( Map.class,
                            new Input( "example:" ),
                            new Output( "shuffle:default" ) );

            controller.reduce( Reduce.class,
                               new Input( "shuffle:default" ),
                               new Output( "example:" ) );

        } finally {
            controller.shutdown();
        }

    }

    public static void main( String[] args ) throws Exception {
        runTests();
    }

}

