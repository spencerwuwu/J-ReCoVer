// https://searchcode.com/api/result/11920128/

/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/LRUHashtable.java#15 $
    
    Responsible: hcai
        
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.util.List;
import ariba.util.log.Logger;

/**
    A hashtable that tracks the age of entries.  Adding and entry
    makes the entry the newest.  Putting an entry in again will make
    the entry newest again.
    <p>
    Tables always grow in size by doubling.  The index computation
    depends on the table size being a power of two.  This is a
    tiny performance hack, and could easily be removed.
    <p>
    The table grows up to a given target size of elements, after which
    it start purging items from the table.
    <p>
    We don't want to purge the table on every insert, so when we
    purge, we purge a bunch.  If we fail to purge as much as we want
    to -- probably a very bad sign for our client -- we increase the
    purgeTrigger.
    <p>
    We always try to purge to the targetSize.  After a purge, we set
    the purgeTrigger to be "purgeMargin" more than the current size.
    <p>
    We only attempt to purge at the point at which we would have to
    grow the table.  This might be well after we have reached the
    purgeTrigger number of entries.
    <p>

    The point of the purge margin is to make sure we bundle together
    purges.  If you pick an bad targetEntries and purgeMargin, then
    the purges can run more frequently than you might expect.  To
    avoid this, targetEntries&#43;purgeMargin should be just less than
    maxLoad*maxSize, where maxSize is allocation size (in number of
    entries) that you expect to run with.  Then the point at which the
    table fills up will be just beyond the purgeTrigger, so you'll
    always purge before trying to grow beyond your target size.  As
    long as the purges reduce the table entries to the target, purges
    will only be triggered after having done at least purgeTrigger
    adds to the table.  If a purge does not yield enough, then the
    trigger will be pushed up over the permitted number of entries for
    the table size, and the table will grow the next time it fills up.

    <p>
    For example:
    new LRUHashtable(<code>name</code>, 16, 140, 50, .75, <code>removeListener</code>)
    Allocates a table of 16 entries (which can hold 16*.75=12 entries
    before growing).  The table will grow to size 256 (which can hold
    192 entries).  When the table fills up, and is about to grow, a
    purge will be triggered -- because the trigger is 140+50=190.
    <p>
    <code>name</code> is used to label debugging output.
    <p>

    Supplying a <code>removeListener</code> gives the creator of the
    LRUHashtable veto power on the removal of objects from the table.
    When the LRUHashtable scans to purge old entries, it calls
    <code>removeListener</code>.okToRemove(<code>object</code>) on
    each object that is old enough to be removed.  If okToRemove
    returns false, the object is not removed.  It is possible for a
    table to grow beyond its specified limits because nothing can be
    removed.

    <p>

    A <code>removeListener</code> of null is the same as a
    removeListener with an okToRemove() method that always returns
    true.

    <p>Synchronization: This class is <b>NOT thread safe</b>. The caller
    is repsonsible to ensure thread-safe access if needed.
    <p>

    @aribaapi ariba
*/

public class LRUHashtable
{

    private LRUEntry[] ents = null;

    private int oldest = 0;
    private int newest = 0;

    public final Object lock;

    /**
        To identify this table in error messages
    */
    String name;

    /**
        Number of entries currently occupied
    */
    public int inUse;

    /**
        Maximum number of entries allowed to be occupied
    */
    int inUseMax;

    /**
        Number of entries allocated in the table
    */
    protected int allocated;

    /**
        log base 2 of "allocated"
    */
    int logSize;

    /**
        Maximum number of entries allowed to be allocated in the table
    */
    int growTo;

    /**
        The maximum load factor for the table. inUseMax = maxLoad * allocated
    */
    double maxLoad;

    /**
        The number of elements over the target that we will allow
        before triggering a purge
    */
    protected int purgeMargin;

    /**
        Number of elements that will trigger the next purge
    */
    protected int purgeTrigger;

    /**
        The number of live entries we try to reduce to during a purge
    */
    protected int targetEntries;

    //--- Statistics ----------------------------------------------------------

    /**
        Total # of lookups (map from key -> val)
    */
    int lookups = 0;

    /**
        Total # of lookups (map from key -> val) that found a val
    */
    int hits = 0;

