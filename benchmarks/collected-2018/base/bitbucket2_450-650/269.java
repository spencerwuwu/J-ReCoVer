// https://searchcode.com/api/result/61250516/

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
package peregrine.shuffle;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import peregrine.*;
import peregrine.util.*;
import peregrine.util.netty.*;
import peregrine.config.*;
import peregrine.io.chunk.*;

import org.jboss.netty.buffer.*;

import com.spinn3r.log5j.Logger;

/**
 * Reads packets from a shuffle (usually in 16k blocks) and then splits these
 * packets into key/value pairs and implements 
 * 
 */
public class ShuffleInputChunkReader implements ChunkReader {

    public static int QUEUE_CAPACITY = 100;

    private static PrefetchReaderManager prefetchReaderManager
        = new PrefetchReaderManager();

    private SimpleBlockingQueueWithFailure<ShufflePacket,IOException> queue = null;

    private PrefetchReader prefetcher = null;
    
    private Partition partition;
    
    private String path;

    /**
     * The current shuffle packet.
     */
    private ShufflePacket pack = null;
    
    /**
     * Our current position in the key/value stream of items for this partition.
     */
    private Index partition_idx;;

    /**
     * The index of packet reads.
     */
    private Index packet_idx;
    
    private int keyOffset;
    private int key_length;

    private int value_offset;
    private int value_length;

    private VarintReader varintReader;

    private boolean closed = false;

    /**
     * The header for this partition.
     */
    private ShuffleHeader header = null;
    
    public ShuffleInputChunkReader( Config config, Partition partition, String path )
        throws IOException {

        this.partition = partition;
        this.path = path;
        
        prefetcher = prefetchReaderManager.getInstance( config, path );

        // get the path that we should be working with.
        queue = prefetcher.lookup.get( partition );

        if ( queue == null )
            throw new IOException( "Queue is not defined for partition: " + partition );
        
        header = prefetcher.reader.getHeader( partition );

        if ( header == null ) {
            throw new IOException( "Unable to find header for partition: " + partition );
        }

        partition_idx  = new Index( header.count );
        packet_idx     = new Index( header.nr_packets );
        
    }

    public ShufflePacket getShufflePacket() {
        return pack;
    }

    public ShuffleHeader getShuffleHeader() {
        return header;
    }
    
    @Override
    public boolean hasNext() throws IOException {

        assertPrefetchReaderNotFailed();

        boolean result = partition_idx.hasNext();

        if ( result == false )
            prefetcher.finished( partition );

        return result;

    }
    
    @Override
    public void next() throws IOException {

        assertPrefetchReaderNotFailed();

        while( true ) {

            if ( pack != null && pack.data.readerIndex() < pack.data.capacity() - 1 ) {
                
                this.key_length     = varintReader.read();
                this.keyOffset      = pack.data.readerIndex();

                pack.data.readerIndex( pack.data.readerIndex() + key_length );
                
                this.value_length   = varintReader.read();
                this.value_offset   = pack.data.readerIndex();

                pack.data.readerIndex( pack.data.readerIndex() + value_length ); 

                partition_idx.next();
                
                return;
                
            } else if ( nextShufflePacket() ) {

                // we need to read the next ... 
                continue;

            } else {
                return;
            }

        }

    }

    private void assertPrefetchReaderNotFailed() throws IOException {

        Throwable failure = prefetcher.failure.peek();
        
        if ( failure != null ) {

            if ( failure instanceof IOException )
                throw (IOException) failure;

            throw new IOException( failure );

        }

    }
    
    private boolean nextShufflePacket() throws IOException {
        
        if ( packet_idx.hasNext() ) {
            
            pack = queue.take();
            
            varintReader  = new VarintReader( pack.data );
            pack.data.readerIndex( 0 );

            packet_idx.next();
            
            return true;
            
        } else {
            return false;
        }

    }

    public ChannelBuffer getBuffer() {
        return prefetcher.reader.getBuffer();
    }

    public StreamReader getStreamReader() {

        ChannelBuffer buffer = prefetcher.reader.getBuffer();
        buffer = buffer.slice( 0, buffer.writerIndex() );

        return new StreamReader( buffer );
    }

    /**
     * Get the key offset for external readers.
     */
    @Override
    public int keyOffset() {
        return getShufflePacket().getOffset() + keyOffset;
    }

    @Override
    public StructReader key() throws IOException {
        return readBytes( keyOffset, key_length );
        
    }

    @Override
    public StructReader value() throws IOException {
        return readBytes( value_offset, value_length );
    }

    public StructReader readBytes( int offset, int length ) throws IOException {

        return new StructReader( pack.data.slice( offset, length ) );

    }

    public int size() {
        return header.count;
    }

    @Override
    public void close() {

        if ( closed )
            return;
        
        prefetcher.closedPartitonQueue.put( partition );

        closed = true;
    }
    
    @Override
    public String toString() {
        return String.format( "%s:%s:%s" , getClass().getName(), path, partition );
    }

    /**
     * Index for moving forward over items.
     */
    static class Index {

        protected int idx = 0;
        protected int max;
        
        public Index( int max ) {
            this.max = max;
        }
        
        public boolean hasNext() {
            return idx < max;
        }

        public void next() {
            ++idx;
        }

