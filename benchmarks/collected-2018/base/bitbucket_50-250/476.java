// https://searchcode.com/api/result/125476873/

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
import peregrine.Job;
import peregrine.Mapper;
import peregrine.Reducer;
import peregrine.io.BroadcastInput;
import peregrine.io.JobOutput;

import java.util.List;

/**
 * Map reduce job which does the full global sort.
 */
public class GlobalSortJob {

    private static final Logger log = Logger.getLogger();
    
    public static class Map extends Mapper {

        @Override
        public void init( Job job, List<JobOutput> output ) {

            super.init( job, output );

            GlobalSortPartitioner partitioner = (GlobalSortPartitioner)job.getPartitionerInstance();
            BroadcastInput partitionTableBroadcastInput = getBroadcastInput().get( 0 );

            partitioner.init( job, partitionTableBroadcastInput );
            
        }
        
    }

    public static class Reduce extends Reducer {

    }

}


