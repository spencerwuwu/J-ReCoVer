// https://searchcode.com/api/result/4465631/

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.config;

import org.infinispan.CacheException;
import org.infinispan.commons.hash.Hash;
import org.infinispan.commons.hash.MurmurHash3;
import org.infinispan.config.FluentConfiguration.*;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.container.DataContainer;
import org.infinispan.container.DefaultDataContainer;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.DefaultConsistentHash;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionThreadPolicy;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.SurvivesRestarts;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.remoting.ReplicationQueue;
import org.infinispan.remoting.ReplicationQueueImpl;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionSynchronizationRegistryLookup;
import org.infinispan.util.InfinispanCollections;
import org.infinispan.util.TypedProperties;
import org.infinispan.util.Util;
import org.infinispan.util.concurrent.IsolationLevel;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.infinispan.config.Configuration.CacheMode.*;

/**
 * Encapsulates the configuration of a Cache. Configures the default cache which can be retrieved via
 * CacheManager.getCache(). These default settings are also used as a starting point when configuring namedCaches, since
 * the default settings are inherited by any named cache.
 * <p />
 * @deprecated This class is deprecated.  Use {@link org.infinispan.configuration.cache.Configuration} instead.
 * @author <a href="mailto:manik@jboss.org">Manik Surtani (manik@jboss.org)</a>
 * @author Vladimir Blagojevic
 * @author Galder Zamarre?o
 * @author Mircea.Markus@jboss.com
 * @see <a href="../../../config.html#ce_infinispan_default">Configuration reference</a>
 * @since 4.0
 */
@SurvivesRestarts
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
@XmlRootElement(name = "namedCacheConfiguration")
@ConfigurationDoc(name = "default")
@Deprecated
@SuppressWarnings("boxing")
public class Configuration extends AbstractNamedCacheConfigurationBean {

   private static final long serialVersionUID = 5553791890144997466L;
   private static final Log log = LogFactory.getLog(Configuration.class);

   // reference to a global configuration
   @XmlTransient
   private GlobalConfiguration globalConfiguration;

   @XmlAttribute
   @ConfigurationDoc(desc = "Only used with the namedCache element, this attribute specifies the name of the cache.  Can be any String, but must be unique in a given configuration.")
   protected String name;


   // ------------------------------------------------------------------------------------------------------------
   //   CONFIGURATION OPTIONS
   // ------------------------------------------------------------------------------------------------------------

   @XmlTransient
   FluentConfiguration fluentConfig = new FluentConfiguration(this);
   
   @XmlTransient
   private ClassLoader cl;

   @XmlElement
   LockingType locking = new LockingType().setConfiguration(this);

   @XmlElement
   CacheLoaderManagerConfig loaders = new CacheLoaderManagerConfig().setConfiguration(this);

   @XmlElement
   TransactionType transaction = new TransactionType(null).setConfiguration(this);

   @XmlElement
   CustomInterceptorsType customInterceptors = new CustomInterceptorsType().setConfiguration(this);

   @XmlElement
   DataContainerType dataContainer = new DataContainerType().setConfiguration(this);

   @XmlElement
   EvictionType eviction = new EvictionType().setConfiguration(this);

   @XmlElement
   ExpirationType expiration = new ExpirationType().setConfiguration(this);

   @XmlElement
   UnsafeType unsafe = new UnsafeType().setConfiguration(this);

   @XmlElement
   ClusteringType clustering = new ClusteringType(LOCAL).setConfiguration(this);

   @XmlElement
   JmxStatistics jmxStatistics = new JmxStatistics().setConfiguration(this);

   @XmlElement
   StoreAsBinary storeAsBinary = new StoreAsBinary().setConfiguration(this);

   @Deprecated
   @XmlElement
   LazyDeserialization lazyDeserialization = new LazyDeserialization().setConfiguration(this);

   @XmlTransient
   InvocationBatching invocationBatching = new InvocationBatching().setConfiguration(this);

   @XmlElement
   DeadlockDetectionType deadlockDetection = new DeadlockDetectionType().setConfiguration(this);

   @XmlElement
   QueryConfigurationBean indexing = new QueryConfigurationBean().setConfiguration(this);

   @XmlElement
   VersioningConfigurationBean versioning = new VersioningConfigurationBean().setConfiguration(this);

   @SuppressWarnings("unused")
   @Start(priority = 1)
   private void correctIsolationLevels() {
      // ensure the correct isolation level upgrades and/or downgrades are performed.
      switch (locking.isolationLevel) {
         case NONE:
            if (clustering.mode.isClustered())
               locking.isolationLevel = IsolationLevel.READ_COMMITTED;
            break;
         case READ_UNCOMMITTED:
            locking.isolationLevel = IsolationLevel.READ_COMMITTED;
            break;
         case SERIALIZABLE:
            locking.isolationLevel = IsolationLevel.REPEATABLE_READ;
            break;
      }
   }

   public void applyOverrides(Configuration overrides) {
      OverrideConfigurationVisitor v1 = new OverrideConfigurationVisitor();
      this.accept(v1);
      OverrideConfigurationVisitor v2 = new OverrideConfigurationVisitor();
      overrides.accept(v2);
      v1.override(v2);
   }

   @Override
   public void inject(ComponentRegistry cr) {
      this.accept(new InjectComponentRegistryVisitor(cr));
   }

   /**
    * Use the {@link org.infinispan.configuration.cache.ConfigurationBuilder}
    * hierarchy to configure Infinispan caches fluently.
    */
   @Deprecated
   public FluentConfiguration fluent() {
      return fluentConfig;
   }

   private void setInvocationBatching(InvocationBatching invocationBatching) {
      this.invocationBatching = invocationBatching;
      this.invocationBatching.setConfiguration(this);
   }

   @XmlElement
   private InvocationBatching getInvocationBatching() {
      return invocationBatching;
   }

   // ------------------------------------------------------------------------------------------------------------
   //   SETTERS - MAKE SURE ALL SETTERS PERFORM testImmutability()!!!
   // ------------------------------------------------------------------------------------------------------------


   public GlobalConfiguration getGlobalConfiguration() {
      return globalConfiguration;
   }

   public void setGlobalConfiguration(GlobalConfiguration gc) {
      this.globalConfiguration = gc;
   }

   /**
     * Returns the name of the cache associated with this configuration.
   */
   public final String getName() {
      return name;
   }

   public ClassLoader getClassLoader() {
      if (cl != null)
         // The classloader has been set for this configuration
         return cl;
      else if (cl == null && globalConfiguration != null)
         // The classloader is not set for this configuration, and we have a global config
         return globalConfiguration.getClassLoader();
      else 
         // Return the default CL 
         return Thread.currentThread().getContextClassLoader();
   }
   
   public void setClassLoader(ClassLoader cl) {
      this.cl = cl;
   }

   public boolean isStateTransferEnabled() {
      return clustering.stateRetrieval.fetchInMemoryState || (loaders != null && loaders.isFetchPersistentState());
   }

   public long getDeadlockDetectionSpinDuration() {
      return deadlockDetection.spinDuration;
   }


   /**
    * Time period that determines how often is lock acquisition attempted within maximum time allowed to acquire a
    * particular lock
    *
    * @param eagerDeadlockSpinDuration
    * @deprecated Use {@link FluentConfiguration.DeadlockDetectionConfig#spinDuration(Long)} instead
    */
   @Deprecated
   public void setDeadlockDetectionSpinDuration(long eagerDeadlockSpinDuration) {
      this.deadlockDetection.setSpinDuration(eagerDeadlockSpinDuration);
   }

   /**
    * @deprecated Use {@link #isDeadlockDetectionEnabled()} instead.
    */
   @Deprecated
   public boolean isEnableDeadlockDetection() {
      return deadlockDetection.enabled;
   }

