// https://searchcode.com/api/result/12062004/

/*
  $Id: AbstractConnectionPool.java 2300 2012-03-02 17:04:59Z dfisher $

  Copyright (C) 2003-2012 Virginia Tech.
  All rights reserved.

  SEE LICENSE FOR MORE INFORMATION

  Author:  Middleware Services
  Email:   middleware@vt.edu
  Version: $Revision: 2300 $
  Updated: $Date: 2012-03-02 18:04:59 +0100 (Fri, 02 Mar 2012) $
*/
package org.ldaptive.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.ldaptive.Connection;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.LdapUtil;
import org.ldaptive.Response;

/**
 * Contains the base implementation for pooling connections. The main design
 * objective for the supplied pooling implementations is to provide a pool that
 * does not block on connection creation or destruction. This is what accounts
 * for the multiple locks available on this class. The pool is backed by two
 * queues, one for available connections and one for active connections.
 * Connections that are available via {@link #getConnection()} exist in the
 * available queue. Connections that are actively in use exist in the active
 * queue.
 *
 * @author  Middleware Services
 * @version  $Revision: 2300 $ $Date: 2012-03-02 18:04:59 +0100 (Fri, 02 Mar 2012) $
 */
public abstract class AbstractConnectionPool extends AbstractPool<Connection>
{

  /** Lock for the entire pool. */
  protected final ReentrantLock poolLock = new ReentrantLock();

  /** Condition for notifying threads that a connection was returned. */
  protected final Condition poolNotEmpty = poolLock.newCondition();

  /** Lock for check ins. */
  protected final ReentrantLock checkInLock = new ReentrantLock();

  /** Lock for check outs. */
  protected final ReentrantLock checkOutLock = new ReentrantLock();

  /** List of available connections in the pool. */
  protected final Queue<PooledConnectionHandler> available =
    new LinkedList<PooledConnectionHandler>();

  /** List of connections in use. */
  protected final Queue<PooledConnectionHandler> active =
    new LinkedList<PooledConnectionHandler>();

  /** Connection factory to create connections with. */
  private DefaultConnectionFactory connectionFactory;

  /** Whether to connect to the ldap on connection creation. */
  private boolean connectOnCreate = true;

  /** Executor for scheduling pool tasks. */
  private ScheduledExecutorService poolExecutor =
    Executors.newSingleThreadScheduledExecutor(
      new ThreadFactory() {
        public Thread newThread(final Runnable r)
        {
          final Thread t = new Thread(r);
          t.setDaemon(true);
          return t;
        }
      });


  /**
   * Returns the connection factory for this pool.
   *
   * @return  connection factory
   */
  public DefaultConnectionFactory getConnectionFactory()
  {
    return connectionFactory;
  }


  /**
   * Sets the connection factory for this pool.
   *
   * @param  cf  connection factory
   */
  public void setConnectionFactory(final DefaultConnectionFactory cf)
  {
    connectionFactory = cf;
  }


  /**
   * Returns whether connections will attempt to connect after creation. Default
   * is true.
   *
   * @return  whether connections will attempt to connect after creation
   */
  public boolean getConnectOnCreate()
  {
    return connectOnCreate;
  }


  /**
   * Sets whether newly created connections will attempt to connect. Default is
   * true.
   *
   * @param  b  connect on create
   */
  public void setConnectOnCreate(final boolean b)
  {
    connectOnCreate = b;
  }


  /**
   * Initialize this pool for use. Once invoked the pool config is made
   * immutable. See {@link PoolConfig#makeImmutable()}.
   */
  public void initialize()
  {
    logger.debug("beginning pool initialization");

    getPoolConfig().makeImmutable();

    final Runnable prune = new Runnable() {
      public void run()
      {
        logger.debug("Begin prune task for {}", this);
        prune();
        logger.debug("End prune task for {}", this);
      }
    };
    poolExecutor.scheduleAtFixedRate(
      prune,
      getPoolConfig().getPrunePeriod(),
      getPoolConfig().getPrunePeriod(),
      TimeUnit.SECONDS);
    logger.debug("prune pool task scheduled");

    final Runnable validate = new Runnable() {
      public void run()
      {
        logger.debug("Begin validate task for {}", this);
        validate();
        logger.debug("End validate task for {}", this);
      }
    };
    poolExecutor.scheduleAtFixedRate(
      validate,
      getPoolConfig().getValidatePeriod(),
      getPoolConfig().getValidatePeriod(),
      TimeUnit.SECONDS);
    logger.debug("validate pool task scheduled");

    initializePool();

    logger.debug("pool initialized to size {}", available.size());
  }


