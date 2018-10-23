// https://searchcode.com/api/result/122323449/

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
import peregrine.*;
import peregrine.io.BroadcastInput;
import peregrine.io.JobOutput;
import peregrine.util.Hashcode;

import java.io.IOException;
import java.util.List;

public class IterJob {

    private static final Logger log = Logger.getLogger();

    public static final double DAMPENING = 0.9;
    
    // join graph_by_source, rank_vector, dangling
    
    public static class Map extends Merger {
        
        protected int nr_nodes;

        /**
         * Running count of dangling rank sum so that we can build
         * teleport_grant for the next iteration.
         */
        protected double dangling_rank_sum = 0.0;

        private JobOutput danglingRankSumBroadcast = null;

        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );
            
            danglingRankSumBroadcast = output.get(1);
            
            BroadcastInput nrNodesBroadcastInput = getBroadcastInput().get( 0 );
            
            nr_nodes = nrNodesBroadcastInput.getValue().readInt();

            log.info( "Working with nr_nodes: %,d", nr_nodes );
            
        }

        @Override
        public void merge( StructReader key,
                           List<StructReader> values ) {

        	StructReader outbound         = values.get( 0 );
        	StructReader rank_vector      = values.get( 1 );
        	StructReader dangling         = values.get( 2 );

            double rank;

            if ( rank_vector != null ) {
                rank = rank_vector.readDouble();
            } else { 

                // for the first pass, the rank_vector will be null but after
                // that it will contain a value from the previous iteration
                // which we're going to join against.

                rank = 1 / (double)nr_nodes;

                // TODO: we can add support for custom teleportation here.
                
            }

            if ( dangling != null ) {

                // this is a dangling node.  It will not have any outbound
                // links so don't attempt to index them.

                dangling_rank_sum += rank;

            } else { 

                int outdegree = outbound.length() / Hashcode.HASH_WIDTH;

                double grant = rank / (double)outdegree;

                // TODO: migrate this to split() 
                while ( outbound.isReadable() ) {

                    StructReader target = outbound.readSlice( Hashcode.HASH_WIDTH );
                        
                    StructReader value = new StructWriter()
                    	.writeDouble( grant )
                    	.toStructReader();

                    //System.out.printf( "TRACE flowing: %s -> %s: %s\n", source, Base64.encode( target.toByteArray() ), grant );
                    
                    emit( target, value );

                }

            }
                
        }

        @Override
        public void close() throws IOException {

            StructReader key   = StructReaders.hashcode( "id" );
            StructReader value = StructReaders.wrap( dangling_rank_sum );

            danglingRankSumBroadcast.emit( key, value );
            
        }

    }

    public static class Combine extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            double node_rank_sum = 0.0;
            
            // sum up the values... 
            for ( StructReader value : values ) {
                node_rank_sum += value.readDouble();
            }
            
            emit( key, StructReaders.wrap( node_rank_sum ) );
            
        }

    }
    
    public static class Reduce extends Reducer {

        /**
         * 
         */
        protected double teleport_grant = -1;

        protected int nr_nodes;

        protected int nr_dangling = 0;

        // the rank sum for this partition.
        protected double partition_rank_sum = 0.0;

        protected JobOutput rankSumBroadcastOutput = null;

        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );

            rankSumBroadcastOutput = output.get(1);

            nr_nodes = getBroadcastInput()
                           .get( 0 )
                           .getValue()
                           .readInt();

            nr_dangling = getBroadcastInput()
                           .get( 1 )
                           .getValue()
                           .readInt();

            teleport_grant = getBroadcastInput()
                           .get( 2 )
                           .getValue( StructReaders.wrap( teleport_grant ) )
                           .readDouble();
            
            if ( teleport_grant == -1 ) {

                // for iter 0 teleport_grant is computed easily 

                double teleport_grant_sum = nr_dangling * ( 1 / (double)nr_nodes );
                teleport_grant = (1.0 - ( DAMPENING * ( 1.0 - teleport_grant_sum ) ) ) / (double)nr_nodes;

                log.info( "Using default teleport_grant: %s ", teleport_grant );
                
            } 

            log.info( "Using teleport_grant: %15f", teleport_grant );
            
        }
        
        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            double node_rank_sum = 0.0;
            
            // sum up the values... 
            for ( StructReader value : values ) {
                node_rank_sum += value.readDouble();
            }

            double rank = (DAMPENING * node_rank_sum) + teleport_grant;

            emit( key, StructReaders.wrap( rank ) );

            // keep track of the global rank sum.
            partition_rank_sum += rank;
            
        }

        @Override
        public void close() throws IOException {

            // broadcast the rank_sum for this partition now.
            StructReader key = StructReaders.hashcode( "id" );
            
            rankSumBroadcastOutput.emit( key, StructReaders.wrap( partition_rank_sum ) );
            
        }
        
    }

}