   public boolean isDeadlockDetectionEnabled() {
      return deadlockDetection.enabled;
   }

   /**
    * Toggle to enable/disable deadlock detection
    *
    * @param useEagerDeadlockDetection
    * @deprecated Use {@link FluentConfiguration#deadlockDetection()} instead
    */
   @Deprecated
   public void setEnableDeadlockDetection(boolean useEagerDeadlockDetection) {
      this.deadlockDetection.setEnabled(useEagerDeadlockDetection);
   }

   /**
    * If true, a pool of shared locks is maintained for all entries that need to be locked. Otherwise, a lock is created
    * per entry in the cache. Lock striping helps control memory footprint but may reduce concurrency in the system.
    *
    * @param useLockStriping
    * @deprecated Use {@link FluentConfiguration.LockingConfig#useLockStriping(Boolean)} instead
    */
   @Deprecated
   public void setUseLockStriping(boolean useLockStriping) {
      locking.setUseLockStriping(useLockStriping);
   }

   public boolean isUseLockStriping() {
      return locking.useLockStriping;
   }

   public boolean isUnsafeUnreliableReturnValues() {
      return unsafe.unreliableReturnValues;
   }


   /**
    * Toggle to enable/disable return value fetching
    *
    * @param unsafeUnreliableReturnValues
    * @deprecated Use {@link FluentConfiguration.UnsafeConfig#unreliableReturnValues(Boolean)} instead
    */
   @Deprecated
   public void setUnsafeUnreliableReturnValues(boolean unsafeUnreliableReturnValues) {
      this.unsafe.setUnreliableReturnValues(unsafeUnreliableReturnValues);
   }

   /**
    * Rehashing timeout
    *
    * @param rehashRpcTimeout
    * @deprecated Use {@link FluentConfiguration.HashConfig#rehashRpcTimeout(Long)} instead
    */
   @Deprecated
   public void setRehashRpcTimeout(long rehashRpcTimeout) {
      this.clustering.hash.setRehashRpcTimeout(rehashRpcTimeout);
   }

   public long getRehashRpcTimeout() {
      return clustering.hash.rehashRpcTimeout;
   }

   public boolean isWriteSkewCheck() {
      return locking.writeSkewCheck;
   }

   /**
    * This setting is only applicable in the case of REPEATABLE_READ. When write skew check is set to false, if the
    * writer at commit time discovers that the working entry and the underlying entry have different versions, the
    * working entry will overwrite the underlying entry. If true, such version conflict - known as a write-skew - will
    * throw an Exception.
    *
    * @param writeSkewCheck
    * @deprecated Use {@link FluentConfiguration.LockingConfig#writeSkewCheck(Boolean)} instead
    */
   @Deprecated
   public void setWriteSkewCheck(boolean writeSkewCheck) {
      locking.setWriteSkewCheck(writeSkewCheck);
   }

   public int getConcurrencyLevel() {
      return locking.concurrencyLevel;
   }

   /**
    * Concurrency level for lock containers. Adjust this value according to the number of concurrent threads interating
    * with Infinispan. Similar to the concurrencyLevel tuning parameter seen in the JDK's ConcurrentHashMap.
    *
    * @param concurrencyLevel
    * @deprecated Use {@link FluentConfiguration.LockingConfig#concurrencyLevel(Integer)} instead
    */
   @Deprecated
   public void setConcurrencyLevel(int concurrencyLevel) {
      locking.setConcurrencyLevel(concurrencyLevel);
   }

   /**
    * If useReplQueue is set to true, this attribute can be used to trigger flushing of the queue when it reaches a
    * specific threshold.
    *
    * @param replQueueMaxElements
    * @deprecated Use {@link FluentConfiguration.AsyncConfig#replQueueMaxElements(Integer)} instead
    */
   @Deprecated
   public void setReplQueueMaxElements(int replQueueMaxElements) {
      this.clustering.async.setReplQueueMaxElements(replQueueMaxElements);
   }

