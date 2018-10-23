// https://searchcode.com/api/result/125476701/

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
package peregrine.app.pagerank;

import com.spinn3r.log5j.Logger;
import peregrine.Batch;
import peregrine.Job;
import peregrine.Mapper;
import peregrine.config.Config;
import peregrine.io.Input;
import peregrine.io.Output;
import peregrine.util.Strings;

/**
 * <p> Pagerank implementation which uses Peregrine as a backend for fast
 * computation.
 *
 * <p>
 * Our pagerank implementation takes an input file and writes resulting files to
 * /pr/out/ for external analysis and use within external systems.
 */
public class Pagerank extends Batch {
    
    private static final Logger log = Logger.getLogger();

    public static final int DEFAULT_ITERATIONS = 5;
    
    private Config config;

    /**
     * The number of iterations we should perform.
     */
    private int iterations = DEFAULT_ITERATIONS;

    /**
     */
    private int step = 0;

    private boolean sortedGraph = false;

    private String dir = "/pr";

    // path to the input graph.
    private String pathToGraph = null;

    private String pathToNodesByHashcode = null;

    private String pathToGraphBySource = null;

    private String pathToTmpNodeIndegree = null;

    private String pathToNodeMetadata = null;

    private String pathToDangling = null;

    private String pathToNonlinked = null;

    private String pathToNrNodes = null;

    private String pathToNrDangling = null;

    private String pathToRankVector = null;

    private String pathToTeleportationGrant = null;

    private String pathToRankMetadata = null;

    private String pathToRankSum = null;

    private String pathToRankMetadataByIndegree = null;

    private String pathToRankMetadataByRank = null;

    public Pagerank( Config config ) {

        this.config = config;

        setName( getClass().getName() );
        setDescription( getName() );

    }

    // use the dir parameter to build all paths for input and output.  This is
    // idempotent as long as 'dir' isn't changed between calls.
    private void initPaths() {

        if ( pathToGraph == null )
            throw new RuntimeException( "pathToGraph" );

        if ( pathToNodesByHashcode == null )
            throw new RuntimeException( "pathToNodesByHashcode" );

        pathToGraphBySource            = Strings.path( dir, "/graph_by_source" );
        pathToTmpNodeIndegree          = Strings.path( dir, "/tmp/node_indegree" );
        pathToNodeMetadata             = Strings.path( dir, "/out/node_metadata" );
        pathToDangling                 = Strings.path( dir, "/out/dangling" );
        pathToNonlinked                = Strings.path( dir, "/out/nonlinked" );
        pathToNrNodes                  = Strings.path( dir, "/out/nr_nodes" );
        pathToNrDangling               = Strings.path( dir, "/out/nr_dangling" );
        pathToRankVector               = Strings.path( dir, "/out/rank_vector" );
        pathToTeleportationGrant       = Strings.path( dir, "/out/teleportation_grant" );
        pathToRankMetadata             = Strings.path( dir, "/out/rank_metadata" );
        pathToRankSum                  = Strings.path( dir, "/out/rank_sum" );
        pathToRankMetadataByIndegree   = Strings.path( dir, "/out/rank_metadata_by_indegree" );
        pathToRankMetadataByRank       = Strings.path( dir, "/out/rank_metadata_by_rank" );

    }

    /**
     * Init PR ... setup all our vectors, node metadata table, etc.
     */
    public void init() {

        initPaths();

        // init directory variables...

        // ***** INIT stage... 

        log.info( "Running startup() stage." );
        
        // TODO: We can elide this and the next step by reading the input
        // once and writing two two destinations.  this would read from
        // 'graph' and then wrote to node_indegree and pathToGraphBySource at the
        // same time.

        // ***
        //
        // compute the node_indegree 

        map( NodeIndegreeJob.Map.class,
             new Input( pathToGraph ),
             new Output( "shuffle:default" ) );

        reduce( new Job().setDelegate( NodeIndegreeJob.Reduce.class )
                         .setCombiner( NodeIndegreeJob.Combine.class )
                         .setInput( "shuffle:default" )
                         .setOutput( pathToTmpNodeIndegree ) );

        // ***
        //
        // sort the graph by source since we aren't certain to have have the
        // keys in the right order and store in pathToGraphBySource for joining
        // across every iteration.  This is invariant so we should store it
        // to the filesystem.
        // 

        if ( sortedGraph == false ) {
        
            map( Mapper.class,
                 new Input( pathToGraph ),
                 new Output( "shuffle:default" ) );
            
            reduce( GraphBySourceJob.Reduce.class,
                    new Input( "shuffle:default" ),
                    new Output( pathToGraphBySource ) );

        } else {
            pathToGraphBySource = pathToGraph;
        }
            
        // ***
        //
        // now create node metadata...  This will write the dangling vector,
        // the nonlinked vector and node_metadata which are all invariant.

        merge( new Job().setDelegate( NodeMetadataJob.Map.class )
                        .setInput( pathToTmpNodeIndegree, pathToGraphBySource )
                        .setOutput( pathToNodeMetadata,
                                    pathToDangling,
                                    pathToNonlinked,
                                    "broadcast:nr_nodes",
                                    "broadcast:nr_dangling" ) );
        
        reduce( NodeMetadataJob.Reduce.class,
                new Input( "shuffle:nr_nodes" ),
                new Output( pathToNrNodes ) );

        reduce( NodeMetadataJob.Reduce.class,
                new Input( "shuffle:nr_dangling" ),
                new Output( pathToNrDangling ) );
        
        // startup empty files which we can still join against.

        // make sure these files exist.
        truncate( pathToRankVector );
        truncate( pathToTeleportationGrant );

    }