  /**
   * Attempts to fill the pool to its minimum size.
   *
   * @throws  IllegalStateException  if the pool does not contain at least one
   * connection and it's minimum size is greater than zero
   */
  private void initializePool()
  {
    logger.debug(
      "checking ldap pool size >= {}",
      getPoolConfig().getMinPoolSize());

    int count = 0;
    poolLock.lock();
    try {
      while (
        available.size() < getPoolConfig().getMinPoolSize() &&
          count < getPoolConfig().getMinPoolSize() * 2) {
        final PooledConnectionHandler pc = createAvailableConnection();
        if (getPoolConfig().isValidateOnCheckIn()) {
          if (validate(pc.getConnection())) {
            logger.trace("connection passed initialize validation: {}", pc);
          } else {
            logger.warn("connection failed initialize validation: {}", pc);
            removeAvailableConnection(pc);
          }
        }
        count++;
      }
      if (available.size() == 0 && getPoolConfig().getMinPoolSize() > 0) {
        throw new IllegalStateException("Could not initialize pool");
      }
    } finally {
      poolLock.unlock();
    }
  }


  /** Empty this pool, freeing any resources. */
  public void close()
  {
    poolLock.lock();
    try {
      while (available.size() > 0) {
        final PooledConnectionHandler pc = available.remove();
        pc.getConnection().close();
        logger.trace("destroyed connection: {}", pc);
      }
      while (active.size() > 0) {
        final PooledConnectionHandler pc = active.remove();
        pc.getConnection().close();
        logger.trace("destroyed connection: {}", pc);
      }
      logger.debug("pool closed");
    } finally {
      poolLock.unlock();
    }

    logger.debug("shutting down executor");
    poolExecutor.shutdown();
    logger.debug("executor shutdown");
  }


  /**
   * Returns a connection from the pool.
   *
   * @return  connection
   *
   * @throws  PoolException  if this operation fails
   * @throws  BlockingTimeoutException  if this pool is configured with a block
   * time and it occurs
   * @throws  PoolInterruptedException  if this pool is configured with a block
   * time and the current thread is interrupted
   */
  public abstract Connection getConnection()
    throws PoolException;


  /**
   * Returns a connection to the pool.
   *
   * @param  c  connection
   */
  public abstract void putConnection(final Connection c);


  /**
   * Create a new connection. If {@link #connectOnCreate} is true, the
   * connection will be opened.
   *
   * @return  pooled connection
   */
  protected PooledConnectionHandler createConnection()
  {
    Connection c = connectionFactory.getConnection();
    Response<Void> r = null;
    if (connectOnCreate) {
      try {
        r = c.open();
      } catch (LdapException e) {
        logger.error("unable to connect to the ldap", e);
        c = null;
      }
    }
    if (c != null) {
      return new PooledConnectionHandler(c, r);
    } else {
      return null;
    }
  }


  /**
   * Create a new connection and place it in the available pool.
   *
   * @return  connection that was placed in the available pool
   */
  protected PooledConnectionHandler createAvailableConnection()
  {
    final PooledConnectionHandler pc = createConnection();
    if (pc != null) {
      poolLock.lock();
      try {
        available.add(pc);
      } finally {
        poolLock.unlock();
      }
    } else {
      logger.warn("unable to create available connection");
    }
    return pc;
  }


  /**
   * Create a new connection and place it in the active pool.
   *
   * @return  connection that was placed in the active pool
   */
  protected PooledConnectionHandler createActiveConnection()
  {
    final PooledConnectionHandler pc = createConnection();
    if (pc != null) {
      poolLock.lock();
      try {
        active.add(pc);
      } finally {
        poolLock.unlock();
      }
    } else {
      logger.warn("unable to create active connection");
    }
    return pc;
  }


  /**
   * Remove a connection from the available pool.
   *
   * @param  pc  connection that is in the available pool
   */
  protected void removeAvailableConnection(final PooledConnectionHandler pc)
  {
    boolean destroy = false;
    poolLock.lock();
    try {
      if (available.remove(pc)) {
        destroy = true;
      } else {
        logger.warn("attempt to remove unknown available connection: {}", pc);
      }
    } finally {
      poolLock.unlock();
    }
    if (destroy) {
      logger.trace("removing available connection: {}", pc);
      pc.getConnection().close();
      logger.trace("destroyed connection: {}", pc);
    }
  }


