// https://searchcode.com/api/result/122323447/

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
package peregrine.task;

import com.spinn3r.log5j.Logger;
import peregrine.Reducer;
import peregrine.StructReader;
import peregrine.io.Input;
import peregrine.io.driver.shuffle.ShuffleInputReference;
import peregrine.reduce.ReduceRunner;
import peregrine.reduce.SortListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Task and actual code used for running a reduce on shuffled data.
 */
public class ReducerTask extends BaseTask implements Callable {

    private static final Logger log = Logger.getLogger();
    
    private ShuffleInputReference shuffleInput;

    private ReduceRunner reduceRunner = null;
    
    public ReducerTask() {}

    @Override
    public void setInput( Input input ) {

        super.setInput( input );

        this.shuffleInput = (ShuffleInputReference)input.getReferences().get( 0 );
        log.info( "Using shuffle input : %s ", shuffleInput.getName() );

    }
    
    @Override
    public Object call() throws Exception {

        if ( output.getReferences().size() == 0 )
            throw new IOException( "Reducer tasks require output." );

        return super.call();
        
    }

    protected void doCall() throws Exception {
    	
    	SortListener listener = new ReducerTaskSortListener();

        reduceRunner = new ReduceRunner( config, this, partition, listener, shuffleInput, getJobOutput() );

        String shuffle_dir = config.getShuffleDir( shuffleInput.getName() );

        log.info( "Trying to find shuffle files in: %s", shuffle_dir );

        File shuffle_dir_file = new File( shuffle_dir );

        if ( ! shuffle_dir_file.exists() ) {
            throw new IOException( String.format( "Shuffle dir for %s does not exist at %s",
                                                  shuffleInput.getName(), shuffle_dir ) );
        }

        File[] shuffles = shuffle_dir_file.listFiles();

        //TODO: we should probably make sure these look like shuffle files with
        //the right extension, etc.
        for( File shuffle : shuffles ) {
        	reduceRunner.add( shuffle );
        }
        
        int nr_readers = shuffles.length;

        reduceRunner.reduce();

        log.info( "Sorted %,d entries in %,d chunk readers for partition %s",
                  report.getConsumed().get() , nr_readers, partition );

    }

    @Override
    public void teardown() throws IOException {
        reduceRunner.close();
        super.teardown();
    }

    /**
     * Call the reduce() method when we have a final value on the merge.
     */
    class ReducerTaskSortListener implements SortListener {
        
    	Reducer reducer = (Reducer)jobDelegate;

        @Override
        public void onFinalValue( StructReader key, List<StructReader> values ) {

            try {

                reducer.reduce( key, values );

                report.getConsumed().incr();

            } catch ( Exception e ) {
                throw new RuntimeException( "Reduce failed: " , e );
            }
                
        }

    }
    
}


