// https://searchcode.com/api/result/61250349/

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
package peregrine;

import java.io.*;
import java.util.*;

import peregrine.app.pagerank.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;
import peregrine.io.chunk.*;
import peregrine.io.driver.shuffle.*;
import peregrine.reduce.merger.*;
import peregrine.reduce.sorter.*;
import peregrine.shuffle.*;
import peregrine.task.*;
import peregrine.util.*;

public class TestCombinerEfficiency extends peregrine.BaseTestWithMultipleConfigs {

    public static boolean PREP = true;

    public static long SHUFFLE_BUFFER_SIZE = (long)(2 * Math.pow( 2, 27 ));
    
    public static class Reduce extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            // emit these flat 
            for( StructReader value : values ) {
                emit( key, value );
            }

        }
        
    }

    @Override
    public void doTest() throws Exception {

        System.out.printf( "prep: %s\n", PREP );
        System.out.printf( "shuffleBufferSize: %s\n", SHUFFLE_BUFFER_SIZE );

        doTest( 5000 * getFactor() , 100 ); 

    }

    private void doTest( int nr_nodes,
                         int max_edges_per_node ) throws Exception {

        for ( Config config : configs ) {
            config.setShuffleBufferSize( SHUFFLE_BUFFER_SIZE );
        }
        
        if ( PREP ) {
            
            String path = "/pr/test.graph";

            ExtractWriter writer = new ExtractWriter( config, path );

            GraphBuilder builder = new GraphBuilder( writer );
            
            builder.buildRandomGraph( nr_nodes , max_edges_per_node );
            
            writer.close();

            Controller controller = new Controller( config );

            try {

                // TODO: We can elide this and the next step by reading the input
                // once and writing two two destinations.  this would read from
                // 'path' and then wrote to node_indegree and graph_by_source at the
                // same time.
                
                controller.map( NodeIndegreeJob.Map.class,
                                new Input( path ),
                                new Output( "shuffle:default" ) );

                controller.flushAllShufflers();

                /*
                controller.reduce( Reduce.class,
                                   new Input( "shuffle:default" ),
                                   new Output( "/pr/tmp/node_indegree" ) );
                */

            } finally {
                controller.shutdown();
            }

        }
            
        // now attempt to open the main shuffle file... 

        //combine( "/tmp/peregrine-fs/localhost/11112/0/pr/tmp/shuffled_out/chunk000000.dat" );

        /*
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000000.tmp" );
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000001.tmp" );
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000002.tmp" );
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000003.tmp" );
        */

        /*
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000000.tmp" );
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000001.tmp" );
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000002.tmp" );
        combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000003.tmp" );
        */
        
        combineSamples( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/" , 20 );
        
        //combineAll( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/" );

        //combine( "/tmp/peregrine-fs/localhost/11112/tmp/shuffle/default/0000000007.tmp" );

        //combine2( "/tmp/peregrine-fs/localhost/11112/0/pr/tmp/node_indegree/" );

        System.out.printf( "TEST DONE\n" );
        
    }

    private void combineSamples( String dir, int nr_samples ) throws Exception {

        String[] files = getTempFiles( dir );

        int offset = 0;
        
        for( int i = 0; i < nr_samples; ++i ) {
            
            combine( files[offset] );
            
            offset += (int)Math.ceil( (double)files.length / (double)nr_samples);

            if ( offset > files.length - 1 )
                break;
            
        }
        
    }
    
    private void combineAll( String dir ) throws Exception {

        combine( getTempFiles( dir ) );
        
    }

    public String[] getTempFiles( String dir ) {

        File[] files = new File( dir ).listFiles(); 

        List<String> result = new ArrayList();
        
        for( File file : files ) {

            if ( file.getName().endsWith( ".tmp" ) ) {
                result.add( file.getPath() );
            }

        }

        Collections.sort( result );
        
        return Strings.toArray( result );

    }
    
    private void combine( String... paths ) throws Exception {

        for( String path : paths ) {
        
            if ( ! new File( path ).exists() ) {
                System.out.printf( "WARN: %s does not exist!\n", path );
                return;
            }

        }
        
        Config config = configs.get( 0 );
        
        //ChunkSorter sorter = new ChunkSorter 

        ShuffleInputReference shuffleInput = new ShuffleInputReference( "default" );

        Partition partition = new Partition( 0 );

        List<JobOutput> jobOutput = new ArrayList();

        List<SequenceReader> mergeInput = new ArrayList();

        int id = 0;

        long input_size = 0;
        
        for( String path : paths ) {

            File sorted_chunk = new File( "/tmp/sorted.chunk." + id++ );

            List<ChunkReader> work = new ArrayList();

            ShuffleInputChunkReader shuffleInputChunkReader =
                new ShuffleInputChunkReader( config, partition, path );
            
            work.add( shuffleInputChunkReader );

            ChunkSorter sorter = new ChunkSorter( config , partition );

            SequenceReader sorted = sorter.sort( work, sorted_chunk, jobOutput );

            mergeInput.add( sorted );

            input_size += shuffleInputChunkReader.getShuffleHeader().length;
            
        }

        File combined = new File( "/tmp/combined.chunk" );
        
        DefaultChunkWriter writer = new DefaultChunkWriter( config, combined );

        ReducerTask task = new ReducerTask();
        
        ChunkMerger merger = new ChunkMerger( task, null, partition, mergeInput, jobOutput );
        merger.merge( writer );

        writer.close();

        double efficiency = ( combined.length() / (double)input_size ) * 100;

        String desc = "";

        for( String path : paths ) {
            desc += new File( path ).getName() + " ";
        }

        desc = desc.trim();
        
        System.out.printf( "efficiency(%s): %f combine length is %,d and input length is %,d \n",
                           desc, efficiency, combined.length(), input_size );
        
    }

    public static void main( String[] args ) throws Exception {

        Getopt getopt = new Getopt( args );

        PREP = getopt.getBoolean( "prep", true );
        SHUFFLE_BUFFER_SIZE = getopt.getLong( "shuffleBufferSize", SHUFFLE_BUFFER_SIZE );

        if ( PREP == false ) {
            BaseTest.REMOVE_BASEDIR = false;
        }

        //System.setProperty( "peregrine.test.config", "04:01:32" ); 
        //System.setProperty( "peregrine.test.config", "01:01:1" ); 
        //System.setProperty( "peregrine.test.config", "8:1:32" );
        //System.setProperty( "peregrine.test.config", "2:1:3" ); 
        //System.setProperty( "peregrine.test.config", "2:1:3" ); 

        // FIXME: test with larger numbers of files....... FUCK.... so my tests
        // are TOTALLY wrong because the shuffle output os the RECEIVED output
        // not that which we're sending... :-( 
        
        System.setProperty( "peregrine.test.factor", "200" ); 
        System.setProperty( "peregrine.test.config", "1:1:1" ); 
        //System.setProperty( "peregrine.test.config", "8:1:32" );

        runTests();
        
    }

}

