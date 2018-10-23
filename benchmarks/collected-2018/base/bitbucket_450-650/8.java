// https://searchcode.com/api/result/125476993/

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
package peregrine.reduce;

import com.spinn3r.log5j.Logger;
import peregrine.Job;
import peregrine.config.Config;
import peregrine.config.Host;
import peregrine.config.Partition;
import peregrine.io.JobOutput;
import peregrine.io.SequenceReader;
import peregrine.io.buffer.JVMReservedMemory;
import peregrine.io.chunk.ChunkReader;
import peregrine.io.chunk.DefaultChunkReader;
import peregrine.io.driver.shuffle.ShuffleInputReference;
import peregrine.io.partition.DefaultPartitionWriter;
import peregrine.io.partition.LocalPartitionReader;
import peregrine.io.util.Closer;
import peregrine.io.util.Files;
import peregrine.os.MappedFileReader;
import peregrine.reduce.merger.ChunkMerger;
import peregrine.reduce.sorter.ChunkSorter;
import peregrine.reduce.sorter.SortLookup;
import peregrine.shuffle.ShuffleHeader;
import peregrine.shuffle.ShuffleInputChunkReader;
import peregrine.shuffle.ShuffleInputReader;
import peregrine.sysstat.SystemProfiler;
import peregrine.task.Task;
import peregrine.util.Duration;
import peregrine.util.Integers;
import peregrine.util.Longs;
import peregrine.util.netty.PrefetchReader;
import peregrine.util.netty.PrefetchReaderListener;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Run a reduce over the the given partition.  This handles intermerging, memory
 * allocation of sorting, etc.
 */
public class ReduceRunner implements Closeable {

    private static final Logger log = Logger.getLogger();

    /**
     * This is needed I *think* because we are not closing direct buffers. I
     * can back this out later once I track down exactly why we are running out
     * of memory here.
     */

    // TODO
    //
    // https://bitbucket.org/burtonator/peregrine/issue/184/remove-trigger_gc_after_sort-in-10-ideally
    //
    // I had to push this because we were having trouble running with a real
    // load and not running out of memory. This is just a pragmatic work
    // around. I need to figure out why our direct memory isn't being released
    // correctly.

    public static final boolean TRIGGER_GC_AFTER_SORT = false;

    //TODO a memory overhead of > 2 (vs the theoretical 2) is
    //required.  I'm not sure why this is the case?  We're still trying to
    //track this down.
    private static final int SORT_MEMORY_OVERHEAD_FACTOR = 4;

    private List<File> input = new ArrayList<File>();

    private SortListener listener = null;
    private Config config;
    private Task task = null;
    private Partition partition;
    private ShuffleInputReference shuffleInput;

    private List<JobOutput> jobOutput = null;
    private Job job = null;

    private PrefetchReader finalPrefetchReader = null;
    
    private ChunkMerger finalMerger = null;

    private List<SequenceReader> finalReaders = null;

    public ReduceRunner( Config config,
                         Task task,
                         Partition partition,
                         SortListener listener,
                         ShuffleInputReference shuffleInput,
                         List<JobOutput> jobOutput ) {

        this.config = config;
        this.task = task;
        this.partition = partition;
        this.listener = listener;
        this.shuffleInput = shuffleInput;
        this.jobOutput = jobOutput;
        this.job = task.getJob();

    }

    /**
     * Add a given file to sort. 
     */
    public void add( File in ) {
        this.input.add( in );
    }
    
    public void reduce() throws IOException {

        // The input list should first be sorted so that we sort by the order of
        // the shuffle files and not an arbitrary order.
        Collections.sort( input );

        int pass = 0;
        
        String sort_dir = getTargetDir( pass );

        // on the first pass we're going to sort and use shuffle input...

        List<SequenceReader> readers = sort( input, sort_dir );
        
        while( true ) {

            log.info( "Working with %,d readers now." , readers.size() );

            ++pass;

            try {
                
                if ( readers.size() < config.getShuffleSegmentMergeParallelism() ) {

                    finalMerge( readers, pass );
                    break;

                } else {
                    readers = interMerge( readers, pass );
                }

            } finally {
                //TODO: should we purge EVEN when we fail?  
                purge( pass - 1 );
            }
                
        }

        cleanup();

    }

    /**
     * Purge shuffle data for a given pass.
     */
    private void purge( int pass ) throws IOException {

        String dir = getTargetDir( pass );

        log.info( "Purging directory %s for pass %,d", dir, pass );
        
        Files.remove( dir );

    }
    
    private void cleanup() throws IOException {

        // Now cleanup after ourselves.  See if the temporary directories exists
        // and if so purge them.
        
        for( int i = 0; i < Integer.MAX_VALUE; ++i ) {

            String path = getTargetDir( i );

            File dir = new File( path );
            if ( dir.exists() ) {
                Files.remove( dir );
            } else {
                break;
            }
            
        }

    }

