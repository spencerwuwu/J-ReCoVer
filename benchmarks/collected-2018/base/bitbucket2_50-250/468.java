// https://searchcode.com/api/result/125477008/

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
package peregrine.reduce.merger;

import peregrine.KeyValuePair;
import peregrine.StructReader;
import peregrine.io.SequenceReader;

import java.io.IOException;

public class MergeQueueEntry implements KeyValuePair {
    
	public byte[] keyAsByteArray;
    
	public StructReader key;
    
    public StructReader value;

    public int id = -1;
    
    protected SequenceReader reader = null;

    protected MergeQueueEntry() {}

    public MergeQueueEntry( SequenceReader reader, int id ) throws IOException {

    	reader.next();
    	
        init( reader.key(), reader.value(), id );
        
        this.reader = reader;

    }

    public MergeQueueEntry( StructReader key, StructReader value, int id ) {
    	init( key, value, id );
    }

    private void init( StructReader key, StructReader value, int id ) {    	
        setKey( key );
        setValue( value );
        this.id = id;
    }
    
    public void setKey( StructReader key ) {
    	this.keyAsByteArray = key.toByteArray();
    	this.key = key;

    }

    public void setValue( StructReader value ) {
        this.value = value;
    }

    public StructReader getKey() {
        return key;
    }

    public StructReader getValue() {
        return value;
    }

    public MergeQueueEntry copy() {

        MergeQueueEntry copy = new MergeQueueEntry();

        copy.keyAsByteArray = keyAsByteArray;
        copy.key = key;
        copy.value = value;
        copy.reader = reader;
        copy.id = id;
        
        return copy;
        
    }
    
}


