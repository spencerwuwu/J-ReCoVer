// https://searchcode.com/api/result/122323751/

/*
 * Copyright 2011-2013 Kevin A. Burton
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

import peregrine.StructReader;
import peregrine.StructReaders;
import peregrine.io.JobOutput;
import peregrine.io.chunk.DefaultChunkWriter;

import java.io.IOException;

/**
 *
 */
public class CombinerJobOutput implements JobOutput {

    private DefaultChunkWriter writer;

    public CombinerJobOutput( DefaultChunkWriter writer ) {
        this.writer = writer;
    }

    @Override
    public void emit( StructReader key, StructReader value ) {

        try {

            // NOTE: that the merger expects to read a varint on the number of
            // items stored here so we need to 'wrap' it even though it's a
            // single value.  This is a slight overhead but we should probably
            // ignore it.  Most combiner use will probably take N input items
            // and map to 1 output item.  In this case we don't care if there is
            // a 1 byte varint overhead.

            writer.write( key, StructReaders.splice( value ) );

        } catch ( IOException e ) {
            throw new RuntimeException( e ) ;
        }

    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

}