    @Override
    public void close() throws IOException {

        Closer closer = new Closer( finalPrefetchReader, finalMerger );

        if ( finalReaders != null ) {
            for( SequenceReader reader : finalReaders ) {
                closer.add( reader );
            }
        }

        closer.close();

    }
    
    /**
     * Do the final merge including writing to listener when we are finished.
     */
    public void finalMerge( List<SequenceReader> readers, int pass ) throws IOException {

        String message = String.format( "Merging on final merge with %,d readers (strategy=finalMerge, pass=%,d)", readers.size(), pass );
        log.info( message );

        Duration duration = new Duration();

        SystemProfiler profiler = config.getSystemProfiler();

        finalPrefetchReader = createPrefetchReader( readers );

        finalReaders = readers;
        finalMerger = new ChunkMerger( task, listener, partition, readers, jobOutput, job.getComparatorInstance() );

        finalMerger.merge();

        log.info( "Merged with profiler rate: \n%s", profiler.rate() );

        log.info( "%s (DONE) duration=%s", message, duration );

    }

    /**
     *
     * Do an intermediate merge writing to a mp directory.
     */
    public List<SequenceReader> interMerge( List<SequenceReader> readers, int pass )
        throws IOException {

        try {

            String target_path = getTargetPath( pass );

            // chunk readers pending merge.
            List<SequenceReader> pending = new ArrayList<SequenceReader>();
            pending.addAll( readers );

            List<SequenceReader> result = new ArrayList<SequenceReader>();

            int id = 0;

            while( pending.size() != 0 ) {

                String path = String.format( "%s/merged-%s.tmp" , target_path, id++ );

                List<SequenceReader> work = new ArrayList<SequenceReader>();

                // move readers from pending into work until work is full .
                while( work.size() < config.getShuffleSegmentMergeParallelism() && pending.size() > 0 ) {
                    work.add( pending.remove( 0 ) );
                }

                Duration duration = new Duration();

                String message = String.format( "Merging %,d work readers into %s on intermediate pass %,d (strategy=interMerge)",
                                                work.size(), path, pass );

                log.info( message );

                PrefetchReader prefetchReader = null;

                ChunkMerger merger = null;

                final DefaultPartitionWriter writer = newInterChunkWriter( path );

                try {

                    SystemProfiler profiler = config.getSystemProfiler();

                    prefetchReader = createPrefetchReader( work );

                    merger = new ChunkMerger( task, null, partition, work, null, job.getComparatorInstance() );

                    // when the cache is exhausted we first have to flush it to disk.
                    prefetchReader.setListener( new PrefetchReaderListener() {

                            public void onCacheExhausted() {

                                try {
                                    log.info( "Writing %s to disk with flush()." , writer );
                                    writer.flush();
                                } catch ( IOException e ) {
                                    // this should NOT happen because we are only
                                    // using a MappedByteBuffer here which shouldn't
                                    // throw an exception but we need this interface
                                    // to throw an exception because it could be
                                    // doing other types of IO like networked IO
                                    // which may in fact have an exception.
                                    throw new RuntimeException( e );
                                }

                            }

                        } );

                    merger.merge( writer );

                    log.info( "Merged with profiler rate: \n%s", profiler.rate() );

                } finally {

                    new Closer( prefetchReader, writer, merger ).close();

                    // log how long this interMerge took.

                    log.info( "%s (DONE) duration=%s", message, duration );

                }

                // we should only do this AFTER we have closed out the merger and prefetchReader
                result.add( newInterChunkReader( path ) );

            }

            return result;

        } finally {
            new Closer( readers ).close();
        }

    }

    protected PrefetchReader createPrefetchReader( List<SequenceReader> readers ) throws IOException {
        
        List<MappedFileReader> mappedFiles = new ArrayList();

        for( SequenceReader reader : readers ) {

            if ( reader instanceof DefaultChunkReader ) {

                DefaultChunkReader defaultChunkReader = (DefaultChunkReader) reader;
                mappedFiles.add( defaultChunkReader.getMappedFile() );

            } else if ( reader instanceof LocalPartitionReader ) {

                LocalPartitionReader localPartitionReader = (LocalPartitionReader)reader;
                
                List<DefaultChunkReader> defaultChunkReaders = localPartitionReader.getDefaultChunkReaders();

                for( DefaultChunkReader defaultChunkReader : defaultChunkReaders ) {
                    mappedFiles.add( defaultChunkReader.getMappedFile() );
                }

            } else {
                throw new IOException( "Unknown reader type: " + reader.getClass().getName() );
            }

        }

        PrefetchReader prefetchReader = new PrefetchReader( config, mappedFiles );
        prefetchReader.setEnableLog( true );
        
        return prefetchReader;

    }

    protected SequenceReader newInterChunkReader( String path ) throws IOException {

        return new LocalPartitionReader( config, partition, path );
        
    }
    
