// https://searchcode.com/api/result/122323456/

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

import peregrine.Mapper;
import peregrine.Reducer;
import peregrine.StructReader;
import peregrine.StructReaders;
import peregrine.util.Hashcode;

import java.util.List;

public class NodeIndegreeJob {

    public static class Map extends Mapper {

        @Override
        public void map( StructReader key,
                         StructReader value) {

            List<StructReader> targets = StructReaders.split( value, Hashcode.HASH_WIDTH );
            
            for( StructReader target : targets ) {
                emit( target, StructReaders.TRUE );
            }
            
        }

    }

    public static class Combine extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            emit( key, StructReaders.wrap( values.size() ) );

        }
        
    }

    public static class Reduce extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            int sum = 0;
            
            for ( StructReader value : values ) {
                sum += value.readInt();
            }
            
            emit( key, StructReaders.wrap( sum ) );
            
        }
        
    }

}
