// https://searchcode.com/api/result/14380844/

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package akka.jsr166y;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import akka.util.Unsafe;

/**
 * An {@link ExecutorService} for running {@link ForkJoinTask}s.
 * A {@code ForkJoinPool} provides the entry point for submissions
 * from non-{@code ForkJoinTask} clients, as well as management and
 * monitoring operations.
 *
 * <p>A {@code ForkJoinPool} differs from other kinds of {@link
 * ExecutorService} mainly by virtue of employing
 * <em>work-stealing</em>: all threads in the pool attempt to find and
 * execute tasks submitted to the pool and/or created by other active
 * tasks (eventually blocking waiting for work if none exist). This
 * enables efficient processing when most tasks spawn other subtasks
 * (as do most {@code ForkJoinTask}s), as well as when many small
 * tasks are submitted to the pool from external clients.  Especially
 * when setting <em>asyncMode</em> to true in constructors, {@code
 * ForkJoinPool}s may also be appropriate for use with event-style
 * tasks that are never joined.
 *
 * <p>A {@code ForkJoinPool} is constructed with a given target
 * parallelism level; by default, equal to the number of available
 * processors. The pool attempts to maintain enough active (or
 * available) threads by dynamically adding, suspending, or resuming
 * internal worker threads, even if some tasks are stalled waiting to
 * join others. However, no such adjustments are guaranteed in the
 * face of blocked IO or other unmanaged synchronization. The nested
 * {@link ManagedBlocker} interface enables extension of the kinds of
 * synchronization accommodated.
 *
 * <p>In addition to execution and lifecycle control methods, this
 * class provides status check methods (for example
 * {@link #getStealCount}) that are intended to aid in developing,
 * tuning, and monitoring fork/join applications. Also, method
 * {@link #toString} returns indications of pool state in a
 * convenient form for informal monitoring.
 *
 * <p> As is the case with other ExecutorServices, there are three
 * main task execution methods summarized in the following table.
 * These are designed to be used primarily by clients not already
 * engaged in fork/join computations in the current pool.  The main
 * forms of these methods accept instances of {@code ForkJoinTask},
 * but overloaded forms also allow mixed execution of plain {@code
 * Runnable}- or {@code Callable}- based activities as well.  However,
 * tasks that are already executing in a pool should normally instead
 * use the within-computation forms listed in the table unless using
 * async event-style tasks that are not usually joined, in which case
 * there is little difference among choice of methods.
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER> <b>Call from non-fork/join clients</b></td>
 *    <td ALIGN=CENTER> <b>Call from within fork/join computations</b></td>
 *  </tr>
 *  <tr>
 *    <td> <b>Arrange async execution</td>
 *    <td> {@link #execute(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork}</td>
 *  </tr>
 *  <tr>
 *    <td> <b>Await and obtain result</td>
 *    <td> {@link #invoke(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#invoke}</td>
 *  </tr>
 *  <tr>
 *    <td> <b>Arrange exec and obtain Future</td>
 *    <td> {@link #submit(ForkJoinTask)}</td>
 *    <td> {@link ForkJoinTask#fork} (ForkJoinTasks <em>are</em> Futures)</td>
 *  </tr>
 * </table>
 *
 * <p><b>Sample Usage.</b> Normally a single {@code ForkJoinPool} is
 * used for all parallel task execution in a program or subsystem.
 * Otherwise, use would not usually outweigh the construction and
 * bookkeeping overhead of creating a large set of threads. For
 * example, a common pool could be used for the {@code SortTasks}
 * illustrated in {@link RecursiveAction}. Because {@code
 * ForkJoinPool} uses threads in {@linkplain java.lang.Thread#isDaemon
 * daemon} mode, there is typically no need to explicitly {@link
 * #shutdown} such a pool upon program exit.
 *
 *  <pre> {@code
 * static final ForkJoinPool mainPool = new ForkJoinPool();
 * ...
 * public void sort(long[] array) {
 *   mainPool.invoke(new SortTask(array, 0, array.length));
 * }}</pre>
 *
 * <p><b>Implementation notes</b>: This implementation restricts the
 * maximum number of running threads to 32767. Attempts to create
 * pools with greater than the maximum number result in
 * {@code IllegalArgumentException}.
 *
 * <p>This implementation rejects submitted tasks (that is, by throwing
 * {@link RejectedExecutionException}) only when the pool is shut down
 * or internal resources have been exhausted.
 *
 * @since 1.7
 * @author Doug Lea
 */
public class ForkJoinPool extends AbstractExecutorService {