    /**
     * Run one pagerank step.
     */
    public void iter() {

        // TODO: migrate these to the new syntax of new Job() 

        String desc = String.format( "Iteration %s of %s", step, iterations ); 
        
        merge( new Job().setDelegate( IterJob.Map.class )
                        .setInput( pathToGraphBySource,
                                   pathToRankVector ,
                                   pathToDangling ,
                                   pathToNonlinked ,
                                   "broadcast:" + pathToNrNodes )
                        .setOutput( "shuffle:default",
                                    "broadcast:dangling_rank_sum" )
                        .setDescription( desc ) );

        reduce( new Job().setDelegate( IterJob.Reduce.class )
                         .setCombiner( IterJob.Combine.class )
                         .setInput( "shuffle:default",
                                    "broadcast:" + pathToNrNodes,
                                    "broadcast:" + pathToNrDangling,
                                    "broadcast:" + pathToTeleportationGrant )
                         .setOutput( pathToRankVector,
                                     "broadcast:rank_sum" )
                         .setDescription( desc ) );

        // now reduce the broadcast rank sum to an individual file for analysis
        // and reading
        reduce( new Job().setDelegate( GlobalRankSumJob.Reduce.class )
                         .setInput( "shuffle:rank_sum" )
                         .setOutput( pathToRankSum ) );
        
        // ***
        // 
        // write out the new ranking vector
        if ( step < iterations - 1 ) {

            // now compute the dangling rank sum for the next iteration

            reduce( TeleportationGrantJob.Reduce.class, 
                    new Input( "shuffle:dangling_rank_sum",
                               "broadcast:" + pathToNrNodes ),
                    new Output( pathToTeleportationGrant ) );
            
        }

        ++step;
        
    }

    /**
     * Finalize PR.
     */
    public void term() {

        // merge the rank vector, node metadata (indegree, outdegree) as well as name of the node, title, and description.
        merge( MergeNodeAndRankMetaJob.Merge.class,
               new Input( pathToNodeMetadata, pathToRankVector, pathToNodesByHashcode ),
               new Output( pathToRankMetadata ) );

        sort();
        
    }

    /**
     * Sort the resulting output files.
     */
    public void sort() {

        initPaths();

        sort( pathToRankMetadata,
              pathToRankMetadataByIndegree,
              RankMetadataByIndegreeComparator.class );

        sort( pathToRankMetadata,
              pathToRankMetadataByRank,
              RankMetadataByRankSortComparator.class );
    }

    /**
     * Build the batch from all existing configuration variables.
     */
    public Pagerank prepare() {

        init();

        // startup the empty rank_vector table ... we need to merge against it.

        // ***** ITER stage... 

        for( int step = 0; step < iterations; ++step ) {
            iter();
        }

        term();

        return this;
        
    }
    
    public int getIterations() { 
        return this.iterations;
    }

    public void setIterations( int iterations ) { 
        this.iterations = iterations;
    }

    public String getPathToGraph() {
        return pathToGraph;
    }

    public void setPathToGraph(String pathToGraph) {
        this.pathToGraph = pathToGraph;
    }

    public String getPathToNodesByHashcode() {
        return pathToNodesByHashcode;
    }

    public void setPathToNodesByHashcode(String pathToNodesByHashcode) {
        this.pathToNodesByHashcode = pathToNodesByHashcode;
    }

    public boolean isSortedGraph() {
        return sortedGraph;
    }

    public void setSortedGraph(boolean sortedGraph) {
        this.sortedGraph = sortedGraph;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}

