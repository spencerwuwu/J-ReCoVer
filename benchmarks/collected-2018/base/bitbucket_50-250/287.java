// https://searchcode.com/api/result/122323749/

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
package peregrine.reduce.sorter;

import com.spinn3r.log5j.Logger;
import peregrine.Job;
import peregrine.Record;
import peregrine.Reducer;
import peregrine.StructReader;
import peregrine.config.Config;
import peregrine.config.Partition;
import peregrine.io.JobOutput;
import peregrine.io.SequenceReader;
import peregrine.io.buffer.region.ByteRegionAllocationTracker;
import peregrine.io.buffer.region.ByteRegionAllocator;
import peregrine.io.chunk.ChunkReader;
import peregrine.io.chunk.CompositeChunkReader;
import peregrine.io.chunk.DefaultChunkReader;
import peregrine.io.chunk.DefaultChunkWriter;
import peregrine.io.util.Closer;
import peregrine.io.util.Flusher;
import peregrine.reduce.SortEntry;
import peregrine.reduce.SortListener;
import peregrine.reduce.SortResult;
import peregrine.sort.SortComparator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sort the given chunk readers based on the key.
 */
public class ChunkSorter extends BaseChunkSorter {

    private static final Logger log = Logger.getLogger();
    
    private Partition partition;
    
    private Config config;

    private Job job = null;

    public ChunkSorter( Config config,
                        Partition partition,
                        Job job,
                        SortComparator comparator ) {

        super( comparator );

    	this.config = config;
		this.partition = partition;
        this.job = job;

    }

    public SequenceReader sort( List<ChunkReader> input,
                                File output,
                                List<JobOutput> jobOutput )
        throws IOException {

        return sort( input, output, jobOutput, null );
        
    }
    
    public SequenceReader sort( List<ChunkReader> input,
                                File output,
                                List<JobOutput> jobOutput,
                                SortListener sortListener )
        throws IOException {

        CompositeChunkReader reader    = null;
        DefaultChunkWriter writer      = null;
        DefaultChunkWriter sortWriter  = null;
        SortResult sortResult          = null;
        KeyLookup lookup               = null;
        KeyLookup sorted               = null;

        ByteRegionAllocationTracker globalMemoryAllocationTracker = config.getByteRegionAllocationTracker();
        ByteRegionAllocationTracker localMemoryAllocationTracker = new ByteRegionAllocationTracker( config );
        ByteRegionAllocator allocator = new ByteRegionAllocator( config, globalMemoryAllocationTracker, localMemoryAllocationTracker );

        try {

            log.info( "Going to sort: %s (memory %s)", input, globalMemoryAllocationTracker );

            // TODO: do this async so that we can read from disk and compute at
            // the same time... we need a background thread to trigger the
            // pre-read.
            
            reader = new CompositeChunkReader( config, input );
            
            lookup = new SortLookup( allocator, reader );

            log.debug( "Key lookup for %s has %,d entries." , partition, lookup.size() );

            sorted = sort( lookup );

            //write this into the final ChunkWriter now.

            if ( output != null ) {
                writer = new DefaultChunkWriter( config, output );
                sortWriter = writer;
            }

            // setup a combiner here... instantiate the Combiner and call
            //
            // startup( Job, List<JobOutput> )
            //
            // and we don't need to pass the writer as we can just emit() from
            // the combiner.

            if ( job.getCombiner() != null ) {

                final Reducer combiner = newCombiner( job, writer );

                sortListener = new SortListener() {

                        public void onFinalValue( StructReader key, List<StructReader> values ) {
                            combiner.reduce( key, values );
                        }

                    };

                // set the sort writer to null so that SortResult doesn't have a
                // writer which means that it functions just to call
                // onFinalValue.  TODO: in the future it might be nice to make
                // SortResult ONLY work via this method so that the code is
                // easier to maintain.
                
                sortWriter = null;
                
            }
            
            sortResult = new SortResult( sortWriter, sortListener );

            sorted.reset();
            while( sorted.hasNext() ) {

                sorted.next();

                Record current = sorted.get();

                sortResult.accept( new SortEntry( current.getKey(), current.getValue() ) );

            }

            log.debug( "Sort output file %s has %,d entries. (memory %s)", output, sorted.size(), globalMemoryAllocationTracker );

        } catch ( Throwable t ) {

            //TODO: this error is actually incorrect because we actually WERE
            // able to sort some of the input files but not ALL of them.
            String error = String.format( "Unable to sort input=%s, output=%s, jobOutput=%s, sortListener=%s, host=%s for %s" ,
                                          input, output, jobOutput, sortListener, config.getHost(), partition );

            log.error( "%s", error, t );
            
            throw new IOException( error , t );
            
        } finally {

            //TODO: these should be the same flusher.
            new Flusher( jobOutput ).flush();

            new Flusher( writer ).flush();
            
            // NOTE: it is important that the writer be closed before the reader
            // because if not then the writer will attempt to read values from 
            // closed reader and we will segfault.
            new Closer( sortResult, writer, reader, sorted, lookup ).close();

        }

        // if we got to this part we're done... 

        DefaultChunkReader result = null;
        
        if ( output != null ) {
            result = new DefaultChunkReader( config, output );
        }

        if ( localMemoryAllocationTracker.get() > 0 ) {
            throw new IOException( "Remaining direct memory not released: " + localMemoryAllocationTracker.get() );
        }

        return result;

    }

    /**
     * Create a combiner instance and return it as a Reducer (they use the same
     * interface).
     */
    public static Reducer newCombiner(Job job, DefaultChunkWriter writer) {

        try {

            List<JobOutput> jobOutput = new ArrayList<JobOutput>();
            jobOutput.add( new CombinerJobOutput( writer ) );
            
            Reducer result = (Reducer)job.getCombiner().newInstance();
            result.init( job, jobOutput );

            return result;
            
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        
    }

}


