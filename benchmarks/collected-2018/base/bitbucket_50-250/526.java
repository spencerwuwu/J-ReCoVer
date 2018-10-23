// https://searchcode.com/api/result/122323469/

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
import peregrine.*;
import peregrine.io.ChunkStreamListener;
import peregrine.io.JobOutput;
import peregrine.io.chunk.ChunkReference;
import peregrine.split.InputSplit;
import peregrine.split.InputSplitter;
import peregrine.split.LineBasedRecordFinder;
import peregrine.util.Strings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A job which takes an input file, parses the splits, then emits them.
 */
public class CorpusExtractJob {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper implements ParserListener {

        JobOutput nodeOutput = null;
        JobOutput linkOutput = null;

        // total number of nodes written.
        private int written = 0;

        private boolean caseInsensitive = false;
        
        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );

            nodeOutput = output.get( 0 );
            linkOutput = output.get( 1 );

            caseInsensitive = job.getParameters().getBoolean( "caseInsensitive" );

            log.info( "Using case insensitive graph: %s", caseInsensitive );
            
            ChunkStreamListener nodeOutputListener = (ChunkStreamListener)nodeOutput;
            ChunkStreamListener linkOutputListener = (ChunkStreamListener)linkOutput;

            try {
            
                String path = job.getParameters().getString( "path" );
                
                if ( path == null ) {
                    throw new IllegalArgumentException( "path" );
                }

                path = new File( config.getRoot(), path ).getPath();

                log.info( "Going to extract from path: %s", path );
                
                InputSplitter splitter = new InputSplitter( path, new LineBasedRecordFinder() );

                List<InputSplit> splits = splitter.getInputSplitsForPartitions( config, getPartition() );

                log.info( "Found %,d input splits on %s (path=%s)", splits.size(), getPartition(), path );

                ChunkReference chunkRef = new ChunkReference( getPartition() );

                for( InputSplit split : splits ) {

                    chunkRef.incr();

                    if ( chunkRef.getLocal() > job.getMaxChunks() )
                        break;
                    
                    nodeOutputListener.onChunk( chunkRef );
                    linkOutputListener.onChunk( chunkRef );

                    try {
                        log.info( "Processing split: %s on %s", split, getPartition() );
                        CorpusExtractor extractor = new CorpusExtractor( split.getChannelBuffer(), this );
                        extractor.exec();
                    } finally {
                        split.close();
                    }

                    log.info( "Closing split %s at chunkRef %s on %s", split, chunkRef, getPartition() );
                    
                    nodeOutputListener.onChunkEnd( chunkRef );
                    linkOutputListener.onChunkEnd( chunkRef );

                }
                
            } catch ( Exception e ) {
                log.error( "Unable to run job: ", e );
                throw new RuntimeException( e );
            }
            
        }

        @Override
        public void onEntry( String source, List<String> targets ) throws Exception {

            if ( targets.size() == 0 )
                return;

            if ( Strings.empty( source ) ) {
                return;
            }

            targets = cleanup( targets );

            if ( caseInsensitive ) {

                source = source.toLowerCase();

                List<String> tmp = new ArrayList( targets.size() );

                for( String target : targets ) {
                    tmp.add( target.toLowerCase() );
                }

                targets = tmp;

            }

            emitNode( source );

            for( String target : targets ) {
                emitNode( target );
            }

            emitLink( source, targets );
            
            ++written;
            
        }

        private List<String> cleanup( List<String> targets ) {

            // remove empty target URLs
            List<String> tmp = new ArrayList( targets.size() );

            for( String target : targets ) {

                if ( Strings.empty( target ) )
                    continue;
                    
                tmp.add( target );
                
            }

            return tmp;

        }
        
        private void emitNode( String name ) throws Exception {

            StructReader key = StructReaders.hashcode( name );
            StructReader value = StructReaders.wrap( name );

            nodeOutput.emit( key, value );
            
        }

        private void emitLink( String source, List<String> targets ) throws Exception {

            linkOutput.emit( StructReaders.hashcode( source ), StructReaders.hashcode( Strings.toArray( targets ) ) );

        }

        @Override
        public void map( StructReader key, StructReader value ) {
            //noop for now.  
        }

    }

    public static class Reduce extends Reducer {

    }

}