    protected DefaultPartitionWriter newInterChunkWriter( String path ) throws IOException {

        boolean append = false;

        // we set autoSync to false for now so that pages don't get
        // automatically sent do disk.
        boolean autoSync = false;

        List<Host> hosts = new ArrayList<Host>() {{
            add( config.getHost() );
        }};

        DefaultPartitionWriter result = new DefaultPartitionWriter( config, partition, path );
        result.setAutoSync( autoSync );
        result.init();

        return result;

    }

    protected String getTargetPath( int pass ) {

        return String.format( "/tmp/%s.%s" , shuffleInput.getName(), pass );
        
    }
    
    protected String getTargetDir( int pass ) {

        return config.getPath( partition, getTargetPath( pass ) );

    }

    /**
     * Sort a given set of input files and write the results to the output
     * directory.
     */
    public List<SequenceReader> sort( List<File> input, String target_dir ) throws IOException {

        SystemProfiler profiler = config.getSystemProfiler();

        List<SequenceReader> sorted = new ArrayList<SequenceReader>();

        // keeps track of the sorting pass we are on.
        int sortPass = 0;

        Duration sortDuration = new Duration();

        // make the parent dir for holding sort files.
        Files.mkdirs( target_dir );
        
        log.info( "Going to sort %,d files for %s", input.size(), partition );

        List<File> pending = new ArrayList<File>();
        pending.addAll( input );

        Iterator<File> pendingIterator = pending.iterator();

        Map<File,Integer> counts = new HashMap<File,Integer>();

        // TODO: to make this code more readable, first sort these into sort
        // units of work and then process those units of work. Right now we
        // build the units of work in this function and it's confusing.
        
        while( pendingIterator.hasNext() ) {
        	
        	List<ChunkReader> work = new ArrayList<ChunkReader>();
        	long workCapacity = 0;
            
            //factor in the overhead of the key lookup before we sort.
            //We will have to create the shuffle input readers HERE and then
            //pass them INTO the chunk sorter.  I also factor in the
            //amount of data IN this partition and not the ENTIRE file size.
        	while( pendingIterator.hasNext() ) {
        		
        		task.assertActiveJob();
        		
        		File current = pendingIterator.next();
                String path = current.getPath();                
                
                ShuffleInputReader reader = null;
                ShuffleHeader header = null;

                try {

                    reader = new ShuffleInputReader( config, path, partition );
                    header = reader.getHeader( partition );

                    counts.put( current , header.count );
                    
                } finally {
                    new Closer( reader ).close();
                }

        		workCapacity += header.length;

                long capacityViaRawDataLength = current.length() / config.getConcurrency();

                long capacityViaRawKeyStorage = SortLookup.computeCapacity( header.count, header.length );

                long computedCapacity = Math.max( capacityViaRawDataLength, capacityViaRawKeyStorage );

                log.info( "Computed capacity for %s is %,d.", path, computedCapacity );

                workCapacity += computedCapacity * SORT_MEMORY_OVERHEAD_FACTOR;

        		if ( workCapacity > config.getSortBufferSize() ) {
        			pendingIterator = pending.iterator();
        			break;        			
        		}

                // it doesn't make sense to sort files with no records.

                work.add( new ShuffleInputChunkReader( config, partition, path ) );

                pendingIterator.remove();
        		
        	}

            if ( work.size() == 0 && pending.size() > 0 ) {
                throw new IOException( "Attempt to sort with no work.  sortBufferSize may be too small." );
            }

            String path = String.format( "%s/sorted-%s.tmp" , target_dir, sortPass++ );
            File out    = new File( path );
            
            log.info( "Writing temporary sort file %s", path );

            Duration duration = new Duration();

            int percRemaining = Integers.perc( pending.size(), input.size() );

            String message = String.format( "Going to sort %,d files requiring %s of memory (sortPass=%,d, perc remaining: %s, JVM reserved memory=%,d, JVM max direct memory=%,d)",
                                            work.size(), Longs.formatBytes( workCapacity ), sortPass, percRemaining, JVMReservedMemory.getReservedMemory(), JVMReservedMemory.getMaxMemory() );

            log.info( message );

            ChunkSorter sorter = new ChunkSorter( config , partition, job, job.getComparatorInstance() );

            SequenceReader result = sorter.sort( work, out, jobOutput );

            log.info( "%s DONE (duration=%s)", message, duration );

            // this is necessary as it helped GC with JDK 1.6
            sorter = null;

            if ( TRIGGER_GC_AFTER_SORT ) {
                System.gc();
            }

            if ( result != null )
                sorted.add( result );

        }

        log.info( "Sorted %,d files for %s with %,d passes (duration=%s)", sorted.size(), partition, sortPass, sortDuration );
        
        log.info( "Sorted with profiler rate: \n%s", profiler.rate() );

        return sorted;

    }

}

