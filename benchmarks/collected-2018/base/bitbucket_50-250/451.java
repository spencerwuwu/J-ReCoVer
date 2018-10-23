// https://searchcode.com/api/result/61250359/

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
package peregrine.util.netty;

import java.io.*;
import java.util.*;

import peregrine.util.*;
import peregrine.util.primitive.LongBytes;
import peregrine.reduce.*;
import peregrine.reduce.merger.*;
import peregrine.io.chunk.*;
import peregrine.config.*;
import peregrine.os.*;

public class TestPrefetchReader extends peregrine.BaseTest {

    public void test1() throws Exception {

        // create a test file ...
        File file = new File( "/tmp/test.dat" );
        FileOutputStream out = new FileOutputStream( file );

        byte[] data = new byte[ (int)PrefetchReader.DEFAULT_PAGE_SIZE ]; 
        
        for( int i = 0; i < 20; ++i ) {
            out.write( data );
        }

        out.close();

        Config config = new Config();
        config.initEnabledFeatures();
        
        MappedFileReader mappedFileReader = new MappedFileReader( config, file );

        StreamReader reader = mappedFileReader.getStreamReader();
        
        List<MappedFileReader> input = new ArrayList();
        input.add( mappedFileReader );
        
        PrefetchReader prefetchReader = new PrefetchReader( config, input );

        prefetchReader.setEnableLog( true );
        //prefetchReader.setCapacity( file.length() );
        //prefetchReader.start();

        long cached = 0;

        int read = 0;
        int width = 10;
        long length = file.length();
        
        while( true ) {

            if ( read + width > length )
                width = (int)(length - read);
            
            reader.read( width );
            
            read += width;

            if ( read >= length )
                break;
            
        }

        // FIXME: make sure the right pages were read.
        
        prefetchReader.close();

    }

    // FIXME: build a test reading say 10 bytes at a time form the
    // StreamReader until we are a the end of the file.
    
    public static void main( String[] args ) throws Exception {
        runTests();
    }

}

