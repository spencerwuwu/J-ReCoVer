// https://searchcode.com/api/result/125476962/

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
import org.jboss.netty.buffer.ChannelBuffers;
import peregrine.StructWriter;
import peregrine.config.Config;
import peregrine.config.Partition;
import peregrine.config.Replica;
import peregrine.io.util.Closer;
import peregrine.io.util.Files;
import peregrine.os.ByteBufferCloser;
import peregrine.os.MappedFileWriter;
import peregrine.util.Integers;
import peregrine.util.SimpleBlockingQueue;
import peregrine.util.netty.ChannelBufferWritable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Write shuffle output to disk.
 * 
 */
public class ShuffleOutputWriter implements Closeable {

    public static final int LOOKUP_HEADER_SIZE = Integers.LENGTH * 5;
    public static final int PACKET_HEADER_SIZE = Integers.LENGTH * 4;

    private static final Logger log = Logger.getLogger();

    /**
     * Magic number for this file.  Right now it is 'PSO1' which stands for
     * Peregrine.Reduce Output version 1.
     */
    public static final byte[] MAGIC =
        new byte[] { (byte)'P', (byte)'S', (byte)'O', (byte)'1' };

    // NOTE: in the future if we moved to a single threaded netty implementation
    // we won't need this but for now if multiple threads are writing to the
    // shuffler we can end up with corruption...
    
    private SimpleBlockingQueue<ShufflePacket> index = new SimpleBlockingQueue();

    /**
     * Path to store this output buffer once closed.
     */
    private String path;

    private AtomicLong length = new AtomicLong();

    private Config config;
    
    private ChannelBufferWritable output;

    private Closer closer = new Closer();

    private boolean closed = false;
    
    public ShuffleOutputWriter( Config config, String path ) {

        this.path = path;
        this.config = config;

    }
    
    public void accept( int from_partition,
                        int from_chunk,
                        int to_partition,
                        int count,
                        ChannelBuffer data ) throws IOException {

        if ( closed )
            throw new IOException( "writer is closed" );

        if ( count < 0 )
            throw new IOException( "invalid count: " + count );
        
        ShufflePacket pack = new ShufflePacket( from_partition, from_chunk, to_partition, -1, count, data );
        
        this.length.getAndAdd( data.capacity() );
        
        index.put( pack );

    }

    public boolean hasCapacity() {

        // we must use /2 here becauuse we keep two copies of writers while we
        // are accepting data from remote
        return length.get() < (config.getShuffleBufferSize() / 2);

    }

    private Map<Integer,ShuffleOutputPartition> buildLookup() throws IOException {

        // we are done working with this buffer.  serialize it to disk now and
        // close it out.

        List<Partition> partitions = config.getMembership().getPartitions( config.getHost() );

        log.debug( "Going to write shuffle for %s to %s", partitions , path );
        
        if ( partitions == null || partitions.size() == 0 )
            throw new IOException( "No partitions defined for: " + config.getHost() );

        Map<Integer,ShuffleOutputPartition> lookup = new HashMap();

        // startup the lookup with one ArrayList per partition.
        for( Partition part : partitions ) {
            lookup.put( part.getId(), new ShuffleOutputPartition() );
        }

        Iterator<ShufflePacket> it = index.iterator();
        
        while( it.hasNext() ) {

            ShufflePacket current = it.next();
            
            if ( current == null ) {
                log.error( "Skipping null packet." );
                continue;
            }
            
            ShuffleOutputPartition shuffleOutputPartition = lookup.get( current.to_partition );

            if ( shuffleOutputPartition == null )
                throw new IOException( "No locally defined partition for: " + current.to_partition );

            if ( current.count < 0 )
                throw new IOException( "count < 0: " + current.count );

            shuffleOutputPartition.count += current.count;
            
            shuffleOutputPartition.packets.add( current );
            
        }

        return lookup;
        
    }
    
    @Override
    public void close() throws IOException {

        try {

            if ( closed )
                return;
            
            closed = true;

            Map<Integer,ShuffleOutputPartition> lookup = buildLookup();

            log.debug( "Going write output buffer with %,d entries.", lookup.size() );

            File file = new File( path );
            
            // make sure the parent directory exists first.
            Files.mkdirs( file.getParent() );
            
            this.output = new MappedFileWriter( config, file );
            
            output.write( ChannelBuffers.wrappedBuffer( MAGIC ) );
            
            output.write( new StructWriter( Integers.LENGTH )
                          .writeInt( lookup.size() )
                          .getChannelBuffer() );
            
            // the offset in this chunk to start reading the data from this
            // partition and chunk.
            
            int offset =
                MAGIC.length + Integers.LENGTH + (lookup.size() * LOOKUP_HEADER_SIZE);

            // TODO: these should be stored in the primary order that they will be
            // reduce by priority on this box.

            // *** STEP 0 .. compute the order that we should write in
            List<Replica> replicas = config.getMembership().getReplicas( config.getHost() );

            // *** STEP 1 .. write the header information

            for( Replica replica : replicas ) {

                int part = replica.getPartition().getId();
                
                ShuffleOutputPartition shuffleOutputPartition = lookup.get( part );

                // the length of ALL the data for this partition.
                int length = 0;

                for( ShufflePacket pack : shuffleOutputPartition.packets ) {
                    
                    length += PACKET_HEADER_SIZE;
                    length += pack.data.writerIndex();
                    
                }

                int nr_packets = shuffleOutputPartition.packets.size();

                //TODO: make sure ALL of these are acceptable.
                if ( shuffleOutputPartition.count < 0 )
                    throw new IOException( "Header corrupted: count < 0" );

                int count = shuffleOutputPartition.count;

                output.write( new StructWriter( LOOKUP_HEADER_SIZE )
                              .writeInt( part )
                              .writeInt( offset )
                              .writeInt( nr_packets )
                              .writeInt( count )
                              .writeInt( length )
                              .getChannelBuffer() );
                
                offset += length;
                    
            }

            // *** STEP 2 .. write the actual data packets
            
            for( Replica replica : replicas ) {

                int part = replica.getPartition().getId();

                ShuffleOutputPartition shuffleOutputPartition = lookup.get( part );

                for( ShufflePacket pack : shuffleOutputPartition.packets ) {

                    output.write( new StructWriter( PACKET_HEADER_SIZE )
                                  .writeInt( pack.from_partition )
                                  .writeInt( pack.from_chunk )
                                  .writeInt( pack.to_partition )
                                  .writeInt( pack.data.writerIndex() )
                                  .getChannelBuffer() );
                    
                    output.write( pack.data );

                    // release the bytes after we are done with them.
                    
                    new ByteBufferCloser( pack.data ).close();
                    
                }
                
            }

            index = null; // This is required for the JVM to more aggresively
                          // recover memory.  I did extensive testing with this
                          // and without setting index to null the JVM does not
                          // recover memory and it eventually leaks.  I don't
                          // think anything could be holding a reference to this
                          // though but this is a good pragmatic defense and
                          // solved the problem.

        } catch ( Throwable t ) {

            IOException e = null;

            if ( t instanceof IOException ) {
                e = (IOException) t;
            } else {
                e = new IOException( t );
            }

            log.error( "Unable to close: " , t );

            throw e;

        } finally {

            closer.add( output );
            
            closer.close();
            
        }
        
    }

    public String toString() {
        return String.format( "%s:%s", getClass().getSimpleName() , path );
    }
    
}

class ShuffleOutputPartition {

    public int count = 0;

    public List<ShufflePacket> packets = new ArrayList();
    
}

