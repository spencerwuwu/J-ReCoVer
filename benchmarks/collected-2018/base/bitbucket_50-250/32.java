// https://searchcode.com/api/result/125476490/

/*
 * Copyright 2011-2013 Kevin A. Burton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

public class TestMapReduceWithBlackhole extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper {

        @Override
        public void map( StructReader key,
                         StructReader value ) {

            emit( key, value );
            
        }

    }

    public static class Reduce extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {
        }

    }

    @Override
    public void doTest() throws Exception {

        doTest( 2500 * getFactor() );
        
    }

    private void doTest( int max ) throws Exception {

        Controller controller = getController();

        Batch batch = new Batch(getClass());

        batch.map( Map.class,
                   new Input( "blackhole:" ),
                   new Output( "shuffle:default" ) );

        batch.reduce( Reduce.class,
                      new Input( "shuffle:default" ),
                      new Output( "blackhole:" ) );

        controller.exec( batch );
    }

    public static void main( String[] args ) throws Exception {

        runTests();

    }

}