    /**
        Total # of probes (a lookup may take >1 probes)
    */
    int probes = 0;

    /**
        Max number of probes on a single lookup for the currently allocated
        table. Reset on resize.
    */
    int maxProbes = 0;

    /**
        Max number of probes on a single lookup for the all
        allocations of this table. Not reset on resize.
    */
    int maxProbesAll = 0;

    /**
        The biggest table allocated for this set
    */
    int maxTableSize = 0;

    /**
        Number of entries purged
    */
    int purged = 0;

    /**
        Number of entries that we looked at but could not purge
    */
    int unPurged = 0;


    /*-----------------------------------------------------------------------

      -----------------------------------------------------------------------*/

    /**
        Called to check if it is OK to remove an object from the table.
    */
    LRURemoveListener removeListener;

    /*-----------------------------------------------------------------------
        Construction
      -----------------------------------------------------------------------*/

    /**
        Initial entries should be a power of 2.
    */
    public LRUHashtable (
        String name,
        int initialEntries,
        int targetEntries,
        int purgeMargin,
        double maxLoad,
        LRURemoveListener removeListener)
    {

        this.name = name;
        this.removeListener = removeListener;

        this.targetEntries = targetEntries;
        this.purgeMargin = purgeMargin;
        this.maxLoad = maxLoad;

        this.purgeTrigger = this.targetEntries + this.purgeMargin;

        this.lock = new Object();

        initializeEntries(initialEntries);
    }

    protected void initializeEntries (int initialEntries)
    {
        Assert.that(initialEntries > 0,
                    "Invalid initial entries size %s, must be > 0",
                    Constants.getInteger(initialEntries));
        allocateEntries((int)MathUtil.power2(MathUtil.log2(initialEntries)));
    }

    /*-----------------------------------------------------------------------
        Basic Accessors
      -----------------------------------------------------------------------*/
    public int getLookups ()
    {
        return lookups;
    }

    public int getHits ()
    {
        return hits;
    }

    /*-----------------------------------------------------------------------
        Put and get
      -----------------------------------------------------------------------*/

    /*
        Record in the LRU Table that "key" is most recently used.
        Client must ensure there is enough space to add an entry.
        It is illegal to put null key or null value into the LRUHashtable.
        Return true if an entry was added.
    */
    public final boolean put (Object key, Object value)
    {
        Assert.that(key != null, "Cannot put null key into LRUHashtable");
        Assert.that(value != null, "Cannot put null value into LRUHashtable");

        int probe = findEntry(key);
        boolean found = this.ents[probe].key != null;

        if (found) {
            this.ents[probe].value = value;
            promote(probe);
        }
        else {
            add(probe, key, value);
            if (this.inUse >= this.inUseMax) {
                growOrPurge(key);
            }
        }

        if (Log.lruCheck.isDebugEnabled()) {
            sane(Log.lruCheck);
        }
        return !found;
    }


    /**
        Return the value associated with 'key', or null if not found.
        Make the entry found newest.
    */
    public final Object get (Object key)
    {
        Assert.that(key != null, "Null key not allowed");

        int probe = findEntry(key);
        boolean found = this.ents[probe].key != null;

        this.lookups++;

        if (found) {
            this.hits++;
            promote(probe);
            return this.ents[probe].value;
        }
        else {
            return null;
        }
    }

    /**
        Like get, but does not promote the found item.  For printing
        to the log, for example.
    */
    public final Object peek (Object key)
    {
        Assert.that(key != null, "Null key not allowed");

        int probe = findEntry(key);
        boolean found = this.ents[probe].key != null;

        if (found) {
            return this.ents[probe].value;
        }
        else {
            return null;
        }
    }


    /**
       Return the oldest object in the LRU
     */
    public final Object getOldestKey ()
    {
        if (this.inUse == 0) {
            return null;
        }

        return this.ents[this.oldest].key;
    }

