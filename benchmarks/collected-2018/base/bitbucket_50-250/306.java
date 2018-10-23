// https://searchcode.com/api/result/122323247/

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

import peregrine.controller.Controller;
import peregrine.io.Input;
import peregrine.io.JobOutput;
import peregrine.io.Output;
import peregrine.io.driver.IODriverRegistry;
import peregrine.io.driver.example.ExampleIODriver;

import java.util.List;

public class TestExampleDriver extends BaseTestWithDaemonBackendAndController {

    public static class Map extends Mapper {

        @Override
        public void init( Job job, List<JobOutput> output ) {

            //register the driver we want to use for jobs.
            IODriverRegistry.register( new ExampleIODriver() );

            super.init( job, output );

        }

    }

    public static class Reduce extends Reducer {

    }

    @Override
    public void doTest() throws Exception {
        
        //register the driver
        IODriverRegistry.register( new ExampleIODriver() );

        Controller controller = getController();

        Batch batch = new Batch( getClass() );

        batch.map( Map.class,
                   new Input( "example:" ),
                   new Output( "shuffle:default" ) );

        batch.reduce( Reduce.class,
                      new Input( "shuffle:default" ),
                      new Output( "example:" ) );

        controller.exec( batch );

    }

    public static void main( String[] args ) throws Exception {
        runTests();
    }

}