    /*
     * Implementation Overview
     *
     * This class and its nested classes provide the main
     * functionality and control for a set of worker threads:
     * Submissions from non-FJ threads enter into submission queues.
     * Workers take these tasks and typically split them into subtasks
     * that may be stolen by other workers.  Preference rules give
     * first priority to processing tasks from their own queues (LIFO
     * or FIFO, depending on mode), then to randomized FIFO steals of
     * tasks in other queues.
     *
     * WorkQueues
     * ==========
     *
     * Most operations occur within work-stealing queues (in nested
     * class WorkQueue).  These are special forms of Deques that
     * support only three of the four possible end-operations -- push,
     * pop, and poll (aka steal), under the further constraints that
     * push and pop are called only from the owning thread (or, as
     * extended here, under a lock), while poll may be called from
     * other threads.  (If you are unfamiliar with them, you probably
     * want to read Herlihy and Shavit's book "The Art of
     * Multiprocessor programming", chapter 16 describing these in
     * more detail before proceeding.)  The main work-stealing queue
     * design is roughly similar to those in the papers "Dynamic
     * Circular Work-Stealing Deque" by Chase and Lev, SPAA 2005
     * (http://research.sun.com/scalable/pubs/index.html) and
     * "Idempotent work stealing" by Michael, Saraswat, and Vechev,
     * PPoPP 2009 (http://portal.acm.org/citation.cfm?id=1504186).
     * The main differences ultimately stem from GC requirements that
     * we null out taken slots as soon as we can, to maintain as small
     * a footprint as possible even in programs generating huge
     * numbers of tasks. To accomplish this, we shift the CAS
     * arbitrating pop vs poll (steal) from being on the indices
     * ("base" and "top") to the slots themselves.  So, both a
     * successful pop and poll mainly entail a CAS of a slot from
     * non-null to null.  Because we rely on CASes of references, we
     * do not need tag bits on base or top.  They are simple ints as
     * used in any circular array-based queue (see for example
     * ArrayDeque).  Updates to the indices must still be ordered in a
     * way that guarantees that top == base means the queue is empty,
     * but otherwise may err on the side of possibly making the queue
     * appear nonempty when a push, pop, or poll have not fully
     * committed. Note that this means that the poll operation,
     * considered individually, is not wait-free. One thief cannot
     * successfully continue until another in-progress one (or, if
     * previously empty, a push) completes.  However, in the
     * aggregate, we ensure at least probabilistic non-blockingness.
     * If an attempted steal fails, a thief always chooses a different
     * random victim target to try next. So, in order for one thief to
     * progress, it suffices for any in-progress poll or new push on
     * any empty queue to complete.
     *
     * This approach also enables support of a user mode in which local
     * task processing is in FIFO, not LIFO order, simply by using
     * poll rather than pop.  This can be useful in message-passing
     * frameworks in which tasks are never joined.  However neither
     * mode considers affinities, loads, cache localities, etc, so
     * rarely provide the best possible performance on a given
     * machine, but portably provide good throughput by averaging over
     * these factors.  (Further, even if we did try to use such
     * information, we do not usually have a basis for exploiting it.
     * For example, some sets of tasks profit from cache affinities,
     * but others are harmed by cache pollution effects.)
     *
     * WorkQueues are also used in a similar way for tasks submitted
     * to the pool. We cannot mix these tasks in the same queues used
     * for work-stealing (this would contaminate lifo/fifo
     * processing). Instead, we loosely associate submission queues
     * with submitting threads, using a form of hashing.  The
     * ThreadLocal Submitter class contains a value initially used as
     * a hash code for choosing existing queues, but may be randomly
     * repositioned upon contention with other submitters.  In
     * essence, submitters act like workers except that they never
     * take tasks, and they are multiplexed on to a finite number of
     * shared work queues. However, classes are set up so that future
     * extensions could allow submitters to optionally help perform
     * tasks as well. Insertion of tasks in shared mode requires a
     * lock (mainly to protect in the case of resizing) but we use
     * only a simple spinlock (using bits in field runState), because
     * submitters encountering a busy queue move on to try or create
     * other queues, so never block.
     *
     * Management
     * ==========
     *
     * The main throughput advantages of work-stealing stem from
     * decentralized control -- workers mostly take tasks from
     * themselves or each other. We cannot negate this in the
     * implementation of other management responsibilities. The main
     * tactic for avoiding bottlenecks is packing nearly all
     * essentially atomic control state into two volatile variables
     * that are by far most often read (not written) as status and
     * consistency checks.
     *
     * Field "ctl" contains 64 bits holding all the information needed
     * to atomically decide to add, inactivate, enqueue (on an event
     * queue), dequeue, and/or re-activate workers.  To enable this
     * packing, we restrict maximum parallelism to (1<<15)-1 (which is
     * far in excess of normal operating range) to allow ids, counts,
     * and their negations (used for thresholding) to fit into 16bit
     * fields.
     *
     * Field "runState" contains 32 bits needed to register and
     * deregister WorkQueues, as well as to enable shutdown. It is
     * only modified under a lock (normally briefly held, but
     * occasionally protecting allocations and resizings) but even
     * when locked remains available to check consistency. An
     * auxiliary field "growHints", also only modified under lock,
     * contains a candidate index for the next WorkQueue and
     * a mask for submission queue indices.
     *
     * Recording WorkQueues.  WorkQueues are recorded in the
     * "workQueues" array that is created upon pool construction and
     * expanded if necessary.  Updates to the array while recording
     * new workers and unrecording terminated ones are protected from
     * each other by a lock but the array is otherwise concurrently
     * readable, and accessed directly.  To simplify index-based
     * operations, the array size is always a power of two, and all
     * readers must tolerate null slots. Shared (submission) queues
     * are at even indices, worker queues at odd indices. Grouping
     * them together in this way simplifies and speeds up task
     * scanning. To avoid flailing during start-up, the array is
     * presized to hold twice #parallelism workers (which is unlikely
     * to need further resizing during execution). But to avoid
     * dealing with so many null slots, variable runState includes a
     * mask for the nearest power of two that contains all currently
     * used indices.
     *
     * All worker thread creation is on-demand, triggered by task
     * submissions, replacement of terminated workers, and/or
     * compensation for blocked workers. However, all other support
     * code is set up to work with other policies.  To ensure that we
     * do not hold on to worker references that would prevent GC, ALL
     * accesses to workQueues are via indices into the workQueues
     * array (which is one source of some of the messy code
     * constructions here). In essence, the workQueues array serves as
     * a weak reference mechanism. Thus for example the wait queue
     * field of ctl stores indices, not references.  Access to the
     * workQueues in associated methods (for example signalWork) must
     * both index-check and null-check the IDs. All such accesses
     * ignore bad IDs by returning out early from what they are doing,
     * since this can only be associated with termination, in which
     * case it is OK to give up.  All uses of the workQueues array
     * also check that it is non-null (even if previously
     * non-null). This allows nulling during termination, which is
     * currently not necessary, but remains an option for
     * resource-revocation-based shutdown schemes. It also helps
     * reduce JIT issuance of uncommon-trap code, which tends to
     * unnecessarily complicate control flow in some methods.
     *
     * Event Queuing. Unlike HPC work-stealing frameworks, we cannot
     * let workers spin indefinitely scanning for tasks when none can
     * be found immediately, and we cannot start/resume workers unless
     * there appear to be tasks available.  On the other hand, we must
     * quickly prod them into action when new tasks are submitted or
     * generated. In many usages, ramp-up time to activate workers is
     * the main limiting factor in overall performance (this is
     * compounded at program start-up by JIT compilation and
     * allocation). So we try to streamline this as much as possible.
     * We park/unpark workers after placing in an event wait queue
     * when they cannot find work. This "queue" is actually a simple
     * Treiber stack, headed by the "id" field of ctl, plus a 15bit
     * counter value (that reflects the number of times a worker has
     * been inactivated) to avoid ABA effects (we need only as many
     * version numbers as worker threads). Successors are held in
     * field WorkQueue.nextWait.  Queuing deals with several intrinsic
     * races, mainly that a task-producing thread can miss seeing (and
     * signalling) another thread that gave up looking for work but
     * has not yet entered the wait queue. We solve this by requiring
     * a full sweep of all workers (via repeated calls to method
     * scan()) both before and after a newly waiting worker is added
     * to the wait queue. During a rescan, the worker might release
     * some other queued worker rather than itself, which has the same
     * net effect. Because enqueued workers may actually be rescanning
     * rather than waiting, we set and clear the "parker" field of
     * WorkQueues to reduce unnecessary calls to unpark.  (This
     * requires a secondary recheck to avoid missed signals.)  Note
     * the unusual conventions about Thread.interrupts surrounding
     * parking and other blocking: Because interrupts are used solely
     * to alert threads to check termination, which is checked anyway
     * upon blocking, we clear status (using Thread.interrupted)
     * before any call to park, so that park does not immediately
     * return due to status being set via some other unrelated call to
     * interrupt in user code.
     *
     * Signalling.  We create or wake up workers only when there
     * appears to be at least one task they might be able to find and
     * execute.  When a submission is added or another worker adds a
     * task to a queue that previously had fewer than two tasks, they
     * signal waiting workers (or trigger creation of new ones if
     * fewer than the given parallelism level -- see signalWork).
     * These primary signals are buttressed by signals during rescans;
     * together these cover the signals needed in cases when more
     * tasks are pushed but untaken, and improve performance compared
     * to having one thread wake up all workers.
     *
     * Trimming workers. To release resources after periods of lack of
     * use, a worker starting to wait when the pool is quiescent will
     * time out and terminate if the pool has remained quiescent for
     * SHRINK_RATE nanosecs. This will slowly propagate, eventually
     * terminating all workers after long periods of non-use.
     *
     * Shutdown and Termination. A call to shutdownNow atomically sets
     * a runState bit and then (non-atomically) sets each worker's
     * runState status, cancels all unprocessed tasks, and wakes up
     * all waiting workers.  Detecting whether termination should
     * commence after a non-abrupt shutdown() call requires more work
     * and bookkeeping. We need consensus about quiescence (i.e., that
     * there is no more work). The active count provides a primary
     * indication but non-abrupt shutdown still requires a rechecking
     * scan for any workers that are inactive but not queued.
     *
     * Joining Tasks
     * =============
     *
     * Any of several actions may be taken when one worker is waiting
     * to join a task stolen (or always held) by another.  Because we
     * are multiplexing many tasks on to a pool of workers, we can't
     * just let them block (as in Thread.join).  We also cannot just
     * reassign the joiner's run-time stack with another and replace
     * it later, which would be a form of "continuation", that even if
     * possible is not necessarily a good idea since we sometimes need
     * both an unblocked task and its continuation to progress.
     * Instead we combine two tactics:
     *
     *   Helping: Arranging for the joiner to execute some task that it
     *      would be running if the steal had not occurred.
     *
     *   Compensating: Unless there are already enough live threads,
     *      method tryCompensate() may create or re-activate a spare
     *      thread to compensate for blocked joiners until they unblock.
     *
     * A third form (implemented in tryRemoveAndExec and
     * tryPollForAndExec) amounts to helping a hypothetical
     * compensator: If we can readily tell that a possible action of a
     * compensator is to steal and execute the task being joined, the
     * joining thread can do so directly, without the need for a
     * compensation thread (although at the expense of larger run-time
     * stacks, but the tradeoff is typically worthwhile).
     *
     * The ManagedBlocker extension API can't use helping so relies
     * only on compensation in method awaitBlocker.
     *
     * The algorithm in tryHelpStealer entails a form of "linear"
     * helping: Each worker records (in field currentSteal) the most
     * recent task it stole from some other worker. Plus, it records
     * (in field currentJoin) the task it is currently actively
     * joining. Method tryHelpStealer uses these markers to try to
     * find a worker to help (i.e., steal back a task from and execute
     * it) that could hasten completion of the actively joined task.
     * In essence, the joiner executes a task that would be on its own
     * local deque had the to-be-joined task not been stolen. This may
     * be seen as a conservative variant of the approach in Wagner &
     * Calder "Leapfrogging: a portable technique for implementing
     * efficient futures" SIGPLAN Notices, 1993
     * (http://portal.acm.org/citation.cfm?id=155354). It differs in
     * that: (1) We only maintain dependency links across workers upon
     * steals, rather than use per-task bookkeeping.  This sometimes
     * requires a linear scan of workQueues array to locate stealers, but
     * often doesn't because stealers leave hints (that may become
     * stale/wrong) of where to locate them.  A stealHint is only a
     * hint because a worker might have had multiple steals and the
     * hint records only one of them (usually the most current).
     * Hinting isolates cost to when it is needed, rather than adding
     * to per-task overhead.  (2) It is "shallow", ignoring nesting
     * and potentially cyclic mutual steals.  (3) It is intentionally
     * racy: field currentJoin is updated only while actively joining,
     * which means that we miss links in the chain during long-lived
     * tasks, GC stalls etc (which is OK since blocking in such cases
     * is usually a good idea).  (4) We bound the number of attempts
     * to find work (see MAX_HELP_DEPTH) and fall back to suspending
     * the worker and if necessary replacing it with another.
     *
     * It is impossible to keep exactly the target parallelism number
     * of threads running at any given time.  Determining the
     * existence of conservatively safe helping targets, the
     * availability of already-created spares, and the apparent need
     * to create new spares are all racy, so we rely on multiple
     * retries of each.  Currently, in keeping with on-demand
     * signalling policy, we compensate only if blocking would leave
     * less than one active (non-waiting, non-blocked) worker.
     * Additionally, to avoid some false alarms due to GC, lagging
     * counters, system activity, etc, compensated blocking for joins
     * is only attempted after rechecks stabilize in
     * ForkJoinTask.awaitJoin. (Retries are interspersed with
     * Thread.yield, for good citizenship.)
     *
     * Style notes: There is a lot of representation-level coupling
     * among classes ForkJoinPool, ForkJoinWorkerThread, and
     * ForkJoinTask.  The fields of WorkQueue maintain data structures
     * managed by ForkJoinPool, so are directly accessed.  There is
     * little point trying to reduce this, since any associated future
     * changes in representations will need to be accompanied by
     * algorithmic changes anyway. Several methods intrinsically
     * sprawl because they must accumulate sets of consistent reads of
     * volatiles held in local variables.  Methods signalWork() and
     * scan() are the main bottlenecks, so are especially heavily
     * micro-optimized/mangled.  There are lots of inline assignments
     * (of form "while ((local = field) != 0)") which are usually the
     * simplest way to ensure the required read orderings (which are
     * sometimes critical). This leads to a "C"-like style of listing
     * declarations of these locals at the heads of methods or blocks.
     * There are several occurrences of the unusual "do {} while
     * (!cas...)"  which is the simplest way to force an update of a
     * CAS'ed variable. There are also other coding oddities that help
     * some methods perform reasonably even when interpreted (not
     * compiled).
     *
     * The order of declarations in this file is:
     * (1) Static utility functions
     * (2) Nested (static) classes
     * (3) Static fields
     * (4) Fields, along with constants used when unpacking some of them
     * (5) Internal control methods
     * (6) Callbacks and other support for ForkJoinTask methods
     * (7) Exported methods
     * (8) Static block initializing statics in minimally dependent order
     */

