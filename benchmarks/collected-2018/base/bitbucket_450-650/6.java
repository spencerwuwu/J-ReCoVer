// https://searchcode.com/api/result/122323621/

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
package peregrine.sort;

import com.spinn3r.log5j.Logger;
import peregrine.*;
import peregrine.config.Config;
import peregrine.config.Partition;
import peregrine.io.JobOutput;
import peregrine.util.Hex;
import peregrine.util.Integers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * Map reduce job which computes the partition routing table for sorting data.
 */
public class ComputePartitionTableJob {

    private static final Logger log = Logger.getLogger();

    /**
     * The maximum percentage to tolerate in key distribution.  If we have a
     * percentage gap greater than this we abort.
     */
    public static int MAX_DISTRIBUTION_PERC_DELTA = 10;
    
    /**
     * The maximum number of keys to read into memory to compute the training
     * set..
     * 
     * <p> TODO: Technically this should be a function of the number of
     * partitions and data because our accuracy will fail with the total number of
     * partitions we have.
     * TODO: this should ALSO be kept outside of the JVM and allocated from an
     * external buffer.  We could/should probably use something like the sort
     * buffer size which isn't going to be used during reduce jobs.
     */
    public static int MAX_TRAIN_SIZE = 100000;

    /**
     * The point at which we essentially consider that we have no data and don't
     * even bother computing the partition table.
     */
    public static int NO_DATA_THRESHOLD = 1000;
    
    /**
     * <p>
     * The key length for partition boundaries.  Eight (8) bites for the first
     * component and 8 bytes for the second component.
     *
     * <p>
     * The first component is the value we are sorting by.  Eight bytes give us
     * enough room for doubles/longs.
     *
     * <p>
     * The second component is an 8 byte value for the hashcode for that item.
     * This way we have enough bytes to split data even when the sorting column
     * is the same for 2^64 items.
     * 
     * <p>In the future we should consider a way to have custom width sort keys.
     */
    public static final int KEY_LEN = 16;

    public static final StructReader LAST_BOUNDARY  = createByteArray( 127, KEY_LEN );
    
    /**
     * Create a byte array of the given values.
     */
    private static StructReader createByteArray( int val, int len ) {

        byte[] data = new byte[ len ];

        for( int i = 0; i < data.length; ++i ) {
            data[i] = (byte)val;
        }

        return StructReaders.wrap( data );
        
    }

    public static List<StructReader> computePartitionBoundaries( Config config,
                                                                 Partition part,
                                                                 SortComparator comparator,
                                                                 List<StructReader> train ) {

        log.info( "Training set size is: %,d items", train.size() );

        List<StructReader> result = new ArrayList<StructReader>();

        if ( train.size() <= NO_DATA_THRESHOLD ) {

            // It is totally reasonable to want to sort a file with no
            // entries.
            
            log.warn( "No training data for job." );

            return result;
            
        }

        Collections.sort( train, comparator );

        // now down sample the training set so that we can grok what is
        // happening by looking at a smaller number of items.  In practice the
        // partition boundaries won't be off significantly using this technique.

        for( StructReader current : summarize( train, 80 ) ) {
            log.info( "Training data set summary on %s %s", part, format( current ) );
        }
        
        int nr_partitions = config.getMembership().size();

        result = summarize( train, nr_partitions );

        // we have to remove the last partition as it is pointless essentially.

        result.remove( result.size() - 1 );

        // add the max partition boundary.
        result.add( LAST_BOUNDARY );

        for( StructReader current : result ) {
            log.info( "Resulting partition table %s %s", part, format( current ) );
        }

        return result;

    }

    public static String format( StructReader sr ) {
        return String.format( "%22s(%s)", sr.toInteger(), sr.toString() );
    }
    
