// https://searchcode.com/api/result/125476717/

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
package peregrine.app.pagerank.extract;

import com.spinn3r.log5j.Logger;
import peregrine.Batch;
import peregrine.Job;
import peregrine.io.Input;
import peregrine.io.Output;

/**
 */
public class Extract extends Batch {

    private static final Logger log = Logger.getLogger();

    private String path;
    private String graph;
    private String nodes_by_hashcode;
    private boolean caseInsensitive;
    private int maxChunks = Integer.MAX_VALUE;

    public Extract( String path, String graph, String nodes_by_hashcode, boolean caseInsensitive ) {

        super( Extract.class );

        this.path = path;
        this.graph = graph;
        this.nodes_by_hashcode = nodes_by_hashcode;
        this.caseInsensitive = caseInsensitive;

    }

    public void prepare() {

        map( new Job().setDelegate( CorpusExtractJob.Map.class )
                      .setInput( new Input( "blackhole:" ) )
                      .setOutput( new Output( "shuffle:nodes", "shuffle:links" ) )
                      .setMaxChunks( maxChunks )
                      .setParameters( "path", path,
                                      "caseInsensitive", caseInsensitive )
                      .setDescription( String.format( "Extract to create graph and nodes_by_hashcode path=%s, caseInsensitive=%s", path, caseInsensitive ) ) );
       
        reduce( new Job().setDelegate( UniqueNodeJob.Reduce.class )
                         .setCombiner( UniqueNodeJob.Reduce.class )
                         .setInput( "shuffle:nodes" )
                         .setOutput( nodes_by_hashcode )
                         .setDescription( String.format( "Create nodes_by_hashcode=%s", nodes_by_hashcode ) ) );

        reduce( new Job().setDelegate( UniqueOutboundLinksJob.Reduce.class )
                         .setInput( "shuffle:links" )
                         .setOutput( graph )
                         .setDescription( String.format( "Create graph=%s", graph ) ) );

    }

    public int getMaxChunks() { 
        return this.maxChunks;
    }

    public void setMaxChunks( int maxChunks ) { 
        this.maxChunks = maxChunks;
    }

}