    // Static utilities

    /**
     * Computes an initial hash code (also serving as a non-zero
     * random seed) for a thread id. This method is expected to
     * provide higher-quality hash codes than using method hashCode().
     */
    static final int hashId(long id) {
        int h = (int)id ^ (int)(id >>> 32); // Use MurmurHash of thread id
        h ^= h >>> 16; h *= 0x85ebca6b;
        h ^= h >>> 13; h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return (h == 0) ? 1 : h; // ensure nonzero
    }

    /**
     * If there is a security manager, makes sure caller has
     * permission to modify threads.
     */
    private static void checkPermission() {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(modifyThreadPermission);
    }

    // Nested classes

    /**
     * Factory for creating new {@link ForkJoinWorkerThread}s.
     * A {@code ForkJoinWorkerThreadFactory} must be defined and used
     * for {@code ForkJoinWorkerThread} subclasses that extend base
     * functionality or initialize threads with different contexts.
     */
    public static interface ForkJoinWorkerThreadFactory {
        /**
         * Returns a new worker thread operating in the given pool.
         *
         * @param pool the pool this thread works in
         * @throws NullPointerException if the pool is null
         */
        public ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }

    /**
     * Default ForkJoinWorkerThreadFactory implementation; creates a
     * new ForkJoinWorkerThread.
     */
    static class DefaultForkJoinWorkerThreadFactory
        implements ForkJoinWorkerThreadFactory {
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
        }
    }

    /**
     * A simple non-reentrant lock used for exclusion when managing
     * queues and workers. We use a custom lock so that we can readily
     * probe lock state in constructions that check among alternative
     * actions. The lock is normally only very briefly held, and
     * sometimes treated as a spinlock, but other usages block to
     * reduce overall contention in those cases where locked code
     * bodies perform allocation/resizing.
     */
    static final class Mutex extends AbstractQueuedSynchronizer {
        public final boolean tryAcquire(int ignore) {
            return compareAndSetState(0, 1);
        }
        public final boolean tryRelease(int ignore) {
            setState(0);
            return true;
        }
        public final void lock() { acquire(0); }
        public final void unlock() { release(0); }
        public final boolean isHeldExclusively() { return getState() == 1; }
        public final Condition newCondition() { return new ConditionObject(); }
    }

    /**
     * Class for artificial tasks that are used to replace the target
     * of local joins if they are removed from an interior queue slot
     * in WorkQueue.tryRemoveAndExec. We don't need the proxy to
     * actually do anything beyond having a unique identity.
     */
    static final class EmptyTask extends ForkJoinTask<Void> {
        EmptyTask() { status = ForkJoinTask.NORMAL; } // force done
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void x) {}
        public final boolean exec() { return true; }
    }

    /**
     * Queues supporting work-stealing as well as external task
     * submission. See above for main rationale and algorithms.
     * Implementation relies heavily on "Unsafe" intrinsics
     * and selective use of "volatile":
     *
     * Field "base" is the index (mod array.length) of the least valid
     * queue slot, which is always the next position to steal (poll)
     * from if nonempty. Reads and writes require volatile orderings
     * but not CAS, because updates are only performed after slot
     * CASes.
     *
     * Field "top" is the index (mod array.length) of the next queue
     * slot to push to or pop from. It is written only by owner thread
     * for push, or under lock for trySharedPush, and accessed by
     * other threads only after reading (volatile) base.  Both top and
     * base are allowed to wrap around on overflow, but (top - base)
     * (or more commonly -(base - top) to force volatile read of base
     * before top) still estimates size.
     *
     * The array slots are read and written using the emulation of
     * volatiles/atomics provided by Unsafe. Insertions must in
     * general use putOrderedObject as a form of releasing store to
     * ensure that all writes to the task object are ordered before
     * its publication in the queue. (Although we can avoid one case
     * of this when locked in trySharedPush.) All removals entail a
     * CAS to null.  The array is always a power of two. To ensure
     * safety of Unsafe array operations, all accesses perform
     * explicit null checks and implicit bounds checks via
     * power-of-two masking.
     *
     * In addition to basic queuing support, this class contains
     * fields described elsewhere to control execution. It turns out
     * to work better memory-layout-wise to include them in this
     * class rather than a separate class.
     *
     * Performance on most platforms is very sensitive to placement of
     * instances of both WorkQueues and their arrays -- we absolutely
     * do not want multiple WorkQueue instances or multiple queue
     * arrays sharing cache lines. (It would be best for queue objects
     * and their arrays to share, but there is nothing available to
     * help arrange that).  Unfortunately, because they are recorded
     * in a common array, WorkQueue instances are often moved to be
     * adjacent by garbage collectors. To reduce impact, we use field
     * padding that works OK on common platforms; this effectively
     * trades off slightly slower average field access for the sake of
     * avoiding really bad worst-case access. (Until better JVM
     * support is in place, this padding is dependent on transient
     * properties of JVM field layout rules.)  We also take care in
     * allocating, sizing and resizing the array. Non-shared queue
     * arrays are initialized (via method growArray) by workers before
     * use. Others are allocated on first use.
     */
    static final class WorkQueue {
        /**
         * Capacity of work-stealing queue array upon initialization.
         * Must be a power of two; at least 4, but set larger to
         * reduce cacheline sharing among queues.
         */
        static final int INITIAL_QUEUE_CAPACITY = 1 << 8;

        /**
         * Maximum size for queue arrays. Must be a power of two less
         * than or equal to 1 << (31 - width of array entry) to ensure
         * lack of wraparound of index calculations, but defined to a
         * value a bit less than this to help users trap runaway
         * programs before saturating systems.
         */
        static final int MAXIMUM_QUEUE_CAPACITY = 1 << 26; // 64M

        volatile long totalSteals; // cumulative number of steals
        int seed;                  // for random scanning; initialize nonzero
        volatile int eventCount;   // encoded inactivation count; < 0 if inactive
        int nextWait;              // encoded record of next event waiter
        int rescans;               // remaining scans until block
        int nsteals;               // top-level task executions since last idle
        final int mode;            // lifo, fifo, or shared
        int poolIndex;             // index of this queue in pool (or 0)
        int stealHint;             // index of most recent known stealer
        volatile int runState;     // 1: locked, -1: terminate; else 0
        volatile int base;         // index of next slot for poll
        int top;                   // index of next slot for push
        ForkJoinTask<?>[] array;   // the elements (initially unallocated)
        final ForkJoinWorkerThread owner; // owning thread or null if shared
        volatile Thread parker;    // == owner during call to park; else null
        ForkJoinTask<?> currentJoin;  // task being joined in awaitJoin
        ForkJoinTask<?> currentSteal; // current non-local task being executed
        // Heuristic padding to ameliorate unfortunate memory placements
        Object p00, p01, p02, p03, p04, p05, p06, p07, p08, p09, p0a;

        WorkQueue(ForkJoinWorkerThread owner, int mode) {
            this.owner = owner;
            this.mode = mode;
            // Place indices in the center of array (that is not yet allocated)
            base = top = INITIAL_QUEUE_CAPACITY >>> 1;
        }

        /**
         * Returns number of tasks in the queue.
         */
        final int queueSize() {
            int n = base - top; // non-owner callers must read base first
            return (n >= 0) ? 0 : -n;
        }

        /**
         * Pushes a task. Call only by owner in unshared queues.
         *
         * @param task the task. Caller must ensure non-null.
         * @param p if non-null, pool to signal if necessary
         * @throw RejectedExecutionException if array cannot be resized
         */
        final void push(ForkJoinTask<?> task, ForkJoinPool p) {
            ForkJoinTask<?>[] a;
            int s = top, m, n;
            if ((a = array) != null) {    // ignore if queue removed
                U.putOrderedObject
                    (a, (((m = a.length - 1) & s) << ASHIFT) + ABASE, task);
                if ((n = (top = s + 1) - base) <= 2) {
                    if (p != null)
                        p.signalWork();
                }
                else if (n >= m)
                    growArray(true);
            }
        }

        /**
         * Pushes a task if lock is free and array is either big
         * enough or can be resized to be big enough.
         *
         * @param task the task. Caller must ensure non-null.
         * @return true if submitted
         */
        final boolean trySharedPush(ForkJoinTask<?> task) {
            boolean submitted = false;
            if (runState == 0 && U.compareAndSwapInt(this, RUNSTATE, 0, 1)) {
                ForkJoinTask<?>[] a = array;
                int s = top;
                try {
                    if ((a != null && a.length > s + 1 - base) ||
                        (a = growArray(false)) != null) { // must presize
                        int j = (((a.length - 1) & s) << ASHIFT) + ABASE;
                        U.putObject(a, (long)j, task);    // don't need "ordered"
                        top = s + 1;
                        submitted = true;
                    }
                } finally {
                    runState = 0;                         // unlock
                }
            }
            return submitted;
        }

        /**
         * Takes next task, if one exists, in FIFO order.
         */
        final ForkJoinTask<?> poll() {
            ForkJoinTask<?>[] a; int b; ForkJoinTask<?> t;
            while ((b = base) - top < 0 && (a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if ((t = (ForkJoinTask<?>)U.getObjectVolatile(a, j)) != null &&
                    base == b &&
                    U.compareAndSwapObject(a, j, t, null)) {
                    base = b + 1;
                    return t;
                }
            }
            return null;
        }

        /**
         * Takes next task, if one exists, in LIFO order.  Call only
         * by owner in unshared queues. (We do not have a shared
         * version of this method because it is never needed.)
         */
        final ForkJoinTask<?> pop() {
            ForkJoinTask<?> t; int m;
            ForkJoinTask<?>[] a = array;
            if (a != null && (m = a.length - 1) >= 0) {
                for (int s; (s = top - 1) - base >= 0;) {
                    int j = ((m & s) << ASHIFT) + ABASE;
                    if ((t = (ForkJoinTask<?>)U.getObjectVolatile(a, j)) == null)
                        break;
                    if (U.compareAndSwapObject(a, j, t, null)) {
                        top = s;
                        return t;
                    }
                }
            }
            return null;
        }

        /**
         * Takes next task, if one exists, in order specified by mode.
         */
        final ForkJoinTask<?> nextLocalTask() {
            return mode == 0 ? pop() : poll();
        }

        /**
         * Returns next task, if one exists, in order specified by mode.
         */
        final ForkJoinTask<?> peek() {
            ForkJoinTask<?>[] a = array; int m;
            if (a == null || (m = a.length - 1) < 0)
                return null;
            int i = mode == 0 ? top - 1 : base;
            int j = ((i & m) << ASHIFT) + ABASE;
            return (ForkJoinTask<?>)U.getObjectVolatile(a, j);
        }

        /**
         * Returns task at index b if b is current base of queue.
         */
        final ForkJoinTask<?> pollAt(int b) {
            ForkJoinTask<?> t; ForkJoinTask<?>[] a;
            if ((a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if ((t = (ForkJoinTask<?>)U.getObjectVolatile(a, j)) != null &&
                    base == b &&
                    U.compareAndSwapObject(a, j, t, null)) {
                    base = b + 1;
                    return t;
                }
            }
            return null;
        }

        /**
         * Pops the given task only if it is at the current top.
         */
        final boolean tryUnpush(ForkJoinTask<?> t) {
            ForkJoinTask<?>[] a; int s;
            if ((a = array) != null && (s = top) != base &&
                U.compareAndSwapObject
                (a, (((a.length - 1) & --s) << ASHIFT) + ABASE, t, null)) {
                top = s;
                return true;
            }
            return false;
        }

        /**
         * Polls the given task only if it is at the current base.
         */
        final boolean pollFor(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a; int b;
            if ((b = base) - top < 0 && (a = array) != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                if (U.getObjectVolatile(a, j) == task && base == b &&
                    U.compareAndSwapObject(a, j, task, null)) {
                    base = b + 1;
                    return true;
                }
            }
            return false;
        }

        /**
         * If present, removes from queue and executes the given task, or
         * any other cancelled task. Returns (true) immediately on any CAS
         * or consistency check failure so caller can retry.
         *
         * @return false if no progress can be made
         */
        final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
            boolean removed = false, empty = true, progress = true;
            ForkJoinTask<?>[] a; int m, s, b, n;
            if ((a = array) != null && (m = a.length - 1) >= 0 &&
                (n = (s = top) - (b = base)) > 0) {
                for (ForkJoinTask<?> t;;) {           // traverse from s to b
                    int j = ((--s & m) << ASHIFT) + ABASE;
                    t = (ForkJoinTask<?>)U.getObjectVolatile(a, j);
                    if (t == null)                    // inconsistent length
                        break;
                    else if (t == task) {
                        if (s + 1 == top) {           // pop
                            if (!U.compareAndSwapObject(a, j, task, null))
                                break;
                            top = s;
                            removed = true;
                        }
                        else if (base == b)           // replace with proxy
                            removed = U.compareAndSwapObject(a, j, task,
                                                             new EmptyTask());
                        break;
                    }
                    else if (t.status >= 0)
                        empty = false;
                    else if (s + 1 == top) {          // pop and throw away
                        if (U.compareAndSwapObject(a, j, t, null))
                            top = s;
                        break;
                    }
                    if (--n == 0) {
                        if (!empty && base == b)
                            progress = false;
                        break;
                    }
                }
            }
            if (removed)
                task.doExec();
            return progress;
        }

        /**
         * Initializes or doubles the capacity of array. Call either
         * by owner or with lock held -- it is OK for base, but not
         * top, to move while resizings are in progress.
         *
         * @param rejectOnFailure if true, throw exception if capacity
         * exceeded (relayed ultimately to user); else return null.
         */
        final ForkJoinTask<?>[] growArray(boolean rejectOnFailure) {
            ForkJoinTask<?>[] oldA = array;
            int size = oldA != null ? oldA.length << 1 : INITIAL_QUEUE_CAPACITY;
            if (size <= MAXIMUM_QUEUE_CAPACITY) {
                int oldMask, t, b;
                ForkJoinTask<?>[] a = array = new ForkJoinTask<?>[size];
                if (oldA != null && (oldMask = oldA.length - 1) >= 0 &&
                    (t = top) - (b = base) > 0) {
                    int mask = size - 1;
                    do {
                        ForkJoinTask<?> x;
                        int oldj = ((b & oldMask) << ASHIFT) + ABASE;
                        int j    = ((b &    mask) << ASHIFT) + ABASE;
                        x = (ForkJoinTask<?>)U.getObjectVolatile(oldA, oldj);
                        if (x != null &&
                            U.compareAndSwapObject(oldA, oldj, x, null))
                            U.putObjectVolatile(a, j, x);
                    } while (++b != t);
                }
                return a;
            }
            else if (!rejectOnFailure)
                return null;
            else
                throw new RejectedExecutionException("Queue capacity exceeded");
        }

        /**
         * Removes and cancels all known tasks, ignoring any exceptions.
         */
        final void cancelAll() {
            ForkJoinTask.cancelIgnoringExceptions(currentJoin);
            ForkJoinTask.cancelIgnoringExceptions(currentSteal);
            for (ForkJoinTask<?> t; (t = poll()) != null; )
                ForkJoinTask.cancelIgnoringExceptions(t);
        }

        /**
         * Computes next value for random probes.  Scans don't require
         * a very high quality generator, but also not a crummy one.
         * Marsaglia xor-shift is cheap and works well enough.  Note:
         * This is manually inlined in several usages in ForkJoinPool
         * to avoid writes inside busy scan loops.
         */
        final int nextSeed() {
            int r = seed;
            r ^= r << 13;
            r ^= r >>> 17;
            return seed = r ^= r << 5;
        }

        // Execution methods

        /**
         * Removes and runs tasks until empty, using local mode
         * ordering.
         */
        final void runLocalTasks() {
            if (base - top < 0) {
                for (ForkJoinTask<?> t; (t = nextLocalTask()) != null; )
                    t.doExec();
            }
        }

        /**
         * Executes a top-level task and any local tasks remaining
         * after execution.
         *
         * @return true unless terminating
         */
        final boolean runTask(ForkJoinTask<?> t) {
            boolean alive = true;
            if (t != null) {
                currentSteal = t;
                t.doExec();
                runLocalTasks();
                ++nsteals;
                currentSteal = null;
            }
            else if (runState < 0)            // terminating
                alive = false;
            return alive;
        }

        /**
         * Executes a non-top-level (stolen) task.
         */
        final void runSubtask(ForkJoinTask<?> t) {
            if (t != null) {
                ForkJoinTask<?> ps = currentSteal;
                currentSteal = t;
                t.doExec();
                currentSteal = ps;
            }
        }

        /**
         * Returns true if owned and not known to be blocked.
         */
        final boolean isApparentlyUnblocked() {
            Thread wt; Thread.State s;
            return (eventCount >= 0 &&
                    (wt = owner) != null &&
                    (s = wt.getState()) != Thread.State.BLOCKED &&
                    s != Thread.State.WAITING &&
                    s != Thread.State.TIMED_WAITING);
        }

        /**
         * If this owned and is not already interrupted, try to
         * interrupt and/or unpark, ignoring exceptions.
         */
        final void interruptOwner() {
            Thread wt, p;
            if ((wt = owner) != null && !wt.isInterrupted()) {
                try {
                    wt.interrupt();
                } catch (SecurityException ignore) {
                }
            }
            if ((p = parker) != null)
                U.unpark(p);
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe U;
        private static final long RUNSTATE;
        private static final int ABASE;
        private static final int ASHIFT;
        static {
            int s;
            try {
                U = getUnsafe();
                Class<?> k = WorkQueue.class;
                Class<?> ak = ForkJoinTask[].class;
                RUNSTATE = U.objectFieldOffset
                    (k.getDeclaredField("runState"));
                ABASE = U.arrayBaseOffset(ak);
                s = U.arrayIndexScale(ak);
            } catch (Exception e) {
                throw new Error(e);
            }
            if ((s & (s-1)) != 0)
                throw new Error("data type scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(s);
        }
    }

    /**
     * Per-thread records for threads that submit to pools. Currently
     * holds only pseudo-random seed / index that is used to choose
     * submission queues in method doSubmit. In the future, this may
     * also incorporate a means to implement different task rejection
     * and resubmission policies.
     */
    static final class Submitter {
        int seed;
        Submitter() { seed = hashId(Thread.currentThread().getId()); }
    }

    /** ThreadLocal class for Submitters */
    static final class ThreadSubmitter extends ThreadLocal<Submitter> {
        public Submitter initialValue() { return new Submitter(); }
    }

    // static fields (initialized in static initializer below)

    /**
     * Creates a new ForkJoinWorkerThread. This factory is used unless
     * overridden in ForkJoinPool constructors.
     */
    public static final ForkJoinWorkerThreadFactory
        defaultForkJoinWorkerThreadFactory;

    /**
     * Generator for assigning sequence numbers as pool names.
     */
    private static final AtomicInteger poolNumberGenerator;

    /**
     * Permission required for callers of methods that may start or
     * kill threads.
     */
    private static final RuntimePermission modifyThreadPermission;

    /**
     * Per-thread submission bookeeping. Shared across all pools
     * to reduce ThreadLocal pollution and because random motion
     * to avoid contention in one pool is likely to hold for others.
     */
    private static final ThreadSubmitter submitters;

    // static constants

    /**
     * The wakeup interval (in nanoseconds) for a worker waiting for a
     * task when the pool is quiescent to instead try to shrink the
     * number of workers.  The exact value does not matter too
     * much. It must be short enough to release resources during
     * sustained periods of idleness, but not so short that threads
     * are continually re-created.
     */
    private static final long SHRINK_RATE =
        4L * 1000L * 1000L * 1000L; // 4 seconds

    /**
     * The timeout value for attempted shrinkage, includes
     * some slop to cope with system timer imprecision.
     */
    private static final long SHRINK_TIMEOUT = SHRINK_RATE - (SHRINK_RATE / 10);

    /**
     * The maximum stolen->joining link depth allowed in tryHelpStealer.
     * Depths for legitimate chains are unbounded, but we use a fixed
     * constant to avoid (otherwise unchecked) cycles and to bound
     * staleness of traversal parameters at the expense of sometimes
     * blocking when we could be helping.
     */
    private static final int MAX_HELP_DEPTH = 16;

    /**
     * Bits and masks for control variables
     *
     * Field ctl is a long packed with:
     * AC: Number of active running workers minus target parallelism (16 bits)
     * TC: Number of total workers minus target parallelism (16 bits)
     * ST: true if pool is terminating (1 bit)
     * EC: the wait count of top waiting thread (15 bits)
     * ID: poolIndex of top of Treiber stack of waiters (16 bits)
     *
     * When convenient, we can extract the upper 32 bits of counts and
     * the lower 32 bits of queue state, u = (int)(ctl >>> 32) and e =
     * (int)ctl.  The ec field is never accessed alone, but always
     * together with id and st. The offsets of counts by the target
     * parallelism and the positionings of fields makes it possible to
     * perform the most common checks via sign tests of fields: When
     * ac is negative, there are not enough active workers, when tc is
     * negative, there are not enough total workers, and when e is
     * negative, the pool is terminating.  To deal with these possibly
     * negative fields, we use casts in and out of "short" and/or
     * signed shifts to maintain signedness.
     *
     * When a thread is queued (inactivated), its eventCount field is
     * set negative, which is the only way to tell if a worker is
     * prevented from executing tasks, even though it must continue to
     * scan for them to avoid queuing races. Note however that
     * eventCount updates lag releases so usage requires care.
     *
     * Field runState is an int packed with:
     * SHUTDOWN: true if shutdown is enabled (1 bit)
     * SEQ:  a sequence number updated upon (de)registering workers (15 bits)
     * MASK: mask (power of 2 - 1) covering all registered poolIndexes (16 bits)
     *
     * The combination of mask and sequence number enables simple
     * consistency checks: Staleness of read-only operations on the
     * workQueues array can be checked by comparing runState before vs
     * after the reads. The low 16 bits (i.e, anding with SMASK) hold
     * the smallest power of two covering all indices, minus
     * one.
     */

    // bit positions/shifts for fields
    private static final int  AC_SHIFT   = 48;
    private static final int  TC_SHIFT   = 32;
    private static final int  ST_SHIFT   = 31;
    private static final int  EC_SHIFT   = 16;

    // bounds
    private static final int  POOL_MAX   = 0x7fff;  // max #workers - 1
    private static final int  SMASK      = 0xffff;  // short bits
    private static final int  SQMASK     = 0xfffe;  // even short bits
    private static final int  SHORT_SIGN = 1 << 15;
    private static final int  INT_SIGN   = 1 << 31;

    // masks
    private static final long STOP_BIT   = 0x0001L << ST_SHIFT;
    private static final long AC_MASK    = ((long)SMASK) << AC_SHIFT;
    private static final long TC_MASK    = ((long)SMASK) << TC_SHIFT;

    // units for incrementing and decrementing
    private static final long TC_UNIT    = 1L << TC_SHIFT;
    private static final long AC_UNIT    = 1L << AC_SHIFT;

    // masks and units for dealing with u = (int)(ctl >>> 32)
    private static final int  UAC_SHIFT  = AC_SHIFT - 32;
    private static final int  UTC_SHIFT  = TC_SHIFT - 32;
    private static final int  UAC_MASK   = SMASK << UAC_SHIFT;
    private static final int  UTC_MASK   = SMASK << UTC_SHIFT;
    private static final int  UAC_UNIT   = 1 << UAC_SHIFT;
    private static final int  UTC_UNIT   = 1 << UTC_SHIFT;

    // masks and units for dealing with e = (int)ctl
    private static final int E_MASK      = 0x7fffffff; // no STOP_BIT
    private static final int E_SEQ       = 1 << EC_SHIFT;

    // runState bits
    private static final int SHUTDOWN    = 1 << 31;
    private static final int RS_SEQ      = 1 << 16;
    private static final int RS_SEQ_MASK = 0x7fff0000;

    // access mode for WorkQueue
    static final int LIFO_QUEUE          =  0;
    static final int FIFO_QUEUE          =  1;
    static final int SHARED_QUEUE        = -1;

    // Instance fields

    /*
     * Field layout order in this class tends to matter more than one
     * would like. Runtime layout order is only loosely related to
     * declaration order and may differ across JVMs, but the following
     * empirically works OK on current JVMs.
     */

    volatile long ctl;                         // main pool control
    final int parallelism;                     // parallelism level
    final int localMode;                       // per-worker scheduling mode
    int growHints;                             // for expanding indices/ranges
    volatile int runState;                     // shutdown status, seq, and mask
    WorkQueue[] workQueues;                    // main registry
    final Mutex lock;                          // for registration
    final Condition termination;               // for awaitTermination
    final ForkJoinWorkerThreadFactory factory; // factory for new workers
    final Thread.UncaughtExceptionHandler ueh; // per-worker UEH
    final AtomicLong stealCount;               // collect counts when terminated
    final AtomicInteger nextWorkerNumber;      // to create worker name string
    final String workerNamePrefix;             // to create worker name string

    //  Creating, registering, deregistering and running workers

    /**
     * Tries to create and start a worker
     */
    private void addWorker() {
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        try {
            if ((wt = factory.newThread(this)) != null) {
                wt.start();
                return;
            }
        } catch (Throwable e) {
            ex = e;
        }
        deregisterWorker(wt, ex); // adjust counts etc on failure
    }

    /**
     * Callback from ForkJoinWorkerThread constructor to assign a
     * public name. This must be separate from registerWorker because
     * it is called during the "super" constructor call in
     * ForkJoinWorkerThread.
     */
    final String nextWorkerName() {
        return workerNamePrefix.concat
            (Integer.toString(nextWorkerNumber.addAndGet(1)));
    }

    /**
     * Callback from ForkJoinWorkerThread constructor to establish and
     * record its WorkQueue.
     *
     * @param wt the worker thread
     */
    final void registerWorker(ForkJoinWorkerThread wt) {
        WorkQueue w = wt.workQueue;
        Mutex lock = this.lock;
        lock.lock();
        try {
            int g = growHints, k = g & SMASK;
            WorkQueue[] ws = workQueues;
            if (ws != null) {                       // ignore on shutdown
                int n = ws.length;
                if ((k & 1) == 0 || k >= n || ws[k] != null) {
                    for (k = 1; k < n && ws[k] != null; k += 2)
                        ;                           // workers are at odd indices
                    if (k >= n)                     // resize
                        workQueues = ws = Arrays.copyOf(ws, n << 1);
                }
                w.eventCount = w.poolIndex = k;     // establish before recording
                ws[k] = w;
                growHints = (g & ~SMASK) | ((k + 2) & SMASK);
                int rs = runState;
                int m = rs & SMASK;                 // recalculate runState mask
                if (k > m)
                    m = (m << 1) + 1;
                runState = (rs & SHUTDOWN) | ((rs + RS_SEQ) & RS_SEQ_MASK) | m;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Final callback from terminating worker, as well as upon failure
     * to construct or start a worker in addWorker.  Removes record of
     * worker from array, and adjusts counts. If pool is shutting
     * down, tries to complete termination.
     *
     * @param wt the worker thread or null if addWorker failed
     * @param ex the exception causing failure, or null if none
     */
    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        WorkQueue w = null;
        if (wt != null && (w = wt.workQueue) != null) {
            w.runState = -1;                // ensure runState is set
            stealCount.getAndAdd(w.totalSteals + w.nsteals);
            int idx = w.poolIndex;
            Mutex lock = this.lock;
            lock.lock();
            try {                           // remove record from array
                WorkQueue[] ws = workQueues;
                if (ws != null && idx >= 0 && idx < ws.length && ws[idx] == w) {
                    ws[idx] = null;
                    growHints = (growHints & ~SMASK) | idx;
                }
            } finally {
                lock.unlock();
            }
        }

        long c;                             // adjust ctl counts
        do {} while (!U.compareAndSwapLong
                     (this, CTL, c = ctl, (((c - AC_UNIT) & AC_MASK) |
                                           ((c - TC_UNIT) & TC_MASK) |
                                           (c & ~(AC_MASK|TC_MASK)))));

        if (!tryTerminate(false, false) && w != null) {
            w.cancelAll();                  // cancel remaining tasks
            if (w.array != null)            // suppress signal if never ran
                signalWork();               // wake up or create replacement
            if (ex == null)                 // help clean refs on way out
                ForkJoinTask.helpExpungeStaleExceptions();
        }

        if (ex != null)                     // rethrow
            U.throwException(ex);
    }

    /**
     * Top-level runloop for workers, called by ForkJoinWorkerThread.run.
     */
    final void runWorker(ForkJoinWorkerThread wt) {
        // Initialize queue array and seed in this thread
        WorkQueue w = wt.workQueue;
        w.growArray(false);
        w.seed = hashId(Thread.currentThread().getId());

        do {} while (w.runTask(scan(w)));
    }

    // Submissions

    /**
     * Unless shutting down, adds the given task to a submission queue
     * at submitter's current queue index (modulo submission
     * range). If no queue exists at the index, one is created unless
     * pool lock is busy.  If the queue and/or lock are busy, another
     * index is randomly chosen. The mask in growHints controls the
     * effective index range of queues considered. The mask is
     * expanded, up to the current workerQueue mask, upon any detected
     * contention but otherwise remains small to avoid needlessly
     * creating queues when there is no contention.
     */
    private void doSubmit(ForkJoinTask<?> task) {
        if (task == null)
            throw new NullPointerException();
        Submitter s = submitters.get();
        for (int r = s.seed, m = growHints >>> 16;;) {
            WorkQueue[] ws; WorkQueue q; Mutex lk;
            int k = r & m & SQMASK;          // use only even indices
            if (runState < 0 || (ws = workQueues) == null || ws.length <= k)
                throw new RejectedExecutionException(); // shutting down
            if ((q = ws[k]) == null && (lk = lock).tryAcquire(0)) {
                try {                        // try to create new queue
                    if (ws == workQueues && (q = ws[k]) == null) {
                        int rs;              // update runState seq
                        ws[k] = q = new WorkQueue(null, SHARED_QUEUE);
                        runState = (((rs = runState) & SHUTDOWN) |
                                    ((rs + RS_SEQ) & ~SHUTDOWN));
                    }
                } finally {
                    lk.unlock();
                }
            }
            if (q != null) {
                if (q.trySharedPush(task)) {
                    signalWork();
                    return;
                }
                else if (m < parallelism - 1 && m < (runState & SMASK)) {
                    Mutex lock = this.lock;
                    lock.lock();             // block until lock free
                    int g = growHints;
                    if (g >>> 16 == m)       // expand range
                        growHints = (((m << 1) + 1) << 16) | (g & SMASK);
                    lock.unlock();           // no need for try/finally
                }
                else if ((r & m) == 0)
                    Thread.yield();          // occasionally yield if busy
            }
            if (m == (m = growHints >>> 16)) {
                r ^= r << 13;                // update seed unless new range
                r ^= r >>> 17;               // same xorshift as WorkQueues
                s.seed = r ^= r << 5;
            }
        }
    }

    // Maintaining ctl counts

    /**
     * Increments active count; mainly called upon return from blocking.
     */
    final void incrementActiveCount() {
        long c;
        do {} while (!U.compareAndSwapLong(this, CTL, c = ctl, c + AC_UNIT));
    }

    /**
     * Tries to activate or create a worker if too few are active.
     */
    final void signalWork() {
        long c; int u;
        while ((u = (int)((c = ctl) >>> 32)) < 0) {     // too few active
            WorkQueue[] ws = workQueues; int e, i; WorkQueue w; Thread p;
            if ((e = (int)c) > 0) {                     // at least one waiting
                if (ws != null && (i = e & SMASK) < ws.length &&
                    (w = ws[i]) != null && w.eventCount == (e | INT_SIGN)) {
                    long nc = (((long)(w.nextWait & E_MASK)) |
                               ((long)(u + UAC_UNIT) << 32));
                    if (U.compareAndSwapLong(this, CTL, c, nc)) {
                        w.eventCount = (e + E_SEQ) & E_MASK;
                        if ((p = w.parker) != null)
                            U.unpark(p);                // activate and release
                        break;
                    }
                }
                else
                    break;
            }
            else if (e == 0 && (u & SHORT_SIGN) != 0) { // too few total
                long nc = (long)(((u + UTC_UNIT) & UTC_MASK) |
                                 ((u + UAC_UNIT) & UAC_MASK)) << 32;
                if (U.compareAndSwapLong(this, CTL, c, nc)) {
                    addWorker();
                    break;
                }
            }
            else
                break;
        }
    }

    /**
     * Tries to decrement active count (sometimes implicitly) and
     * possibly release or create a compensating worker in preparation
     * for blocking. Fails on contention or termination.
     *
     * @return true if the caller can block, else should recheck and retry
     */
    final boolean tryCompensate() {
        WorkQueue w; Thread p;
        int pc = parallelism, e, u, ac, tc, i;
        long c = ctl;
        WorkQueue[] ws = workQueues;
        if ((e = (int)c) >= 0) {
            if ((ac = ((u = (int)(c >>> 32)) >> UAC_SHIFT)) <= 0 &&
                e != 0 && ws != null && (i = e & SMASK) < ws.length &&
                (w = ws[i]) != null) {
                long nc = (long)(w.nextWait & E_MASK) | (c & (AC_MASK|TC_MASK));
                if (w.eventCount == (e | INT_SIGN) &&
                    U.compareAndSwapLong(this, CTL, c, nc)) {
                    w.eventCount = (e + E_SEQ) & E_MASK;
                    if ((p = w.parker) != null)
                        U.unpark(p);
                    return true;             // release an idle worker
                }
            }
            else if ((tc = (short)(u >>> UTC_SHIFT)) >= 0 && ac + pc > 1) {
                long nc = ((c - AC_UNIT) & AC_MASK) | (c & ~AC_MASK);
                if (U.compareAndSwapLong(this, CTL, c, nc))
                    return true;             // no compensation needed
            }
            else if (tc + pc < POOL_MAX) {
                long nc = ((c + TC_UNIT) & TC_MASK) | (c & ~TC_MASK);
                if (U.compareAndSwapLong(this, CTL, c, nc)) {
                    addWorker();
                    return true;             // create replacement
           
