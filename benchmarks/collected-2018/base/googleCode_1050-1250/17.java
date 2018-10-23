// https://searchcode.com/api/result/13968133/


/*
 * Copyright (c) 1998 - 2005 Versant Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Versant Corporation - initial API and implementation
 */
package com.versant.core.jdbc.conn;

import com.versant.core.common.Debug;
import com.versant.core.jdo.PoolStatus;
import com.versant.core.metric.BaseMetric;
import com.versant.core.metric.Metric;
import com.versant.core.metric.PercentageMetric;
import com.versant.core.logging.LogEventStore;
import com.versant.core.jdbc.JdbcConfig;
import com.versant.core.jdbc.logging.JdbcConnectionEvent;
import com.versant.core.jdbc.logging.JdbcLogEvent;
import com.versant.core.jdbc.logging.JdbcPoolEvent;
import com.versant.core.jdbc.JdbcConnectionSource;
import com.versant.core.jdbc.sql.SqlDriver;
import com.versant.core.util.BeanUtils;

import java.sql.*;
import java.util.*;

import com.versant.core.common.BindingSupportImpl;
import com.versant.core.metric.HasMetrics;

/**
 * JDBC connection pool with a PreparedStatement cache for each connection.
 *
 * @see PooledConnection
 */
public final class JDBCConnectionPool
        implements Runnable, JdbcConnectionSource, HasMetrics {

    private final Driver jdbcDriver;
    private final SqlDriver sqlDriver;
    private final Properties props;
    private final String url;
    private final LogEventStore pes;
    private boolean clearBatch;
    private final boolean waitForConOnStartup;
    private boolean jdbcDisablePsCache;
    private int psCacheMax;
    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int reserved;
    private final boolean testOnAlloc;
    private final boolean testOnReturn;
    private final boolean testOnException;
    private boolean testWhenIdle;
    private String initSQL;
    private boolean commitAfterInitSQL;
    private String validateSQL;
    private final int retryIntervalMs;
    private final int retryCount;
    private boolean closed;
    private int conTimeout;
    private int testInterval;
    private boolean blockWhenFull;
    private int maxConAge;
    private int isolationLevel;

    private PooledConnection idleHead;      // next con to be given out
    private PooledConnection idleTail;      // last con returned
    private PooledConnection idleHeadAC;    // next autocommit con to be given out
    private PooledConnection idleTailAC;    // last autocommit con returned
    private int idleCount;

    private PooledConnection activeHead;    // most recently allocated con
    private PooledConnection activeTail;    // least recently allocated con
    private int activeCount;

    private Thread cleanupThread;
    private long timeLastTest = System.currentTimeMillis();

    private Properties userProps;

    private BaseMetric metricActive;
    private BaseMetric metricIdle;
    private BaseMetric metricMaxActive;
    private BaseMetric metricCreated;
    private BaseMetric metricClosed;
    private BaseMetric metricAllocated;
    private BaseMetric metricValidated;
    private BaseMetric metricBad;
    private BaseMetric metricTimedOut;
    private BaseMetric metricExpired;
    private BaseMetric metricWait;
    private BaseMetric metricFull;

    private int createdCount;
    private int closedCount;
    private int allocatedCount;
    private int validatedCount;
    private int badCount;
    private int timedOutCount;
    private int waitCount;
    private int fullCount;
    private int expiredCount;

    private static final String CAT_POOL = "Con Pool";

    /**
     * Create the pool. Note that changes to jdbcConfig have no effect on the
     * pool after construction i.e. fields in jdbcConfig are copied not
     * referenced. The sqlDriver parameter is used to customize the pool
     * to workaround bugs and so on in the JDBC driver or database. It can
     * be null.
     */
    public JDBCConnectionPool(JdbcConfig jdbcConfig, LogEventStore pes,
            Driver jdbcDriver, SqlDriver sqlDriver) {
        this.pes = pes;
        this.jdbcDriver = jdbcDriver;
        this.sqlDriver = sqlDriver;

        url = jdbcConfig.url;
        props = new Properties();
        if (jdbcConfig.user != null) {
            props.put("user",
                    jdbcConfig.user);
        }
        if (jdbcConfig.password != null) {
            props.put("password",
                    jdbcConfig.password);
        }
        if (jdbcConfig.properties != null) {
            BeanUtils.parseProperties(jdbcConfig.properties, props);
        }

        //create a props for user
        userProps = new Properties();
        if (jdbcConfig.user != null) {
            userProps.put("user", jdbcConfig.user);
        }
        userProps.put("url", url);
        userProps.put("driver", jdbcDriver.getClass().getName());
        if (jdbcConfig.properties != null) {
            BeanUtils.parseProperties(jdbcConfig.properties, userProps);
        }

        jdbcDisablePsCache = jdbcConfig.jdbcDisablePsCache;
        psCacheMax = jdbcConfig.psCacheMax;
        waitForConOnStartup = jdbcConfig.waitForConOnStartup;
        maxActive = jdbcConfig.maxActive;
        maxIdle = jdbcConfig.maxIdle;
        minIdle = jdbcConfig.minIdle;
        reserved = jdbcConfig.reserved;
        testOnAlloc = jdbcConfig.testOnAlloc;
        testOnReturn = jdbcConfig.testOnRelease;
        testOnException = jdbcConfig.testOnException;
        testWhenIdle = jdbcConfig.testWhenIdle;
        retryIntervalMs = jdbcConfig.retryIntervalMs > 0
                ? jdbcConfig.retryIntervalMs
                : 100;
        retryCount = jdbcConfig.retryCount;
        conTimeout = jdbcConfig.conTimeout;
        testInterval = jdbcConfig.testInterval;
        blockWhenFull = jdbcConfig.blockWhenFull;
        maxConAge = jdbcConfig.maxConAge;
        isolationLevel = jdbcConfig.isolationLevel;

        setValidateSQL(trimToNull(jdbcConfig.validateSQL));
        setInitSQL(trimToNull(jdbcConfig.initSQL));

        if (sqlDriver != null) {
            if (psCacheMax == 0) {
                psCacheMax = sqlDriver.getDefaultPsCacheMax();
            }
            clearBatch = sqlDriver.isClearBatchRequired();
            if (validateSQL == null) {
                setValidateSQL(sqlDriver.getConnectionValidateSQL());
            }
            if (initSQL == null) {
                setInitSQL(sqlDriver.getConnectionInitSQL());
            }
            if (!sqlDriver.isSetTransactionIsolationLevelSupported()) {
                isolationLevel = 0;
            }
        }

        // sanity check some settings
        if (maxIdle > maxActive) maxIdle = maxActive;
        if (minIdle > maxIdle) minIdle = maxIdle;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        return s;
    }

    public void init() {
        if (cleanupThread != null) return;
        cleanupThread = new Thread(this, "VOA Pool " + url);
        cleanupThread.setDaemon(true);
        cleanupThread.setPriority(Thread.MIN_PRIORITY);
        cleanupThread.start();
    }

    /**
     * Check that we can connect to the database and that the initSQL (if any)
     * works.
     */
    public void check() throws Exception {
        PooledConnection con = null;
        try {
            if (waitForConOnStartup) {
                con = createPooledConnection(0, 10000);
            } else {
                con = createPooledConnection(-1, 10000);
            }
            if (!validateConnection(con)) {
                throw BindingSupportImpl.getInstance().runtime("First connection failed validation SQL:\n" +
                        validateSQL);
            }
        } finally {
            if (con != null) destroy(con);
        }
    }

    /**
     * Create a connection. If this fails then sleep for millis and try again.
     * If retryCount is 0 then retry is done forever, if < 0 then there are
     * no retries.
     */
    private Connection createRealConWithRetry(int retryCount, int millis) {
        for (int n = 0; ;) {
            try {
                return createRealCon();
            } catch (RuntimeException e) {
            	if( BindingSupportImpl.getInstance().isOwnDatastoreException(e) )
            	{
	                if (retryCount < 0 || (retryCount > 0 && ++n > retryCount)) throw e;
	                if (pes.isWarning()) {
	                    JdbcLogEvent ev = new JdbcLogEvent(0,
	                            JdbcLogEvent.POOL_CON_FAILED,
	                            n + " " + e.toString());
	                    ev.updateTotalMs();
	                    pes.log(ev);
	                }
	                try {
	                    Thread.sleep(millis);
	                } catch (InterruptedException e1) {
	                    // ignore the interruption
	                }
				}
				else
				{
					throw e;	
				}	                
            }
        }
    }

    public int getIsolationLevel() {
        return isolationLevel;
    }

    /**
     * If isolationLevel != 0 then the isolation level of newly created
     * Connections is set to this.
     *
     * @see Connection#TRANSACTION_READ_COMMITTED etc
     */
    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public void setClearBatch(boolean clearBatch) {
        this.clearBatch = clearBatch;
    }

    public boolean isClearBatch() {
        return clearBatch;
    }

    public boolean isJdbcDisablePsCache() {
        return jdbcDisablePsCache;
    }

    public void setJdbcDisablePsCache(boolean jdbcDisablePsCache) {
        this.jdbcDisablePsCache = jdbcDisablePsCache;
    }

    public int getPsCacheMax() {
        return psCacheMax;
    }

    public void setPsCacheMax(int psCacheMax) {
        this.psCacheMax = psCacheMax;
    }

    public String getInitSQL() {
        return initSQL;
    }

    public void setInitSQL(String initSQL) {
        String s = endsWithCommit(initSQL);
        if (s != null) {
            commitAfterInitSQL = true;
            this.initSQL = s;
        } else {
            commitAfterInitSQL = false;
            this.initSQL = initSQL;
        }
    }

    /**
     * If s ends with ;[{ws}]commit[;][{ws}] then return s minus this part. Otherwise
     * return null.
     */
    private String endsWithCommit(String s) {
        if (s == null) return null;
        s = s.trim();
        if (!s.endsWith("commit") && !s.endsWith("COMMIT")) return null;
        for (int i = s.length() - 7; i >= 0; i--) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                if (c == ';') {
                    return s.substring(0, i);
                } else {
                    break;
                }
            }
        }
        return null;
    }

    public String getValidateSQL() {
        return validateSQL;
    }

    public void setValidateSQL(String validateSQL) {
        this.validateSQL = validateSQL;
    }

    public int getConTimeout() {
        return conTimeout;
    }

    public void setConTimeout(int conTimeout) {
        this.conTimeout = conTimeout;
    }

    public int getTestInterval() {
        return testInterval;
    }

    public void setTestInterval(int testInterval) {
        this.testInterval = testInterval;
    }

    public boolean isTestWhenIdle() {
        return testWhenIdle;
    }

    public void setTestWhenIdle(boolean on) {
        testWhenIdle = on;
    }

    public boolean isBlockWhenFull() {
        return blockWhenFull;
    }

    public void setBlockWhenFull(boolean blockWhenFull) {
        this.blockWhenFull = blockWhenFull;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
        if (cleanupThread != null) cleanupThread.interrupt();
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
        if (cleanupThread != null) cleanupThread.interrupt();
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
        if (cleanupThread != null) cleanupThread.interrupt();
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getIdleCount() {
        return idleCount;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public int getMaxConAge() {
        return maxConAge;
    }

    public void setMaxConAge(int maxConAge) {
        this.maxConAge = maxConAge;
    }

    /**
     * Fill s with status info for this pool.
     */
    public void fillStatus(PoolStatus s) {
        s.fill(maxActive, activeCount, maxIdle, idleCount);
    }

    /**
     * Get our JDBC URL.
     */
    public String getURL() {
        return url;
    }

    /**
     * Return the connection prop of the pool.
     */
    public Properties getConnectionProperties() {
        return userProps;
    }

    /**
     * Get the JDBC driver instance.
     */
    public Driver getJdbcDriver() {
        return jdbcDriver;
    }

    /**
     * Get the JDBC driver class or null if not known.
     */
    public String getDriverName() {
        return jdbcDriver == null ? "(unknown)" : jdbcDriver.getClass().getName();
    }

    /**
     * Allocate a PooledConnection from the pool.
     *
     * @param highPriority If this is true then reserved high priority
     * @param autoCommit Must the connection have autoCommit set?
     */
    public Connection getConnection(boolean highPriority,
            boolean autoCommit) throws SQLException {
        // adjust local maxActive to maintain reserved connections if needed
        int maxActive = this.maxActive - (highPriority ? 0 : reserved);
        PooledConnection con = null;
        for (; ;) {
            synchronized (this) {
                for (; ;) {
                    if (closed) {
                        throw BindingSupportImpl.getInstance().fatal(
                                "Connection pool has been closed");
                    }
                    if (activeCount >= maxActive) {
                        if (pes.isWarning()) {
                            JdbcPoolEvent event = new JdbcPoolEvent(0,
                                    JdbcLogEvent.POOL_FULL, highPriority);
                            update(event);
                            pes.log(event);
                            fullCount++;
                        }
                        if (blockWhenFull) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                // ignore
                            }
                        } else {
                            throw BindingSupportImpl.getInstance().poolFull(
                                    "JDBC connection pool is full: " + this);
                        }
                    } else {
                        con = removeFromIdleHead(autoCommit);
                        ++activeCount;
                        break;
                    }
                }
            }
            if (con == null) {
                try {
                    waitCount++;
                    con = createPooledConnection(retryCount, retryIntervalMs);
                } finally {
                    if (con == null) {
                        synchronized (this) {
                            --activeCount;
                        }
                    }
                }
                break;
            } else {
                if (testOnAlloc && !validateConnection(con)) {
                    destroy(con);
                    synchronized (this) {
                        --activeCount;
                    }
                } else {
                    break;
                }
            }
        }
        // make sure the autoCommit status on the con is correct
        if (con.getCachedAutoCommit() != autoCommit) {
            if (autoCommit) { // do rollback if going from false -> true
                con.rollback();
            }
            con.setAutoCommit(autoCommit);
        }
        con.updateLastActivityTime();
        addToActiveHead(con);
        log(0, JdbcLogEvent.POOL_ALLOC, con, highPriority);
        allocatedCount++;
        return con;
    }

    /**
     * Test 1 idle connection. If it fails, close it and repeat.
     */
    public void testIdleConnections() {
        for (; ;) {
            PooledConnection con;
            synchronized (this) {
                // don't test if the pool is almost full - this will just
                // reduce throughput by making some thread wait for a con
                if (activeCount >= (maxActive - reserved - 1)) break;
                con = removeFromIdleHead(false);
                if (con == null) break;
                ++activeCount;
            }
            if (validateConnection(con)) {
                synchronized (this) {
                    addToIdleTail(con);
                    --activeCount;
                }
                break;
            } else {
                destroy(con);
                synchronized (this) {
                    --activeCount;
                }
            }
        }
    }

    /**
     * Check the integrity of the double linked with head and tail.
     */
    private void checkList(PooledConnection tail, PooledConnection head,
            int size) {
        if (tail == null) {
            testTrue(head == null);
            return;
        }
        if (head == null) {
            testTrue(tail == null);
            return;
        }
        checkList(tail, size);
        checkList(head, size);
        testTrue(tail.prev == null);
        testTrue(head.next == null);
    }

    /**
     * Check the integrity of the double linked list containing pc.
     */
    private void checkList(PooledConnection pc, int size) {
        if (pc == null) return;
        int c = -1;
        // check links to tail
        for (PooledConnection i = pc; i != null; i = i.prev) {
            if (i.prev != null) testTrue(i.prev.next == i);
            ++c;
        }
        // check links to head
        for (PooledConnection i = pc; i != null; i = i.next) {
            if (i.next != null) testTrue(i.next.prev == i);
            ++c;
        }
        if (size >= 0) {
            testEquals(size, c);
        }
    }

    private static void testEquals(int a, int b) {
        if (a != b) {
            throw BindingSupportImpl.getInstance().internal(
                    "assertion failed: expected " + a + " got " + b);
        }
    }

    private static void testTrue(boolean t) {
        if (!t) {
            throw BindingSupportImpl.getInstance().internal(
                    "assertion failed: expected true");
        }
    }

    public void returnConnection(Connection con) throws SQLException {
        con.close();
    }

    /**
     * Return a PooledConnection to the pool. This is called by
     * PooledConnection when it is closed. This is a NOP if the connection
     * has been destroyed.
     *
     * @see PooledConnection#close
     */
    public void returnConnection(PooledConnection con) {
        if (con.isDestroyed()) return;
        if ((testOnReturn || testOnException && con.isNeedsValidation())
                && !validateConnection(con)) {
            removeFromActiveList(con);
            destroy(con);
        } else if (maxConAge > 0 && ++con.age >= maxConAge) {
            ++expiredCount;
            if (pes.isFine()) {
                JdbcLogEvent ev = new JdbcLogEvent(0,
                        JdbcLogEvent.POOL_CON_EXPIRED,
                        Integer.toString(con.age));
                ev.updateTotalMs();
                pes.log(ev);
            }
            removeFromActiveList(con);
            destroy(con);
        } else {
            synchronized (this) {
                removeFromActiveList(con);
                addToIdleTail(con);
            }
        }
        log(0, JdbcLogEvent.POOL_RELEASE, con, false);
    }

    /**
     * Close all idle connections. This method closes idleCount connections.
     * If another thread is making new connections at the same time the
     * idle list will not be empty on return.
     */
    public void closeIdleConnections() {
        for (int i = idleCount; i > 0; i--) {
            PooledConnection con = removeFromIdleHead(false);
            if (con == null) break;
            destroy(con);
        }
        if (cleanupThread != null) cleanupThread.interrupt();
    }

    /**
     * Destroy a connection silently ignoring SQLException's.
     */
    private void destroy(PooledConnection con) {
        closedCount++;
        con.destroy();
    }

    /**
     * Close all connections and shutdown the pool.
     */
    public void destroy() {
        closed = true;
        if (cleanupThread != null) cleanupThread.interrupt();
        for (; ;) {
            PooledConnection con = removeFromIdleHead(false);
            if (con == null) break;
            destroy(con);
        }
        // this will cause any threads waiting for connections to get a
        // closed exception
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Close excess idle connections or create new idle connections if less
     * than minIdle (but do not exceed maxActive in total).
     */
    public void checkIdleConnections() throws Exception {
        // close excess idle connections
        for (; idleCount > maxIdle;) {
            PooledConnection con = removeFromIdleHead(false);
            if (con == null) break;
            destroy(con);
        }
        // Start creating a new connection if there is space in the pool for 2
        // more. Only add the connection to the pool once created if there is
        // space for 1 more.
        for (; ;) {
            if (!needMoreIdleConnections(1)) break;
            PooledConnection con = createPooledConnection(retryCount,
                    retryIntervalMs);
            synchronized (this) {
                if (needMoreIdleConnections(0)) {
                    addToIdleTail(con);
                    continue;
                }
            }
            destroy(con);
            break;
        }
    }

    private synchronized boolean needMoreIdleConnections(int space) {
        return idleCount < minIdle && idleCount + activeCount < (maxActive - space);
    }

    /**
     * Close active connections that have been out of the pool for too long.
     * This is a NOP if activeTimeout <= 0.
     */
    public void closeTimedOutConnections() {
        if (conTimeout <= 0) return;
        for (; ;) {
            PooledConnection con;
            synchronized (this) {
                if (activeTail == null) return;
                long t = activeTail.getLastActivityTime();
                if (t == 0) return;
                int s = (int)((System.currentTimeMillis() - t) / 1000);
                if (s < conTimeout) return;
                con = activeTail;
                removeFromActiveList(con);
            }
            timedOutCount++;
            if (pes.isWarning()) {
                JdbcPoolEvent event = new JdbcPoolEvent(0,
                        JdbcLogEvent.POOL_CON_TIMEOUT, false);
                event.setConnectionID(System.identityHashCode(con));
                update(event);
                pes.log(event);
            }
            destroy(con);
        }
    }

    /**
     * Return the con at the head of the idle list removing it from the list.
     * If there is no idle con then null is returned. The con has its idle
     * flag cleared.
     *
     * @param autoCommit An attempt is made to return a connction with
     *      autoCommit in this state. If this is not possible a connection
     *      with a different setting may be returned.
     */
    private synchronized PooledConnection removeFromIdleHead(boolean autoCommit) {
        PooledConnection ans = removeFromIdleHeadImp(autoCommit);
        return ans == null ? removeFromIdleHeadImp(!autoCommit) : ans;
    }

    private PooledConnection removeFromIdleHeadImp(boolean autoCommit) {
        PooledConnection con;
        if (autoCommit) {
            con = idleHeadAC;
            if (con == null) return null;
            idleHeadAC = con.prev;
            con.prev = null;
            if (idleHeadAC == null) {
                idleTailAC = null;
            } else {
                idleHeadAC.next = null;
            }
        } else {
            con = idleHead;
            if (con == null) return null;
            idleHead = con.prev;
            con.prev = null;
            if (idleHead == null) {
                idleTail = null;
            } else {
                idleHead.next = null;
            }
        }
        --idleCount;
        con.idle = false;
        if (Debug.DEBUG) {
            checkList(idleTail, idleHead, -1);
            checkList(idleTailAC, idleHeadAC, -1);
        }
        return con;
    }

    /**
     * Add con to the tail of the idle list. The con has its idle flag set.
     * This will notify any blocked threads so they can get the newly idle
     * connection.
     */
    private synchronized void addToIdleTail(PooledConnection con) {
        if (Debug.DEBUG) {
            if (con.prev != null || con.next != null) {
                throw BindingSupportImpl.getInstance().internal("con belongs to a list");
            }
        }
        con.idle = true;
        if (con.getCachedAutoCommit()) {
            if (idleTailAC == null) {
                idleHeadAC = idleTailAC = con;
            } else {
                con.next = idleTailAC;
                idleTailAC.prev = con;
                idleTailAC = con;
            }
        } else {
            if (idleTail == null) {
                idleHead = idleTail = con;
            } else {
                con.next = idleTail;
                idleTail.prev = con;
                idleTail = con;
            }
        }
        ++idleCount;
        if (Debug.DEBUG) {
            checkList(idleTail, idleHead, -1);
            checkList(idleTailAC, idleHeadAC, -1);
        }
        notify();
    }

    /**
     * Add con to the head of the active list. Note that this does not
     * bump up activeCount.
     */
    private synchronized void addToActiveHead(PooledConnection con) {
        if (Debug.DEBUG) {
            if (con.prev != null || con.next != null) {
                throw BindingSupportImpl.getInstance().internal("con belongs to a list");
            }
        }
        if (activeHead == null) {
            activeHead = activeTail = con;
        } else {
            con.prev = activeHead;
            activeHead.next = con;
            activeHead = con;
        }
        if (Debug.DEBUG) checkList(activeTail, activeHead, -1);
    }

    /**
     * Remove con from the active list.
     */
    private synchronized void removeFromActiveList(PooledConnection con) {
        if (con.prev != null) {
            con.prev.next = con.next;
        } else {
            activeTail = con.next;
        }
        if (con.next != null) {
            con.next.prev = con.prev;
        } else {
            activeHead = con.prev;
        }
        con.prev = con.next = null;
        --activeCount;
        if (Debug.DEBUG) checkList(activeTail, activeHead, -1);
    }

    private JdbcPoolEvent log(long txId, int type, PooledConnection con,
            boolean highPriority) {
        if (pes.isFine()) {
            JdbcPoolEvent event = new JdbcPoolEvent(txId, type, highPriority);
            event.setAutoCommit(con.getCachedAutoCommit());
            event.setConnectionID(System.identityHashCode(con));
            update(event);
            pes.log(event);
            return event;
        }
        return null;
    }

    private void update(JdbcPoolEvent ev) {
        ev.update(maxActive, activeCount, maxIdle, idleCount);
    }

    private Connection createRealCon() {
        JdbcConnectionEvent ev = null;
        if (pes.isFine()) {
            ev = new JdbcConnectionEvent(0, null, url,
                    JdbcConnectionEvent.CON_OPEN);
            pes.log(ev);
        }
        Connection realCon = null;
        try {
            realCon = jdbcDriver.connect(url, props);
            if (realCon == null) {
                throw BindingSupportImpl.getInstance().fatalDatastore(
                        formatConnectionErr("Unable to connect to " + url));
            }
        } catch (SQLException x) {
            RuntimeException e = sqlDriver.mapException(x,
                    formatConnectionErr("Unable to connect to " + url + ":\n" + x),
                    false);
            if (ev != null) {
                ev.setErrorMsg(e);
            }
            throw e;
        } catch (RuntimeException x) {
            if (ev != null) {
                ev.setErrorMsg(x);
            }
            throw x;
        } finally {
            if (ev != null) {
                ev.updateTotalMs();
            }
        }
        createdCount++;
        return realCon;
    }

    private String formatConnectionErr(String msg) {
        StringBuffer s = new StringBuffer();
        s.append(msg);
        s.append("\nJDBC Driver: " + jdbcDriver.getClass().getName());
        ArrayList a = new ArrayList(props.keySet());
        Collections.sort(a);
        for (Iterator i = a.iterator(); i.hasNext(); ) {
            String p = (String)i.next();
            Object v = "password".equals(p) ? "(hidden)" : props.get(p);
            s.append('\n');
            s.append(p);
            s.append('=');
            s.append(v);
        }
        return s.toString();
    }

    /**
     * Create an initialize a PooledConnection. This will run the initSQL
     * and do setAutoCommit(false). It will not add it to the idle or active
     * lists.
     */
    private PooledConnection createPooledConnection(int retryCount,
            int retryIntervalMs) throws SQLException {
        Connection realCon = createRealConWithRetry(retryCount,
                retryIntervalMs);
        PooledConnection pooledCon = new PooledConnection(
                JDBCConnectionPool.this, realCon, pes,
                !jdbcDisablePsCache, psCacheMax);
        boolean ok = false;
        try {
            initConnection(pooledCon);
            pooledCon.setAutoCommit(false);
            if (isolationLevel != 0) {
                pooledCon.setTransactionIsolation(isolationLevel);
            }
            ok = true;
        } finally {
            if (!ok && realCon != null) {
                try {
                    pooledCon.closeRealConnection();
                } catch (SQLException x) {
                    // ignore
                }
            }
        }
        return pooledCon;
    }

    /**
     * Initialize the connection with the initSQL (if any).
     */
    private void initConnection(Connection con) {
        if (initSQL == null) return;
        Statement stat = null;
        try {
            stat = con.createStatement();
            stat.execute(initSQL);
        } catch (SQLException e) {
			throw sqlDriver.mapException(e,
                    "Error executing initSQL on new Connection: " + e +
				    "\n" + initSQL, true);
        } finally {
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
        if (commitAfterInitSQL) {
            try {
                con.commit();
            } catch (SQLException e) {
				throw sqlDriver.mapException(e,
                        "Error doing commit after executing initSQL on new Connection: " + e + "\n" +
					    initSQL, true);
            }
        }
    }

    /**
     * Check that the connection is open and if so validate the connection with
     * validateSQL (if any). Returns true if the connection is ok.
     */
    private boolean validateConnection(PooledConnection con) {
        validatedCount++;
        boolean ans = validateConnectionImp(con);
        if (!ans) badCount++;
        return ans;
    }

    private boolean validateConnectionImp(PooledConnection con) {
        try {
            if (con.isClosed()) return false;
            if (validateSQL != null) {
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    ps = con.prepareStatement(validateSQL);
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        if (pes.isWarning()) {
                            JdbcLogEvent ev = new JdbcLogEvent(0,
                                    JdbcLogEvent.POOL_BAD_CON,
                                    "No row returned");
                            ev.updateTotalMs();
                            pes.log(ev);
                        }
                        return false;
                    }
                } finally {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                }
                if (!con.getCachedAutoCommit()) {
                    con.rollback();
                }
            }
            return true;
        } catch (SQLException e) {
            if (pes.isWarning()) {
                JdbcLogEvent ev = new JdbcLogEvent(0,
                        JdbcLogEvent.POOL_BAD_CON, e.toString());
                ev.updateTotalMs();
                pes.log(ev);
            }
            return false;
        }
    }

    
    
    /**
     * Perform maintenance operations on the pool at periodic intervals.
     */
    public void run() {
        
        for (; ;) {
              
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // ignore
            }
             
            if (closed) break;

            if (duration(timeLastTest) >= testInterval) {
                timeLastTest = System.currentTimeMillis();
                if (testWhenIdle) testIdleConnections();
                if (conTimeout > 0) closeTimedOutConnections();
            }

            try {
                checkIdleConnections();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private long duration(long start) {
        return (System.currentTimeMillis() - start) / 1000;
    }

    /**
     * Return our name and status.
     */
    public String toString() {
        PoolStatus ps = new PoolStatus(url);
        fillStatus(ps);
        return ps.toString();
    }

    /**
     * Add all BaseMetric's for this store to the List.
     */
    public void addMetrics(List list) {
        list.add(metricActive = new BaseMetric("JDBCPoolActive",
                "Pool Active", CAT_POOL, "Number of active JDBC connections in pool",
                0, Metric.CALC_AVERAGE));
        list.add(metricMaxActive = new BaseMetric("JDBCPoolMaxActive",
                "Pool Max Active", CAT_POOL, "Max number of JDBC connections allowed in pool",
                0, Metric.CALC_AVERAGE));
        list.add(metricIdle = new BaseMetric("JDBCPoolIdle",
                "Pool Idle", CAT_POOL, "Number of idle JDBC connections in pool",
                0, Metric.CALC_AVERAGE));
        list.add(metricWait = new BaseMetric("JDBCPoolWait",
                "Pool Wait", CAT_POOL, "Number of times that a caller had to wait for a connection",
                0, Metric.CALC_DELTA));
        list.add(metricFull = new BaseMetric("JDBCPoolFull",
                "Pool Full", CAT_POOL, "Number of times that the pool was full and a connection was needed",
                0, Metric.CALC_DELTA));
        list.add(metricTimedOut = new BaseMetric("JDBCConTimedOut",
                "Con Timed Out", CAT_POOL, "Number of active JDBC connections timed out and closed",
                0, Metric.CALC_DELTA));
        list.add(metricExpired = new BaseMetric("JDBCConExpired",
                "Con Expired", CAT_POOL, "Number of JDBC connections closed due to their age reaching the maximum lifespan",
                0, Metric.CALC_DELTA));
        list.add(metricBad = new BaseMetric("JDBCConBad",
                "Con Bad", CAT_POOL, "Number of JDBC connections that failed validation test",
                0, Metric.CALC_DELTA));
        list.add(metricCreated = new BaseMetric("JDBCConCreated",
                "Con Created", CAT_POOL, "Number of JDBC connections created",
                0, Metric.CALC_DELTA));
        list.add(metricClosed = new BaseMetric("JDBCConClosed",
                "Con Closed", CAT_POOL, "Number of JDBC connections closed",
                0, Metric.CALC_DELTA));
        list.add(metricAllocated = new BaseMetric("JDBCConAllocated",
                "Con Allocated", CAT_POOL, "Number of JDBC connections given out by the pool",
                3, Metric.CALC_DELTA_PER_SECOND));
        list.add(metricValidated = new BaseMetric("JDBCConValidated",
                "Con Validated", CAT_POOL, "Number of JDBC connections tested by the pool",
                0, Metric.CALC_DELTA));
        list.add(new PercentageMetric("JdbcPoolPercentFull",
                "Pool % Full ", CAT_POOL,
                "Active connections as a percentage of the maximum",
                metricActive, metricMaxActive));
    }

    /**
     * Get values for our metrics.
     */
    public void sampleMetrics(int[][] buf, int pos) {
        buf[metricActive.getIndex()][pos] = activeCount;
        buf[metricMaxActive.getIndex()][pos] = maxActive;
        buf[metricIdle.getIndex()][pos] = idleCount;
        buf[metricWait.getIndex()][pos] = waitCount;
        buf[metricFull.getIndex()][pos] = fullCount;
        buf[metricTimedOut.getIndex()][pos] = timedOutCount;
        buf[metricExpired.getIndex()][pos] = expiredCount;
        buf[metricBad.getIndex()][pos] = badCount;
        buf[metricAllocated.getIndex()][pos] = allocatedCount;
        buf[metricValidated.getIndex()][pos] = validatedCount;
        buf[metricCreated.getIndex()][pos] = createdCount;
        buf[metricClosed.getIndex()][pos] = closedCount;
    }

}