    /*
        Find LRU entry for key. If the entry is empty then the entry
        was not in the table and the returned index is where the entry
        should be inserted.
    */
    private int findEntry (Object key)
    {
        int probe;
        int probes = 1;
        Object storedKey;

        if (this.allocated == 1) {
            probe = 0;
        }
        else {
            probe = firstProbe(key);
        }

            // look in hash bucket first, if not there, scan backwards
        for (;;) {
            storedKey = this.ents[probe].key;
            if (storedKey == null || objectsAreEqualEnough(storedKey, key)) {
                break;
            }

                // Keep looking, subtract one circularly
            probe = (probe == 0) ? (this.allocated - 1) : probe - 1;

                // Too many probes implies a bug
            if (++probes > this.allocated) {
                Assert.that(false, "probes(%s) < this.allocated(%s)",
                            Constants.getInteger(probes),
                            Constants.getInteger(this.allocated));
            }
        }

            // Note statistics
        this.probes += probes;
        if (this.maxProbes < probes) {
            this.maxProbes = probes;
        }

        return probe;
    }


    /*
        Make a new entry in the LRU table: <uid> at position <probe>.
    */
    private void add (int probe, Object key, Object value)
    {
        this.ents[probe].key = key;
        this.ents[probe].value = value;

        if (this.inUse == 0) {
            this.ents[probe].newer = probe;
            this.ents[probe].older = probe;
            this.newest            = probe;
            this.oldest            = probe;
        }
        else {
            this.ents[this.newest].newer = probe;
            this.ents[probe      ].newer = probe;
            this.ents[probe      ].older = this.newest;
            this.newest                  = probe;
        }

        this.inUse++;
    }

    /*
        Make an entry in LRU table the newest.  A number of checks are
        avoided by making the newest and oldest entries point to
        themselves.
    */
    private void promote (int probe)
    {
        int n = this.ents[probe].newer;
        int o = this.ents[probe].older;

        if (this.newest == probe) {
            return;
        }

            // Splice out entry that just became newest
        this.ents[n].older = o;
        this.ents[o].newer = n;

            // If oldest became newest, move up oldest pointer
        if (this.oldest == probe) {
            this.oldest = n;
            this.ents[n].older = n;
        }

            // Move probe to head of list
        this.ents[probe].newer = probe;
        this.ents[probe].older = this.newest;
        this.ents[this.newest].newer = probe;
        this.newest = probe;
    }



    /*-----------------------------------------------------------------------
        Allocate, resize
      -----------------------------------------------------------------------*/

    /*
        Allocate a table with of size 'entries'.  'entries' should be
        a power of two.
    */
    protected void allocateEntries (int entries)
    {
        Log.lruDebug.debug("%s: allocating %s entries", name, entries);
        this.allocated = entries;
        this.logSize   = MathUtil.log2(entries);
        this.inUseMax  = (int)(this.maxLoad * this.allocated);
        this.inUse     = 0;
        this.ents      = new LRUEntry[this.allocated];
        this.oldest    = 0;
        this.newest    = 0;

        for (int i = 0; i < this.allocated; i++) {
            this.ents[i] = new LRUEntry();
        }

            // Statistic
        if (this.allocated > this.maxTableSize) {
            this.maxTableSize = this.allocated;
        }
    }

    /*
        We've run out of room in the table.  Either grow the table, or
        remove some old entries
    */
    private void growOrPurge (Object leave)
    {
        if (this.inUse < this.purgeTrigger) {
            grow();
        }
        else {
            removeOldestUntil(this.targetEntries, leave);
            int oldTrigger = this.purgeTrigger;
            this.purgeTrigger = this.inUse + this.purgeMargin;

            if (this.purgeTrigger != oldTrigger) {
                Log.util.warning(2802, name, oldTrigger, this.purgeTrigger);
            }
                // In the unlikely case that the purge didn't recover
                // space, we grow the table.
            if (this.inUse >= this.inUseMax) {
                Log.util.warning(2803, name);
                grow();
            }
        }
    }