        public String toString() {
            return String.format( "idx: %s, max: %s", idx, max );
        }
        
    }
    
    static class PrefetchReader implements Callable {

        private static final Logger log = Logger.getLogger();

        private static ThreadFactory threadFactory = new DefaultThreadFactory( PrefetchReader.class );

        protected SimpleBlockingQueue<Throwable> failure = new SimpleBlockingQueue();

        protected ShuffleInputReader reader = null;

        private String path;

        private PrefetchReaderManager manager = null;

        private Map<Partition,AtomicInteger> packetsReadPerPartition = new HashMap();

        public Map<Partition,SimpleBlockingQueueWithFailure<ShufflePacket,IOException>> lookup = new HashMap();

        private Map<Partition,SimpleBlockingQueue<Boolean>> finished = new ConcurrentHashMap();

        /**
         * Used so that readers can signal when they are complete.
         */
        protected SimpleBlockingQueue<Partition> closedPartitonQueue = new SimpleBlockingQueue();

        protected ExecutorService executor =
            Executors.newCachedThreadPool( threadFactory );

        public PrefetchReader( PrefetchReaderManager manager, Config config, String path )
            throws IOException {

            this.manager = manager;
            this.path = path;

            // get the top priority replicas to reduce over.
            List<Replica> replicas = config.getMembership().getReplicasByPriority( config.getHost() );

            log.info( "Working with replicas %s for blocking queue on host %s", replicas, config.getHost() );
            
            List<Partition> partitions = new ArrayList();
            
            for( Replica replica : replicas ) {

                Partition part = replica.getPartition(); 
                
                lookup.put( part, new SimpleBlockingQueueWithFailure( QUEUE_CAPACITY ) );
                finished.put( part, new SimpleBlockingQueue( 1 ) );
                
                packetsReadPerPartition.put( part, new AtomicInteger() );
                partitions.add( part );
                
            }
            
            // now open the shuffle file and read in the shuffle packets adding
            // them to the right queues.

            this.reader = new ShuffleInputReader( config, path, partitions );

        }

        /**
         * Called so that partitions that are read can note when they are finished.
         */
        public void finished( Partition partition ) {
            finished.get( partition ).put( Boolean.TRUE );
        }
        
        public Object call() throws IOException {

            try {

                try {
                    
                    log.info( "Reading from %s ...", path );

                    int count = 0;

                    while( reader.hasNext() ) {
                        
                        ShufflePacket pack = reader.next();

                        Partition part = new Partition( pack.to_partition ); 
                        
                        packetsReadPerPartition.get( part ).getAndIncrement();
                            
                        lookup.get( part ).put( pack );

                        ++count;
                        
                    }

                    // FIXME: I don't think we no longer need the 'finished' code
                    // because we now have the close() code

                    // make sure all partitions are finished reading.
                    for ( SimpleBlockingQueue _finished : finished.values() ) {
                        _finished.take();
                    }

                    // not only finished pulling out all packets but actually close()d 
                    for( int i = 0; i < lookup.keySet().size(); ++i ) {
                        closedPartitonQueue.take();
                    }

                    log.info( "Reading from %s ...done (read %,d packets as %s)", path, count, packetsReadPerPartition );

                } finally {
                    
                    reader.close();

                }

            } catch ( Throwable t ) {

                // NOTE: it's important to catch Throwable becuase it could be
                // an OutOfMemoryError or any other type of throwable and this
                // needs to be accounted for.
                
                log.error( "Unable to read from: " + path, t );
                
                // note the exception so callers can also fail.
                failure.put( t );

                IOException cause = null;

                if ( t instanceof IOException ) {
                    cause = (IOException)t;
                } else {
                    cause = new IOException( t );
                }

                raise( cause );
                
                throw cause;
                
            } finally {

                log.info( "Leaving thread for %s", path );

                // remove thyself so that next time around there isn't a reference
                // to this path and a new reader will be created.
                manager.reset( path );

            }
                
            return null;
            
        }

        /**
         * Raise the given exception to all callers so that they then fail to
         * execute as well.
         */
        private void raise( IOException cause ) {

            for( SimpleBlockingQueueWithFailure<ShufflePacket,IOException> current : lookup.values() ) {
                current.raise( cause );
            }
            
        }
        
    }

    static class PrefetchReaderManager {

        private static final Logger log = Logger.getLogger();

        static ConcurrentHashMap<String,PrefetchReader> instances = new ConcurrentHashMap();

        public void reset( String path ) {

            synchronized( instances ) {

                // FIXME: right now this means that we startup 1 thread per
                // chunk which is not super efficient... 
                PrefetchReader reader = instances.remove( path );
                reader.executor.shutdown();
                
            }
            
        }
        
        public PrefetchReader getInstance( Config config, String path )
            throws IOException {

            PrefetchReader result;

            result = instances.get( path );

            if ( result == null ) {

                // double check idiom.
                synchronized( instances ) {

                    result = instances.get( path );
                    
                    if ( result == null ) {

                        log.debug( "Creating new prefetch reader for path: %s", path );
                        
                        result = new PrefetchReader( this, config, path );
                        instances.putIfAbsent( path, result );
                        result = instances.get( path );

                        result.executor.submit( result );

                    } 

                }
                
            } 

            return result;
            
        }

    }
    
}