   /**
    * If useReplQueue is set to true, this attribute controls how often the asynchronous thread used to flush the
    * replication queue runs. This should be a positive integer which represents thread wakeup time in milliseconds.
    *
    * @param replQueueInterval
    * @deprecated Use {@link FluentConfiguration.AsyncConfig#replQueueInterval(Long)} instead
    */
   @Deprecated
   public void setReplQueueInterval(long replQueueInterval) {
      this.clustering.async.setReplQueueInterval(replQueueInterval);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.AsyncConfig#replQueueInterval(Long)} instead
    */
   @Deprecated
   public void setReplQueueInterval(long replQueueInterval, TimeUnit timeUnit) {
      setReplQueueInterval(timeUnit.toMillis(replQueueInterval));
   }

   /**
    * This overrides the replication queue implementation class. Overriding the default allows you to add behavior to
    * the queue, typically by subclassing the default implementation.
    *
    * @param classname
    * @deprecated Use {@link FluentConfiguration.AsyncConfig#replQueueClass(Class)} instead
    */
   @Deprecated
   public void setReplQueueClass(String classname) {
      this.clustering.async.setReplQueueClass(classname);
   }

   /**
    * @deprecated Use {@link FluentConfiguration#jmxStatistics()} instead
    */
   @Deprecated
   public void setExposeJmxStatistics(boolean useMbean) {
      jmxStatistics.setEnabled(useMbean);
   }

   /**
    * Enables invocation batching if set to <tt>true</tt>.  You still need to use {@link
    * org.infinispan.Cache#startBatch()} and {@link org.infinispan.Cache#endBatch(boolean)} to demarcate the start and
    * end of batches.
    *
    * @param enabled if true, batching is enabled.
    * @since 4.0
    * @deprecated Use {@link FluentConfiguration#invocationBatching()} instead
    */
   @Deprecated
   public void setInvocationBatchingEnabled(boolean enabled) {
      invocationBatching.setEnabled(enabled);
   }

   /**
    * If true, this will cause the cache to ask neighboring caches for state when it starts up, so the cache starts
    * 'warm', although it will impact startup time.
    *
    * @param fetchInMemoryState
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#fetchInMemoryState(Boolean)} instead
    */
   @Deprecated
   public void setFetchInMemoryState(boolean fetchInMemoryState) {
      this.clustering.stateRetrieval.setFetchInMemoryState(fetchInMemoryState);
   }

   /**
    * If true, this will allow the cache to provide in-memory state to a neighbor, even if the cache is not configured
    * to fetch state from its neighbors (fetchInMemoryState is false)
    *
    * @param alwaysProvideInMemoryState
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#alwaysProvideInMemoryState(Boolean)} instead
    */
   @Deprecated
   public void setAlwaysProvideInMemoryState(boolean alwaysProvideInMemoryState) {
      this.clustering.stateRetrieval.setAlwaysProvideInMemoryState(alwaysProvideInMemoryState);
   }

   /**
    * Maximum time to attempt a particular lock acquisition
    *
    * @param lockAcquisitionTimeout
    * @deprecated Use {@link FluentConfiguration.LockingConfig#lockAcquisitionTimeout(Long)} instead
    */
   @Deprecated
   public void setLockAcquisitionTimeout(long lockAcquisitionTimeout) {
      locking.setLockAcquisitionTimeout(lockAcquisitionTimeout);
   }

   /**
    * Maximum time to attempt a particular lock acquisition
    *
    * @param lockAcquisitionTimeout
    * @param timeUnit
    * @deprecated Use {@link FluentConfiguration.LockingConfig#lockAcquisitionTimeout(Long)} instead
    */
   @Deprecated
   public void setLockAcquisitionTimeout(long lockAcquisitionTimeout, TimeUnit timeUnit) {
      setLockAcquisitionTimeout(timeUnit.toMillis(lockAcquisitionTimeout));
   }


   /**
    * This is the timeout (in ms) used to wait for an acknowledgment when making a remote call, after which the call is
    * aborted and an exception is thrown.
    *
    * @param syncReplTimeout
    * @deprecated Use {@link FluentConfiguration.SyncConfig#replTimeout(Long)} instead
    */
   @Deprecated
   public void setSyncReplTimeout(long syncReplTimeout) {
      this.clustering.sync.setReplTimeout(syncReplTimeout);
   }

   /**
    * This is the timeout used to wait for an acknowledgment when making a remote call, after which the call is aborted
    * and an exception is thrown.
    *
    * @param syncReplTimeout
    * @param timeUnit
    * @deprecated Use {@link FluentConfiguration.SyncConfig#replTimeout(Long)} instead
    */
   @Deprecated
   public void setSyncReplTimeout(long syncReplTimeout, TimeUnit timeUnit) {
      setSyncReplTimeout(timeUnit.toMillis(syncReplTimeout));
   }

   /**
    * Cache mode. For distribution, set mode to either 'd', 'dist' or 'distribution'. For replication, use either 'r',
    * 'repl' or 'replication'. Finally, for invalidation, 'i', 'inv' or 'invalidation'.  If the cache mode is set to
    * 'l' or 'local', the cache in question will not support clustering even if its cache manager does.
    * When no transport is enabled, the default is 'local' (instead of 'dist').
    *
    * @deprecated Use {@link FluentConfiguration.ClusteringConfig#mode(org.infinispan.config.Configuration.CacheMode)} instead
    */
   @Deprecated
   public void setCacheMode(CacheMode cacheModeInt) {
      clustering.setMode(cacheModeInt);
   }

   /**
    * Cache mode. For distribution, set mode to either 'd', 'dist' or 'distribution'. For replication, use either 'r',
    * 'repl' or 'replication'. Finally, for invalidation, 'i', 'inv' or 'invalidation'.  If the cache mode is set to
    * 'l' or 'local', the cache in question will not support clustering even if its cache manager does.
    * When no transport is enabled, the default is 'local' (instead of 'dist').
    *
    * @deprecated Use {@link FluentConfiguration.ClusteringConfig#mode(org.infinispan.config.Configuration.CacheMode)} instead
    */
   @Deprecated
   public void setCacheMode(String cacheMode) {
      if (cacheMode == null) throw new ConfigurationException("Cache mode cannot be null", "CacheMode");
      clustering.setMode(CacheMode.valueOf(uc(cacheMode)));
      if (clustering.mode == null) {
         log.warn("Unknown cache mode '" + cacheMode + "', using defaults.");
         clustering.setMode(LOCAL);
      }
   }

   public String getCacheModeString() {
      return clustering.mode == null ? "none" : clustering.mode.toString();
   }

   /**
    * @deprecated Use {@link FluentConfiguration.ClusteringConfig#mode(org.infinispan.config.Configuration.CacheMode)} instead
    */
   @Deprecated
   public void setCacheModeString(String cacheMode) {
      setCacheMode(cacheMode);
   }

   /**
    * Pluggable data container class which must implement
    * {@link org.infinispan.container.DataContainer}
    */
   public String getDataContainerClass() {
      return dataContainer.dataContainerClass;
   }

   public DataContainer getDataContainer() {
      return dataContainer.dataContainer;
   }

   public TypedProperties getDataContainerProperties() {
      return dataContainer.properties;
   }

   /**
    * @deprecated Use {@link #getExpirationWakeUpInterval()}
    */
   @Deprecated
   public long getEvictionWakeUpInterval() {
      return getExpirationWakeUpInterval();
   }

   /**
    * @deprecated Use {@link FluentConfiguration.ExpirationConfig#wakeUpInterval(Long)} instead
    */
   @Deprecated
   public void setEvictionWakeUpInterval(long evictionWakeUpInterval) {
      this.eviction.setWakeUpInterval(evictionWakeUpInterval);
   }

   public EvictionStrategy getEvictionStrategy() {
      return eviction.strategy;
   }

   /**
    * Eviction strategy. Available options are 'UNORDERED', 'FIFO', 'LRU', 'LIRS' and 'NONE' (to disable eviction).
    *
    * @param evictionStrategy
    * @deprecated Use {@link FluentConfiguration.EvictionConfig#strategy(org.infinispan.eviction.EvictionStrategy)} instead
    */
   @Deprecated
   public void setEvictionStrategy(EvictionStrategy evictionStrategy) {
      this.eviction.setStrategy(evictionStrategy);
   }

   /**
    * Eviction strategy. Available options are 'UNORDERED', 'FIFO', 'LRU', 'LIRS' and 'NONE' (to disable eviction).
    *
    * @param eStrategy
    * @deprecated Use {@link FluentConfiguration.EvictionConfig#strategy(org.infinispan.eviction.EvictionStrategy)} instead
    */
   @Deprecated
   public void setEvictionStrategy(String eStrategy) {
      this.eviction.strategy = EvictionStrategy.valueOf(uc(eStrategy));
      if (this.eviction.strategy == null) {
         log.warn("Unknown evictionStrategy  '" + eStrategy + "'!  Using EvictionStrategy.NONE.");
         this.eviction.setStrategy(EvictionStrategy.NONE);
      }
   }

   public EvictionThreadPolicy getEvictionThreadPolicy() {
      return eviction.threadPolicy;
   }

   /**
    * Threading policy for eviction.
    *
    * @param policy
    * @deprecated Use {@link FluentConfiguration.EvictionConfig#threadPolicy(org.infinispan.eviction.EvictionThreadPolicy)} instead
    */
   @Deprecated
   public void setEvictionThreadPolicy(EvictionThreadPolicy policy) {
      this.eviction.setThreadPolicy(policy);
   }

   /**
    * Threading policy for eviction.
    *
    * @param policy
    * @deprecated Use {@link FluentConfiguration.EvictionConfig#threadPolicy(org.infinispan.eviction.EvictionThreadPolicy)} instead
    */
   @Deprecated
   public void setEvictionThreadPolicy(String policy) {
      this.eviction.threadPolicy = EvictionThreadPolicy.valueOf(uc(policy));
      if (this.eviction.threadPolicy == null) {
         log.warn("Unknown thread eviction policy  '" + policy + "'!  Using EvictionThreadPolicy.DEFAULT");
         this.eviction.setThreadPolicy(EvictionThreadPolicy.DEFAULT);
      }
   }

   public int getEvictionMaxEntries() {
      return eviction.maxEntries;
   }

   /**
    * Maximum number of entries in a cache instance. If selected value is not a power of two the actual value will
    * default to the least power of two larger than selected value. -1 means no limit.
    *
    * @param evictionMaxEntries
    * @deprecated Use {@link FluentConfiguration.EvictionConfig#maxEntries(Integer)} instead
    */
   @Deprecated
   public void setEvictionMaxEntries(int evictionMaxEntries) {
      this.eviction.setMaxEntries(evictionMaxEntries);
   }

   @Deprecated
   public void setVersioningScheme(VersioningScheme versioningScheme) {
      this.versioning.setVersioningScheme(versioningScheme);
   }

   @Deprecated
   public void setEnableVersioning(boolean enabled) {
      this.versioning.setEnabled(enabled);
   }

   /**
    * Expiration lifespan, in milliseconds
    */
   public long getExpirationLifespan() {
      return expiration.lifespan;
   }

   @Deprecated
   public VersioningScheme getVersioningScheme() {
      return this.versioning.versioningScheme;
   }

   @Deprecated
   public boolean isEnableVersioning() {
      return this.versioning.enabled;
   }



   /**
    * Maximum lifespan of a cache entry, after which the entry is expired cluster-wide, in milliseconds. -1 means the
    * entries never expire. <br /> <br /> Note that this can be overriden on a per-entry basis by using the Cache API.
    *
    * @param expirationLifespan
    * @deprecated Use {@link FluentConfiguration.ExpirationConfig#lifespan(Long)} instead
    */
   @Deprecated
   public void setExpirationLifespan(long expirationLifespan) {
      this.expiration.setLifespan(expirationLifespan);
   }

   /**
    * Expiration max idle time, in milliseconds
    */
   public long getExpirationMaxIdle() {
      return expiration.maxIdle;
   }


   /**
    * Maximum idle time a cache entry will be maintained in the cache, in milliseconds. If the idle time is exceeded,
    * the entry will be expired cluster-wide. -1 means the entries never expire. <br /> <br /> Note that this can be
    * overriden on a per-entry basis by using the Cache API.
    *
    * @param expirationMaxIdle
    * @deprecated Use {@link FluentConfiguration.ExpirationConfig#maxIdle(Long)} instead
    */
   @Deprecated
   public void setExpirationMaxIdle(long expirationMaxIdle) {
      this.expiration.setMaxIdle(expirationMaxIdle);
   }

   /**
    * Eviction thread wake up interval, in milliseconds.
    */
   public long getExpirationWakeUpInterval() {
      return expiration.wakeUpInterval;
   }

   /**
    * Fully qualified class name of a class that looks up a reference to a {@link javax.transaction.TransactionManager}.
    * The default provided is capable of locating the default TransactionManager in most popular Java EE systems, using
    * a JNDI lookup. Calling this method marks the cache as transactional.
    *
    * @param transactionManagerLookupClass
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#transactionManagerLookupClass(Class)} instead
    */
   @Deprecated
   public void setTransactionManagerLookupClass(String transactionManagerLookupClass) {
      this.transaction.setTransactionManagerLookupClass(transactionManagerLookupClass);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#transactionManagerLookup(TransactionManagerLookup)} instead
    */
   @Deprecated
   public void setTransactionManagerLookup(TransactionManagerLookup transactionManagerLookup) {
      this.transaction.transactionManagerLookup(transactionManagerLookup);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.LoadersConfig#addCacheLoader(org.infinispan.loaders.CacheLoaderConfig...)} instead
    */
   @Deprecated
   public void setCacheLoaderManagerConfig(CacheLoaderManagerConfig cacheLoaderManagerConfig) {
      this.loaders = cacheLoaderManagerConfig;
   }

   /**
    * If true, the cluster-wide commit phase in two-phase commit (2PC) transactions will be synchronous, so Infinispan
    * will wait for responses from all nodes to which the commit was sent. Otherwise, the commit phase will be
    * asynchronous. Keeping it as false improves performance of 2PC transactions, since any remote failures are trapped
    * during the prepare phase anyway and appropriate rollbacks are issued.
    *
    * @param syncCommitPhase
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#syncCommitPhase(Boolean)} instead
    */
   @Deprecated
   public void setSyncCommitPhase(boolean syncCommitPhase) {
      this.transaction.setSyncCommitPhase(syncCommitPhase);
   }

   /**
    * If true, the cluster-wide rollback phase in two-phase commit (2PC) transactions will be synchronous, so Infinispan
    * will wait for responses from all nodes to which the rollback was sent. Otherwise, the rollback phase will be
    * asynchronous. Keeping it as false improves performance of 2PC transactions.
    *
    * @param syncRollbackPhase
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#syncRollbackPhase(Boolean)} instead
    */
   @Deprecated
   public void setSyncRollbackPhase(boolean syncRollbackPhase) {
      this.transaction.setSyncRollbackPhase(syncRollbackPhase);
   }

   /**
    * Only has effect for DIST mode and when useEagerLocking is set to true. When this is enabled, then only one node is
    * locked in the cluster, disregarding numOwners config. On the opposite, if this is false, then on all cache.lock()
    * calls numOwners RPCs are being performed. The node that gets locked is the main data owner, i.e. the node where
    * data would reside if numOwners==1. If the node where the lock resides crashes, then the transaction is marked for
    * rollback - data is in a consistent state, no fault tolerance.
    *
    * @param useEagerLocking
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#useEagerLocking(Boolean)} instead
    */
   @Deprecated
   public void setUseEagerLocking(boolean useEagerLocking) {
      this.transaction.setUseEagerLocking(useEagerLocking);
   }

   /**
    * Only has effect for DIST mode and when useEagerLocking is set to true. When this is enabled, then only one node is
    * locked in the cluster, disregarding numOwners config. On the opposite, if this is false, then on all cache.lock()
    * calls numOwners RPCs are being performed. The node that gets locked is the main data owner, i.e. the node where
    * data would reside if numOwners==1. If the node where the lock resides crashes, then the transaction is marked for
    * rollback - data is in a consistent state, no fault tolerance.
    *
    * @param eagerLockSingleNode
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#eagerLockSingleNode(Boolean)} instead
    */
   @Deprecated
   public void setEagerLockSingleNode(boolean eagerLockSingleNode) {
      this.transaction.setEagerLockSingleNode(eagerLockSingleNode);
   }

   /**
    * If there are any ongoing transactions when a cache is stopped,
    * Infinispan waits for ongoing remote and local transactions to finish.
    * The amount of time to wait for is defined by the cache stop timeout.
    * It is recommended that this value does not exceed the transaction
    * timeout because even if a new transaction was started just before the
    * cache was stopped, this could only last as long as the transaction
    * timeout allows it.
    *
    * @deprecated Use {@link FluentConfiguration.TransactionConfig#cacheStopTimeout(Integer)} instead
    */
   @Deprecated
   public Configuration setCacheStopTimeout(int cacheStopTimeout) {
      this.transaction.setCacheStopTimeout(cacheStopTimeout);
      return this;
   }

   /**
    * If true, this forces all async communications to be queued up and sent out periodically as a batch.
    *
    * @param useReplQueue
    * @deprecated Use {@link FluentConfiguration.AsyncConfig#useReplQueue(Boolean)} instead
    */
   @Deprecated
   public void setUseReplQueue(boolean useReplQueue) {
      this.clustering.async.setUseReplQueue(useReplQueue);
   }

   /**
    * Cache isolation level. Infinispan only supports READ_COMMITTED or REPEATABLE_READ isolation levels. See <a
    * href='http://en.wikipedia.org/wiki/Isolation_level'>http://en.wikipedia.org/wiki/Isolation_level</a> for a
    * discussion on isolation levels.
    *
    * @param isolationLevel
    * @deprecated Use {@link FluentConfiguration.LockingConfig#isolationLevel(org.infinispan.util.concurrent.IsolationLevel)} instead
    */
   @Deprecated
   public void setIsolationLevel(IsolationLevel isolationLevel) {
      locking.setIsolationLevel(isolationLevel);
   }

   /**
    * This is the maximum amount of time - in milliseconds - to wait for state from neighboring caches, before throwing
    * an exception and aborting startup.
    *
    * @param stateRetrievalTimeout
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#timeout(Long)} instead
    */
   @Deprecated
   public void setStateRetrievalTimeout(long stateRetrievalTimeout) {
      this.clustering.stateRetrieval.setTimeout(stateRetrievalTimeout);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#timeout(Long)} instead
    */
   @Deprecated
   public void setStateRetrievalTimeout(long stateRetrievalTimeout, TimeUnit timeUnit) {
      setStateRetrievalTimeout(timeUnit.toMillis(stateRetrievalTimeout));
   }

   /**
    * This is the maximum amount of time to run a cluster-wide flush, to allow for syncing of transaction logs.
    *
    * @param logFlushTimeout
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#logFlushTimeout(Long)} instead
    */
   @Deprecated
   public void setStateRetrievalLogFlushTimeout(long logFlushTimeout) {
      this.clustering.stateRetrieval.setLogFlushTimeout(logFlushTimeout);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#logFlushTimeout(Long)} instead
    */
   @Deprecated
   public void setStateRetrievalLogFlushTimeout(long logFlushTimeout, TimeUnit timeUnit) {
      this.clustering.stateRetrieval.setLogFlushTimeout(timeUnit.toMillis(logFlushTimeout));
   }


   /**
    * This is the maximum number of non-progressing transaction log writes after which a brute-force flush approach is
    * resorted to, to synchronize transaction logs.
    *
    * @param maxNonProgressingLogWrites
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#maxNonProgressingLogWrites(Integer)} instead
    */
   @Deprecated
   public void setStateRetrievalMaxNonProgressingLogWrites(int maxNonProgressingLogWrites) {
      this.clustering.stateRetrieval.setMaxNonProgressingLogWrites(maxNonProgressingLogWrites);
   }

   /**
    * The size of a state transfer "chunk", in cache entries.
    *
    * @param chunkSize
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#chunkSize(Integer)} instead
    */
   @Deprecated
   public void setStateRetrievalChunkSize(int chunkSize) {
      this.clustering.stateRetrieval.setChunkSize(chunkSize);
   }

   /**
    * Initial wait time when backing off before retrying state transfer retrieval
    *
    * @param initialRetryWaitTime
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#initialRetryWaitTime(Long)} instead
    */
   @Deprecated
   public void setStateRetrievalInitialRetryWaitTime(long initialRetryWaitTime) {
      clustering.stateRetrieval.setInitialRetryWaitTime(initialRetryWaitTime);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#initialRetryWaitTime(Long)} instead
    */
   @Deprecated
   public void setStateRetrievalInitialRetryWaitTime(long initialRetryWaitTime, TimeUnit timeUnit) {
      setStateRetrievalInitialRetryWaitTime(timeUnit.toMillis(initialRetryWaitTime));
   }


   /**
    * Wait time increase factor over successive state retrieval backoffs
    *
    * @param retryWaitTimeIncreaseFactor
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#retryWaitTimeIncreaseFactor(Integer)} instead
    */
   @Deprecated
   public void setStateRetrievalRetryWaitTimeIncreaseFactor(int retryWaitTimeIncreaseFactor) {
      clustering.stateRetrieval.setRetryWaitTimeIncreaseFactor(retryWaitTimeIncreaseFactor);
   }

   /**
    * Number of state retrieval retries before giving up and aborting startup.
    *
    * @param numRetries
    * @deprecated Use {@link FluentConfiguration.StateRetrievalConfig#numRetries(Integer)} instead
    */
   @Deprecated
   public void setStateRetrievalNumRetries(int numRetries) {
      clustering.stateRetrieval.setNumRetries(numRetries);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.LockingConfig#isolationLevel(org.infinispan.util.concurrent.IsolationLevel)} instead
    */
   @Deprecated
   public void setIsolationLevel(String isolationLevel) {
      if (isolationLevel == null) throw new ConfigurationException("Isolation level cannot be null", "IsolationLevel");
      locking.setIsolationLevel(IsolationLevel.valueOf(uc(isolationLevel)));
      if (locking.isolationLevel == null) {
         log.warn("Unknown isolation level '" + isolationLevel + "', using defaults.");
         locking.setIsolationLevel(IsolationLevel.REPEATABLE_READ);
      }
   }

   /**
    * @deprecated Use {@link FluentConfiguration#storeAsBinary()} instead
    */
   @Deprecated
   public void setUseLazyDeserialization(boolean useLazyDeserialization) {
      storeAsBinary.setEnabled(useLazyDeserialization);
   }

   /**
    * Toggle to enable/disable L1 cache.
    *
    * @param l1CacheEnabled
    * @deprecated Use {@link FluentConfiguration#l1()} instead
    */
   @Deprecated
   public void setL1CacheEnabled(boolean l1CacheEnabled) {
      this.clustering.l1.setEnabled(l1CacheEnabled);
   }

   /**
    * Maximum lifespan of an entry placed in the L1 cache.
    *
    * @param l1Lifespan
    * @deprecated Use {@link FluentConfiguration.L1Config#lifespan(Long)} instead
    */
   @Deprecated
   public void setL1Lifespan(long l1Lifespan) {
      this.clustering.l1.setLifespan(l1Lifespan);
   }

   /**
    * If true, entries removed due to a rehash will be moved to L1 rather than being removed altogether.
    *
    * @param l1OnRehash
    * @deprecated Use {@link FluentConfiguration.L1Config#onRehash(Boolean)} instead
    */
   @Deprecated
   public void setL1OnRehash(boolean l1OnRehash) {
      this.clustering.l1.setOnRehash(l1OnRehash);
   }
   
   /**
    * <p>
    * Determines whether a multicast or a web of unicasts are used when performing L1 invalidations.
    * </p>
    * 
    * <p>
    * By default multicast will be used.
    * </p>
    * 
    * <p>
    * If the threshold is set to -1, then unicasts will always be used. If the threshold is set to 0, then multicast 
    * will be always be used.
    * </p>
    * 
    * @param threshold the threshold over which to use a multicast
    * @deprecated Use {@link FluentConfiguration.L1Config#invalidationThreshold(Integer)} instead
    */
   @Deprecated
   public void setL1InvalidationThreshold(int threshold) {
      this.clustering.l1.setInvalidationThreshold(threshold);
   }
   
   public int getL1InvalidationThreshold() {
   	return this.clustering.l1.invalidationThreshold;
   }

   /**
    * @deprecated No longer used since 5.2, use {@link org.infinispan.configuration.cache.HashConfigurationBuilder#consistentHashFactory(org.infinispan.distribution.ch.ConsistentHashFactory)} instead.
    */
   @Deprecated
   public void setConsistentHashClass(String consistentHashClass) {
      this.clustering.hash.setConsistentHashClass(consistentHashClass);
   }

   /**
    * A fully qualified name of the class providing a hash function, used as a bit spreader and a general hash code
    * generator.  Typically used in conjunction with the many default {@link org.infinispan.distribution.ch.ConsistentHash}
    * implementations shipped.
    *
    * @param hashFunctionClass
    * @deprecated Use {@link FluentConfiguration.HashConfig#hashFunctionClass(Class)} instead
    */
   @Deprecated
   public void setHashFunctionClass(String hashFunctionClass) {
      clustering.hash.hashFunctionClass = hashFunctionClass;
   }

   /**
    * Number of cluster-wide replicas for each cache entry.
    *
    * @param numOwners
    * @deprecated Use {@link FluentConfiguration.HashConfig#numOwners(Integer)} instead
    */
   @Deprecated
   public void setNumOwners(int numOwners) {
      this.clustering.hash.setNumOwners(numOwners);
   }

   /**
    * If false, no rebalancing or rehashing will take place when a new node joins the cluster or a node leaves
    *
    * @param rehashEnabled
    * @deprecated Use {@link FluentConfiguration.HashConfig#rehashEnabled(Boolean)} instead
    */
   @Deprecated
   public void setRehashEnabled(boolean rehashEnabled) {
      this.clustering.hash.setRehashEnabled(rehashEnabled);
   }

   /**
    * @deprecated Use {@link FluentConfiguration.HashConfig#rehashWait(Long)} instead
    */
   @Deprecated
   public void setRehashWaitTime(long rehashWaitTime) {
      this.clustering.hash.setRehashWait(rehashWaitTime);
   }

   /**
    * If true, asynchronous marshalling is enabled which means that caller can return even quicker, but it can suffer
    * from reordering of operations. You can find more information <a href=&quot;http://community.jboss.org/docs/DOC-15725&quot;>here</a>
    *
    * @param useAsyncMarshalling
    * @deprecated Use {@link FluentConfiguration.AsyncConfig#asyncMarshalling(Boolean)} instead
    */
   @Deprecated
   public void setUseAsyncMarshalling(boolean useAsyncMarshalling) {
      this.clustering.async.setAsyncMarshalling(useAsyncMarshalling);
   }

   /**
    * If enabled, entries will be indexed when they are added to the cache. Indexes will be updated as entries change or
    * are removed.
    *
    * @param enabled
    * @deprecated Use {@link FluentConfiguration#indexing()} instead
    */
   @Deprecated
   public void setIndexingEnabled(boolean enabled) {
      this.indexing.setEnabled(enabled);
   }

   /**
    * If true, only index changes made locally, ignoring remote changes. This is useful if indexes are shared across a
    * cluster to prevent redundant indexing of updates.
    *
    * @param indexLocalOnly
    * @deprecated Use {@link FluentConfiguration.IndexingConfig#indexLocalOnly(Boolean)} instead
    */
   @Deprecated
   public void setIndexLocalOnly(boolean indexLocalOnly) {
      this.indexing.setIndexLocalOnly(indexLocalOnly);
   }

   // ------------------------------------------------------------------------------------------------------------
   //   GETTERS
   // ------------------------------------------------------------------------------------------------------------

   public boolean isUseAsyncMarshalling() {
      return clustering.async.asyncMarshalling;
   }

   public boolean isUseReplQueue() {
      return clustering.async.useReplQueue;
   }

   public int getReplQueueMaxElements() {
      return clustering.async.replQueueMaxElements;
   }

   public long getReplQueueInterval() {
      return clustering.async.replQueueInterval;
   }

   public String getReplQueueClass() {
      return this.clustering.async.replQueueClass;
   }

   public boolean isExposeJmxStatistics() {
      return jmxStatistics.enabled;
   }

   /**
    * @return true if invocation batching is enabled.
    * @since 4.0
    */
   public boolean isInvocationBatchingEnabled() {
      return invocationBatching.enabled;
   }

   public boolean isIndexingEnabled() {
      return indexing.isEnabled();
   }

   public boolean isIndexLocalOnly() {
      return indexing.isIndexLocalOnly();
   }
   
   public TypedProperties getIndexingProperties() {
      return indexing.properties;
   }

   public boolean isFetchInMemoryState() {
      return clustering.stateRetrieval.fetchInMemoryState;
   }

   public boolean isAlwaysProvideInMemoryState() {
      return clustering.stateRetrieval.alwaysProvideInMemoryState;
   }

   /**
    * Returns true if and only if {@link #isUseEagerLocking()}, {@link #isEagerLockSingleNode()} and the cache is
    * distributed.
    * @deprecated this is deprecated as starting with Infinispan 5.1 a single lock is always acquired disregarding the
    * number of owner.
    */
   @Deprecated
   public boolean isEagerLockingSingleNodeInUse() {
      return isUseEagerLocking() && isEagerLockSingleNode() && getCacheMode().isDistributed();
   }


   public long getLockAcquisitionTimeout() {
      return locking.lockAcquisitionTimeout;
   }

   public long getSyncReplTimeout() {
      return clustering.sync.replTimeout;
   }

   public CacheMode getCacheMode() {
      return clustering.mode;
   }

   /**
    * Returns the locking mode for this cache.
    *
    * @see LockingMode
    */
   public LockingMode getTransactionLockingMode() {
      return transaction.lockingMode;
   }

   /**
    * Returns cache's transaction mode. By default a cache is not transactinal, i.e. the transaction mode
    * is {@link TransactionMode#NON_TRANSACTIONAL}
    * @see TransactionMode
    */
   public TransactionMode getTransactionMode() {
      return transaction.transactionMode;
   }

   /**
    * If the cache is transactional (i.e. {@link #isTransactionalCache()} == true) and transactionAutoCommit is enabled
    * then for single operation transactions the user doesn't need to manually start a transaction, but a transactions
    * is injected by the system. Defaults to true.
    */
   public boolean isTransactionAutoCommit() {
      return transaction.autoCommit;
   }

   /**
    * Enabling this would cause autoCommit transactions ({@link #isTransactionAutoCommit()}) to complete with 1 RPC
    * instead of 2 RPCs (which is default).
    * <br/>
    * Important: enabling this feature might cause inconsistencies when two transactions concurrently write on the same key. This is
    * explained here: {@link org.infinispan.config.Configuration#isSyncCommitPhase()}.
    * <br/>
    * The reason this configuration was added is the following:
    * <ul>
    *    <li>
    *  before infinispan 5.1 caches could be used in a mixed way, i.e. transactional and non transactional
    *    </li>
    *    <li>
    *  for this mixed access mode, the non transactional calls were more efficient (1 RPC vs 2 RPCs needed by 2PC) but
    *    </li>
    * also offer fewer guarantees when it comes to concurrent access
    *    <li>
    *  for these existing use cases, and similar new ones, it makes sense to enable <b>use1PcForAutoCommitTransactions</b>
    * in order to better trade between consistency and performance.
    *   </li>
    * </ul>
    */
   public boolean isUse1PcForAutoCommitTransactions() {
      return transaction.use1PcForAutoCommitTransactions;
   }

   public IsolationLevel getIsolationLevel() {
      return locking.isolationLevel;
   }

   public String getTransactionManagerLookupClass() {
      return transaction.transactionManagerLookupClass;
   }

   public TransactionManagerLookup getTransactionManagerLookup() {
      return transaction.transactionManagerLookup;
   }

   public TransactionSynchronizationRegistryLookup getTransactionSynchronizationRegistryLookup() {
      return transaction.transactionSynchronizationRegistryLookup;
   }

   /**
    * @deprecated Use {@link #getCacheLoaders()}, {@link #isCacheLoaderShared()}
    * {@link #isFetchPersistentState()}, {@link #isCacheLoaderPassivation()}
    * and {@link #isCacheLoaderPreload()} instead
    */
   @Deprecated
   public CacheLoaderManagerConfig getCacheLoaderManagerConfig() {
      return loaders;
   }

   public List<CacheLoaderConfig> getCacheLoaders() {
      return loaders.getCacheLoaderConfigs();
   }

   public boolean isCacheLoaderShared() {
      return loaders.isShared();
   }

   public boolean isFetchPersistentState() {
      return loaders.isFetchPersistentState();
   }

   public boolean isCacheLoaderPassivation() {
      return loaders.isPassivation();
   }

   public boolean isCacheLoaderPreload() {
      return loaders.isPreload();
   }

   /**
    * Important - to be used with caution: if you have two transactions writing to the same key concurrently and
    * the commit is configured to be performed asynchronously then inconsistencies might happen. This is because in
    * order to have such consistency guarantees locks need to be released asynchronously after all the commits are
    * acknowledged on the originator. In the case of an asynchronous commit messages we don't wait for all the
    * commit messages to be acknowledged, but release the locks together with the commit message.
    */
   public boolean isSyncCommitPhase() {
      return transaction.syncCommitPhase;
   }

   public boolean isSyncRollbackPhase() {
      return transaction.syncRollbackPhase;
   }

   /**
    * Returns true if the 2nd phase of the 2PC (i.e. either commit or rollback) is sent asynchronously.
    */
   public boolean isSecondPhaseAsync() {
      return !isSyncCommitPhase() || isUseReplQueue() || !getCacheMode().isSynchronous();
   }

   /**
    * This is now deprecated. An "eager" locking cache is a transactional cache running in pessimistic mode.
    * @see #getTransactionLockingMode()
    */
   @Deprecated
   public boolean isUseEagerLocking() {
      return transaction.useEagerLocking;
   }

   /**
    * @deprecated starting with Infinispan 5.1 single node locking is used by default
    */
   @Deprecated
   public boolean isEagerLockSingleNode() {
      return transaction.eagerLockSingleNode;
   }

   public int getCacheStopTimeout() {
      return transaction.cacheStopTimeout;
   }

   public long getStateRetrievalTimeout() {
      return clustering.stateRetrieval.timeout;
   }

   public long getStateRetrievalInitialRetryWaitTime() {
      return clustering.stateRetrieval.initialRetryWaitTime;
   }

   public int getStateRetrievalRetryWaitTimeIncreaseFactor() {
      return clustering.stateRetrieval.retryWaitTimeIncreaseFactor;
   }

   public int getStateRetrievalNumRetries() {
      return clustering.stateRetrieval.numRetries;
   }

   public int getStateRetrievalMaxNonProgressingLogWrites() {
      return clustering.stateRetrieval.maxNonProgressingLogWrites;
   }

   public int getStateRetrievalChunkSize() {
      return clustering.stateRetrieval.chunkSize;
   }

   public long getStateRetrievalLogFlushTimeout() {
      return clustering.stateRetrieval.logFlushTimeout;
   }

   /**
    * @deprecated Use {@link #isStoreAsBinary()}
    */
   @Deprecated
   public boolean isUseLazyDeserialization() {
      return isStoreAsBinary();
   }

   public boolean isStoreAsBinary() {
      if (lazyDeserialization.enabled) {
         storeAsBinary.enabled = true;
      }
      return storeAsBinary.enabled;
   }

   public boolean isL1CacheEnabled() {
      return clustering.l1.enabled;
   }

   public boolean isL1CacheActivated() {
      return clustering.l1.activated && isL1CacheEnabled();
   }

   public long getL1Lifespan() {
      return clustering.l1.lifespan;
   }

   public boolean isL1OnRehash() {
      return clustering.l1.onRehash;
   }

   /**
    * @deprecated No longer used since 5.2, use {@link org.infinispan.configuration.cache.HashConfigurationBuilder#consistentHashFactory(org.infinispan.distribution.ch.ConsistentHashFactory)} instead.
    */
   public String getConsistentHashClass() {
      return clustering.hash.consistentHashClass;
   }

   /**
    * @deprecated No longer useful, since {@link #getConsistentHashClass()} is not used.
    */
   public boolean isCustomConsistentHashClass() {
      return false;
   }

   public boolean isCustomHashFunctionClass() {
      return clustering.hash.hashFunctionClass != null &&
            !clustering.hash.hashFunctionClass.equals(MurmurHash3.class.getName());
   }

   public String getHashFunctionClass() {
      return clustering.hash.hashFunctionClass;
   }

   public int getNumOwners() {
      return clustering.hash.numOwners;
   }
   
   public int getNumVirtualNodes() {
      return clustering.hash.numVirtualNodes;
   }
   
   public boolean isGroupsEnabled() {
      clustering.hash.groups.setConfiguration(this);
      return clustering.hash.groups.enabled;
   }
   
   public List<Grouper<?>> getGroupers() {
      clustering.hash.groups.setConfiguration(this);
      return clustering.hash.groups.groupers; 
   }

   public boolean isRehashEnabled() {
      return clustering.hash.rehashEnabled;
   }

   public long getRehashWaitTime() {
      return clustering.hash.rehashWait;
   }

   /**
    * Returns true if transaction recovery information is collected.
    */
   public boolean isTransactionRecoveryEnabled() {
      return transaction.recovery.isEnabled();
   }

   /**
    * Returns the name of the cache used in order to keep recovery information.
    */
   public String getTransactionRecoveryCacheName() {
      return transaction.recovery.getRecoveryInfoCacheName();
   }

   /**
    * If enabled Infinispan enlists within transactions as a {@link javax.transaction.Synchronization}. If disabled
    * (default) then Infinispan enlists as an {@link javax.transaction.xa.XAResource}, being able to fully participate
    * in distributed transaction. More about this <a href="http://community.jboss.org/wiki/Infinispantransactions#Enlisting_Synchronization">here</a>.
    */
   public boolean isUseSynchronizationForTransactions() {
      return transaction.isUseSynchronization();
   }

   // ------------------------------------------------------------------------------------------------------------
   //   HELPERS
   // ------------------------------------------------------------------------------------------------------------

   // ------------------------------------------------------------------------------------------------------------
   //   OVERRIDDEN METHODS
   // ------------------------------------------------------------------------------------------------------------

   public void accept(ConfigurationBeanVisitor v) {
      v.visitConfiguration(this);
      clustering.accept(v);
      customInterceptors.accept(v);
      dataContainer.accept(v);
      deadlockDetection.accept(v);
      eviction.accept(v);
      expiration.accept(v);
      invocationBatching.accept(v);
      jmxStatistics.accept(v);
      storeAsBinary.accept(v);
      lazyDeserialization.accept(v);
      loaders.accept(v);
      locking.accept(v);
      transaction.accept(v);
      unsafe.accept(v);
      indexing.accept(v);
      versioning.accept(v);
   }

   /**
    * Also see {@link #equalsIgnoreName(Object)} for equality that does not consider the name of the configuration.
    */
   @Override
   public boolean equals(Object o) {
      if (!equalsIgnoreName(o)) return false;
      Configuration that = (Configuration) o;
      return !(name != null ? !name.equals(that.name) : that.name != null);
   }

   /**
    * Same as {@link #equals(Object)} but it ignores the {@link #getName()} attribute in the comparison.
    */
   public boolean equalsIgnoreName(Object o) {
      if (this == o) return true;
      if (!(o instanceof Configuration)) return false;

      Configuration that = (Configuration) o;

      if (clustering != null ? !clustering.equals(that.clustering) : that.clustering != null) return false;
      if (customInterceptors != null ? !customInterceptors.equals(that.customInterceptors) : that.customInterceptors != null)
         return false;
      if (dataContainer != null ? !dataContainer.equals(that.dataContainer) : that.dataContainer != null) return false;
      if (deadlockDetection != null ? !deadlockDetection.equals(that.deadlockDetection) : that.deadlockDetection != null)
         return false;
      if (eviction != null ? !eviction.equals(that.eviction) : that.eviction != null) return false;
      if (expiration != null ? !expiration.equals(that.expiration) : that.expiration != null) return false;
      if (globalConfiguration != null ? !globalConfiguration.equals(that.globalConfiguration) : that.globalConfiguration != null)
         return false;
      if (invocationBatching != null ? !invocationBatching.equals(that.invocationBatching) : that.invocationBatching != null)
         return false;
      if (jmxStatistics != null ? !jmxStatistics.equals(that.jmxStatistics) : that.jmxStatistics != null) return false;
      if (storeAsBinary != null ? !storeAsBinary.equals(that.storeAsBinary) : that.storeAsBinary != null)
         return false;
      if (lazyDeserialization != null ? !lazyDeserialization.equals(that.lazyDeserialization) : that.lazyDeserialization != null)
         return false;
      if (loaders != null ? !loaders.equals(that.loaders) : that.loaders != null) return false;
      if (locking != null ? !locking.equals(that.locking) : that.locking != null) return false;
      if (transaction != null ? !transaction.equals(that.transaction) : that.transaction != null) return false;
      if (unsafe != null ? !unsafe.equals(that.unsafe) : that.unsafe != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = globalConfiguration != null ? globalConfiguration.hashCode() : 0;
      result = 31 * result + (name != null ? name.hashCode() : 0);
      result = 31 * result + (locking != null ? locking.hashCode() : 0);
      result = 31 * result + (loaders != null ? loaders.hashCode() : 0);
      result = 31 * result + (transaction != null ? transaction.hashCode() : 0);
      result = 31 * result + (customInterceptors != null ? customInterceptors.hashCode() : 0);
      result = 31 * result + (dataContainer != null ? dataContainer.hashCode() : 0);
      result = 31 * result + (eviction != null ? eviction.hashCode() : 0);
      result = 31 * result + (expiration != null ? expiration.hashCode() : 0);
      result = 31 * result + (unsafe != null ? unsafe.hashCode() : 0);
      result = 31 * result + (clustering != null ? clustering.hashCode() : 0);
      result = 31 * result + (jmxStatistics != null ? jmxStatistics.hashCode() : 0);
      result = 31 * result + (storeAsBinary != null ? storeAsBinary.hashCode() : 0);
      result = 31 * result + (lazyDeserialization != null ? lazyDeserialization.hashCode() : 0);
      result = 31 * result + (invocationBatching != null ? invocationBatching.hashCode() : 0);
      result = 31 * result + (deadlockDetection != null ? deadlockDetection.hashCode() : 0);
      return result;
   }

   @Override
   public Configuration clone() {
      try {
         Configuration dolly = (Configuration) super.clone();
         if (clustering != null) {
            dolly.clustering = clustering.clone();
            dolly.clustering.setConfiguration(dolly);
         }
         // The globalConfiguration reference is shared, shouldn't clone it
         //if (globalConfiguration != null) dolly.globalConfiguration = globalConfiguration.clone();
         if (locking != null) {
            dolly.locking = (LockingType) locking.clone();
            dolly.locking.setConfiguration(dolly);
         }
         if (loaders != null) {
            dolly.loaders = loaders.clone();
            dolly.loaders.setConfiguration(dolly);
         }
         if (transaction != null) {
            dolly.transaction = transaction.clone();
            dolly.transaction.setConfiguration(dolly);
         }
         if (customInterceptors != null) {
            dolly.customInterceptors = customInterceptors.clone();
            dolly.customInterceptors.setConfiguration(dolly);
         }
         if (dataContainer != null) {
            dolly.dataContainer = (DataContainerType) dataContainer.clone();
            dolly.dataContainer.setConfiguration(dolly);
         }
         if (eviction != null) {
            dolly.eviction = (EvictionType) eviction.clone();
            dolly.eviction.setConfiguration(dolly);
         }
         if (expiration != null) {
            dolly.expiration = (ExpirationType) expiration.clone();
            dolly.expiration.setConfiguration(dolly);
         }
         if (unsafe != null) {
            dolly.unsafe = (UnsafeType) unsafe.clone();
            dolly.unsafe.setConfiguration(dolly);
         }
         if (clustering != null) {
            dolly.clustering = clustering.clone();
            dolly.clustering.setConfiguration(dolly);
         }
         if (jmxStatistics != null) {
            dolly.jmxStatistics = (JmxStatistics) jmxStatistics.clone();
            dolly.jmxStatistics.setConfiguration(dolly);
         }
         if (storeAsBinary != null) {
            dolly.storeAsBinary = storeAsBinary.clone();
            dolly.storeAsBinary.setConfiguration(dolly);
         }
         if (lazyDeserialization != null) {
            dolly.lazyDeserialization = (LazyDeserialization) lazyDeserialization.clone();
            dolly.lazyDeserialization.setConfiguration(dolly);
         }
         if (invocationBatching != null) {
            dolly.invocationBatching = (InvocationBatching) invocationBatching.clone();
            dolly.invocationBatching.setConfiguration(dolly);
         }
         if (deadlockDetection != null) {
            dolly.deadlockDetection = (DeadlockDetectionType) deadlockDetection.clone();
            dolly.deadlockDetection.setConfiguration(dolly);
         }
         if (transaction != null) {
            dolly.transaction = transaction.clone();
            dolly.transaction.setConfiguration(dolly);
         }
         if (indexing != null) {
            dolly.indexing = indexing.clone();
            dolly.indexing.setConfiguration(dolly);
         }
         dolly.fluentConfig = new FluentConfiguration(dolly);
         return dolly;
      } catch (CloneNotSupportedException e) {
         throw new CacheException("Unexpected!", e);
      }
   }

   /**
    * Converts this configuration instance to an XML representation containing the current settings.
    *
    * @return a string containing the formatted XML representation of this configuration instance.
    */
   public String toXmlString() {
      return InfinispanConfiguration.toXmlString(this);
   }

   public boolean isUsingCacheLoaders() {
      return getCacheLoaderManagerConfig() != null && !getCacheLoaderManagerConfig().getCacheLoaderConfigs().isEmpty();
   }

   /**
    * Returns the {@link org.infinispan.config.CustomInterceptorConfig}, if any, associated with this configuration
    * object. The custom interceptors will be added to the cache at startup in the sequence defined by this list.
    *
    * @return List of custom interceptors, never null
    */
   @SuppressWarnings("unchecked")
   public List<CustomInterceptorConfig> getCustomInterceptors() {
      return customInterceptors.customInterceptors == null
            ? InfinispanCollections.<CustomInterceptorConfig>emptyList()
            : customInterceptors.customInterceptors;
   }

   public boolean isStoreKeysAsBinary() {
      return storeAsBinary.isStoreKeysAsBinary();
   }

   public boolean isStoreValuesAsBinary() {
      return storeAsBinary.isStoreValuesAsBinary();
   }
   /**
    * @deprecated Use {@link FluentConfiguration.CustomInterceptorsConfig#add(org.infinispan.interceptors.base.CommandInterceptor)}
    */
   @Deprecated
   public void setCustomInterceptors(List<CustomInterceptorConfig> customInterceptors) {
      this.customInterceptors.setCustomInterceptors(customInterceptors);
   }

   public void assertValid() throws ConfigurationException {
      if (clustering.mode.isClustered() && (globalConfiguration != null
              && (globalConfiguration.getTransportClass() == null || globalConfiguration.getTransportClass().length() == 0)))
         throw new ConfigurationException("Cache cannot use a clustered mode (" + clustering.mode + ") mode and not define a transport!");
   }

   public boolean isOnePhaseCommit() {
      return !getCacheMode().isSynchronous() || getTransactionLockingMode() == LockingMode.PESSIMISTIC;
   }

   /**
    * Returns true if the cache is configured to run in transactional mode, false otherwise. Starting with Infinispan
    * version 5.1 a cache doesn't support mixed access: i.e.won't support transactional and non-transactional
    * operations.
    * A cache is transactional if one the following:
    * <pre>
    * - a transactionManagerLookup is configured for the cache
    * - batching is enabled
    * - it is explicitly marked as transactional: config.fluent().transaction().transactionMode(TransactionMode.TRANSACTIONAL).
    *   In this last case a transactionManagerLookup needs to be explicitly set
    * </pre>
    * By default a cache is not transactional.
    *
    * @see #isTransactionAutoCommit()
    */
   public boolean isTransactionalCache() {
      return transaction.transactionMode.equals(TransactionMode.TRANSACTIONAL);
   }

   public boolean isExpirationReaperEnabled() {
       return expiration.reaperEnabled;
    }

   public boolean isHashActivated() {
      return clustering.hash.activated;
   }

   public long getL1InvalidationCleanupTaskFrequency() {
      return clustering.l1.getL1InvalidationCleanupTaskFrequency();
   }

   public void setL1InvalidationCleanupTaskFrequency(long frequencyMillis) {
      clustering.l1.setL1InvalidationCleanupTaskFrequency(frequencyMillis);
   }

   /**
    * Defines transactional (JTA) characteristics of the cache.
    *
    
