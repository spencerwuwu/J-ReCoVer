// https://searchcode.com/api/result/122323479/

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
package peregrine.app.flow;

import com.spinn3r.log5j.Logger;
import peregrine.*;
import peregrine.io.JobOutput;
import peregrine.util.Hashcode;
import peregrine.util.Strings;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlowInitJob {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper {

        Set<StructReader> nodes = new HashSet();

        private int hits = 0;
        
        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );

            boolean caseInsensitive = job.getParameters().getBoolean( "caseInsensitive" );

            log.info( "Working with caseInsensitive graph: %s", caseInsensitive );
            
            String sources = job.getParameters().getString( "sources" );
            
            if ( Strings.empty( sources ) ) {
                throw new RuntimeException( "No sources specified" );
            }

            for( String source : sources.split( ":" ) ) {

                if ( caseInsensitive )
                    source = source.toLowerCase();
                
                nodes.add( StructReaders.hashcode( source ) );
            }

            log.info( "Working with %,d source nodes.", nodes.size() );

        }

        @Override
        public void map( StructReader key, StructReader value ) {

            if ( nodes.contains( key ) == false ) {
                return;
            }
            
            List<StructReader> outbound = StructReaders.split( value, Hashcode.HASH_WIDTH );

            for( StructReader target : outbound ) {
                emit( target, StructReaders.wrap( true ) );
            }

            ++hits;
            
        }

        @Override
        public void close() throws IOException {
            log.info( "Found %,d hits", hits );
        }
        
    }

    public static class Reduce extends Reducer {
        
        @Override
        public void reduce( StructReader key, List<StructReader> values ) {
            emit( key, StructReaders.wrap( true ) );
        }
        
    }

}