    /**
     * Summarize the given list by taking a reading every list.size() / count
     * items and return the result.  
     */
    public static List<StructReader> summarize( List<StructReader> list, int count ) {

        int width = list.size() / count;

        int offset = 0;

        List<StructReader> result = new ArrayList();
        
        for( int i = 0; i < count; ++i ) {

            offset += width;
            
            result.add( list.get( offset - 1) );
            
        }

        return result;
        
    }

    protected static SortComparator getSortComparator( Job job ) {

        try {

            Class clazz = job.getParameters().getClass( "sortComparator" );
            return (SortComparator)clazz.newInstance();

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }
    
    public static class Map extends Mapper {

        private static final Logger log = Logger.getLogger();

        /**
         * The training data.  We sort this list and then take trains at
         * partition boundaries.
         */
        private List<StructReader> train = new ArrayList<StructReader>( MAX_TRAIN_SIZE );

        /**
         * The testing data for verifying the training data.
         */
        private List<StructReader> test  = new ArrayList<StructReader>( MAX_TRAIN_SIZE );

        private SortComparator comparator = null;

        private Random rand = new Random();
        
        /**
         * The ID of the current record we are processing.
         */
        private int id = 0;
        
        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );
            comparator = getSortComparator( job );

        }

        @Override
        public void map( StructReader key,
                         StructReader value ) {

            // NOTE: we have to call toByteArray here because we are keeping
            // these values around for a long time and the maps might not be
            // valid when we try to read them later.

            // NOTE: We must use a random key here because we don't have a full
            // range of key data in this sample since we only read one chunk to
            // train and the keys are sorted.  If we didn't use a random value
            // we would have just a subset of the keys and our distribution
            // wouldn't be perfect for data sets with a non-linear distribution
            // of values (IE where everything is zero and key value range
            // becomes important).

            // FIXME: this breaks our ability to sort variable length keys.

            key   = StructReaders.wrap( rand.nextLong() );
            value = StructReaders.wrap( value.toByteArray() );

            if ( ++id % 2 == 0 ) {
                addTrain( key, value );
            } else {
                addTest( key, value );
            }

        }

        /**
         * Add a data point to the training set. 
         */
        protected void addTrain( StructReader key, StructReader value ) {
            addPoint( key, value, train );
        }

        /**
         * Add a data point to the testing set. 
         */
        protected void addTest( StructReader key, StructReader value ) {
            addPoint( key, value, test );
        }

        /**
         * Add a sort key data point to the given list.
         */
        protected void addPoint( StructReader key,
                                 StructReader value,
                                 List<StructReader> list ) {

            // we are done sampling.
            if ( list.size() > MAX_TRAIN_SIZE )
                return;

            list.add( comparator.getSortKey( key , value ) );

        }

        @Override
        public void close() throws IOException {

            long nr_partitions = config.getMembership().size();

            Collections.sort( train, comparator );

            if ( train.size() <= NO_DATA_THRESHOLD  ) {

                log.warn( "Insufficient number of values to sort: %s", train.size() );

                // write all data to the last boundary position.  It doesn't
                // matter where we write these since there are no records being
                // written.
                for( long i = 0; i < nr_partitions; ++i ) {
                    StructReader key = StructReaders.wrap( i );
                    emit( key, LAST_BOUNDARY );
                }

                return;

            }

            // If we have too few training data points we have to create
            // synthetic data and then generate new training data and then use
            // the remaining eight(8) bytes to distribute values among the
            // cluster.  The problem is that since we only read one chunk we're
            // not getting the full key space but we can just create synthetic
            // keys anyway.  The advantage to this approach is that we can
            // re-use a lot of existing code.

            // ALWAYS making synthetic partitions when we have less than
            // MAX_TRAIN_SIZE ... this train size should be our target so that
            // we have fined grained partitioning of the data.
            
            if ( train.size() < MAX_TRAIN_SIZE ) {
                log.warn( "Training data set it small so distribution may be slightly skewed: %s", train.size() );
            }

            log.info( "Working with %,d training set entries. " , train.size() );

            //at this point we should have all the trains, sort then and then
            //determine partition boundaries.

            List<StructReader> boundaries = computePartitionBoundaries( config, getPartition(), comparator, train );

            //Test if we have >= MAX_TRAIN_SIZE records in the test data as
            //there isn't enough values for a skew to even matter and our test()
            //algorithm will probably be incorrect.
            
            if ( test.size() >= MAX_TRAIN_SIZE ) {
                test( boundaries );
            } else {
                log.warn( "Testing data size is small. Skipping test since it would be inaccurate." );
            }
            
            long partition_id = 0;

            for( StructReader boundary : boundaries ) {
                StructReader key = StructReaders.wrap( partition_id );
                emit( key, boundary );
                ++partition_id;
            }
            
        }

