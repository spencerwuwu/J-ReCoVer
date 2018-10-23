// https://searchcode.com/api/result/122323467/

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
import peregrine.Reducer;
import peregrine.StructReader;
import peregrine.StructReaders;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueOutboundLinksJob {

    private static final Logger log = Logger.getLogger();

    public static class Reduce extends Reducer {

        @Override
        public void reduce( StructReader key, List<StructReader> values ) {

            // make sure we always have unique outbound links.  We also need to
            // do this when we are merging larger sets.
            Set<StructReader> set = new HashSet();

            for( StructReader current : values ) {
                set.add( current );
            }

            emit( key, StructReaders.join( set ) );
            
        }

    }

}

