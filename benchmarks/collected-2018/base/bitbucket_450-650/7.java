// https://searchcode.com/api/result/61250538/

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
package peregrine.reduce;

import java.io.*;
import java.util.*;

import peregrine.config.*;
import peregrine.io.*;
import peregrine.io.chunk.*;
import peregrine.io.driver.shuffle.*;
import peregrine.io.partition.*;
import peregrine.io.util.*;
import peregrine.reduce.sorter.*;
import peregrine.reduce.merger.*;
import peregrine.task.*;
import peregrine.util.netty.*;
import peregrine.os.*;
import peregrine.shuffle.*;
import peregrine.sysstat.*;

import com.spinn3r.log5j.Logger;

/**
 * Run a reduce over the the given partition.  This handles intermerging, memory
 * allocation of sorting, etc.
 */
public class ReduceRunner {

    private static final Logger log = Logger.getLogger();

    private List<File> input = new ArrayList();

    private SortListener listener = null;
    private Config config;
    private Task task = null;
    private Partition partition;
    private ShuffleInputReference shuffleInput;

    private List<JobOutput> jobOutput = null;

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

    }

    /**
     * Add a given file to sort. 
     */
    public void add( File in ) {
        this.input.add( in );
    }
    
    public void reduce() throws IOException {

        // The input list should first be sorted so that we sort by the order of
        // the shuffle files and not an arbitrary order
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
    private void purge( int pass ) {

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
    
    /**
     * Do the final merge including writing to listener when we are finished.
     */
    public void finalMerge( List<SequenceReader> readers, int pass ) throws IOException {

        log.info( "Merging on final merge with %,d readers (strategy=finalMerge, pass=%,d)", readers.size(), pass );
        
        PrefetchReader prefetchReader = null;

        ChunkMerger merger = null;
        
        try {
            
            SystemProfiler profiler = config.getSystemProfiler();

            prefetchReader = createPrefetchReader( readers );

            merger = new ChunkMerger( task, listener, partition, readers, jobOutput );
        
            merger.merge();

            log.info( "Merged with profiler rate: \n%s", profiler.rate() );

        } finally {

            new Closer( prefetchReader, merger ).close();

        }
        
    }

    /**
     * Do an intermediate merge writing to a temp directory.
     */
    public List<SequenceReader> interMerge( List<SequenceReader> readers, int pass )
        throws IOException {

        String target_path = getTargetPath( pass );
        
        // chunk readers pending merge.
        List<SequenceReader> pending = new ArrayList();
        pending.addAll( readers );

        List<SequenceReader> result = new ArrayList();

        int id = 0;
        
        while( pending.size() != 0 ) {

            String path = String.format( "%s/merged-%s.tmp" , target_path, id++ );
            
            List<SequenceReader> work = new ArrayList();

            // move readers from pending into work until work is full .
            while( work.size() < config.getShuffleSegmentMergeParallelism() && pending.size() > 0 ) {
                work.add( pending.remove( 0 ) );
            }

            log.info( "Merging %,d work readers into %s on intermediate pass %,d (strategy=interMerge)",
                      work.size(), path, pass );

            PrefetchReader prefetchReader = null;

            ChunkMerger merger = null;
            
            try { 

                SystemProfiler profiler = config.getSystemProfiler();

                prefetchReader = createPrefetchReader( work );

                merger = new ChunkMerger( task, null, partition, work, jobOutput );

                final DefaultPartitionWriter writer = newInterChunkWriter( path );

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
                new Closer( prefetchReader, merger ).close();
            }

            // we should only do this AFTER we have closed out the merger and prefetchReader
            result.add( newInterChunkReader( path ) );

        }

        return result;

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

        List<Host> hosts = new ArrayList() {{
            add( config.getHost() );
        }};
        
        return new DefaultPartitionWriter( config,
                                           partition,
                                           path,
                                           append,
                                           hosts,
                                           autoSync );

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

        List<SequenceReader> sorted = new ArrayList();

        int id = 0;

        // make the parent dir for holding sort files.
        Files.mkdirs( target_dir );
        
        log.info( "Going to sort() %,d files for %s", input.size(), partition );

        List<File> pending = new ArrayList();
        pending.addAll( input );

        Iterator<File> pendingIterator = pending.iterator();
        
        while( pendingIterator.hasNext() ) {
        	
        	List<ChunkReader> work = new ArrayList();
        	long workCapacity = 0;
            
            //factor in the overhead of the key lookup before we sort.
            //We will have to create the shuffle input readers HERE and then
            //pass them INTO the chunk sorter.  I also factor in the
            //amount of data IN this partition and not the ENTIRE file size.
        	while( pendingIterator.hasNext() ) {
        		
        		task.assertAlive();
        		
        		File current = pendingIterator.next();
                String path = current.getPath();                
                
                ShuffleInputReader reader = null;
                ShuffleHeader header = null;

                try {

                    reader = new ShuffleInputReader( config, path, partition );
                    header = reader.getHeader( partition );

                } finally {
                    new Closer( reader ).close();
                }

        		workCapacity += header.length;
                workCapacity += KeyLookup.computeCapacity( header.count ) * 2;

        		if ( workCapacity > config.getSortBufferSize() ) {
        			pendingIterator = pending.iterator();
        			break;        			
        		}
                
                work.add( new ShuffleInputChunkReader( config, partition, path ) );
                pendingIterator.remove();
        		
        	}
        	
            String path = String.format( "%s/sorted-%s.tmp" , target_dir, id++ );
            File out    = new File( path );
            
            log.info( "Writing temporary sort file %s", path );
            log.info( "Going to sort %,d files requiring %,d bytes of memory", work.size(), workCapacity );
            
            ChunkSorter sorter = new ChunkSorter( config , partition );
            
            SequenceReader result = sorter.sort( work, out, jobOutput );
            
            if ( result != null )
                sorted.add( result );

        }

        log.info( "Sorted %,d files for %s", sorted.size(), partition );
        
        log.info( "Sorted with profiler rate: \n%s", profiler.rate() );

        return sorted;

    }

    /**
     * Sort a given set of input files and write the results to the output
     * directory and return a List of SequenceReaders we can then merge.
     */
    public List<SequenceReader> sort2( List<File> input, String target_dir ) throws IOException {

        SystemProfiler profiler = config.getSystemProfiler();

        List<SequenceReader> sorted = new ArrayList();

        int id = 0;

        // make the parent dir for holding sort files.
        Files.mkdirs( target_dir );
        
        log.info( "Going to sort() %,d files for %s", input.size(), partition );

        // the total number of items we need to sort.
        int total = 0;

        List<ChunkReader> readers = new ArrayList();
        
        for ( File current : input ) {

            String path = current.getPath();                
            
            ShuffleInputReader reader = null;
            ShuffleHeader header = null;

            try {

                reader = new ShuffleInputReader( config, path, partition );
                header = reader.getHeader( partition );
                total += header.count;

            } finally {
                new Closer( reader ).close();
            }

            readers.add( new ShuffleInputChunkReader( config, partition, path ) );

        }

        CompositeChunkReader composite = new CompositeChunkReader( config, readers );

        // the blocks of items we should be sorting.
        List<ChunkReader> blocks = new ArrayList();
        
        return null;
        
    }

}

