// https://searchcode.com/api/result/122323761/

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
package peregrine.reduce.merger;

import com.spinn3r.log5j.Logger;
import peregrine.StructReaders;
import peregrine.config.Partition;
import peregrine.io.JobOutput;
import peregrine.io.SequenceReader;
import peregrine.io.chunk.ChunkWriter;
import peregrine.io.util.Closer;
import peregrine.io.util.Flusher;
import peregrine.reduce.SortEntry;
import peregrine.reduce.SortListener;
import peregrine.reduce.SortResult;
import peregrine.sort.SortComparator;
import peregrine.task.Task;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * http://en.wikipedia.org/wiki/External_sorting
 * 
 * <p>
 * One example of external sorting is the external merge sort algorithm, which
 * sorts chunks that each fit in RAM, then merges the sorted chunks
 * together.[1][2] For example, for sorting 900 megabytes of data using only 100
 * megabytes of RAM:
 *
 * <p>
 * Read 100 MB of the data in main memory and sort by some conventional method,
 * like quicksort.
 * 
 * <p>
 * Write the sorted data to disk.
 * 
 * <p>
 * Repeat steps 1 and 2 until all of the data is in sorted 100 MB chunks (there are
 * 900MB / 100MB = 9 chunks), which now need to be merged into one single output
 * file.
 * 
 * <p>
 * Read the first 10 MB (= 100MB / (9 chunks + 1)) of each sorted chunk into input
 * buffers in main memory and allocate the remaining 10 MB for an output
 * buffer. (In practice, it might provide better performance to make the output
 * buffer larger and the input buffers slightly smaller.)
 * 
 * <p>
 * Perform a 9-way merge and store the result in the output buffer. If the output
 * buffer is full, write it to the final sorted file, and empty it. If any of the 9
 * input buffers gets empty, fill it with the next 10 MB of its associated 100 MB
 * sorted chunk until no more data from the chunk is available. This is the key
 * step that makes external merge sort work externally -- because the merge
 * algorithm only makes one pass sequentially through each of the chunks, each
 * chunk does not have to be loaded completely; rather, sequential parts of the
 * chunk can be loaded as needed.
 * 
 * <p>
 * http://en.wikipedia.org/wiki/Merge_algorithm
 * 
 * <p>
 * Merge algorithms generally run in time proportional to the sum of the lengths of
 * the lists; merge algorithms that operate on large numbers of lists at once will
 * multiply the sum of the lengths of the lists by the time to figure out which of
 * the pointers points to the lowest item, which can be accomplished with a
 * heap-based priority queue in O(log n) time, for O(m log n) time, where n is the
 * number of lists being merged and m is the sum of the lengths of the lists. When
 * merging two lists of length m, there is a lower bound of 2m - 1 comparisons
 * required in the worst case.
 * 
 * <p>
 * The classic merge (the one used in merge sort) outputs the data item with the
 * lowest key at each step; given some sorted lists, it produces a sorted list
 * containing all the elements in any of the input lists, and it does so in time
 * proportional to the sum of the lengths of the input lists.
 * 
 * 
 */
public class ChunkMerger implements Closeable {

    private static final Logger log = Logger.getLogger();

    private SortListener listener = null;

    public int entries = 0;
        
    private Partition partition;

    private List<JobOutput> output;

    private Task task = null;

    private List<SequenceReader> input;

    private SortComparator sortComparator = null;
    
    public ChunkMerger() {
    }

    public ChunkMerger( Task task,
                        SortListener listener,
                        Partition partition,
                        List<SequenceReader> input,
                        List<JobOutput> output,
                        SortComparator sortComparator ) {

        this.task = task;
        this.listener = listener;
        this.partition = partition;
        this.input = input;
        this.output = output;
        this.sortComparator = sortComparator;
        
    }

    public void merge() throws IOException {
        merge( null );
    }
    
    public void merge( ChunkWriter writer ) throws IOException {

        try {
        
            //TODO: if the input length is zero or one then we are done, however
            //everything will need to be written to the writer first which is
            //somewhat inefficient so we should try to reference the original file
            //that was supplied and just return that directly.  In practice though
            //this will only be a single 100MB file so this is not the end of the
            //world.

            if ( input.size() == 0 ) {
                log.info( "No input to merge." );
                return;
            }

            log.info( "Merging %,d readers." , input.size() );
            
            MergerPriorityQueue queue = new MergerPriorityQueue( input, sortComparator );
                            
            SortResult sortResult = new SortResult( writer, listener );

            while( true ) {
                
                MergeQueueEntry entry = queue.poll();

                if ( entry == null )
                    break;

                task.assertActiveJob(); 

                sortResult.accept( new SortEntry( entry.key, StructReaders.unsplice( entry.value ) ) );

                ++entries;

            }

            sortResult.flush();

            if ( writer != null )
                writer.flush();
            
            sortResult.close();

            if ( writer != null )         
                writer.close();

            log.info( "Merged %,d entries for %s" , entries, partition );

            if( output != null )
                new Flusher( output ).flush();

        } catch ( Throwable t ) {
            throw new IOException( "Unable to merge chunks: " + input, t );
        }

    }

    public void close() throws IOException {

        //TODO: these should use one closer.
        if ( output != null ) {
            new Closer( output ).close();
        }

        new Closer( input ).close();

    }

}


