// https://searchcode.com/api/result/122323709/

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
package peregrine.shuffle;

import com.spinn3r.log5j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import peregrine.StructReader;
import peregrine.config.Config;
import peregrine.config.Partition;
import peregrine.config.Replica;
import peregrine.io.chunk.ChunkReader;
import peregrine.util.DefaultThreadFactory;
import peregrine.util.SimpleBlockingQueue;
import peregrine.util.SimpleBlockingQueueWithFailure;
import peregrine.util.VarintReader;
import peregrine.util.netty.StreamReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads packets from a shuffle (usually in 16k blocks) and then splits these
 * packets into key/value pairs and implements the ChunkReader interface so that
 * we can read from the data as it is loaded.
 *
 * <p> This system uses a background thread to prefetch all the primary replica
 * packets for use by external readers.  This is done so that we don't have to
 * have two threads each reading from the same file.
 * 
 */
public class ShuffleInputChunkReader implements ChunkReader {

    private static final Logger log = Logger.getLogger();

    public static int QUEUE_CAPACITY = 100;

    private static ShufflePacketReadaheadTaskManager shufflePacketReadaheadTaskManager
        = new ShufflePacketReadaheadTaskManager();

    private SimpleBlockingQueueWithFailure<ShufflePacket,IOException> queue = null;

    private ShufflePacketReadaheadTask readaheadTask = null;
    
    private Partition partition;
    
    private String path;

    private File file;

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

    private boolean closed = false;

    /**
     * The header for this partition.
     */
    private ShuffleHeader header = null;
    
    public ShuffleInputChunkReader( Config config, Partition partition, String path )
        throws IOException {

        this.partition = partition;
        this.path = path;
        this.file = new File( path );
        
        readaheadTask = shufflePacketReadaheadTaskManager.getInstance( config, path );

        // get the path that we should be working with.
        queue = readaheadTask.lookup.get( partition );

        if ( queue == null )
            throw new IOException( "Queue is not defined for partition: " + partition );
        
        header = readaheadTask.reader.getHeader( partition );

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

        assertReadaheadNotFailed();

        boolean result = partition_idx.hasNext();

        if ( result == false )
            readaheadTask.finished( partition );

        return result;

    }
    
    @Override
    public void next() throws IOException {

        assertReadaheadNotFailed();

        while( true ) {

            if ( pack != null && pack.data.readerIndex() < pack.data.capacity() - 1 ) {
                
                this.key_length     = VarintReader.read( pack.data );
                this.keyOffset      = pack.data.readerIndex();

                pack.data.readerIndex( pack.data.readerIndex() + key_length );
                
                this.value_length   = VarintReader.read( pack.data );
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

    private void assertReadaheadNotFailed() throws IOException {

        Throwable failure = readaheadTask.failure.peek();
        
        if ( failure != null ) {

            if ( failure instanceof IOException )
                throw (IOException) failure;

            throw new IOException( failure );

        }

    }
    
    private boolean nextShufflePacket() throws IOException {
        
        if ( packet_idx.hasNext() ) {
            
            pack = queue.take();
            
            pack.data.readerIndex( 0 );

            packet_idx.next();
            
            return true;
            
        } else {
            return false;
        }

    }

    public ChannelBuffer getBuffer() {
        return readaheadTask.reader.getBuffer();
    }

    public StreamReader getStreamReader() {

        ChannelBuffer buffer = readaheadTask.reader.getBuffer();
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
    public int recordOffset() throws IOException {
        throw new IOException( "not implemented" );
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

    public int count() {
        return header.count;
    }

    @Override
    public void close() {

        if ( closed )
            return;
        
        readaheadTask.closedPartitionQueue.put( partition );

        closed = true;
    }
    
    @Override
    public String toString() {
        return String.format( "%s (length=%s)" , path, file.length() );
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
    
    static class ShufflePacketReadaheadTask implements Callable {

        private static final Logger log = Logger.getLogger();

        private static ThreadFactory threadFactory = new DefaultThreadFactory( ShufflePacketReadaheadTask.class );

        protected SimpleBlockingQueue<Throwable> failure = new SimpleBlockingQueue<Throwable>();

        protected ShuffleInputReader reader = null;

        private String path;

        private ShufflePacketReadaheadTaskManager manager = null;

        private Map<Partition,AtomicInteger> packetsReadPerPartition = new HashMap<Partition,AtomicInteger>();

        public Map<Partition,SimpleBlockingQueueWithFailure<ShufflePacket,IOException>> lookup = new HashMap();

        private Map<Partition,SimpleBlockingQueue<Boolean>> finished = new ConcurrentHashMap();

        /**
         * Used so that readers can signal when they are complete.
         */
        protected SimpleBlockingQueue<Partition> closedPartitionQueue = new SimpleBlockingQueue();

        protected ExecutorService executor =
            Executors.newCachedThreadPool( threadFactory );

        public ShufflePacketReadaheadTask(ShufflePacketReadaheadTaskManager manager, Config config, String path)
            throws IOException {

            this.manager = manager;
            this.path = path;

            // get the top priority replicas to reduce over.
            List<Replica> replicas = config.getMembership().getReplicasByPriority( config.getHost() );

            log.debug( "Working with replicas %s for blocking queue on host %s", replicas, config.getHost() );
            
            List<Partition> partitions = new ArrayList<Partition>();
            
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
                    
                    log.debug( "Reading from %s ...", path );

                    int count = 0;

                    while( reader.hasNext() ) {
                        
                        ShufflePacket pack = reader.next();

                        Partition part = new Partition( pack.to_partition ); 
                        
                        packetsReadPerPartition.get( part ).getAndIncrement();
                            
                        lookup.get( part ).put( pack );

                        ++count;
                        
                    }

                    // TODO: I don't think we no longer need the 'finished' code
                    // because we now have the close() code

                    // make sure all partitions are finished reading.
                    for ( SimpleBlockingQueue _finished : finished.values() ) {
                        _finished.take();
                    }

                    // not only finished pulling out all packets but actually close()d 
                    for( int i = 0; i < lookup.keySet().size(); ++i ) {
                        closedPartitionQueue.take();
                    }

                    log.debug( "Reading from %s ...done (read %,d packets as %s)", path, count, packetsReadPerPartition );

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

                log.debug( "Leaving thread for %s", path );

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

    static class ShufflePacketReadaheadTaskManager {

        private static final Logger log = Logger.getLogger();

        static ConcurrentHashMap<String,ShufflePacketReadaheadTask> instances = new ConcurrentHashMap();

        public void reset( String path ) {

            synchronized( instances ) {

                //TODO: https://bitbucket.org/burtonator/peregrine/issue/185/shuffleinputchunkreader-has-one-thread-per

                // Right now this means that we startup 1 thread per chunk which
                // is not super efficient. I need to look into why this is the
                // case. It would be nice to only start N so that we aren't
                // thrashing the disk. Also are the threads NAMED after the
                // files?
                
                ShufflePacketReadaheadTask reader = instances.remove( path );
                reader.executor.shutdown();
                
            }
            
        }
        
        public ShufflePacketReadaheadTask getInstance( Config config, String path )
            throws IOException {

            ShufflePacketReadaheadTask result;

            result = instances.get( path );

            if ( result == null ) {

                // double check idiom.
                synchronized( instances ) {

                    result = instances.get( path );
                    
                    if ( result == null ) {

                        log.debug( "Creating new readahead for path: %s", path );
                        
                        result = new ShufflePacketReadaheadTask( this, config, path );
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