    /*
        Change the size of the LRU table in "t" to "newSize".  It is
        the caller's responsibilty to make sure newSize is big enough.
    */
    protected void resize (int newSize)
    {
        int i;

        LRUEntry[] old = this.ents;

        int oldInUse  = this.inUse;
        int oldOldest = this.oldest;
        int oldNewest = this.newest;
        int oldSize = this.allocated;

        if (Log.lruCheck.isDebugEnabled()) {
            sane(Log.lruCheck);
        }

        if (Log.lruDebug.isDebugEnabled()) {
            Log.lruDebug.debug("%s: LRU table before resize:", name);
            if (Log.lruCheck.isDebugEnabled()) {
                dump(Log.lruCheck);
            }
            else {
                dumpSummary(Log.lruDebug);
                dumpLRUStats(Log.lruDebug);
            }
        }

        allocateEntries(newSize);
        Assert.that(this.inUseMax >= oldInUse, "this.inUseMax >= oldInUse");

            // Statistic
        if (this.maxProbesAll < this.maxProbes) {
            this.maxProbesAll = this.maxProbes;
        }
        this.maxProbes = 0;

            // Move the entries
        int num = 0;
        for (i = oldOldest; /* null condition*/; i = old[i].newer) {
            put(old[i].key, old[i].value);
            if (i == oldNewest) {
                break;
            }
            if (num == oldSize) {
                    // we can't possibly move more than the original size, something
                    // is wrong here which could possibly get us into an infinite loop.
                    // so log a warning, and break out.
                    //
                    // Future improvement:
                    // A better way is to move the newer
                    // entries first, so if we do get into some problems, only the older
                    // ones would not be moved. The reason for the current implementation
                    // is that we promote the entries when we put. So we might want to
                    // have a private put method that takes a boolean doPromote, have the
                    // public put call this private one with true. Then we can move the
                    // latest entries first and not promote the entries as follows:
                    // int prev = -1;
                    // int num = 0;
                    //for (i = oldNewest;; i= old[i].older) {
                    //if (prev != -1 && prev == i) {
                    //if (i != oldOldest) {
                    //    log a warning
                    //}
                    //break;
                    //}
                    //if (num == oldSize) {
                    //    log warning
                    //break;
                    //}
                    //put(old[i].key, old[i].value, false);
                    //prev = i;
                    //num++;
                    //}
                Log.lruCheck.warning(9094, SystemUtil.stackTrace());
                break;
            }
            num++;
        }

        if (Log.lruCheck.isDebugEnabled()) {
            sane(Log.lruCheck);
        }

        if (Log.lruDebug.isDebugEnabled()) {
            Log.lruDebug.debug("%s: LRU table after resize:", name);
            if (Log.lruCheck.isDebugEnabled()) {
                dump(Log.lruCheck);
            }
            else {
                dumpSummary(Log.lruDebug);
            }
        }

        // restore the inUse value
        this.inUse = oldInUse;
    }

    /**
        Allocate a larger table when the maximum load is reached.
    */
    protected void grow ()
    {
        resize(2 * this.allocated);
    }


    /*-----------------------------------------------------------------------
        Deletion
      -----------------------------------------------------------------------*/

    /*
        Remove an entry from the LRU table, fixup the linked list, and
        fixup the hash table by trying to fill the vacated spot with
        an entry the previously collided.
    */

    private Object removeAtIndex (int indexRemoved)
    {
        if (this.inUse == 0) {
            return null;
        }

        Object removedValue = this.ents[indexRemoved].value;

        if (this.inUse == 1) {
            this.newest = 0;
            this.oldest = 0;
        }
        else if (indexRemoved == this.newest) {
            this.newest = this.ents[indexRemoved].older;
            this.ents[this.newest].newer = this.newest;
        }
        else if (indexRemoved == this.oldest) {
            this.oldest = this.ents[indexRemoved].newer;
            this.ents[this.oldest].older = this.oldest;
        }
        else {
            int n = this.ents[indexRemoved].newer;
            int o = this.ents[indexRemoved].older;
            this.ents[n].older = o;
            this.ents[o].newer = n;
        }

        this.ents[indexRemoved].older = 0;
        this.ents[indexRemoved].newer = 0;
        this.ents[indexRemoved].key = null;
        this.ents[indexRemoved].value = null;
        this.inUse--;

        removeHashEntry(indexRemoved);
        if (Log.lruCheck.isDebugEnabled()) {
            sane(Log.lruCheck);
        }

        return removedValue;
    }

