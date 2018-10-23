// https://searchcode.com/api/result/11920100/

/*
    Copyright 1996-2011 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadPool.java#8 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

/**
    Standard non-extensible thread pool implememtation on top of
    queue.  Users provide Runnable implementations and call execute.
    <p>
    The pool is constituted of Threads retrieve from a
    ThreadFactory. Normally, the number of threads created cannot
    exceed a specified number of threads, and new threads are created
    if necessary until the limit is reached.
    <p>
    Each thread pool is configured with a {@link
    RejectedExecutionHandler}.  When the user call execute, the
    runnable is added to a queue and a thread is notified.  If all
    threads are busy, then one will be added until the thread size
    limit is reached.  When this happens, outstanding runnables are
    queued up. When the queue reaches its (configurable) capacity, the
    RejectedExecutionHandler will be called upon to handle the
    situation.
    <p>
    At present there are 2 handlers:
    <ul>
    <li>{@link DefaultRejectedExecutionHandler} simply drops the runnable
    and throws a {@link RejectedExecutionException}.</li>
    <li>{@link ThreadPoolRejectedExecutionHandler} will increaese the
    queue size by roughly 10% to accommodate the new runnables while simultaneously
    add more threads regardless of the number of threads already created to
    reduce the queue size.</li>
    </ul>
    <p>
    Pooled threads remaining idle for longer than the specified time out, will
    be terminated. A timeout of 0 will result in no idle threads being terminated.
    <p>
    Based on the above discussion, we can arrive at the following guidelines:
    <ul>
    <li>if response time is important, it is better to have a shorter queue size
    and more threads, and use ThreadPoolRejectedExecutionHandler as the handler.
    </li>
    <li>if memory (or CPU) resource is important (so runnables can wait), it is
    better to have a larger queue and less threads, and a handler that does not
    add threads indefinitely.
    </li>
    </ul>
    @aribaapi ariba
*/
public class ThreadPool implements Executor
{
    private final Set/*<Thread>*/ _threads;
    private final RunnableQueue   _queue;
    private final String          _name;
    /**
        specifies how long a thread can remain idle before being terminated.
        The units are in millisecond. A value of 0 is logical equivalent to
        no time out.
    */
    private final long            _inactivityTimeOut;
    private final ThreadManager   _threadManager;
    private final int             _maxThreads;
    private final int             _minThreads;

    private int     _threadId         = 0;
    private int     _availableThreads = 0;
    private final RejectedExecutionHandler _handler;

    private ThreadPoolMonitor _monitor = null;

    /**
        Creates a new ThreadPool starting 1 thread and containing
        up to 5 threads. Threads die after 30 minutes of inactivity.
        @param name the name used for the threads
        @param threadManager the ThreadManager used to manage the threads
        @aribaapi ariba
    */
    public ThreadPool (String name, ThreadManager threadManager)
    {
        this(name, threadManager, 1, 5, 0, 50, null);
    }