  /**
   * Remove a connection from the active pool.
   *
   * @param  pc  connection that is in the active pool
   */
  protected void removeActiveConnection(final PooledConnectionHandler pc)
  {
    boolean destroy = false;
    poolLock.lock();
    try {
      if (active.remove(pc)) {
        destroy = true;
      } else {
        logger.warn("attempt to remove unknown active connection: {}", pc);
      }
    } finally {
      poolLock.unlock();
    }
    if (destroy) {
      logger.trace("removing active connection: {}", pc);
      pc.getConnection().close();
      logger.trace("destroyed connection: {}", pc);
    }
  }


  /**
   * Remove a connection from both the available and active pools.
   *
   * @param  pc  connection that is in both the available and active pools
   */
  protected void removeAvailableAndActiveConnection(
    final PooledConnectionHandler pc)
  {
    boolean destroy = false;
    poolLock.lock();
    try {
      if (available.remove(pc)) {
        destroy = true;
      } else {
        logger.debug("attempt to remove unknown available connection: {}", pc);
      }
      if (active.remove(pc)) {
        destroy = true;
      } else {
        logger.debug("attempt to remove unknown active connection: {}", pc);
      }
    } finally {
      poolLock.unlock();
    }
    if (destroy) {
      logger.trace("removing active connection: {}", pc);
      pc.getConnection().close();
      logger.trace("destroyed connection: {}", pc);
    }
  }


  /**
   * Attempts to activate and validate a connection. Performed before a
   * connection is returned from {@link #getConnection()}.
   *
   * @param  pc  connection
   *
   * @throws  PoolException  if this method fails
   * @throws  ActivationException  if the connection cannot be activated
   * @throws  ValidationException  if the connection cannot be validated
   */
  protected void activateAndValidateConnection(final PooledConnectionHandler pc)
    throws PoolException
  {
    if (!activate(pc.getConnection())) {
      logger.warn("connection failed activation: {}", pc);
      removeAvailableAndActiveConnection(pc);
      throw new ActivationException("Activation of connection failed");
    }
    if (
      getPoolConfig().isValidateOnCheckOut() &&
        !validate(pc.getConnection())) {
      logger.warn("connection failed check out validation: {}", pc);
      removeAvailableAndActiveConnection(pc);
      throw new ValidationException("Validation of connection failed");
    }
  }


  /**
   * Attempts to validate and passivate a connection. Performed when a
   * connection is given to {@link #putConnection(Connection)}.
   *
   * @param  pc  connection
   *
   * @return  whether both validate and passivation succeeded
   */
  protected boolean validateAndPassivateConnection(
    final PooledConnectionHandler pc)
  {
    boolean valid = false;
    if (getPoolConfig().isValidateOnCheckIn()) {
      if (!validate(pc.getConnection())) {
        logger.warn("connection failed check in validation: {}", pc);
      } else {
        valid = true;
      }
    } else {
      valid = true;
    }
    if (valid && !passivate(pc.getConnection())) {
      valid = false;
      logger.warn("connection failed passivation: {}", pc);
    }
    return valid;
  }


  /**
   * Attempts to reduce the size of the pool back to it's configured minimum.
   * {@link PoolConfig#setMinPoolSize(int)}.
   */
  public void prune()
  {
    logger.trace(
      "waiting for pool lock to prune {}",
      poolLock.getQueueLength());
    poolLock.lock();
    try {
      if (active.size() == 0) {
        logger.debug("pruning pool of size {}", available.size());
        while (available.size() > getPoolConfig().getMinPoolSize()) {
          PooledConnectionHandler pc = available.peek();
          final long time = System.currentTimeMillis() - pc.getCreatedTime();
          if (
            time >
              TimeUnit.SECONDS.toMillis(getPoolConfig().getExpirationTime())) {
            pc = available.remove();
            logger.trace("removing {} in the pool for {}ms", pc, time);
            pc.getConnection().close();
            logger.trace("destroyed connection: {}", pc);
          } else {
            break;
          }
        }
        logger.debug("pool size pruned to {}", available.size());
      } else {
        logger.debug("pool is currently active, no connections pruned");
      }
    } finally {
      poolLock.unlock();
    }
  }


  /**
   * Attempts to validate all objects in the pool. {@link
   * PoolConfig#setValidatePeriodically(boolean)}.
   */
  public void validate()
  {
    poolLock.lock();
    try {
      if (active.size() == 0) {
        if (getPoolConfig().isValidatePeriodically()) {
          logger.debug("validate for pool of size {}", available.size());

          final Queue<PooledConnectionHandler> remove =
            new LinkedList<PooledConnectionHandler>();
          for (PooledConnectionHandler pc : available) {
            logger.trace("validating {}", pc);
            if (validate(pc.getConnection())) {
              logger.trace("connection passed validation: {}", pc);
            } else {
              logger.warn("connection failed validation: {}", pc);
              remove.add(pc);
            }
          }
          for (PooledConnectionHandler pc : remove) {
            logger.trace("removing {} from the pool", pc);
            available.remove(pc);
            pc.getConnection().close();
            logger.trace("destroyed connection: {}", pc);
          }
        }
        initializePool();
        logger.debug("pool size after validation is {}", available.size());
      } else {
        logger.debug("pool is currently active, no validation performed");
      }
    } finally {
      poolLock.unlock();
    }
  }


