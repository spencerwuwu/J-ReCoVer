// https://searchcode.com/api/result/61250541/

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
package peregrine.reduce.sorter;

import java.io.*;
import java.util.*;
import peregrine.util.primitive.LongBytes;

import org.jboss.netty.buffer.*;

public class SorterPriorityQueue {

    private PriorityQueue<SortQueueEntry> queue = null;

    protected int key_offset = 0;

    protected SortQueueEntry result = new SortQueueEntry();

    protected SortMergeComparator comparator = new SortMergeComparator();
    
    public SorterPriorityQueue( List<KeyLookup> lookups ) throws IOException {

        this.queue = new PriorityQueue( lookups.size(), comparator );
        
        for( KeyLookup lookup : lookups ) {

            if ( lookup.hasNext() == false )
                continue;

            lookup.next();
            SortQueueEntry entry = new SortQueueEntry();
            entry.lookup = lookup;
            entry.queue  = this;

            queue.add( entry );
            
        }
        
    }

    public SortQueueEntry poll() throws IOException {

        //TODO: there's an optimization here where we get down to only one
        //priority queue as we can just return from it directly.
        
        SortQueueEntry entry = queue.poll();

        if ( entry == null )
            return null;

        KeyLookup lookup = entry.lookup;

        // provide the lookup so we can debug this if necessary.
        this.result.lookup = lookup;

        //return a copy of the ptr so that we can read the ptr value.           
        this.result.entry = lookup.get();
        
        if ( lookup.hasNext() ) {
            lookup.next();
            add( entry );
        }

        return this.result;
        
    }
    
    private void add( SortQueueEntry entry ) {
        queue.add( entry );
    }

}

class SortMergeComparator implements Comparator<SortQueueEntry> {

    public int compare( SortQueueEntry e0, SortQueueEntry e1 ) {
                
        KeyEntry entry0 = e0.lookup.get();
        KeyEntry entry1 = e1.lookup.get();
        
        int diff = 0;

        for( int offset = 0; offset < LongBytes.LENGTH; ++offset ) {

            diff = entry0.read( offset ) - entry1.read( offset );

            if ( diff != 0 )
                return diff;
            
        }

        return diff;

    }

}

class SortQueueEntry {

    protected SorterPriorityQueue queue = null;

    protected KeyLookup lookup = null;

    /**
     * Used to store the offset pointer.
     */
    protected KeyEntry entry = null;
    
}


