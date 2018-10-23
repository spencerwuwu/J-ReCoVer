// https://searchcode.com/api/result/122323450/

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
package peregrine.app.pagerank;

import com.spinn3r.log5j.Logger;
import peregrine.Job;
import peregrine.Reducer;
import peregrine.StructReader;
import peregrine.StructReaders;
import peregrine.io.BroadcastInput;
import peregrine.io.JobOutput;

import java.util.List;

public class TeleportationGrantJob {

    private static final Logger log = Logger.getLogger();

    public static class Reduce extends Reducer {

        int nr_nodes;

        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );

            BroadcastInput nrNodesBroadcastInput = getBroadcastInput().get( 0 );
            
            nr_nodes = nrNodesBroadcastInput.getValue()
                .readInt()
                ;

            if ( nr_nodes < 0 ) {
                throw new RuntimeException( "Invalid node count: " + nr_nodes );
            }
            
        }

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            double sum = 0.0;
            
            // sum up the values... 
            for ( StructReader value : values ) {
                sum += value.readDouble();
            }

            double result = (1.0 - (IterJob.DAMPENING * (1.0 - sum))) / (double)nr_nodes;

            if ( result < 0 )
                throw new RuntimeException( "Invalid teleportation_grant: " + result );
            
            log.info( "New computed teleportation_grant is : %s", result );
            
            emit( key, StructReaders.wrap( result ) );

        }

    }

}