    /**
        Creates a new ThreadPool.
        @param name the name used for the threads
        @param threadManager the ThreadManager used to manage the threads
        @param minThreads the number of threads to start with
        @param maxThreads the maximum number of threads managed by the pool
        @param timeout the maximum time a thread remains inactive
        @param queueCapacity the size of the queue to hold the runnables. Note
        that it is possible for the queue to grow beyond this initial capacity.
        @param handlerClassName the name of the handling class. If <code>null</code>
        {@link DefaultRejectedExecutionHandler} will be used.
        @aribaapi ariba
    */
    public ThreadPool (String name,
                       ThreadManager threadManager,
                       int minThreads,
                       int maxThreads,
                       long timeout,
                       int queueCapacity,
                       String handlerClassName)
    {
        this._inactivityTimeOut = timeout;
        this._maxThreads = maxThreads;
        this._minThreads = minThreads;
        this._name = name;
        this._threadManager = threadManager;
        this._queue = new RunnableQueue(queueCapacity, _name);
        this._threads = Collections.synchronizedSet(SetUtil.set(minThreads));
        this._handler = (handlerClassName == null) ?
            new DefaultRejectedExecutionHandler() :
            (RejectedExecutionHandler)ClassUtil.newInstance(
                handlerClassName, "ariba.util.core.RejectedExecutionHandler");
        Assert.that(this._handler != null,
                    "Error instantiating %s", handlerClassName);
        for (int i = 0; i < minThreads; i++) {
            createExecutor();
        }
        if (Log.threadpool.isDebugEnabled()) {
            Log.threadpool.debug(
                "ThreadPool %s created with %s threads to start with",
                _name,
                Constants.getInteger(minThreads));
            Log.threadpool.debug(
                "ThreadPool %s is associated with a queue of " +
                "%s runnables",
                _name,
                Constants.getInteger(queueCapacity));
            Log.threadpool.debug(
                "ThreadPool %s is associated with handler %s",
                _name,
                _handler.getClass().getName());
        }
    }
    /**
        Retrieves a thread from the pool and have it execute
        the given runnable.
        @param command the task to be executed
        @exception RejectedExecutionException if the execution is rejected.
        @aribaapi ariba
    */
    public synchronized void execute (Runnable command)
    {
        executeWithoutNotify(command, false, true);
        notify();
    }

    /**
        This method is provided merely for use by
        {@link ThreadPoolRejectedExecutionHandler}. Use with
        caution!! Do not make this public method.
    */
    synchronized void forceExecuteWithoutNotify (Runnable command)
    {
        executeWithoutNotify(command, true, false);
    }

    private void executeWithoutNotify (Runnable command,
                                       boolean force,
                                       boolean shouldMonitor)
    {
        if (!_queue.insertRunnable(command, force)) {
            _handler.handle(command, this);
        }
        if ((_availableThreads == 0 && getThreadCount() < _maxThreads) ||
            (force && getThreadCount() < (_maxThreads * 10))) {
            createExecutor();
        }
        if (shouldMonitor && _monitor != null) {
            _monitor.monitor(this);
        }
    }

    /** not synchronized, last one wins */
    public void setMonitor (ThreadPoolMonitor monitor)
    {
        _monitor = monitor;
    }
    public ThreadPoolMonitor getMonitor ()
    {
        return _monitor;
    }

    /**
        Returns the size of the ThreadPool
        @aribaapi ariba
    */
    public int getThreadCount ()
    {
        return _threads.size();
    }
    
    /**
     * Returns the number of objects waiting in the queue
     * @aribaapi private
     */
    public int getQueueSize ()
    {
        // this method is not synchronized like any of the
        // other methods accessing the queue. But we're just 
        // accessing the size of the list so it's fine
        return _queue.getSize();
    }

    /**
     * Returns the configured max thread limit.
     * @return
     */
    public int getMaxThreadCount()
    {
        return _maxThreads;
    }

    /**
        Creates a new PooledExecutor. We don't synchronize on this
        method because the caller synchronizes.
    */
    private void createExecutor ()
    {
        Thread t = null;
        try {
            PooledExecutor executor = new PooledExecutor();
            t = executor.getThread();
            addThread(t);
        }
        catch (OutOfMemoryError oome) {
            // Cannot create and/or start the thread because of lack of memory or
            // native threads.  If thread was created, remove it from _threads.
            // Command is still in queue and will be run later.
            if (t != null && _threads.contains(t)) {
                _threads.remove(t);
            }
            Log.threadpool.warning(9863, _name, Constants.getInteger(getThreadCount()),
                                   SystemUtil.stackTrace(oome));
        }
    }

    private void addThread (Thread t)
    {
        _threads.add(t);
        if (getThreadCount() > _maxThreads) {
            Log.threadpool.info(9032,
                                _name,
                                t.getName(),
                                Constants.getInteger(getThreadCount()));
        }
        t.start();
    }