  /**
   * Returns the number of connections available for use.
   *
   * @return  count
   */
  public int availableCount()
  {
    return available.size();
  }


  /**
   * Returns the number of connections in use.
   *
   * @return  count
   */
  public int activeCount()
  {
    return active.size();
  }


  /**
   * Creates a connection proxy using the supplied pool connection.
   *
   * @param  pc  pool connection to create proxy with
   *
   * @return  connection proxy
   */
  protected Connection createConnectionProxy(final PooledConnectionHandler pc)
  {
    return
      (Connection) Proxy.newProxyInstance(
        Connection.class.getClassLoader(),
        new Class[] {Connection.class},
        pc);
  }


  /**
   * Retrieves the invocation handler from the supplied connection proxy.
   *
   * @param  proxy  connection proxy
   *
   * @return  pooled connection handler
   */
  protected PooledConnectionHandler retrieveInvocationHandler(
    final Connection proxy)
  {
    return (PooledConnectionHandler) Proxy.getInvocationHandler(proxy);
  }


  /**
   * Called by the garbage collector on an object when garbage collection
   * determines that there are no more references to the object.
   *
   * @throws  Throwable  if an exception is thrown by this method
   */
  @Override
  protected void finalize()
    throws Throwable
  {
    try {
      close();
    } finally {
      super.finalize();
    }
  }


  /**
   * Provides a descriptive string representation of this instance.
   *
   * @return  string representation
   */
  @Override
  public String toString()
  {
    return
      String.format(
        "[%s@%d::connectOnCreate=%s, connectionFactory=%s, poolConfig=%s]",
        getClass().getName(),
        hashCode(),
        connectOnCreate,
        connectionFactory,
        getPoolConfig());
  }


  /**
   * Contains a connection that is participating in this pool. Used to track how
   * long a connection has been in use and override close invocations.
   *
   * @author  Middleware Services
   * @version  $Revision: 2300 $ $Date: 2012-03-02 18:04:59 +0100 (Fri, 02 Mar 2012) $
   */
  protected class PooledConnectionHandler implements InvocationHandler
  {

    /** hash code seed. */
    private static final int HASH_CODE_SEED = 503;

    /** Underlying connection. */
    private Connection conn;

    /** Response produced when the connection was opened. */
    private Response<Void> openResponse;

    /** Time this connection was created. */
    private long createdTime = System.currentTimeMillis();


    /**
     * Creates a new pooled connection.
     *
     * @param  c  connection to participate in this pool
     * @param  r  response produced by opening the connection
     */
    public PooledConnectionHandler(final Connection c, final Response<Void> r)
    {
      conn = c;
      openResponse = r;
    }


    /**
     * Returns the connection.
     *
     * @return  underlying connection
     */
    public Connection getConnection()
    {
      return conn;
    }


    /**
     * Returns the time this connection was created.
     *
     * @return  creation time
     */
    public long getCreatedTime()
    {
      return createdTime;
    }


    /**
     * Returns whether the supplied object is the same as this one.
     *
     * @param  o  to compare against
     *
     * @return  whether the supplied object is the same as this one
     */
    @Override
    public boolean equals(final Object o)
    {
      if (o == null) {
        return false;
      }
      return
        o == this || (getClass() == o.getClass() &&
          o.hashCode() == hashCode());
    }


    /**
     * Returns the hash code for this object.
     *
     * @return  hash code
     */
    @Override
    public int hashCode()
    {
      return LdapUtil.computeHashCode(HASH_CODE_SEED, conn);
    }


    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(
      final Object proxy,
      final Method method,
      final Object[] args)
      throws Throwable
    {
      Object retValue = null;
      if ("open".equals(method.getName())) {
        if (!conn.isOpen()) {
          openResponse = (Response<Void>) method.invoke(conn, args);
        }
        retValue = openResponse;
      } else if ("close".equals(method.getName())) {
        putConnection((Connection) proxy);
      } else {
        retValue = method.invoke(conn, args);
      }
      return retValue;
    }
  }
}

