// https://searchcode.com/api/result/61250698/

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
import peregrine.util.*;
import peregrine.util.primitive.IntBytes;
import peregrine.io.*;

public class NodeMetadataJob {

    public static class Map extends Merger {

        JobOutput nodeMetadataOutput         = null;
        JobOutput danglingOutput             = null;
        JobOutput nonlinkedOutput            = null;

        JobOutput nrNodesBroadcastOutput     = null;
        JobOutput nrDanglingBroadcastOutput  = null;

        int nrNodes = 0;
        int nrDangling = 0;
        
        @Override
        public void init( List<JobOutput> output ) {
            nodeMetadataOutput           = output.get(0);
            danglingOutput               = output.get(1);
            nonlinkedOutput              = output.get(2);
            nrNodesBroadcastOutput       = output.get(3);
            nrDanglingBroadcastOutput    = output.get(4);
        }

        @Override
        public void merge( StructReader key,
                           List<StructReader> values ) {

            // left should be node_indegree , right should be the graph... 

            int indegree  = 0;
            int outdegree = 0;
            
            if ( values.get(0) != null ) {
                indegree = values.get(0).readInt();
            }

            if ( values.get(1) != null ) {

                StructReader graph_by_source = values.get(1);

                outdegree = graph_by_source.length() / Hashcode.HASH_WIDTH;
                
            }

            if ( outdegree == 0 ) {
                
                ++nrDangling;
                
                // TODO would be NICE to support a sequence file where the
                // values are optional for better storage efficiency.  This
                // would essentially be a 'set' of just keys.

                danglingOutput.emit( key, StructReaders.TRUE );
                
            }

            if ( indegree == 0 ) {
                nonlinkedOutput.emit( key, StructReaders.TRUE );
            }

            nodeMetadataOutput.emit( key, new StructWriter()
                                             .writeInt( indegree )
                                             .writeInt( outdegree )
                                             .toStructReader() );

            ++nrNodes;
            
        }

        @Override
        public void cleanup() {

            StructReader key = StructReaders.hashcode( "id" );

            if ( nrNodes > 0 ) {
                // *** broadcast nr_nodes.
                nrNodesBroadcastOutput.emit( key, StructReaders.wrap( nrNodes ) );
            }

            // *** broadcast nr dangling.
            nrDanglingBroadcastOutput.emit( key, StructReaders.wrap( nrDangling ) );

        }

    }

    public static class Reduce extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            int count = 0;
            
            for( StructReader val : values ) {
                count += val.readInt();
            }

            emit( key, StructReaders.wrap( count ) );

        }

    }

}