    /*
        Delete an LRU hash table entry.  Move collided entries up to fill
        the deleted slot. See Knuth V3 p527.  In Knuth, "del" ~ "j" and
        "chain" ~ "i".
    */
    private void removeHashEntry (int del)
    {
        LRUEntry[] ents = this.ents;
        int      probe;
        int      chain;

        chain = del;

        for (;;) {
                // Check next element in chain (circular decrement)
            if (chain == 0) {
                chain = this.allocated;
            }
            chain--;

                // Empty slot indicates end of chain
            if (ents[chain].key == null) {
                break;
            }

            probe = firstProbe(ents[chain].key);
            if (! circularlyBetween(chain, del, probe)) {
                moveRecord(chain, del);
                del = chain;
            }
        }
    }

    /*
        Move record at position i to position j
    */
    private void moveRecord (int i, int j)
    {
        LRUEntry[] ents  = this.ents;
        int        n     = ents[i].newer;
        int        o     = ents[i].older;

            // Move entry
        ents[j].key   = ents[i].key;
        ents[j].value = ents[i].value;
        ents[j].older = ents[i].older;
        ents[j].newer = ents[i].newer;

        ents[i].key   = null;
        ents[i].value = null;
        ents[i].older = 0;
        ents[i].newer = 0;

            // Fixup LRU linked list
        if (this.inUse == 1) {
            this.oldest = this.newest = ents[j].older = ents[j].newer = j;
        }
        else if (i == this.oldest) {
            ents[n].older = j;
            ents[j].older = j;
            this.oldest   = j;
        }
        else if (i == this.newest) {
            ents[o].newer = j;
            ents[j].newer = j;
            this.newest   = j;
        }
        else {
            ents[n].older = j;
            ents[o].newer = j;
        }
    }


    /*
        Does probe fall between  low and high, circularly?
    */
    private static final boolean circularlyBetween (int low, int high, int probe)
    {
        boolean llth = low < high;
        boolean pgel = probe >= low;
        boolean plth = probe < high;

        return llth ? pgel && plth : pgel || plth;
    }


    /*-----------------------------------------------------------------------
        Higher level deletion
      -----------------------------------------------------------------------*/

    /*
        Remove the oldest entry from the LRU.
    */
    public final Object removeOldest ()
    {
        Assert.that(this.inUse > 0, "this.inUse > 0");
        return removeAtIndex(this.oldest);
    }


    public final Object remove (Object key)
    {
        Assert.that(key != null, "Null key not allowed");

        int probe = findEntry(key);
        boolean found = this.ents[probe].key != null;

        if (found) {
            return removeAtIndex(probe);
        }
        return null;
    }


    /**
        Clears the entire hashtable.

        SHOULD BE USED WITH EXTREME CARE.
    */
    public final void clear ()
    {
            // clear each entry
        for (int i = 0; i < this.ents.length; ++i) {
            this.ents[i].older = 0;
            this.ents[i].newer = 0;
            this.ents[i].key = null;
            this.ents[i].value = null;
        }

            // clear the global state
        this.newest = 0;
        this.oldest = 0;
        this.inUse = 0;
        this.lookups = 0;
        this.hits = 0;
    }

    protected void removeOldestUntil (int target, Object leave)
    {
        deleteOldestUntil(target, leave);
    }
    
    public int getTotalInUseEntries ()
    {
        return this.inUse;
    }
    
    public List purgeOldestEntries (int totalOldestEntries)
    {
        return deleteOldestUntil(this.inUse - totalOldestEntries, null);
    }

