// https://searchcode.com/api/result/61250679/

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
package peregrine.combine;

import java.util.*;
import java.io.*;

import peregrine.*;
import peregrine.reduce.*;
import peregrine.reduce.sorter.*;
import peregrine.config.*;
import peregrine.io.*;
import peregrine.io.chunk.*;
import peregrine.util.*;
import peregrine.util.primitive.*;

/**
 * Run a combine on the given ChunkReader and use the given combiner to
 * merge/compress/collapse the results before we send them over the wire. 
 */
public class CombineRunner {

    /**
     * Take the given reader and combine records.
     */
    public void combine( Config config,
                         Partition partition,
                         ChunkReader reader,
                         final Combiner combiner ) throws IOException {

        ChunkSorter sorter = new ChunkSorter( config, partition );

        List<ChunkReader> input = new ArrayList();
        input.add( reader );

        File output = null;
        
        List<JobOutput> jobOutput = new ArrayList();

        SortListener sortListener = new SortListener() {

                public void onFinalValue( StructReader key, List<StructReader> values ) {
                    combiner.combine( key, values );
                }

            };
        
        sorter.sort( input , output, jobOutput, sortListener );

    }
    
}

