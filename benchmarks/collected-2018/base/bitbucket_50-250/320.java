// https://searchcode.com/api/result/122323741/

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
package peregrine.combine;

import peregrine.Reducer;
import peregrine.StructReader;
import peregrine.config.Config;
import peregrine.config.Partition;
import peregrine.io.JobOutput;
import peregrine.io.chunk.ChunkReader;
import peregrine.reduce.SortListener;
import peregrine.reduce.sorter.ChunkSorter;
import peregrine.sort.DefaultSortComparator;
import peregrine.sort.SortComparator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                         final Reducer combiner ) throws IOException {

        SortComparator comparator = new DefaultSortComparator();
        
        ChunkSorter sorter = new ChunkSorter( config, partition, null, comparator );

        List<ChunkReader> input = new ArrayList();
        input.add( reader );

        File output = null;
        
        List<JobOutput> jobOutput = new ArrayList();

        SortListener sortListener = new SortListener() {

                public void onFinalValue( StructReader key, List<StructReader> values ) {
                    combiner.reduce( key, values );
                }

            };
        
        sorter.sort( input , output, jobOutput, sortListener );

    }
    
}

