// https://searchcode.com/api/result/122323755/

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

import peregrine.Record;

import java.io.Closeable;

/**
 * Interface for changing the implementation of KeyLookup.
 */
public interface KeyLookup extends Closeable {

    public boolean hasNext();

    public void next();

    public int size();

    public void set(Record entry);

    public Record get();

    public void reset();

    public boolean isTemporary();

    // zero copy slice implementation.
    public KeyLookup slice(int sliceStartInclusive, int sliceEndInclusive);

    /**
     * Allocate a new lookup with enough capacity to store both given lookups.
     */
    public KeyLookup allocate(KeyLookup kl0, KeyLookup kl1);

}

