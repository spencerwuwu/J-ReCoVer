// https://searchcode.com/api/result/61250706/

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
package peregrine.app.benchmark;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import peregrine.*;
import peregrine.config.*;
import peregrine.controller.*;
import peregrine.io.*;
import peregrine.io.partition.*;
import peregrine.util.*;
import peregrine.util.primitive.*;
import com.spinn3r.log5j.*;

public class Benchmark {

    private static final Logger log = Logger.getLogger();

    public static class Map extends Mapper {

        public static boolean EMIT = true;
        
        @Override
        public void map( StructReader key,
                         StructReader value ) {

            if ( EMIT )
                emit( key, value );
            
        }

    }

    public static class Reduce extends Reducer {

        AtomicInteger count = new AtomicInteger();
        
        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            List<Integer> ints = new ArrayList();

            // decode these so we know what they actually mean.
            for( StructReader val : values ) {
                ints.add( val.readInt() );
            }

            count.getAndIncrement();

        }

        @Override
        public void cleanup() {

            if ( count.get() == 0 )
               throw new RuntimeException( "count is zero" );
            
        }

    }

    private Config config;
    
    public Benchmark( Config config ) {
        this.config = config;
    }

}