    private List deleteOldestUntil (int target, Object leave)
    {

        if (Log.lruCheck.isDebugEnabled()) {
            sane(Log.lruCheck);
        }

        if (Log.lruDebug.isDebugEnabled()) {
            Log.lruDebug.debug("%s: LRU table before removeOldestUntil:", name);
            if (Log.lruCheck.isDebugEnabled()) {
                dump(Log.lruCheck);
            }
            else {
                dumpSummary(Log.lruDebug);
                dumpLRUStats(Log.lruDebug);
            }
        }

        int i,j;

        i = this.oldest;
        int limit = this.inUse;
        List values = ListUtil.list();
        
        for (j = 0; j < limit ; j++) {

            if (this.inUse == target) {
                break;
            }

            if (i == this.newest) {
                break;
            }
                // Remember the next slot before deleting the one we
                // have.  Record the key so we can check to see in the
                // entry moved as a result of the delete.
            int next = this.ents[i].newer;
            Object nextKey = this.ents[next].key;

                // Callback the value to see if it can be removed.
            Object key = this.ents[i].key;
            Object o = this.ents[i].value;
            if (! objectsAreEqualEnough(key, leave)) {
                if ((removeListener == null) || removeListener.okToRemove(o)) {
                    this.purged++;

                    if (Log.lruPurge.isDebugEnabled()) {
                        Log.lruPurge.debug("Remove %s [%s]", o, i);
                    }

                    this.removeAtIndex(i);
                    values.add(o);
                }
                else {
                    this.unPurged++;
                }
            }

                // Check to see if the next entry moved as a result of
                // the delete.  If it did move, then rehash.
            if (this.ents[next].key != nextKey) {
                next = findEntry(nextKey);
                if (this.ents[next].key == null) {
                    Log.util.error(2760, nextKey, i, next);
                    break;
                }
            }

            i = next;
        }

        if (Log.lruCheck.isDebugEnabled()) {
            sane(Log.lruCheck);
        }

        if (Log.lruDebug.isDebugEnabled()) {
            Log.lruDebug.debug("%s: LRU table after removeOldestUntil:", name);
            if (Log.lruCheck.isDebugEnabled()) {
                dump(Log.lruCheck);
            }
            else {
                dumpSummary(Log.lruDebug);
            }
        }
        
        return values;
    }


    /*-----------------------------------------------------------------------
        Hashing
      -----------------------------------------------------------------------*/

    /*
        Reduce a 64-bit scramble to a hash table index.
    */
    protected static final long Multiplier = 0x9e3779b9;
    protected static final int  WordSize   = 32;

    protected int firstProbe (Object key)
    {
        int mixed = (int)(getHashValueForObject(key) * Multiplier);
        int reduced = mixed >>> (WordSize - this.logSize);
        return reduced;
    }


    /*-----------------------------------------------------------------------
        Enumeration
      -----------------------------------------------------------------------*/

    /*
        Return a freshly allocated array of all the keys in the table.
    */
    public final Object[] allKeys ()
    {
        Object[] keys = new Object[this.inUse];

        int i = this.newest;
        for (int j = 0; j < this.inUse; j++) {
            Object key = this.ents[i].key;
            keys[j] = key;
            if (i == this.oldest) {
                break;
            }
            i = this.ents[i].older;
        }

        return keys;
    }

    /*
        Return a freshly allocated array of all the values in the table.
    */
    public final Object[] allValues ()
    {
        Object[] values = new Object[this.inUse];

        int i = this.newest;
        for (int j = 0; j < this.inUse; j++) {
            Object value = this.ents[i].value;
            values[j] = value;
            if (i == this.oldest) {
                break;
            }
            i = this.ents[i].older;
        }

        return values;
    }

    /*
        Low level access for statistics gathering
    */
    public final LRUEntry[] entries ()
    {
        return ents;
    }

    /*
        Add all the keys in the table to the given vector.
    */
    public final void addKeysTo (List v)
    {
        int i = this.newest;
        for (int j = 0; j < this.inUse; j++) {
            Object key = this.ents[i].key;
            v.add(key);
            if (i == this.oldest) {
                break;
            }
            i = this.ents[i].older;
        }
    }

    /*
        Add all the values in the table to the given vector.
    */
    public final void addValuesTo (List v)
    {
        int i = this.newest;
        for (int j = 0; j < this.inUse; j++) {
            Object value = this.ents[i].value;
            v.add(value);
            if (i == this.oldest) {
                break;
            }
            i = this.ents[i].older;
        }
    }
    /*-----------------------------------------------------------------------
        Debugging and statistics
      -----------------------------------------------------------------------*/

    /**
        Write a summary of the table's key attributes.
    */
    public final void dumpSummary (Logger log)
    {
        log.debug("%s: newest: %s, oldest %s, allocated %s, inuse %s",
                  name,
                  Constants.getInteger(newest),
                  Constants.getInteger(oldest),
                  Constants.getInteger(allocated),
                  Constants.getInteger(inUse));

        log.debug("%s: purgeTrigger: %s, purged: %s, unpurged: %s",
                  name,
                  Constants.getInteger(purgeTrigger),
                  Constants.getInteger(purged),
                  Constants.getInteger(unPurged));
    }

