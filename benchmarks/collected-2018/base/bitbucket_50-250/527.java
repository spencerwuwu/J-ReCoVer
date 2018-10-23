// https://searchcode.com/api/result/122323567/

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
package peregrine.io.partition;

import com.spinn3r.log5j.Logger;
import peregrine.StructReader;
import peregrine.io.JobOutput;
import peregrine.task.Report;

import java.io.IOException;

/**
 * JobOutput for writing directly to a partition on the FS either in a reduce or
 * map-only or merge-only job.
 */
public class PartitionWriterJobOutput implements JobOutput {

    private static final Logger log = Logger.getLogger();

    protected PartitionWriter writer;

    protected Report report;
    
    public PartitionWriterJobOutput( PartitionWriter writer, Report report ) {
        this.writer = writer;
        this.report = report;
    }

    @Override
    public void emit( StructReader key , StructReader value ) {

        try {
            
            writer.write( key, value );

            report.getEmitted().incr();
            
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
    
}

