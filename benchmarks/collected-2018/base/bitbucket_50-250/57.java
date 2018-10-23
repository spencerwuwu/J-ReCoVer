// https://searchcode.com/api/result/61250696/

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
package peregrine.app.pagerank;

import java.util.*;
import peregrine.*;
import peregrine.io.*;

public class TeleportationGrantJob {

    public static class Reduce extends Reducer {

        int nr_nodes;

        @Override
        public void init( List<JobOutput> output ) {

            super.init( output );

            BroadcastInput nrNodesBroadcastInput = getBroadcastInput().get( 0 );
            
            nr_nodes = nrNodesBroadcastInput.getValue()
                .readVarint()
                ;

            System.out.printf( "Working with nr_nodes: %,d\n", nr_nodes );
            
        }

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            double sum = 0.0;
            
            // sum up the values... 
            for ( StructReader value : values ) {
                sum += value.readDouble();
            }

            double result = (1.0 - (IterJob.DAMPENING * (1.0 - sum))) / (double)nr_nodes;

            emit( key, StructReaders.wrap( result ) );
            
        }

    }

}