    /**
        Write LRU statistics.
    */
    public final void dumpLRUStats (Logger log)
    {
        double lruHitRate = this.lookups == 0 ? 0.0 :
            100.0 * (double)this.hits / (double)this.lookups;

        log.debug("%s: hit rate %s%%, maxTableSize %s; lookups %s",
                  name,
                  Double.toString(lruHitRate),
                  Constants.getInteger(maxTableSize),
                  Constants.getInteger(lookups));

        log.debug("%s: probes %s; maxProbes %s; maxProbesAll %s",
                  name,
                  Constants.getInteger(probes),
                  Constants.getInteger(maxProbes),
                  Constants.getInteger(maxProbesAll));
    }


    /**
        Print every slot in the table, even slots not currently
        occupied, to the log.
    */
    public final void dump (Logger log)
    {
        dumpLRUStats(log);
        dumpSummary(log);

        for (int i = 0; i < this.allocated; i++) {
            String key;
            String value;
            if (this.ents[i].key == null) {
                key ="none";
                value = "none";
            }
            else {
                key = this.ents[i].key.toString();
                value = this.ents[i].value.toString();
            }

            log.debug("%5s: %20s %5s %5s -- %s",
                      Constants.getInteger(i),
                      value,
                      Constants.getInteger(this.ents[i].newer),
                      Constants.getInteger(this.ents[i].older),
                      key);
        }

        dumpInOrder(log, 1000);
    }

    /**
        Print the first LIMIT entries in the table from newest to
        oldest.
    */
    public final void dumpInOrder (Logger log, int limit)
    {
        log.debug("Newest to oldest:");
        int i = this.newest;
        int j;
        for (j = 0; j < Math.min(this.inUse, limit); j++) {
            String value;
            if (this.ents[i].key == null) {
                value = "none";
            }
            else {
                value = this.ents[i].value.toString();
            }

            log.debug("%5s: %20s %5s %5s",
                      Constants.getInteger(i),
                      value,
                      Constants.getInteger(this.ents[i].newer),
                      Constants.getInteger(this.ents[i].older));
            if (i == this.oldest) {
                break;
            }
            i = this.ents[i].older;
        }

        if ((this.inUse > 0) && (this.inUse <= limit)) {
            if (j < this.inUse - 1) {
                Assert.that(
                    false, "LRUHashtable.dumpInOrder: got to oldest too soon.");
            }
            else if (j ==  this.inUse - 1) {
                    // ok
            }
            else if (j ==  this.inUse) {
                Assert.that(
                    false, "LRUHashtable.dumpInOrder: never got to oldest.");
            }
        }
    }


    /**
        Perform a simple-minded check that the LRU state is consistent.
        Start at newest and scan toward oldest.  For a table with N
        entries in use, after scanning N entries, we should be looking at
        the oldest entry.  If not, something is wrong.
    */
    public final void sane (Logger log)
    {
        int i, j;

        i = this.newest;
        for (j = 0; j < this.inUse; j++) {
            if (i == this.oldest) {
                break;
            }
            i = this.ents[i].older;
        }

        if (this.inUse > 0) {
            if (j < this.inUse - 1) {
                dump(log);
                Assert.that(
                    false, "LRUHashtable.sane: got to oldest too soon.");
            }
            else if (j == this.inUse - 1) {
                    // ok
            }
            else if (j == this.inUse) {
                dump(log);
                Assert.that(false, "LRUHashtable.sane: never got to oldest.");
            }
        }
    }

    /**
        Helper function that returns the appropriate hash code for the
        object <b>o</b>. Unless overriden by a subclass, this will
        return the object's hashCode().
    */
    protected int getHashValueForObject (Object o)
    {
        return o.hashCode();
    }

    /**
        Helper function to determine if two objects are equal. This
        method is overriden for different types of hash tables. Unless
        overriden by a subclass, it returns true if <b>obj1</b> and
        <b>obj2</b> compare as equal using the equals() method on
        <b>obj1</b>
    */
    protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
    {
            // Looking at the implementation of visualcafe 1.1 and sun
            // String.equals() isn't smart enough to avoid full
            // compares when strings are pointer eq. (HP does)
        if (obj1 == obj2) {
            return true;
        }
        return obj1.equals(obj2);
    }
}

