// https://searchcode.com/api/result/123706310/

/* Copyright (c) 2002, David Peterson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  -  Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer. 
 *
 *  -  Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution. 
 *
 *  -  Neither the name of randombits.org nor the names of its contributors may
 *     be used to endorse or promote products derived from this software without
 *     specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*!
 *  <source>
 *    <file>ABitSet.java</file>
 *    <create-date>2002-01-31 23:08</create-date>
 *
 *    <changes>
 *    </changes>
 *  <source>
 */
package org.randombits.utils.collection;

import org.randombits.utils.lang.API;

/**
 * This is an extension of java.util.BitSet with a couple of utility methods.
 *
 * @author David Peterson
 */
@API
public class ABitSet extends java.util.BitSet {

    boolean lockable = false;

    boolean locked = false;

    /**
     * Creates new, not lockable, ABitSet.
     */
    @API
    public ABitSet() {
        this( false );
    }

    /**
     * Creates new ABitSet.
     *
     * @param lockable If <code>true</code>, the set may be locked.
     */
    @API
    public ABitSet( boolean lockable ) {
        this.lockable = lockable;
    }

    /**
     * Creates new ABitSet with the specified bit cache size.
     *
     * @param nbits    The initial bits to set.
     * @param lockable If <code>true</code>, the set may be locked.
     */
    @API
    public ABitSet( int nbits, boolean lockable ) {
        super( nbits );
        this.lockable = lockable;
    }

    /**
     * If this returns <code>true</code>, the set can be locked from
     * having further modifications.
     *
     * @return <code>true</code> if the set can be locked.
     */
    @API
    public boolean isLockable() {
        return lockable;
    }

    /**
     * Locks the bit set.
     * Once the bit set is locked, no changes may be made to it. It cannot
     * be unlocked again.
     *
     * @throws UnsupportedOperationException if the bit set is not lockable
     */
    @API
    public void lock() {
        if ( lockable )
            locked = true;
        else
            throw new UnsupportedOperationException( "This ABitSet is not lockable." );
    }

    /**
     * Sets each of the characters in the String into this set.
     * Characters may occur more than once without side effects.
     *
     * @param chars The set of characters to add to this set.
     */
    @API
    public void set( String chars ) {
        if ( chars != null ) {
            int length = chars.length();
            for ( int i = 0; i < length; i++ ) {
                set( chars.charAt( i ) );
            }
        }
    }

    @API
    public void set( int[] bits ) {
        if ( bits != null ) {
            for ( int bit : bits ) set( bit );
        }
    }

    @API
    public void clear( int[] bits ) {
        if ( bits != null ) {
            for ( int bit : bits ) clear( bit );
        }
    }

    /**
     * Clears each of the characters in the String into this BitSet.
     *
     * @param chars The characters to clear.
     */
    @API
    public void clear( String chars ) {
        if ( chars != null ) {
            int length = chars.length();
            for ( int i = 0; i < length; i++ ) {
                clear( chars.charAt( i ) );
            }
        }
    }

    /**
     * Sets the specified range of bits.
     *
     * @param from The start of the range to set.
     * @param to   The end of the range to set.
     */
    @API
    public void setRange( int from, int to ) {
        // set them from highest to lowest to reduce the
        // amount of buffer resizing.
        if ( from > to ) {
            int temp = from;
            from = to;
            to = temp;
        }

        for ( int i = to; i >= from; i-- ) set( i );
    }

    /**
     * Clears the specified range of bits.
     *
     * @param from The start of the range to clear.
     * @param to   The end of the range to clear.
     */
    @API
    public void clearRange( int from, int to ) {
        if ( to < from ) {
            int temp = from;
            from = to;
            to = temp;
        }

        for ( int i = from; i <= to; i++ ) clear( i );
    }

    @Override
    public void set( int bitIndex ) {
        checkLock();
        super.set( bitIndex );
    }

    @Override
    public void clear( int bitIndex ) {
        checkLock();
        super.clear( bitIndex );
    }

    private void checkLock() {
        if ( locked )
            throw new UnsupportedOperationException( "The bit set cannot be modified when locked" );
    }
}