    /**
        Remove the thread from the set of managed threads. Returns
        a boolean indicating if the specified thread is removed. The
        specified thread is not removed if either one of the following
        is true:
        <ul>
        <li>it is null</li>
        <li>the total number of threads being managed does not
        exceed the minimum configured threads and the forceRemoval
        boolean is false</li>
        </ul>
        @param t the specified thread, can be null, in which case the
        thread is not removed.
        @return <code>true</code> if the thread is removed,
        <code>false</code> otherwise.
    */
    private boolean removeThread (Thread t)
    {
        if (t != null) {
            synchronized (this) {
                if (getThreadCount() > _minThreads) {
                    _threads.remove(t);
                    Log.threadpool.info(9033, _name,
                                        t.getName(),
                                        Constants.getInteger(getThreadCount()));
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized Runnable nextRunnable ()
      throws InterruptedException
    {
        _availableThreads++;
        try {
            if (_queue.isEmpty()) {
                if (_availableThreads > _maxThreads) {
                    //
                    return null;
                }
                this.wait(_inactivityTimeOut);
            }
            if (_queue.isEmpty()) {
                return null;
            }
            else {
                return _queue.nextRunnable();
            }
        }
        finally {
            _availableThreads--;
        }
    }

    /**
        Non synchronized queue to store the runnables
        No need to synchronize are all the accesses are done
        within blocks synchronized on the ThreadPool
    */
    private static final class RunnableQueue
    {
        private final LinkedList/*<Runnable>*/ _runnables = new LinkedList();
        private final int _initialMaxQueueSize;
        private final int _triggerIncrement;
        private int _warningTriggerLevel;
        private final String _threadPoolName;
        private static final int MinimumTriggerLevel = 10;

        RunnableQueue (int maxSize, String threadPoolName)
        {
            this._initialMaxQueueSize = maxSize;
            this._warningTriggerLevel = maxSize;
            int triggerSize = (maxSize + 5) / 10;
            this._triggerIncrement = triggerSize < MinimumTriggerLevel ?
                MinimumTriggerLevel : triggerSize;
            this._threadPoolName = threadPoolName;
        }

        boolean insertRunnable (Runnable r, boolean forceInsert)
        {
            if (getSize() >= _initialMaxQueueSize &&
                !forceInsert)
            {
                return false;
            }
            if (getSize() == _warningTriggerLevel) {
                Log.threadpool.warning(9031, _threadPoolName,
                                       Constants.getInteger(_warningTriggerLevel));
                _warningTriggerLevel += _triggerIncrement;
            }
            return _runnables.add(r);
        }

        Runnable nextRunnable ()
        {
            return (Runnable)_runnables.removeFirst();
        }

        boolean isEmpty ()
        {
            return _runnables.isEmpty();
        }
        
        int getSize ()
        {
            return _runnables.size();
        }
    }


    private class PooledExecutor implements Runnable
    {
        // a PooledExecutor can only be attached to one single thread
        private Thread _thread;

        PooledExecutor ()
        {
            _thread = new Thread(this,
                                 Fmt.S("PooledExecutor - %s - %s",
                                       _name,
                                       String.valueOf(++_threadId)));
            _thread.setDaemon(true);
        }

        public Thread getThread ()
        {
            return _thread;
        }

        //----- Runnable implementation -----
        public void run ()
        {
            try {
                while (true) {
                    Runnable runnable = null;
                    try {
                        runnable = nextRunnable();
                    }
                    catch (InterruptedException e) {

                    }
                    if (runnable != null) {
                          _threadManager.setupThread();
                        try {
                            runnable.run();
                        }
                        catch (Throwable t) { // OK
                            Log.threadpool.error(2867,
                                                 _name,
                                                 runnable.getClass().getName(),
                                                 SystemUtil.stackTrace(t));
                        }
                        finally {
                            _threadManager.cleanupThread();
                        }
                    }
                    else {
                            // thread has been inactive for too long
                            // Ask the threadPool if we should deallocate
                            // this thread.
                        if (removeThread(_thread)) {
                                // set to null so that we don't remove
                                // it again in the finally block.
                            _thread = null;
                            break;
                        }
                    }
                }
            }
            finally {
                removeThread(_thread);
            }
        }
    }
}


