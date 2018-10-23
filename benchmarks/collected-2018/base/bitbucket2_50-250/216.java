// https://searchcode.com/api/result/125476546/

/*
 * Copyright 2011-2013 Kevin A. Burton
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

package peregrine.globalsort;

import com.spinn3r.log5j.Logger;
import peregrine.*;
import peregrine.config.Config;
import peregrine.config.Partition;
import peregrine.controller.Controller;
import peregrine.io.ExtractWriter;
import peregrine.io.util.Files;
import peregrine.sort.ComputePartitionTableJob;
import peregrine.sort.SortComparator;
import peregrine.util.Integers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class BaseSortViaMapReduce extends BaseTestWithDaemonBackendAndController {

    private static final Logger log = Logger.getLogger();

    public static final int MAX_DELTA_PERC = 5;
    
    private static String MODE = "all";

    @Override
    public abstract void doTest() throws Exception;

    protected void doTest( SortComparator comparator ) throws Exception {

        Config config = getConfig();

        int nr_partitions = config.getMembership().size();

        ComputePartitionTableJob.MAX_TRAIN_SIZE=10000;
        ComputePartitionTableJob.NO_DATA_THRESHOLD=1000;

        int max = getMaxFromFactor( 20000, ComputePartitionTableJob.MAX_TRAIN_SIZE * 3 * nr_partitions );

        doTest( 0, 2, comparator );
        doTest( max, max, comparator );
        doTest( max, 10000, comparator );
        doTest( max, 1000, comparator );
        doTest( max, 10, comparator );
        doTest( max, 2, comparator );
        doTest( max, 1, comparator );

    }

    protected void doTest( int max, int range, SortComparator comparator ) throws Exception {

        log.info( "Testing with %,d records." , max );

        Config config = getConfig();

        String path = String.format( "/test/%s/test1.in", getClass().getName() );

        ExtractWriter writer = new ExtractWriter( config, path );

        Random r = new Random();

        for( long i = 0; i < max; ++i ) {

            StructReader key = StructReaders.hashcode( i );
            StructReader value = StructReaders.wrap( (long)r.nextInt( range ) );

            writer.write( key, value );

        }

        writer.close();

        String output = String.format( "/test/%s/test1.out", getClass().getName() );

        Controller controller = getController();

        // technically, we can't just use the input data which is written
        // via the ExtractWriter because the keys won't be written in the
        // same order as a mapper since the keys are ordered on disk.  We
        // have to map it like it would be in production.

        Batch batch = new Batch( getClass() );

        batch.map( new Job().setDelegate( Mapper.class )
                            .setInput( path )
                            .setOutput( "shuffle:default" ) );

        batch.reduce( new Job().setDelegate( Reducer.class )
                               .setInput( "shuffle:default" )
                               .setOutput( path ) );

        batch.sort( path, output, comparator.getClass() );

        controller.exec( batch );

        // ********** local work which reads directly from the filesystem to
        // ********** make sure we have correct results

        // now test the distribution of the keys...

        // map from partition to disk usage in bytes
        Map<Partition,Long> usage = new HashMap<Partition, Long>();

        for ( Config c : getConfigs() ) {

            System.out.printf( "host: %s\n", c.getHost() );

            // /tmp/peregrine/fs-11112/localhost/11112/0/

            List<Partition> partitions = c.getMembership().getPartitions( c.getHost() );

            for( Partition part : partitions ) {

                int port = c.getHost().getPort();

                String dir = String.format( "/tmp/peregrine/fs-%s/localhost/%s/%s/%s", port, port, part.getId(), output );

                File file = new File( dir );

                if( ! file.exists() )
                    continue;

                long du = Files.usage( file );

                System.out.printf( "%s=%s\n", file.getPath(), du );

                usage.put( part, du );

            }

        }

        double total = 0;

        for( long val : usage.values() ) {
            total += val;
        }

        Map<Partition,Integer> perc = new HashMap<Partition, Integer>();

        for( Partition part : usage.keySet() ) {

            long du = usage.get( part );

            int p = (int)((du / total) * 100);

            perc.put( part , p );

        }

        System.out.printf( "perc: %s\n", perc );

        long delta = Math.abs( Integers.max( perc.values() ) - Integers.min( perc.values() ) );

        assertTrue( String.format( "invalid partition layout with max=%s, range=%s, perc=%s, delta=%s, MAX_DELTA_PERC=%s",
                                   max, range, perc, delta, MAX_DELTA_PERC ), delta < MAX_DELTA_PERC );

        // now make sure we actually have records in the right order.

        // verify that EACH partition has the records in the correct order.
        for( Partition part : config.getMembership().getPartitions() ) {
            assertRecordOrder( comparator, read( output, part ) );
        }

        assertRecordOrder( comparator, read( output ) );

    }

    private void assertRecordOrder( SortComparator comparator, List<StructPair> list ) {

        StructPair lastPair = null;

        for ( StructPair pair : list ) {

            long value = pair.value.readLong();

            if ( lastPair != null && comparator.compare( lastPair , pair ) > 0 ) {
                throw new RuntimeException( "Wrong order!" );
            }

            lastPair = pair;

        }

    }

}