        private void test( List<StructReader> boundaries ) {

            GlobalSortPartitioner partitioner = new GlobalSortPartitioner();
            partitioner.init( job, boundaries );

            int nr_partitions = config.getMembership().size();

            TreeMap<Partition, Integer> hits = new TreeMap<Partition, Integer>();
            
            for( int i = 0; i < nr_partitions; ++i ) {
                hits.put( new Partition( i ), 0 );
            }

            // now partition around all the testing points.
            for( StructReader sr : test ) {

                Partition part = partitioner.partition( sr );
                hits.put( part, hits.get( part ) + 1 );
                
            }

            //analyze the hits and throw an Exception if they don't look acceptable.
            assertDistribution( toPerc( hits ) );

            log.info( "Test results for %,d testing points and %,d training points: %s",
                      test.size(), train.size(), hits );
            
        }

        private void assertDistribution( java.util.Map<Partition, Integer> perc ) {

            int last = -1;

            for( int p : perc.values() ) {

                if ( last > -1 && Math.abs( last - p ) > MAX_DISTRIBUTION_PERC_DELTA  ) {
                    throw new RuntimeException( "Key distribution is skewed: " + perc );
                }

                last = p;
                
            }
            
        }

        private java.util.Map<Partition, Integer> toPerc( java.util.Map<Partition, Integer> data ) {

            int sum = 0;

            for( int val : data.values() ) {
                sum += val;
            }

            java.util.Map<Partition, Integer> result = new HashMap();

            for( Partition part : data.keySet() ) {
                int perc = Integers.perc( data.get( part ) , sum );
                result.put( part , perc );
            }

            return result;
            
        }
        
        @Override
        public void emit( StructReader key, StructReader value ) {

            log.info( "Going to emit partition table entry: %s=%22s (%s)" , key.slice().readLong(), Hex.encode( value ), value.toInteger() );
            super.emit( key, value );
            
        }
        
    }

    public static class Reduce extends Reducer {

        private static final Logger log = Logger.getLogger();

        private List<StructReader> boundaries = new ArrayList();

        private SortComparator comparator = null;
        
        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );
            comparator = getSortComparator( job );

        }

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            StructReader value = mean( values );
            //StructReader value = median( values );

            log.info( "Going to use final broadcast partition boundary: %s", Hex.encode( value ) );

            boundaries.add( value );

            if ( boundaries.size() == getConfig().getMembership().size() ) {
                emit( key, StructReaders.splice( boundaries ) );
            }
            
        }

        protected StructReader mean( List<StructReader> list ) {

            BigInteger sum = BigInteger.valueOf( 0 );

            for( StructReader current : list ) {
                BigInteger bigInteger = new BigInteger( current.toByteArray() );
                sum = sum.add( bigInteger );
            }

            BigInteger mean = sum.divide( BigInteger.valueOf( list.size() ) );

            StructReader median = median( list );

            // make sure the mean is the same byte width as the median.
            byte[] padded = new byte[ median.length() ];
            byte[] data = mean.toByteArray();

            System.arraycopy( data, 0, padded, padded.length - data.length, data.length );

            return StructReaders.wrap( padded );

        }

        private StructReader median( List<StructReader> values ) {
            Collections.sort( values, comparator );
            return values.get( values.size() / 2 );
        }

    }

}

