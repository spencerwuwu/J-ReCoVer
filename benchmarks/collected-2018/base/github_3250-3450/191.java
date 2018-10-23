// https://searchcode.com/api/result/15622885/

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

// Snapshot Tue Jun  5 14:56:09 2012  Doug Lea  (dl at altair)

package jsr166e;
import jsr166e.LongAdder;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import jsr166y.ThreadLocalRandom;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.io.Serializable;

/**
 * A hash table supporting full concurrency of retrievals and
 * high expected concurrency for updates. This class obeys the
 * same functional specification as {@link java.util.Hashtable}, and
 * includes versions of methods corresponding to each method of
 * {@code Hashtable}. However, even though all operations are
 * thread-safe, retrieval operations do <em>not</em> entail locking,
 * and there is <em>not</em> any support for locking the entire table
 * in a way that prevents all access.  This class is fully
 * interoperable with {@code Hashtable} in programs that rely on its
 * thread safety but not on its synchronization details.
 *
 * <p> Retrieval operations (including {@code get}) generally do not
 * block, so may overlap with update operations (including {@code put}
 * and {@code remove}). Retrievals reflect the results of the most
 * recently <em>completed</em> update operations holding upon their
 * onset.  For aggregate operations such as {@code putAll} and {@code
 * clear}, concurrent retrievals may reflect insertion or removal of
 * only some entries.  Similarly, Iterators and Enumerations return
 * elements reflecting the state of the hash table at some point at or
 * since the creation of the iterator/enumeration.  They do
 * <em>not</em> throw {@link ConcurrentModificationException}.
 * However, iterators are designed to be used by only one thread at a
 * time.  Bear in mind that the results of aggregate status methods
 * including {@code size}, {@code isEmpty}, and {@code containsValue}
 * are typically useful only when a map is not undergoing concurrent
 * updates in other threads.  Otherwise the results of these methods
 * reflect transient states that may be adequate for monitoring
 * or estimation purposes, but not for program control.
 *
 * <p> The table is dynamically expanded when there are too many
 * collisions (i.e., keys that have distinct hash codes but fall into
 * the same slot modulo the table size), with the expected average
 * effect of maintaining roughly two bins per mapping (corresponding
 * to a 0.75 load factor threshold for resizing). There may be much
 * variance around this average as mappings are added and removed, but
 * overall, this maintains a commonly accepted time/space tradeoff for
 * hash tables.  However, resizing this or any other kind of hash
 * table may be a relatively slow operation. When possible, it is a
 * good idea to provide a size estimate as an optional {@code
 * initialCapacity} constructor argument. An additional optional
 * {@code loadFactor} constructor argument provides a further means of
 * customizing initial table capacity by specifying the table density
 * to be used in calculating the amount of space to allocate for the
 * given number of elements.  Also, for compatibility with previous
 * versions of this class, constructors may optionally specify an
 * expected {@code concurrencyLevel} as an additional hint for
 * internal sizing.  Note that using many keys with exactly the same
 * {@code hashCode()} is a sure way to slow down performance of any
 * hash table.
 *
 * <p>This class and its views and iterators implement all of the
 * <em>optional</em> methods of the {@link Map} and {@link Iterator}
 * interfaces.
 *
 * <p> Like {@link Hashtable} but unlike {@link HashMap}, this class
 * does <em>not</em> allow {@code null} to be used as a key or value.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * <p><em>jsr166e note: This class is a candidate replacement for
 * java.util.concurrent.ConcurrentHashMap.<em>
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentHashMapV8<K, V>
        implements ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;

    /**
     * A function computing a mapping from the given key to a value.
     * This is a place-holder for an upcoming JDK8 interface.
     */
    public static interface MappingFunction<K, V> {
        /**
         * Returns a non-null value for the given key.
         *
         * @param key the (non-null) key
         * @return a non-null value
         */
        V map(K key);
    }

    /**
     * A function computing a new mapping given a key and its current
     * mapped value (or {@code null} if there is no current
     * mapping). This is a place-holder for an upcoming JDK8
     * interface.
     */
    public static interface RemappingFunction<K, V> {
        /**
         * Returns a new value given a key and its current value.
         *
         * @param key the (non-null) key
         * @param value the current value, or null if there is no mapping
         * @return a non-null value
         */
        V remap(K key, V value);
    }

    /*
     * Overview:
     *
     * The primary design goal of this hash table is to maintain
     * concurrent readability (typically method get(), but also
     * iterators and related methods) while minimizing update
     * contention. Secondary goals are to keep space consumption about
     * the same or better than java.util.HashMap, and to support high
     * initial insertion rates on an empty table by many threads.
     *
     * Each key-value mapping is held in a Node.  Because Node fields
     * can contain special values, they are defined using plain Object
     * types. Similarly in turn, all internal methods that use them
     * work off Object types. And similarly, so do the internal
     * methods of auxiliary iterator and view classes.  All public
     * generic typed methods relay in/out of these internal methods,
     * supplying null-checks and casts as needed. This also allows
     * many of the public methods to be factored into a smaller number
     * of internal methods (although sadly not so for the five
     * variants of put-related operations). The validation-based
     * approach explained below leads to a lot of code sprawl because
     * retry-control precludes factoring into smaller methods.
     *
     * The table is lazily initialized to a power-of-two size upon the
     * first insertion.  Each bin in the table normally contains a
     * list of Nodes (most often, the list has only zero or one Node).
     * Table accesses require volatile/atomic reads, writes, and
     * CASes.  Because there is no other way to arrange this without
     * adding further indirections, we use intrinsics
     * (sun.misc.Unsafe) operations.  The lists of nodes within bins
     * are always accurately traversable under volatile reads, so long
     * as lookups check hash code and non-nullness of value before
     * checking key equality.
     *
     * We use the top two bits of Node hash fields for control
     * purposes -- they are available anyway because of addressing
     * constraints.  As explained further below, these top bits are
     * used as follows:
     *  00 - Normal
     *  01 - Locked
     *  11 - Locked and may have a thread waiting for lock
     *  10 - Node is a forwarding node
     *
     * The lower 30 bits of each Node's hash field contain a
     * transformation of the key's hash code, except for forwarding
     * nodes, for which the lower bits are zero (and so always have
     * hash field == MOVED).
     *
     * Insertion (via put or its variants) of the first node in an
     * empty bin is performed by just CASing it to the bin.  This is
     * by far the most common case for put operations under most
     * key/hash distributions.  Other update operations (insert,
     * delete, and replace) require locks.  We do not want to waste
     * the space required to associate a distinct lock object with
     * each bin, so instead use the first node of a bin list itself as
     * a lock. Blocking support for these locks relies on the builtin
     * "synchronized" monitors.  However, we also need a tryLock
     * construction, so we overlay these by using bits of the Node
     * hash field for lock control (see above), and so normally use
     * builtin monitors only for blocking and signalling using
     * wait/notifyAll constructions. See Node.tryAwaitLock.
     *
     * Using the first node of a list as a lock does not by itself
     * suffice though: When a node is locked, any update must first
     * validate that it is still the first node after locking it, and
     * retry if not. Because new nodes are always appended to lists,
     * once a node is first in a bin, it remains first until deleted
     * or the bin becomes invalidated (upon resizing).  However,
     * operations that only conditionally update may inspect nodes
     * until the point of update. This is a converse of sorts to the
     * lazy locking technique described by Herlihy & Shavit.
     *
     * The main disadvantage of per-bin locks is that other update
     * operations on other nodes in a bin list protected by the same
     * lock can stall, for example when user equals() or mapping
     * functions take a long time.  However, statistically, under
     * random hash codes, this is not a common problem.  Ideally, the
     * frequency of nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average, given the resizing threshold
     * of 0.75, although with a large variance because of resizing
     * granularity. Ignoring variance, the expected occurrences of
     * list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The
     * first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * Lock contention probability for two threads accessing distinct
     * elements is roughly 1 / (8 * #elements) under random hashes.
     *
     * Actual hash code distributions encountered in practice
     * sometimes deviate significantly from uniform randomness.  This
     * includes the case when N > (1<<30), so some keys MUST collide.
     * Similarly for dumb or hostile usages in which multiple keys are
     * designed to have identical hash codes. Also, although we guard
     * against the worst effects of this (see method spread), sets of
     * hashes may differ only in bits that do not impact their bin
     * index for a given power-of-two mask.  So we use a secondary
     * strategy that applies when the number of nodes in a bin exceeds
     * a threshold, and at least one of the keys implements
     * Comparable.  These TreeBins use a balanced tree to hold nodes
     * (a specialized form of red-black trees), bounding search time
     * to O(log N).  Each search step in a TreeBin is around twice as
     * slow as in a regular list, but given that N cannot exceed
     * (1<<64) (before running out of addresses) this bounds search
     * steps, lock hold times, etc, to reasonable constants (roughly
     * 100 nodes inspected per operation worst case) so long as keys
     * are Comparable (which is very common -- String, Long, etc).
     * TreeBin nodes (TreeNodes) also maintain the same "next"
     * traversal pointers as regular nodes, so can be traversed in
     * iterators in the same way.
     *
     * The table is resized when occupancy exceeds a percentage
     * threshold (nominally, 0.75, but see below).  Only a single
     * thread performs the resize (using field "sizeCtl", to arrange
     * exclusion), but the table otherwise remains usable for reads
     * and updates. Resizing proceeds by transferring bins, one by
     * one, from the table to the next table.  Because we are using
     * power-of-two expansion, the elements from each bin must either
     * stay at same index, or move with a power of two offset. We
     * eliminate unnecessary node creation by catching cases where old
     * nodes can be reused because their next fields won't change.  On
     * average, only about one-sixth of them need cloning when a table
     * doubles. The nodes they replace will be garbage collectable as
     * soon as they are no longer referenced by any reader thread that
     * may be in the midst of concurrently traversing table.  Upon
     * transfer, the old table bin contains only a special forwarding
     * node (with hash field "MOVED") that contains the next table as
     * its key. On encountering a forwarding node, access and update
     * operations restart, using the new table.
     *
     * Each bin transfer requires its bin lock. However, unlike other
     * cases, a transfer can skip a bin if it fails to acquire its
     * lock, and revisit it later (unless it is a TreeBin). Method
     * rebuild maintains a buffer of TRANSFER_BUFFER_SIZE bins that
     * have been skipped because of failure to acquire a lock, and
     * blocks only if none are available (i.e., only very rarely).
     * The transfer operation must also ensure that all accessible
     * bins in both the old and new table are usable by any traversal.
     * When there are no lock acquisition failures, this is arranged
     * simply by proceeding from the last bin (table.length - 1) up
     * towards the first.  Upon seeing a forwarding node, traversals
     * (see class InternalIterator) arrange to move to the new table
     * without revisiting nodes.  However, when any node is skipped
     * during a transfer, all earlier table bins may have become
     * visible, so are initialized with a reverse-forwarding node back
     * to the old table until the new ones are established. (This
     * sometimes requires transiently locking a forwarding node, which
     * is possible under the above encoding.) These more expensive
     * mechanics trigger only when necessary.
     *
     * The traversal scheme also applies to partial traversals of
     * ranges of bins (via an alternate InternalIterator constructor)
     * to support partitioned aggregate operations (that are not
     * otherwise implemented yet).  Also, read-only operations give up
     * if ever forwarded to a null table, which provides support for
     * shutdown-style clearing, which is also not currently
     * implemented.
     *
     * Lazy table initialization minimizes footprint until first use,
     * and also avoids resizings when the first operation is from a
     * putAll, constructor with map argument, or deserialization.
     * These cases attempt to override the initial capacity settings,
     * but harmlessly fail to take effect in cases of races.
     *
     * The element count is maintained using a LongAdder, which avoids
     * contention on updates but can encounter cache thrashing if read
     * too frequently during concurrent access. To avoid reading so
     * often, resizing is attempted either when a bin lock is
     * contended, or upon adding to a bin already holding two or more
     * nodes (checked before adding in the xIfAbsent methods, after
     * adding in others). Under uniform hash distributions, the
     * probability of this occurring at threshold is around 13%,
     * meaning that only about 1 in 8 puts check threshold (and after
     * resizing, many fewer do so). But this approximation has high
     * variance for small table sizes, so we check on any collision
     * for sizes <= 64. The bulk putAll operation further reduces
     * contention by only committing count updates upon these size
     * checks.
     *
     * Maintaining API and serialization compatibility with previous
     * versions of this class introduces several oddities. Mainly: We
     * leave untouched but unused constructor arguments refering to
     * concurrencyLevel. We accept a loadFactor constructor argument,
     * but apply it only to initial table capacity (which is the only
     * time that we can guarantee to honor it.) We also declare an
     * unused "Segment" class that is instantiated in minimal form
     * only when serializing.
     */

    /* ---------------- Constants -------------- */

    /**
     * The largest possible table capacity.  This value must be
     * exactly 1<<30 to stay within Java array allocation and indexing
     * bounds for power of two table sizes, and is further required
     * because the top two bits of 32bit hash fields are used for
     * control purposes.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The default initial table capacity.  Must be a power of 2
     * (i.e., at least 1) and at most MAXIMUM_CAPACITY.
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The largest possible (non-power of two) array size.
     * Needed by toArray and related methods.
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The default concurrency level for this table. Unused but
     * defined for compatibility with previous versions of this class.
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * The load factor for this table. Overrides of this value in
     * constructors affect only the initial table capacity.  The
     * actual floating point value isn't normally used -- it is
     * simpler to use expressions such as {@code n - (n >>> 2)} for
     * the associated resizing threshold.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * The buffer size for skipped bins during transfers. The
     * value is arbitrary but should be large enough to avoid
     * most locking stalls during resizes.
     */
    private static final int TRANSFER_BUFFER_SIZE = 32;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  The value reflects the approximate break-even point for
     * using tree-based operations.
     */
    private static final int TREE_THRESHOLD = 8;

    /*
     * Encodings for special uses of Node hash fields. See above for
     * explanation.
     */
    static final int MOVED     = 0x80000000; // hash field for forwarding nodes
    static final int LOCKED    = 0x40000000; // set/tested only as a bit
    static final int WAITING   = 0xc0000000; // both bits set/tested together
    static final int HASH_BITS = 0x3fffffff; // usable bits of normal node hash

    /* ---------------- Fields -------------- */

    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     */
    transient volatile Node[] table;

    /**
     * The counter maintaining number of elements.
     */
    private transient final LongAdder counter;

    /**
     * Table initialization and resizing control.  When negative, the
     * table is being initialized or resized. Otherwise, when table is
     * null, holds the initial table size to use upon creation, or 0
     * for default. After initialization, holds the next element count
     * value upon which to resize the table.
     */
    private transient volatile int sizeCtl;

    // views
    private transient KeySet<K,V> keySet;
    private transient Values<K,V> values;
    private transient EntrySet<K,V> entrySet;

    /** For serialization compatibility. Null unless serialized; see below */
    private Segment<K,V>[] segments;

    /* ---------------- Table element access -------------- */

    /*
     * Volatile access methods are used for table elements as well as
     * elements of in-progress next table while resizing.  Uses are
     * null checked by callers, and implicitly bounds-checked, relying
     * on the invariants that tab arrays have non-zero size, and all
     * indices are masked with (tab.length - 1) which is never
     * negative and always less than length. Note that, to be correct
     * wrt arbitrary concurrency errors by users, bounds checks must
     * operate on local variables, which accounts for some odd-looking
     * inline assignments below.
     */

    static final Node tabAt(Node[] tab, int i) { // used by InternalIterator
        return (Node)UNSAFE.getObjectVolatile(tab, ((long)i<<ASHIFT)+ABASE);
    }

    private static final boolean casTabAt(Node[] tab, int i, Node c, Node v) {
        return UNSAFE.compareAndSwapObject(tab, ((long)i<<ASHIFT)+ABASE, c, v);
    }

    private static final void setTabAt(Node[] tab, int i, Node v) {
        UNSAFE.putObjectVolatile(tab, ((long)i<<ASHIFT)+ABASE, v);
    }

    /* ---------------- Nodes -------------- */

    /**
     * Key-value entry. Note that this is never exported out as a
     * user-visible Map.Entry (see WriteThroughEntry and SnapshotEntry
     * below). Nodes with a hash field of MOVED are special, and do
     * not contain user keys or values.  Otherwise, keys are never
     * null, and null val fields indicate that a node is in the
     * process of being deleted or created. For purposes of read-only
     * access, a key may be read before a val, but can only be used
     * after checking val to be non-null.
     */
    static class Node {
        volatile int hash;
        final Object key;
        volatile Object val;
        volatile Node next;

        Node(int hash, Object key, Object val, Node next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        /** CompareAndSet the hash field */
        final boolean casHash(int cmp, int val) {
            return UNSAFE.compareAndSwapInt(this, hashOffset, cmp, val);
        }

        /** The number of spins before blocking for a lock */
        static final int MAX_SPINS =
            Runtime.getRuntime().availableProcessors() > 1 ? 64 : 1;

        /**
         * Spins a while if LOCKED bit set and this node is the first
         * of its bin, and then sets WAITING bits on hash field and
         * blocks (once) if they are still set.  It is OK for this
         * method to return even if lock is not available upon exit,
         * which enables these simple single-wait mechanics.
         *
         * The corresponding signalling operation is performed within
         * callers: Upon detecting that WAITING has been set when
         * unlocking lock (via a failed CAS from non-waiting LOCKED
         * state), unlockers acquire the sync lock and perform a
         * notifyAll.
         */
        final void tryAwaitLock(Node[] tab, int i) {
            if (tab != null && i >= 0 && i < tab.length) { // bounds check
                int r = ThreadLocalRandom.current().nextInt(); // randomize spins
                int spins = MAX_SPINS, h;
                while (tabAt(tab, i) == this && ((h = hash) & LOCKED) != 0) {
                    if (spins >= 0) {
                        r ^= r << 1; r ^= r >>> 3; r ^= r << 10; // xorshift
                        if (r >= 0 && --spins == 0)
                            Thread.yield();  // yield before block
                    }
                    else if (casHash(h, h | WAITING)) {
                        synchronized (this) {
                            if (tabAt(tab, i) == this &&
                                (hash & WAITING) == WAITING) {
                                try {
                                    wait();
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            else
                                notifyAll(); // possibly won race vs signaller
                        }
                        break;
                    }
                }
            }
        }

        // Unsafe mechanics for casHash
        private static final sun.misc.Unsafe UNSAFE;
        private static final long hashOffset;

        static {
            try {
                UNSAFE = getUnsafe();
                Class<?> k = Node.class;
                hashOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("hash"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- TreeBins -------------- */

    /**
     * Nodes for use in TreeBins
     */
    static final class TreeNode extends Node {
        TreeNode parent;  // red-black tree links
        TreeNode left;
        TreeNode right;
        TreeNode prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, Object key, Object val, Node next, TreeNode parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }
    }

    /**
     * A specialized form of red-black tree for use in bins
     * whose size exceeds a threshold.
     *
     * TreeBins use a special form of comparison for search and
     * related operations (which is the main reason we cannot use
     * existing collections such as TreeMaps). TreeBins contain
     * Comparable elements, but may contain others, as well as
     * elements that are Comparable but not necessarily Comparable<T>
     * for the same T, so we cannot invoke compareTo among them. To
     * handle this, the tree is ordered primarily by hash value, then
     * by getClass().getName() order, and then by Comparator order
     * among elements of the same class.  On lookup at a node, if
     * non-Comparable, both left and right children may need to be
     * searched in the case of tied hash values. (This corresponds to
     * the full list search that would be necessary if all elements
     * were non-Comparable and had tied hashes.)
     *
     * TreeBins also maintain a separate locking discipline than
     * regular bins. Because they are forwarded via special MOVED
     * nodes at bin heads (which can never change once established),
     * we cannot use use those nodes as locks. Instead, TreeBin
     * extends AbstractQueuedSynchronizer to support a simple form of
     * read-write lock. For update operations and table validation,
     * the exclusive form of lock behaves in the same way as bin-head
     * locks. However, lookups use shared read-lock mechanics to allow
     * multiple readers in the absence of writers.  Additionally,
     * these lookups do not ever block: While the lock is not
     * available, they proceed along the slow traversal path (via
     * next-pointers) until the lock becomes available or the list is
     * exhausted, whichever comes first. (These cases are not fast,
     * but maximize aggregate expected throughput.)  The AQS mechanics
     * for doing this are straightforward.  The lock state is held as
     * AQS getState().  Read counts are negative; the write count (1)
     * is positive.  There are no signalling preferences among readers
     * and writers. Since we don't need to export full Lock API, we
     * just override the minimal AQS methods and use them directly.
     */
    static final class TreeBin extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 2249069246763182397L;
        TreeNode root;  // root of tree
        TreeNode first; // head of next-pointer list

        /* AQS overrides */
        public final boolean isHeldExclusively() { return getState() > 0; }
        public final boolean tryAcquire(int ignore) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
        public final boolean tryRelease(int ignore) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
        public final int tryAcquireShared(int ignore) {
            for (int c;;) {
                if ((c = getState()) > 0)
                    return -1;
                if (compareAndSetState(c, c -1))
                    return 1;
            }
        }
        public final boolean tryReleaseShared(int ignore) {
            int c;
            do {} while (!compareAndSetState(c = getState(), c + 1));
            return c == -1;
        }

        /**
         * Return the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        @SuppressWarnings("unchecked") // suppress Comparable cast warning
        final TreeNode getTreeNode(int h, Object k, TreeNode p) {
            Class<?> c = k.getClass();
            while (p != null) {
                int dir, ph;  Object pk; Class<?> pc; TreeNode r;
                if (h < (ph = p.hash))
                    dir = -1;
                else if (h > ph)
                    dir = 1;
                else if ((pk = p.key) == k || k.equals(pk))
                    return p;
                else if (c != (pc = pk.getClass()))
                    dir = c.getName().compareTo(pc.getName());
                else if (k instanceof Comparable)
                    dir = ((Comparable)k).compareTo((Comparable)pk);
                else
                    dir = 0;
                TreeNode pr = p.right;
                if (dir > 0)
                    p = pr;
                else if (dir == 0 && pr != null && h >= pr.hash &&
                         (r = getTreeNode(h, k, pr)) != null)
                    return r;
                else
                    p = p.left;
            }
            return null;
        }

        /**
         * Wrapper for getTreeNode used by CHM.get. Tries to obtain
         * read-lock to call getTreeNode, but during failure to get
         * lock, searches along next links.
         */
        final Object getValue(int h, Object k) {
            Node r = null;
            int c = getState(); // Must read lock state first
            for (Node e = first; e != null; e = e.next) {
                if (c <= 0 && compareAndSetState(c, c - 1)) {
                    try {
                        r = getTreeNode(h, k, root);
                    } finally {
                        releaseShared(0);
                    }
                    break;
                }
                else if ((e.hash & HASH_BITS) == h && k.equals(e.key)) {
                    r = e;
                    break;
                }
                else
                    c = getState();
            }
            return r == null ? null : r.val;
        }

        /**
         * Find or add a node
         * @return null if added
         */
        @SuppressWarnings("unchecked") // suppress Comparable cast warning
        final TreeNode putTreeNode(int h, Object k, Object v) {
            Class<?> c = k.getClass();
            TreeNode p = root;
            int dir = 0;
            if (p != null) {
                for (;;) {
                    int ph;  Object pk; Class<?> pc; TreeNode r;
                    if (h < (ph = p.hash))
                        dir = -1;
                    else if (h > ph)
                        dir = 1;
                    else if ((pk = p.key) == k || k.equals(pk))
                        return p;
                    else if (c != (pc = (pk = p.key).getClass()))
                        dir = c.getName().compareTo(pc.getName());
                    else if (k instanceof Comparable)
                        dir = ((Comparable)k).compareTo((Comparable)pk);
                    else
                        dir = 0;
                    TreeNode pr = p.right, pl;
                    if (dir > 0) {
                        if (pr == null)
                            break;
                        p = pr;
                    }
                    else if (dir == 0 && pr != null && h >= pr.hash &&
                             (r = getTreeNode(h, k, pr)) != null)
                        return r;
                    else if ((pl = p.left) == null)
                        break;
                    else
                        p = pl;
                }
            }
            TreeNode f = first;
            TreeNode r = first = new TreeNode(h, k, v, f, p);
            if (p == null)
                root = r;
            else {
                if (dir <= 0)
                    p.left = r;
                else
                    p.right = r;
                if (f != null)
                    f.prev = r;
                fixAfterInsertion(r);
            }
            return null;
        }

        /**
         * Removes the given node, that must be present before this
         * call.  This is messier than typical red-black deletion code
         * because we cannot swap the contents of an interior node
         * with a leaf successor that is pinned by "next" pointers
         * that are accessible independently of lock. So instead we
         * swap the tree linkages.
         */
        final void deleteTreeNode(TreeNode p) {
            TreeNode next = (TreeNode)p.next; // unlink traversal pointers
            TreeNode pred = p.prev;
            if (pred == null)
                first = next;
            else
                pred.next = next;
            if (next != null)
                next.prev = pred;
            TreeNode replacement;
            TreeNode pl = p.left;
            TreeNode pr = p.right;
            if (pl != null && pr != null) {
                TreeNode s = pr;
                while (s.left != null) // find successor
                    s = s.left;
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                TreeNode sr = s.right;
                TreeNode pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                }
                else {
                    TreeNode sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                replacement = sr;
            }
            else
                replacement = (pl != null) ? pl : pr;
            TreeNode pp = p.parent;
            if (replacement == null) {
                if (pp == null) {
                    root = null;
                    return;
                }
                replacement = p;
            }
            else {
                replacement.parent = pp;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }
            if (!p.red)
                fixAfterDeletion(replacement);
            if (p == replacement && (pp = p.parent) != null) {
                if (p == pp.left) // detach pointers
                    pp.left = null;
                else if (p == pp.right)
                    pp.right = null;
                p.parent = null;
            }
        }

        // CLR code updated from pre-jdk-collections version at
        // http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java

        /** From CLR */
        private void rotateLeft(TreeNode p) {
            if (p != null) {
                TreeNode r = p.right, pp, rl;
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    root = r;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
        }

        /** From CLR */
        private void rotateRight(TreeNode p) {
            if (p != null) {
                TreeNode l = p.left, pp, lr;
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    root = l;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
        }

        /** From CLR */
        private void fixAfterInsertion(TreeNode x) {
            x.red = true;
            TreeNode xp, xpp;
            while (x != null && (xp = x.parent) != null && xp.red &&
                   (xpp = xp.parent) != null) {
                TreeNode xppl = xpp.left;
                if (xp == xppl) {
                    TreeNode y = xpp.right;
                    if (y != null && y.red) {
                        y.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.right) {
                            x = xp;
                            rotateLeft(x);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                rotateRight(xpp);
                            }
                        }
                    }
                }
                else {
                    TreeNode y = xppl;
                    if (y != null && y.red) {
                        y.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.left) {
                            x = xp;
                            rotateRight(x);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                rotateLeft(xpp);
                            }
                        }
                    }
                }
            }
            TreeNode r = root;
            if (r != null && r.red)
                r.red = false;
        }

        /** From CLR */
        private void fixAfterDeletion(TreeNode x) {
            while (x != null) {
                TreeNode xp, xpl;
                if (x.red || (xp = x.parent) == null) {
                    x.red = false;
                    break;
                }
                if (x == (xpl = xp.left)) {
                    TreeNode sib = xp.right;
                    if (sib != null && sib.red) {
                        sib.red = false;
                        xp.red = true;
                        rotateLeft(xp);
                        sib = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (sib == null)
                        x = xp;
                    else {
                        TreeNode sl = sib.left, sr = sib.right;
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) {
                            sib.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                sib.red = true;
                                rotateRight(sib);
                                sib = (xp = x.parent) == null ? null : xp.right;
                            }
                            if (sib != null) {
                                sib.red = (xp == null) ? false : xp.red;
                                if ((sr = sib.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                rotateLeft(xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric
                    TreeNode sib = xpl;
                    if (sib != null && sib.red) {
                        sib.red = false;
                        xp.red = true;
                        rotateRight(xp);
                        sib = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (sib == null)
                        x = xp;
                    else {
                        TreeNode sl = sib.left, sr = sib.right;
                        if ((sl == null || !sl.red) &&
                            (sr == null || !sr.red)) {
                            sib.red = true;
                            x = xp;
                        }
                        else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                sib.red = true;
                                rotateLeft(sib);
                                sib = (xp = x.parent) == null ? null : xp.left;
                            }
                            if (sib != null) {
                                sib.red = (xp == null) ? false : xp.red;
                                if ((sl = sib.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                rotateRight(xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Collision reduction methods -------------- */

    /**
     * Spreads higher bits to lower, and also forces top 2 bits to 0.
     * Because the table uses power-of-two masking, sets of hashes
     * that vary only in bits above the current mask will always
     * collide. (Among known examples are sets of Float keys holding
     * consecutive whole numbers in small tables.)  To counter this,
     * we apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed across bits (so don't benefit
     * from spreading), and because we use trees to handle large sets
     * of collisions in bins, we don't need excessively high quality.
     */
    private static final int spread(int h) {
        h ^= (h >>> 18) ^ (h >>> 12);
        return (h ^ (h >>> 10)) & HASH_BITS;
    }

    /**
     * Replaces a list bin with a tree bin. Call only when locked.
     * Fails to replace if the given key is non-comparable or table
     * is, or needs, resizing.
     */
    private final void replaceWithTreeBin(Node[] tab, int index, Object key) {
        if ((key instanceof Comparable) &&
            (tab.length >= MAXIMUM_CAPACITY || counter.sum() < (long)sizeCtl)) {
            TreeBin t = new TreeBin();
            for (Node e = tabAt(tab, index); e != null; e = e.next)
                t.putTreeNode(e.hash & HASH_BITS, e.key, e.val);
            setTabAt(tab, index, new Node(MOVED, t, null, null));
        }
    }

    /* ---------------- Internal access and update methods -------------- */

    /** Implementation for get and containsKey */
    private final Object internalGet(Object k) {
        int h = spread(k.hashCode());
        retry: for (Node[] tab = table; tab != null;) {
            Node e, p; Object ek, ev; int eh;      // locals to read fields once
            for (e = tabAt(tab, (tab.length - 1) & h); e != null; e = e.next) {
                if ((eh = e.hash) == MOVED) {
                    if ((ek = e.key) instanceof TreeBin)  // search TreeBin
                        return ((TreeBin)ek).getValue(h, k);
                    else {                        // restart with new table
                        tab = (Node[])ek;
                        continue retry;
                    }
                }
                else if ((eh & HASH_BITS) == h && (ev = e.val) != null &&
                         ((ek = e.key) == k || k.equals(ek)))
                    return ev;
            }
            break;
        }
        return null;
    }

    /**
     * Implementation for the four public remove/replace methods:
     * Replaces node value with v, conditional upon match of cv if
     * non-null.  If resulting value is null, delete.
     */
    private final Object internalReplace(Object k, Object v, Object cv) {
        int h = spread(k.hashCode());
        Object oldVal = null;
        for (Node[] tab = table;;) {
            Node f; int i, fh; Object fk;
            if (tab == null ||
                (f = tabAt(tab, i = (tab.length - 1) & h)) == null)
                break;
            else if ((fh = f.hash) == MOVED) {
                if ((fk = f.key) instanceof TreeBin) {
                    TreeBin t = (TreeBin)fk;
                    boolean validated = false;
                    boolean deleted = false;
                    t.acquire(0);
                    try {
                        if (tabAt(tab, i) == f) {
                            validated = true;
                            TreeNode p = t.getTreeNode(h, k, t.root);
                            if (p != null) {
                                Object pv = p.val;
                                if (cv == null || cv == pv || cv.equals(pv)) {
                                    oldVal = pv;
                                    if ((p.val = v) == null) {
                                        deleted = true;
                                        t.deleteTreeNode(p);
                                    }
                                }
                            }
                        }
                    } finally {
                        t.release(0);
                    }
                    if (validated) {
                        if (deleted)
                            counter.add(-1L);
                        break;
                    }
                }
                else
                    tab = (Node[])fk;
            }
            else if ((fh & HASH_BITS) != h && f.next == null) // precheck
                break;                          // rules out possible existence
            else if ((fh & LOCKED) != 0) {
                checkForResize();               // try resizing if can't get lock
                f.tryAwaitLock(tab, i);
            }
            else if (f.casHash(fh, fh | LOCKED)) {
                boolean validated = false;
                boolean deleted = false;
                try {
                    if (tabAt(tab, i) == f) {
                        validated = true;
                        for (Node e = f, pred = null;;) {
                            Object ek, ev;
                            if ((e.hash & HASH_BITS) == h &&
                                ((ev = e.val) != null) &&
                                ((ek = e.key) == k || k.equals(ek))) {
                                if (cv == null || cv == ev || cv.equals(ev)) {
                                    oldVal = ev;
                                    if ((e.val = v) == null) {
                                        deleted = true;
                                        Node en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)
                                break;
                        }
                    }
                } finally {
                    if (!f.casHash(fh | LOCKED, fh)) {
                        f.hash = fh;
                        synchronized (f) { f.notifyAll(); };
                    }
                }
                if (validated) {
                    if (deleted)
                        counter.add(-1L);
                    break;
                }
            }
        }
        return oldVal;
    }

    /*
     * Internal versions of the five insertion methods, each a
     * little more complicated than the last. All have
     * the same basic structure as the first (internalPut):
     *  1. If table uninitialized, create
     *  2. If bin empty, try to CAS new node
     *  3. If bin stale, use new table
     *  4. if bin converted to TreeBin, validate and relay to TreeBin methods
     *  5. Lock and validate; if valid, scan and add or update
     *
     * The others interweave other checks and/or alternative actions:
     *  * Plain put checks for and performs resize after insertion.
     *  * putIfAbsent prescans for mapping without lock (and fails to add
     *    if present), which also makes pre-emptive resize checks worthwhile.
     *  * computeIfAbsent extends form used in putIfAbsent with additional
     *    mechanics to deal with, calls, potential exceptions and null
     *    returns from function call.
     *  * compute uses the same function-call mechanics, but without
     *    the prescans
     *  * putAll attempts to pre-allocate enough table space
     *    and more lazily performs count updates and checks.
     *
     * Someday when details settle down a bit more, it might be worth
     * some factoring to reduce sprawl.
     */

    /** Implementation for put */
    private final Object internalPut(Object k, Object v) {
        int h = spread(k.hashCode());
        int count = 0;
        for (Node[] tab = table;;) {
            int i; Node f; int fh; Object fk;
            if (tab == null)
                tab = initTable();
            else if ((f = tabAt(tab, i = (tab.length - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node(h, k, v, null)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED) {
                if ((fk = f.key) instanceof TreeBin) {
                    TreeBin t = (TreeBin)fk;
                    Object oldVal = null;
                    t.acquire(0);
                    try {
                        if (tabAt(tab, i) == f) {
                            count = 2;
                            TreeNode p = t.putTreeNode(h, k, v);
                            if (p != null) {
                                oldVal = p.val;
                                p.val = v;
                            }
                        }
                    } finally {
                        t.release(0);
                    }
                    if (count != 0) {
                        if (oldVal != null)
                            return oldVal;
                        break;
                    }
                }
                else
                    tab = (Node[])fk;
            }
            else if ((fh & LOCKED) != 0) {
                checkForResize();
                f.tryAwaitLock(tab, i);
            }
            else if (f.casHash(fh, fh | LOCKED)) {
                Object oldVal = null;
                try {                        // needed in case equals() throws
                    if (tabAt(tab, i) == f) {
                        count = 1;
                        for (Node e = f;; ++count) {
                            Object ek, ev;
                            if ((e.hash & HASH_BITS) == h &&
                                (ev = e.val) != null &&
                                ((ek = e.key) == k || k.equals(ek))) {
                                oldVal = ev;
                                e.val = v;
                                break;
                            }
                            Node last = e;
                            if ((e = e.next) == null) {
                                last.next = new Node(h, k, v, null);
                                if (count >= TREE_THRESHOLD)
                                    replaceWithTreeBin(tab, i, k);
                                break;
                            }
                        }
                    }
                } finally {                  // unlock and signal if needed
                    if (!f.casHash(fh | LOCKED, fh)) {
                        f.hash = fh;
                        synchronized (f) { f.notifyAll(); };
                    }
                }
                if (count != 0) {
                    if (oldVal != null)
                        return oldVal;
                    if (tab.length <= 64)
                        count = 2;
                    break;
                }
            }
        }
        counter.add(1L);
        if (count > 1)
            checkForResize();
        return null;
    }

    /** Implementation for putIfAbsent */
    private final Object internalPutIfAbsent(Object k, Object v) {
        int h = spread(k.hashCode());
        int count = 0;
        for (Node[] tab = table;;) {
            int i; Node f; int fh; Object fk, fv;
            if (tab == null)
                tab = initTable();
            else if ((f = tabAt(tab, i = (tab.length - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node(h, k, v, null)))
                    break;
            }
            else if ((fh = f.hash) == MOVED) {
                if ((fk = f.key) instanceof TreeBin) {
                    TreeBin t = (TreeBin)fk;
                    Object oldVal = null;
                    t.acquire(0);
                    try {
                        if (tabAt(tab, i) == f) {
                            count = 2;
                            TreeNode p = t.putTreeNode(h, k, v);
                            if (p != null)
                                oldVal = p.val;
                        }
                    } finally {
                        t.release(0);
                    }
                    if (count != 0) {
                        if (oldVal != null)
                            return oldVal;
                        break;
                    }
                }
                else
                    tab = (Node[])fk;
            }
            else if ((fh & HASH_BITS) == h && (fv = f.val) != null &&
                     ((fk = f.key) == k || k.equals(fk)))
                return fv;
            else {
                Node g = f.next;
                if (g != null) { // at least 2 nodes -- search and maybe resize
                    for (Node e = g;;) {
                        Object ek, ev;
                        if ((e.hash & HASH_BITS) == h && (ev = e.val) != null &&
                            ((ek = e.key) == k || k.equals(ek)))
                            return ev;
                        if ((e = e.next) == null) {
                            checkForResize();
                            break;
                        }
                    }
                }
                if (((fh = f.hash) & LOCKED) != 0) {
                    checkForResize();
                    f.tryAwaitLock(tab, i);
                }
                else if (tabAt(tab, i) == f && f.casHash(fh, fh | LOCKED)) {
                    Object oldVal = null;
                    try {
                        if (tabAt(tab, i) == f) {
                            count = 1;
                            for (Node e = f;; ++count) {
                                Object ek, ev;
                                if ((e.hash & HASH_BITS) == h &&
                                    (ev = e.val) != null &&
                                    ((ek = e.key) == k || k.equals(ek))) {
                                    oldVal = ev;
                                    break;
                                }
                                Node last = e;
                                if ((e = e.next) == null) {
                                    last.next = new Node(h, k, v, null);
                                    if (count >= TREE_THRESHOLD)
                                        replaceWithTreeBin(tab, i, k);
                                    break;
                                }
                            }
                        }
                    } finally {
                        if (!f.casHash(fh | LOCKED, fh)) {
                            f.hash = fh;
                            synchronized (f) { f.notifyAll(); };
                        }
                    }
                    if (count != 0) {
                        if (oldVal != null)
                            return oldVal;
                        if (tab.length <= 64)
                            count = 2;
                        break;
                    }
                }
            }
        }
        counter.add(1L);
        if (count > 1)
            checkForResize();
        return null;
    }

    /** Implementation for computeIfAbsent */
    private final Object internalComputeIfAbsent(K k,
                                                 MappingFunction<? super K, ?> mf) {
        int h = spread(k.hashCode());
        Object val = null;
        int count = 0;
        for (Node[] tab = table;;) {
            Node f; int i, fh; Object fk, fv;
            if (tab == null)
                tab = initTable();
            else if ((f = tabAt(tab, i = (tab.length - 1) & h)) == null) {
                Node node = new Node(fh = h | LOCKED, k, null, null);
                if (casTabAt(tab, i, null, node)) {
                    count = 1;
                    try {
                        if ((val = mf.map(k)) != null)
                            node.val = val;
                    } finally {
                        if (val == null)
                            setTabAt(tab, i, null);
                        if (!node.casHash(fh, h)) {
                            node.hash = h;
                            synchronized (node) { node.notifyAll(); };
                        }
                    }
                }
                if (count != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED) {
                if ((fk = f.key) instanceof TreeBin) {
                    TreeBin t = (TreeBin)fk;
                    boolean added = false;
                    t.acquire(0);
                    try {
                        if (tabAt(tab, i) == f) {
                            count = 1;
                            TreeNode p = t.getTreeNode(h, k, t.root);
                            if (p != null)
                                val = p.val;
                            else if ((val = mf.map(k)) != null) {
                                added = true;
                                count = 2;
                                t.putTreeNode(h, k, val);
                            }
                        }
                    } finally {
                        t.release(0);
                    }
                    if (count != 0) {
                        if (!added)
                            return val;
                        break;
                    }
                }
                else
                    tab = (Node[])fk;
            }
            else if ((fh & HASH_BITS) == h && (fv = f.val) != null &&
                     ((fk = f.key) == k || k.equals(fk)))
                return fv;
            else {
                Node g = f.next;
                if (g != null) {
                    for (Node e = g;;) {
                        Object ek, ev;
                        if ((e.hash & HASH_BITS) == h && (ev = e.val) != null &&
                            ((ek = e.key) == k || k.equals(ek)))
                            return ev;
                        if ((e = e.next) == null) {
                            checkForResize();
                            break;
                        }
                    }
                }
                if (((fh = f.hash) & LOCKED) != 0) {
                    checkForResize();
                    f.tryAwaitLock(tab, i);
                }
                else if (tabAt(tab, i) == f && f.casHash(fh, fh | LOCKED)) {
                    boolean added = false;
                    try {
                        if (tabAt(tab, i) == f) {
                            count = 1;
                            for (Node e = f;; ++count) {
                                Object ek, ev;
                                if ((e.hash & HASH_BITS) == h &&
                                    (ev = e.val) != null &&
                                    ((ek = e.key) == k || k.equals(ek))) {
                                    val = ev;
                                    break;
                                }
                                Node last = e;
                                if ((e = e.next) == null) {
                                    if ((val = mf.map(k)) != null) {
                                        added = true;
                                        last.next = new Node(h, k, val, null);
                                        if (count >= TREE_THRESHOLD)
                                            replaceWithTreeBin(tab, i, k);
                                    }
                                    break;
                                }
                            }
                        }
                    } finally {
                        if (!f.casHash(fh | LOCKED, fh)) {
                            f.hash = fh;
                            synchronized (f) { f.notifyAll(); };
                        }
                    }
                    if (count != 0) {
                        if (!added)
                            return val;
                        if (tab.length <= 64)
                            count = 2;
                        break;
                    }
                }
            }
        }
        if (val == null)
            throw new NullPointerException();
        counter.add(1L);
        if (count > 1)
            checkForResize();
        return val;
    }

    /** Implementation for compute */
    @SuppressWarnings("unchecked")
    private final Object internalCompute(K k,
                                         RemappingFunction<? super K, V> mf) {
        int h = spread(k.hashCode());
        Object val = null;
        boolean added = false;
        int count = 0;
        for (Node[] tab = table;;) {
            Node f; int i, fh; Object fk;
            if (tab == null)
                tab = initTable();
            else if ((f = tabAt(tab, i = (tab.length - 1) & h)) == null) {
                Node node = new Node(fh = h | LOCKED, k, null, null);
                if (casTabAt(tab, i, null, node)) {
                    try {
                        count = 1;
                        if ((val = mf.remap(k, null)) != null) {
                            node.val = val;
                            added = true;
                        }
                    } finally {
                        if (!added)
                            setTabAt(tab, i, null);
                        if (!node.casHash(fh, h)) {
                            node.hash = h;
                            synchronized (node) { node.notifyAll(); };
                        }
                    }
                }
                if (count != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED) {
                if ((fk = f.key) instanceof TreeBin) {
                    TreeBin t = (TreeBin)fk;
                    t.acquire(0);
                    try {
                        if (tabAt(tab, i) == f) {
      
