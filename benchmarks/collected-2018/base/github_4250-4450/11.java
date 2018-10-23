// https://searchcode.com/api/result/74212080/

/*******************************************************************************
 * Copyright (c) 2011, 2012 SunGard CSA LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SunGard CSA LLC - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.stardust.engine.core.persistence.jdbc;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.Date;

import javax.sql.DataSource;

import org.eclipse.stardust.common.Assert;
import org.eclipse.stardust.common.CollectionUtils;
import org.eclipse.stardust.common.Pair;
import org.eclipse.stardust.common.StringUtils;
import org.eclipse.stardust.common.config.GlobalParameters;
import org.eclipse.stardust.common.config.Parameters;
import org.eclipse.stardust.common.config.ValueProvider;
import org.eclipse.stardust.common.error.*;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.LogUtils;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.common.log.NoopLogger;
import org.eclipse.stardust.common.reflect.Reflect;
import org.eclipse.stardust.engine.api.model.IData;
import org.eclipse.stardust.engine.api.model.IModel;
import org.eclipse.stardust.engine.api.model.IParticipant;
import org.eclipse.stardust.engine.api.query.CasePolicy;
import org.eclipse.stardust.engine.api.runtime.ActivityExecutionUtils;
import org.eclipse.stardust.engine.api.runtime.ActivityInstanceState;
import org.eclipse.stardust.engine.api.runtime.BpmRuntimeError;
import org.eclipse.stardust.engine.api.runtime.IDescriptorProvider;
import org.eclipse.stardust.engine.core.cache.AbstractCache;
import org.eclipse.stardust.engine.core.cache.CacheHelper;
import org.eclipse.stardust.engine.core.monitoring.MonitoringUtils;
import org.eclipse.stardust.engine.core.monitoring.PersistentListenerUtils;
import org.eclipse.stardust.engine.core.persistence.*;
import org.eclipse.stardust.engine.core.persistence.Session.FilterOperation.FilterResult;
import org.eclipse.stardust.engine.core.persistence.jdbc.proxy.JdbcProxy;
import org.eclipse.stardust.engine.core.persistence.jdbc.sequence.CachingSequenceGenerator;
import org.eclipse.stardust.engine.core.persistence.jdbc.sequence.SequenceGenerator;
import org.eclipse.stardust.engine.core.persistence.jdbc.transientpi.TransientProcessInstanceSupport;
import org.eclipse.stardust.engine.core.persistence.jms.*;
import org.eclipse.stardust.engine.core.runtime.audittrail.management.ProcessInstanceUtils;
import org.eclipse.stardust.engine.core.runtime.beans.*;
import org.eclipse.stardust.engine.core.runtime.beans.interceptors.PropertyLayerProviderInterceptor;
import org.eclipse.stardust.engine.core.runtime.beans.removethis.KernelTweakingProperties;
import org.eclipse.stardust.engine.core.runtime.internal.changelog.ChangeLogDigester;
import org.eclipse.stardust.engine.core.runtime.logging.RuntimeLog;
import org.eclipse.stardust.engine.core.runtime.logging.RuntimeLogUtils;
import org.eclipse.stardust.engine.core.runtime.setup.*;
import org.eclipse.stardust.engine.core.runtime.utils.PerformerUtils;
import org.eclipse.stardust.engine.core.spi.extensions.runtime.IActivityExecutionStrategy;
import org.eclipse.stardust.engine.core.spi.persistence.IPersistentListener;


/**
 * Serves as an adapter for EJB integration.
 */
public class Session implements org.eclipse.stardust.engine.core.persistence.Session
{

   public static final String KEY_AUDIT_TRAIL_SCHEMA = SessionProperties.DS_NAME_AUDIT_TRAIL + SessionProperties.DS_SCHEMA_SUFFIX;

   private static final String KEY_AUDIT_TRAIL_CONNECTION_HOOK = SessionProperties.DS_NAME_AUDIT_TRAIL + SessionProperties.DS_CONNECTION_HOOK_SUFFIX;

   private static final String KEY_AUDIT_TRAIL_USE_PREPARED_STATEMENTS = SessionProperties.DS_NAME_AUDIT_TRAIL + SessionProperties.DS_USE_PREPARED_STATEMENTS_SUFFIX;

   private static final String KEY_AUDIT_TRAIL_USE_JDBC_RETURN_GENERATED_KEYS = SessionProperties.DS_NAME_AUDIT_TRAIL + SessionProperties.DS_USE_JDBC_RETURN_GENERATED_KEYS;

   private static final Joins NO_JOINS = null;

   private static final Logger trace = LogManager.getLogger(Session.class);

   private static final String EAGER_LINK_FETCH_ALIAS_PREFIX = "ELF";

   private static final SessionCacheComparator SESSION_CACHE_COMPARATOR = new SessionCacheComparator();

   public static final TypeDescriptorRegistryProvider DEFAULT_TYPE_DESCRIPTOR_REGISTRY_PROVIDER = new TypeDescriptorRegistryProvider()
   {
      public TypeDescriptorRegistry getTypeDescriptorRegistry()
      {
         return TypeDescriptorRegistry.current();
      }
   };

   public static final DmlManagerRegistryProvider DEFAULT_DML_MANAGER_REGISTRY_PROVIDER = new RuntimeDmlManagerProvider();

   /**
    * The DB schema used by this session.
    */
   private final String schemaName;

   /**
    * Hashcode of the thread the session is bound to.
    */
   private long threadBinding;

   /**
    * Data source, this session gets its connections from.
    */
   private DataSource dataSource;

   /**
    * The JDBC connectionManager may be exchanged after abort.
    */
   private Connection jdbcConnection;

   private final TypeDescriptorRegistry tdRegistry;

   /**
    * This is the registry for caches holding the persistenceControllers of persistent
    * objects. Registry keys are the type of the persistent object.
    * <p>
    * Keys are the "qualified oids" as returned by <code>TypeDescriptor.getOid</code>.
    * <p>
    * Each PersistenceController contains
    * <ul>
    * <li>a reference to the persistent Java object,</li>
    * <li>a reference to the type manager object and</li>
    * <li>the reference to an object array of primary keys of all linked
    * objects.</li>
    * </ul>
    */
   private Map<Class<?>, Map<Object, PersistenceController>> objCacheRegistry = CollectionUtils.newHashMap();

   /**
    * This registry holds caches holding the persistenceControllers of <b>dead</b>
    * persistent objects.<p>
    */
   private Map/*<Class, Map>*/ deadObjCacheRegistry = new HashMap();

   private Map/*<Class, Set>*/ synchronizationCache;

   private final DmlManagerRegistry dmlManagers;

   private List /*<BulkDeleteStatement>*/ bulkDeleteStatements = new ArrayList();
   private DBDescriptor dbDescriptor;
   private String name;

   private final boolean isArchiveAuditTrail;
   private final boolean isreadOnly;

   private DataCluster[] cachedClusterSetup;

   private SqlUtils sqlUtils;

   private List connectionHooks = Collections.EMPTY_LIST;

   private final Parameters params;

   private final SequenceGenerator uniqueIdGenerator;

   private final boolean forceImmediateInsert;

   public Session(String name)
   {
      this(name, Parameters.instance(), DBDescriptor.create(name),
            DEFAULT_TYPE_DESCRIPTOR_REGISTRY_PROVIDER,
            DEFAULT_DML_MANAGER_REGISTRY_PROVIDER);
   }

   public Session(String name, Parameters params, DBDescriptor dbDescriptor,
         TypeDescriptorRegistryProvider tdRegistryProvider,
         DmlManagerRegistryProvider dmlManagerRegistryProvider)
   {
      this.params = params;

      this.tdRegistry = tdRegistryProvider.getTypeDescriptorRegistry();

      this.name = name;
      this.schemaName = params.getString(SessionProperties.DS_NAME_AUDIT_TRAIL.equals(name)
            ? KEY_AUDIT_TRAIL_SCHEMA
            : name + SessionProperties.DS_SCHEMA_SUFFIX);

      this.dbDescriptor = dbDescriptor;
      this.sqlUtils = new SqlUtils(this.schemaName, this.dbDescriptor);

      this.dmlManagers = dmlManagerRegistryProvider.getDmlManagerRegistry(name, sqlUtils,
            dbDescriptor, tdRegistry);

      if (dbDescriptor.supportsSequences())
      {
         this.uniqueIdGenerator = getUniqueIdGenerator();
      }
      else
      {
         this.uniqueIdGenerator = null;
      }

      this.isArchiveAuditTrail = params.getBoolean(Constants.CARNOT_ARCHIVE_AUDITTRAIL,
            false);
      this.isreadOnly = params.getBoolean(SessionProperties.DS_NAME_READ_ONLY, false);
      
      this.forceImmediateInsert = params.getBoolean(Constants.FORCE_IMMEDIATE_INSERT_ON_SESSION, false);

      String cnConnectionHook = params.getString(SessionProperties.DS_NAME_AUDIT_TRAIL.equals(name)
            ? KEY_AUDIT_TRAIL_CONNECTION_HOOK
            : name + SessionProperties.DS_CONNECTION_HOOK_SUFFIX);
      if ( !StringUtils.isEmpty(cnConnectionHook))
      {
         try
         {
            Object object = Reflect.createInstance(cnConnectionHook);

            if (object instanceof ConnectionHook)
            {
               addConnectionHook((ConnectionHook) object);
            }
            else
            {
               trace.warn(cnConnectionHook + " does not implement ConnectionHook");
            }
         }
         catch(Exception e)
         {
            trace.error(cnConnectionHook + " is no valid type", e);
         }
      }
   }

   public boolean isReadOnly()
   {
      return this.isArchiveAuditTrail || this.isreadOnly;
   }

   public TypeDescriptor getTypeDescriptor(Class type)
   {
      return tdRegistry.getDescriptor(type);
   }

   private SequenceGenerator getUniqueIdGenerator()
   {
      SequenceGenerator generator = (SequenceGenerator) this.params.get(SequenceGenerator.UNIQUE_GENERATOR_PARAMETERS_KEY);

      if (null == generator)
      {
         String sequenceGeneratorName = params.getString(name + ".SequenceGenerator");

         if ( !StringUtils.isEmpty(sequenceGeneratorName))
         {
            generator = (SequenceGenerator) Reflect.createInstance(sequenceGeneratorName);
         }
         else
         {
            generator = new CachingSequenceGenerator();
         }

         generator.init(dbDescriptor, sqlUtils);

         //generator = new NonCachingSequenceGenerator(dbDescriptor, sqlUtils);
         this.params.set(SequenceGenerator.UNIQUE_GENERATOR_PARAMETERS_KEY, generator);
      }
      return generator;
   }

   public String getSchemaName()
   {
      return schemaName;
   }

   public SqlUtils getSqlUtils()
   {
      return sqlUtils;
   }

   public DataCluster[] getClusterSetup()
   {
      return cachedClusterSetup;
   }

   public boolean isUsingDataClusters()
   {
      DataCluster[] clusterSetup = getClusterSetup();
      return (null != clusterSetup) && (0 < clusterSetup.length);
   }

   /**
    *
    */
   public void connect(DataSource dataSource)
   {
      this.dataSource = dataSource;
      this.jdbcConnection = null;

      if (trace.isDebugEnabled())
      {
         trace.debug(this + ", created.");
      }
   }

   /**
    *
    */
   public void postBindingInitialization()
   {
      this.cachedClusterSetup = RuntimeSetup.instance().getDataClusterSetup();
   }

   public Collection<PersistenceController> getCache(Class type)
   {
      Map<Object, PersistenceController> cache = objCacheRegistry.get(type);

      return (null != cache)
            ? Collections.unmodifiableCollection(cache.values())
            : Collections.<PersistenceController>emptyList();
   }

   public void cluster(Persistent persistent)
   {
      if (isArchiveAuditTrail)
      {
         // readonly
         throw new PublicException("Archive AuditTrail does not allow changes.");
      }


      String stmtString = null;
      try
      {
         Class<? extends Persistent> type = persistent.getClass();
         final TypeDescriptor typeDescriptor = TypeDescriptor.get(type);

         DmlManager dmlManager = getDMLManager(persistent.getClass());

         if ((dbDescriptor.supportsSequences())
               && typeDescriptor.requiresPKCreation())
         {
            dmlManager.setPK(persistent, this.uniqueIdGenerator.getNextSequence(typeDescriptor, this));
         }

         // if insert is deferred, it will be performed in flush()
         if (isImmediateInsert(persistent, typeDescriptor))
         {
            if (persistent instanceof LazilyEvaluated)
            {
               ((LazilyEvaluated) persistent).performLazyEvaluation();
            }

            // new DefaultPersistenceController marks itself as OPENED
            insertPersistent(persistent, dmlManager, typeDescriptor);
            update(CacheHelper.getCache(type), persistent);
         }
         else
         {
            if (trace.isDebugEnabled())
            {
               trace.debug("Deferring insert of '" + typeDescriptor.getTableName() + "'.");
            }
            addToPersistenceControllers(
                  typeDescriptor.getIdentityKey(persistent),
                  dmlManager.createPersistenceController(this, persistent));
            // markCreated must be after addToPersistenceControllers
            persistent.markCreated();
         }
      }
      catch (SQLException x)
      {
         throw new InternalException("Statement: " + stmtString, x);
      }
   }

   private boolean isImmediateInsert(Persistent persistent, TypeDescriptor typeDescriptor)
   {
      if (forceImmediateInsert)
      {
         return true;
      }

      if ((persistent instanceof DeferredInsertable)
            && ((DeferredInsertable) persistent).deferInsert())
      {
         return typeDescriptor.requiresPKCreation() && !dbDescriptor.supportsSequences();
      }

      if ( !typeDescriptor.isTryDeferredInsert())
      {
         // immediate insert is required
         return true;
      }
      if (typeDescriptor.requiresPKCreation() && !dbDescriptor.supportsSequences())
      {
         // can not defer insert since pkcreation is required and the DB does not
         // support sequences
         return true;
      }
      return false;
   }

   private void insertPersistent(Persistent persistent, DmlManager dmlManager, TypeDescriptor typeDescriptor) throws SQLException
   {
      Long identityOid = null;
      boolean useJdbc14GeneratedKeys = dbDescriptor.supportsIdentityColumns()
            && typeDescriptor.requiresPKCreation()
            && params.getBoolean(SessionProperties.DS_NAME_AUDIT_TRAIL.equals(name) //
                  ? KEY_AUDIT_TRAIL_USE_JDBC_RETURN_GENERATED_KEYS //
                  : name + SessionProperties.DS_USE_JDBC_RETURN_GENERATED_KEYS, //
                  false);

      if (isUsingPreparedStatements(persistent.getClass()))
      {
         boolean useBatchStatement = true;

         if (DBMSKey.MSSQL8.equals(dbDescriptor.getDbmsKey()))
         {
            // MSSQL needs to use getGeneratedKeys() method,
            // since SELECT SCOPE_IDENTITY() has scope problems in Prepared Statements
            useJdbc14GeneratedKeys = true;

            // getGeneratedKeys() is not supported after executed Batch Statements
            useBatchStatement = false;
         }

         PreparedStatement stmt = null;
         try
         {
            // actually contains no BatchStatement (TODO refactor to StatementWrapper)
            BatchStatementWrapper wrapper = dmlManager.prepareInsertRowStatement(
                  getConnection(), persistent, useJdbc14GeneratedKeys, useBatchStatement);
            stmt = wrapper.getStatement();

            try
            {
               // TODO always use executeUpdate because of only one persistent no
               // BatchStatement is needed?
               if (useBatchStatement)
               {
               stmt.executeBatch();
            }
               else
               {
                  stmt.executeUpdate();
               }

               if (useJdbc14GeneratedKeys)
               {
                  ResultSet rsGeneratedKeys = null;
                  try
                  {
                     rsGeneratedKeys = stmt.getGeneratedKeys();
                     if (rsGeneratedKeys.next())
                     {
                        identityOid = new Long(rsGeneratedKeys.getLong(1));
                     }
                  }
            catch (SQLException sqle)
            {
                     trace.debug("Failed retrieving generated key.", sqle);
                  }
                  finally
                  {
                     QueryUtils.closeResultSet(rsGeneratedKeys);
                  }
               }
            }
            catch (SQLException sqle)
            {
               ExceptionUtils.logAllBatchExceptions(sqle);
               ApplicationException ae = ExceptionUtils.transformException(dbDescriptor,
                     sqle);
               if (null != ae)
               {
                  throw ae;
               }
               throw sqle;
            }
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }
      }
      else
      {
         Statement stmt = null;
         try
         {
            stmt = getConnection().createStatement();
            String stmtString = dmlManager.getInsertRowStatementString(persistent);

            long startTime = System.currentTimeMillis();
            if (useJdbc14GeneratedKeys)
            {
               stmt.executeUpdate(stmtString, Statement.RETURN_GENERATED_KEYS);
               ResultSet rsGeneratedKeys = null;
            try
            {
                  rsGeneratedKeys = stmt.getGeneratedKeys();
                  if (rsGeneratedKeys.next())
                  {
                     identityOid = new Long(rsGeneratedKeys.getLong(1));
                  }
               }
               catch (SQLException sqle)
               {
                  trace.debug("Failed retrieving generated key.", sqle);
               }
               finally
               {
                  QueryUtils.closeResultSet(rsGeneratedKeys);
               }
            }
            else
            {
               try
               {
               stmt.executeUpdate(stmtString);
            }
            catch (SQLException sqle)
            {
               ExceptionUtils.logAllBatchExceptions(sqle);
                  ApplicationException ae = ExceptionUtils.transformException(
                        dbDescriptor, sqle);
               if (null != ae)
               {
                  throw ae;
               }
               throw sqle;
            }
            }

            monitorSqlExecution(stmtString, startTime, System.currentTimeMillis());
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }

      }
      if (dbDescriptor.supportsIdentityColumns()
            && typeDescriptor.requiresPKCreation())
      {
         dmlManager.setPK(persistent, (null != identityOid)
               ? identityOid.longValue()
               : dmlManager.getIdentityOID(this));
      }

      // creating lock table entry just after being sure to have proper PK values
      if (isUsingLockTables() && typeDescriptor.isDistinctLockTableName())
      {
         createLockTableEntry(persistent);
      }

      addToPersistenceControllers(
            dmlManager.getTypeDescriptor().getIdentityKey(persistent),
            dmlManager.createPersistenceController(this, persistent));

      if (dbDescriptor.supportsIdentityColumns()
            && typeDescriptor.requiresPKCreation())
      {
         // NOTE touch self references, to make sure the retrieved OID gets persisted
         // as FK-value

         for (Iterator i = dmlManager.getTypeDescriptor().getLinks().iterator(); i.hasNext();)
         {
            boolean touchLink = false;
            LinkDescriptor link = (LinkDescriptor) i.next();
            try
            {
               if ( !link.getField().isAccessible())
               {
                  link.getField().setAccessible(true);
               }
               touchLink = persistent == link.getField().get(persistent);
            }
            catch (Exception e)
            {
               trace.debug("Failed performing post-PK-create link validation. "
                     + "Possibly touching too much.", e);
               touchLink = true;
            }

            if (touchLink)
            {
               persistent.getPersistenceController().fetchLink(
                     link.getField().getName());
               persistent.markModified(link.getField().getName());
            }
         }
      }

      if (trace.isDebugEnabled())
      {
         trace.debug(this + ", inserted: " + persistent.getClass().getName());
      }
   }

   private void doBatchInsert(List<Persistent> persistentToBeInserted,
         List<Persistent> persistentAlreadyExists, DmlManager dmlManager,
         TypeDescriptor typeDescriptor) throws SQLException
   {
      if(persistentToBeInserted.isEmpty())
      {
         return;
      }

      if (dbDescriptor.supportsIdentityColumns()
            && typeDescriptor.requiresPKCreation())
      {
         // can not perform batch inserts if identity columns are used and PK creation is required
         // do single inserts as before
         doSingleStatementInsert(persistentToBeInserted, persistentAlreadyExists,
               dmlManager, typeDescriptor);
      }
      else
      {
         final Connection connection = getConnection();

         if (isUsingPreparedStatements(typeDescriptor.getType()))
         {
            PreparedStatement stmt = null;
            try
            {
               BatchStatementWrapper wrapper = dmlManager.prepareInsertRowStatement(
                     connection, persistentToBeInserted);
               stmt = wrapper.getStatement();
               String stmtString = wrapper.getStatementString();

               if (trace.isDebugEnabled())
               {
                  trace.debug("SQL: " + stmtString);
               }
               int[] updateCounts = executeBatchAndTransformException(stmt);
               if (trace.isDebugEnabled())
               {
                  trace.debug("Batch insert counts: "
                        + ExceptionUtils.updateCountsToString(updateCounts));
               }
            }
            catch (UniqueConstraintViolatedException x)
            {
               handleBatchUpdateException((BatchUpdateException) x.getCause(),
                     persistentToBeInserted, persistentAlreadyExists, dmlManager,
                     typeDescriptor);
            }
            finally
            {
               QueryUtils.closeStatement(stmt);
            }
         }
         else
         {
            Statement stmt = connection.createStatement();
            try
            {
               for (Iterator i = persistentToBeInserted.iterator(); i.hasNext();)
               {
                  Persistent persistent = (Persistent) i.next();
                  String stmtString = dmlManager.getInsertRowStatementString(persistent);
                  if (trace.isDebugEnabled())
                  {
                     trace.debug("SQL: " + stmtString);
                  }
                  stmt.addBatch(stmtString);
               }
               int[] updateCounts = executeBatchAndTransformException(stmt);
               if (trace.isDebugEnabled())
               {
                  trace.debug("batch insert counts: "
                        + ExceptionUtils.updateCountsToString(updateCounts));
               }
            }
            catch (UniqueConstraintViolatedException x)
            {
               handleBatchUpdateException((BatchUpdateException) x.getCause(),
                     persistentToBeInserted, persistentAlreadyExists, dmlManager,
                     typeDescriptor);
            }
            finally
            {
               QueryUtils.closeStatement(stmt);
            }
         }

         // create lock table entries and register all inserted
         for (Iterator i = persistentToBeInserted.iterator(); i.hasNext();)
         {
            Persistent persistent = (Persistent) i.next();
            if (isUsingLockTables() && typeDescriptor.isDistinctLockTableName())
            {
               createLockTableEntry(persistent);
            }
            addToPersistenceControllers(
                  dmlManager.getTypeDescriptor().getIdentityKey(persistent),
                  dmlManager.createPersistenceController(this, persistent));
            if (trace.isDebugEnabled())
            {
               trace.debug(this + ", inserted: " + persistent.getClass().getName());
            }
         }
      }
   }

   /**
    * Executes statements as batch statement. If an BatchUpdateException is thrown then
    * all nested exceptions will be tested to be a unique constraint violation exception.
    * If this is true then a single UniqueConstraintViolatedException is thrown wrapping
    * that BatchUpdateException, otherwise the original exception is thrown.
    *
    * @param stmt The statements
    * @return result from stmt.executeBatch(), otherwise an exception
    *
    * @throws SQLException
    */
   private int[] executeBatchAndTransformException(Statement stmt) throws SQLException
   {
      try
      {
         return stmt.executeBatch();
      }
      catch (BatchUpdateException x)
      {
         // do not want to log all created UniqueConstraintViolatedException instances.
         final NoopLogger noopLogger = new NoopLogger();

         // check that all exceptions are UniqueConstraintViolatedException
         boolean foundUniqueConstraintViolatedException = false;
         SQLException innerException = x.getNextException();
         while (innerException != null)
         {
            ApplicationException ae = ExceptionUtils.transformException(dbDescriptor,
                  innerException, noopLogger);
            if ( !(ae instanceof UniqueConstraintViolatedException))
            {
               // not all exceptions are about unique constraint violation. Cannot handle it.
               throw x;
            }

            foundUniqueConstraintViolatedException = true;
            innerException = innerException.getNextException();
         }

         if ( !foundUniqueConstraintViolatedException)
         {
            trace.warn("BatchUpdateException did not proof to be caused by an unique constraint violation.");
         }

         // Throw an UniqueConstraintViolatedException which wraps the BatchUpdateException
         throw ExceptionUtils.createUniqueConstraintViolationException(x, noopLogger);
      }
   }

   /**
    * Tries to insert persistent objects taken from list persistentToBeInserted.
    * If unique constraint is violated then the persistent will be removed from that list
    * and copied to list persistentAlreadyExists.
    *
    * @param persistentToBeInserted
    * @param persistentAlreadyExists
    * @param dmlManager
    * @param typeDescriptor
    * @throws SQLException
    */
   private void doSingleStatementInsert(List<Persistent> persistentToBeInserted,
         List<Persistent> persistentAlreadyExists, DmlManager dmlManager,
         TypeDescriptor typeDescriptor) throws SQLException
   {
      for (Iterator i = persistentToBeInserted.iterator(); i.hasNext();)
      {
         Persistent persistent = (Persistent) i.next();

         try
         {
            insertPersistent(persistent, dmlManager, typeDescriptor);
         }
         catch (UniqueConstraintViolatedException x)
         {
            if (persistentAlreadyExists != null)
            {
               i.remove();
               persistentAlreadyExists.add(persistent);
            }
            else
            {
               throw x;
            }
         }
      }
   }

   /**
    * This method handles a BatchUpdateException with the assumption that all
    * nested exceptions are thrown due to unique constraint violation.
    * If unique constraint is violated then the persistent will be removed from
    * list persistentToBeInserted and copied to list persistentAlreadyExists.
    *
    * @param bux the batch exception
    * @param persistentToBeInserted complete list of persistent objects which need to be inserted. Some of them may already be inserted by previous batch statement.
    * @param persistentAlreadyExists list which will be filled with already existing persistent objects.
    * @param dmlManager
    * @param typeDescriptor
    * @throws SQLException
    */
   private void handleBatchUpdateException(BatchUpdateException bux,
         List<Persistent> persistentToBeInserted,
         List<Persistent> persistentAlreadyExists, DmlManager dmlManager,
         TypeDescriptor typeDescriptor) throws SQLException
   {
      if (persistentAlreadyExists == null)
      {
         throw bux;
      }

      trace.warn("Catched batch exception caused by 'unique constraint violated' exceptions thrown during batch insertion of"
           + typeDescriptor.getType().getSimpleName() + ".");

      int[] updateCounts = bux.getUpdateCounts();

      if (updateCounts.length < persistentToBeInserted.size())
      {
         // JDBC driver decided to stop batch update on first exception.
         // Try to insert remaining persistent objects one by one, and move
         // them to "already exists" list if insert fails.

         trace.info("Performing insert of remaining items by single insert statements.");

         // This element at least can be assumed to already exists.
         persistentAlreadyExists.add(persistentToBeInserted.get(updateCounts.length));
         persistentToBeInserted.remove(updateCounts.length);

         // Now try to insert the remaining persistent objects. Next index is the same as
         // before as we removed one item from list.
         int nextIdx = updateCounts.length;
         if (nextIdx < persistentToBeInserted.size())
         {
            doSingleStatementInsert(persistentToBeInserted.subList(nextIdx,
                  persistentToBeInserted.size()), persistentAlreadyExists,
                  dmlManager, typeDescriptor);
         }
      }
      else if (updateCounts.length == persistentToBeInserted.size())
      {
         // JDBC driver decided to continue batch update on exceptions.
         // Move persistent objects which failed to be inserted to "already exists" list.

         trace.info("Preparing conversion from insert to update for items which failed insertion.");

         int idx = 0;
         for (Iterator<Persistent> i = persistentToBeInserted.iterator(); i.hasNext();)
         {
            Persistent persistent = i.next();
            if (Statement.EXECUTE_FAILED == updateCounts[idx])
            {
               persistentAlreadyExists.add(persistent);
               i.remove();
            }
         }
      }
      else
      {
         Assert.lineNeverReached(MessageFormat.format(
               "Batch update performed {0} statements but only {1} persistent objects existed.",
               new Object[] { updateCounts.length, persistentToBeInserted.size() }));
      }
   }

   private void doBatchUpdate(List<Persistent> persistentToBeUpdated,
         DmlManager dmlManager, TypeDescriptor typeDescriptor) throws SQLException
   {
      if(persistentToBeUpdated.isEmpty())
      {
         return;
      }

      final Connection connection = getConnection();

      if (isUsingPreparedStatements(typeDescriptor.getType())) {
         PreparedStatement stmt = null;
         try
         {
            BatchStatementWrapper wrapper = dmlManager.prepareUpdateRowStatement(
                  connection, persistentToBeUpdated, false);
            stmt = wrapper.getStatement();
            String stmtString = wrapper.getStatementString();

            if (trace.isDebugEnabled())
            {
               trace.debug("SQL: " + stmtString);
            }

            //((IdentifiablePersistent) dpc.getPersistent()).lock();
            int [] updateCounts = stmt.executeBatch();
            if (trace.isDebugEnabled())
            {
               trace.debug("Batch update counts: "+ExceptionUtils.updateCountsToString(updateCounts));
            }
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }
      }
      else
      {
         Statement stmt = connection.createStatement();
         try
         {
            for (Iterator i = persistentToBeUpdated.iterator(); i.hasNext();)
            {
               Persistent persistent = (Persistent) i.next();
               String stmtString = dmlManager.getUpdateRowStatementString(connection,
                     persistent);
               if (trace.isDebugEnabled())
               {
                  trace.debug("SQL: " + stmtString);
               }
               //((IdentifiablePersistent) dpc.getPersistent()).lock();
               stmt.addBatch(stmtString);
            }
            int [] updateCounts = stmt.executeBatch();
            if (trace.isDebugEnabled())
            {
               trace.debug("batch update counts: "+ExceptionUtils.updateCountsToString(updateCounts));
            }
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }

      }
   }

   private void createLockTableEntry(Persistent persistent) throws SQLException
   {
      if (isArchiveAuditTrail)
      {
         // readonly
         throw new PublicException("Archive AuditTrail does not allow changes.");
      }

      final DmlManager dmlManager = getDMLManager(persistent.getClass());
      final TypeDescriptor typeDescriptor = dmlManager.getTypeDescriptor();

      if (isUsingLockTables() && typeDescriptor.isDistinctLockTableName())
      {
         long startTime;
         long stopTime;
         String sqlString;

         if (isUsingPreparedStatements(persistent.getClass()))
         {
            Field[] pkFields = typeDescriptor.getPkFields();

            StringBuffer insLckBuffer = new StringBuffer(200);
            insLckBuffer.append("INSERT INTO ");
            if ( !StringUtils.isEmpty(schemaName))
            {
               insLckBuffer.append(schemaName).append(".");
            }
            insLckBuffer.append(typeDescriptor.getLockTableName()).append(" (");

            for (int i = 0; i < pkFields.length; i++ )
            {
               if (0 < i)
               {
                  insLckBuffer.append(", ");
               }
               insLckBuffer.append(pkFields[i].getName());
            }
            insLckBuffer.append(") VALUES (");
            for (int i = 0; i < pkFields.length; i++ )
            {
               if (0 < i)
               {
                  insLckBuffer.append(", ");
               }
               insLckBuffer.append("?");
            }
            insLckBuffer.append(")");

            sqlString = insLckBuffer.toString();
            PreparedStatement insLckStmt = getConnection().prepareStatement(
                  sqlString);
            try
            {
               for (int i = 0; i < pkFields.length; i++ )
               {
                  Object pkValue;
                  try
                  {
                     pkValue = pkFields[i].get(persistent);
                  }
                  catch (Exception e)
                  {
                     throw new InternalException(
                           "Failed binding PK values for lock table entry.", e);
                  }
                  DmlManager.setSQLValue(insLckStmt,
                        1 + typeDescriptor.getFieldColumnIndex(pkFields[i]),
                        pkFields[i].getType(), pkValue, dbDescriptor);
               }
               startTime = System.currentTimeMillis();
               insLckStmt.executeUpdate();
               stopTime = System.currentTimeMillis();
            }
            finally
            {
               QueryUtils.closeStatement(insLckStmt);
            }
         }
         else
         {
            Field[] pkFields = typeDescriptor.getPkFields();

            StringBuffer insLckBuffer = new StringBuffer(200);
            insLckBuffer.append("INSERT INTO ");
            if ( !StringUtils.isEmpty(schemaName))
            {
               insLckBuffer.append(schemaName).append(".");
            }
            insLckBuffer.append(typeDescriptor.getLockTableName()).append(" (");
            for (int i = 0; i < pkFields.length; i++ )
            {
               if (0 < i)
               {
                  insLckBuffer.append(", ");
               }
               insLckBuffer.append(pkFields[i].getName());
            }
            insLckBuffer.append(") VALUES (");
            for (int i = 0; i < pkFields.length; i++ )
            {
               if (0 < i)
               {
                  insLckBuffer.append(", ");
               }
               Object pkValue;
               try
               {
                  pkValue = pkFields[i].get(persistent);
               }
               catch (Exception e)
               {
                  throw new InternalException(
                        "Failed binding PK values for lock table entry.", e);
               }
               insLckBuffer.append(DmlManager.getSQLValue(pkFields[i].getType(),
                     pkValue, dbDescriptor));
            }
            insLckBuffer.append(")");

            Statement insLckStmt = getConnection().createStatement();
            try
            {
               sqlString = insLckBuffer.toString();
               startTime = System.currentTimeMillis();
               insLckStmt.executeUpdate(sqlString);
               stopTime = System.currentTimeMillis();
            }
            finally
            {
               QueryUtils.closeStatement(insLckStmt);
            }
         }

         monitorSqlExecution(sqlString, startTime, stopTime);
      }
   }

   public void cluster(DataClusterInstance cluster)
   {
      StringBuffer insertStmt = new StringBuffer(100);

      insertStmt.append("INSERT INTO ");
      if ( !StringUtils.isEmpty(schemaName))
      {
         insertStmt.append(schemaName).append(".");
      }
      insertStmt.append(cluster.getTableName()).append(" (")
            .append(cluster.getProcessInstanceColumn()).append(") VALUES (");

      long startTime;
      long stopTime;
      String sqlString;

      if (isUsingPreparedStatements(null))
      {
         insertStmt.append("?)");

         PreparedStatement stmt = null;
         try
         {
            sqlString = insertStmt.toString();
            stmt = getConnection().prepareStatement(sqlString);
            stmt.setLong(1, cluster.getProcessInstanceOid());

            startTime = System.currentTimeMillis();
            stmt.executeUpdate();
            stopTime = System.currentTimeMillis();
         }
         catch(SQLException x)
         {
            throw new InternalException("Can't write DataClusterInstance to table "
                  + cluster.getTableName(), x);
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }
      }
      else
      {
         insertStmt.append(cluster.getProcessInstanceOid()).append(")");

         Statement stmt = null;
         try
         {
            stmt = getConnection().createStatement();
            sqlString = insertStmt.toString();

            startTime = System.currentTimeMillis();
            stmt.executeUpdate(sqlString);
            stopTime = System.currentTimeMillis();
         }
         catch(SQLException x)
         {
            throw new InternalException("Can't write DataClusterInstance to table "
                  + cluster.getTableName(), x);
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }
      }

      monitorSqlExecution(sqlString, startTime, stopTime);
   }

   public DmlManager getDMLManager(Class type)
   {
      DmlManager result = dmlManagers.getDmlManager(type);
      if (null == result)
      {
         throw new InternalException("Cannot retrieve DML manager for type " + type);
      }
      return result;
   }

   public void addToPersistenceControllers(Object identityKey,
         PersistenceController controller)
   {
      final Persistent po = controller.getPersistent();
      final Class type = po.getClass();

      Map cache = (Map) objCacheRegistry.get(type);
      if (null == cache)
      {
         cache = new TreeMap(SESSION_CACHE_COMPARATOR);

         // check if a saved session is being reused (archiver)
         // then, the empty maps set in fastCloseAndClearPersistenceControllers
         // must be recreated
         if (objCacheRegistry == Collections.EMPTY_MAP)
         {
            objCacheRegistry = CollectionUtils.newMap();
         }
         objCacheRegistry.put(type, cache);
      }

      cache.put(identityKey, controller);
      TypeDescriptor typeDescriptor = tdRegistry.getDescriptor(type);
      for (int i = 0; i < typeDescriptor.getParents().size(); ++i)
      {
         LinkDescriptor link = (LinkDescriptor) typeDescriptor.getParents().get(i);
         Persistent foreignObject = fetchLink(po, link.getField().getName());
         try
         {
            link.getRegistrar().invoke(foreignObject, new Object[] { po });
         }
         catch (Exception e)
         {
            throw new InternalException(e);
         }
      }
   }

   public void addToDeadPersistenceControllers(Object identityKey,
         PersistenceController cntrl)
   {
      final Class type = cntrl.getPersistent().getClass();

      Map cache = (Map) deadObjCacheRegistry.get(type);
      if (null == cache)
      {
         cache = new TreeMap();

         // check if a saved session is being reused (archiver)
         // then, the empty maps set in fastCloseAndClearPersistenceControllers
         // must be recreated
         if (deadObjCacheRegistry == Collections.EMPTY_MAP)
         {
            deadObjCacheRegistry = CollectionUtils.newMap();
         }

         deadObjCacheRegistry.put(type, cache);
      }
      cache.put(identityKey, cntrl);
   }

   public boolean exists(Class type, QueryExtension queryExtension)
   {
      return 1 <= getCount(type, queryExtension);
      }

   public long getCount(Class type)
   {
      return getCount(type, NO_QUERY_EXTENSION, false, NO_FETCH_PREDICATE, NO_TIMEOUT);
   }

   public long getCount(Class type, QueryExtension queryExtension)
   {
      return getCount(type, queryExtension, false, NO_FETCH_PREDICATE, NO_TIMEOUT);
   }

   public long getCount(Class type, QueryExtension queryExtension, int timeout)
   {
      return getCount(type, queryExtension, false, NO_FETCH_PREDICATE, timeout);
   }

   public long getCount(Class type, QueryExtension queryExtension, boolean mayFail)
   {
      return getCount(type, queryExtension, mayFail, NO_FETCH_PREDICATE, NO_TIMEOUT);
   }

   public long getCount(Class type, QueryExtension queryExtension,
         FetchPredicate fetchPredicate, int timeout)
   {
      return getCount(type, queryExtension, false, fetchPredicate, timeout);
   }

   private long getCount(Class type, QueryExtension queryExtension, boolean mayFail,
         FetchPredicate fetchPredicate, int timeout)
   {
      boolean useTimeout = false;
      if ((mayFail && getDBDescriptor().useQueryTimeout()) || timeout > NO_TIMEOUT)
      {
         useTimeout = true;
         if (timeout <= 0)
         {
            timeout = Parameters.instance().getInteger(
                  KernelTweakingProperties.GET_COUNT_DEFAULT_TIMEOUT, 5);
            if (timeout <= 0)
            {
               trace
                     .warn("The value of property Carnot.Engine.Tuning.GetCountDefaultTimeout should be greater than 0.");
            }
         }
      }

      ResultSet resultSet = null;

      try
      {
         QueryExtension innerQueryExtension = QueryExtension.deepCopy(queryExtension);

         TypeDescriptor typeDescriptor = getDMLManager(type).getTypeDescriptor();
         if (null == fetchPredicate)
         {
            Column countSelectList;
            if (!innerQueryExtension.isDistinct())
            {
               countSelectList = Functions.rowCount();
            }
            else
            {
               Field[] pkFields = typeDescriptor.getPkFields();

               FieldRef[] fields = new FieldRef[pkFields.length];
               for (int i = 0; i < pkFields.length; i++ )
               {
                  fields[i] = typeDescriptor.fieldRef(pkFields[i].getName());
               }
               countSelectList = Functions.countDistinct(fields);
            }
            innerQueryExtension.setSelection(new Column[] {countSelectList});
            innerQueryExtension.setDistinct(false);

            resultSet = executeQuery(type, innerQueryExtension, timeout);

            if (resultSet.next())
            {
               return resultSet.getLong(1);
            }
            else
            {
               throw new InternalException("SELECT COUNT(*) - did not return any record");
            }
         }
         else
         {
            Set selectedFields = new HashSet();
            Field[] pkFields = typeDescriptor.getPkFields();
            List selectList/*<Column>*/ = new ArrayList(pkFields.length);
            selectList.add(Functions.rowCount());
            /* for (int i = 0; i < pkFields.length; i++ )
            {
               selectedFields.add(pkFields[i].getName());
               selectList.add(typeDescriptor.fieldRef(pkFields[i].getName()));
            } */
            FieldRef[] fetchPredFields = fetchPredicate.getReferencedFields();
            if (null != fetchPredFields)
            {
               for (int i = 0; i < fetchPredFields.length; i++ )
               {
                  FieldRef fr = fetchPredFields[i];
                  if ( !selectedFields.contains(fr.fieldName))
                  {
                     //FieldRef fr = typeDescriptor.fieldRef(field);
                     selectList.add(fr);
                     innerQueryExtension.addGroupBy(fr);
                  }
               }
            }
            innerQueryExtension.setSelection((Column[]) selectList.toArray(
                  new Column[selectList.size()]));
            resultSet = executeQuery(type, innerQueryExtension, timeout);

            long count = 0;
            while (resultSet.next())
            {
               if (fetchPredicate.accept(resultSet))
               {
                  // With the case policy it is possible for a single oid to be counted multiple times by the COUNT function
                  // leading to a wrong total count.
                  if (queryExtension.getHints() != null && Boolean.TRUE.equals(queryExtension.getHints().get(CasePolicy.class.getName())))
                  {
                     count++;
                  }
                  else
                  {
                     long partialCount = resultSet.getLong(1);
                     count += partialCount;
                  }
               }
            }

            return count;
         }
      }
      catch(SQLException se)
      {
         if (useTimeout)
         {
            throw new ConcurrencyException(
                  BpmRuntimeError.BPMRT_TIMEOUT_DURING_COUNT.raise(type.getName()));
         }
         else
         {
            throw new InternalException(se);
         }
      }
      finally
      {
         QueryUtils.closeResultSet(resultSet);
      }
   }

   public ResultIterator getIterator(Class type)
   {
      return getIterator(type, NO_QUERY_EXTENSION, 0, -1, NO_FETCH_PREDICATE, false,
            NO_TIMEOUT);
   }

   public ResultIterator getIterator(Class type, QueryExtension queryExtension)
   {
      return getIterator(type, queryExtension, 0, -1, NO_FETCH_PREDICATE, false,
            NO_TIMEOUT);
   }

   public ResultIterator getIterator(Class type, QueryExtension queryExtension,
         int startIndex, int extent)
   {
      return getIterator(type, queryExtension, startIndex, extent, NO_FETCH_PREDICATE,
            false, NO_TIMEOUT);
   }

   public ResultIterator getIterator(Class type, QueryExtension queryExtension,
         int startIndex, int extent, int timeout)
   {
      return getIterator(type, queryExtension, startIndex, extent, NO_FETCH_PREDICATE,
            false, timeout);
   }

   public ResultIterator getIterator(Class type, QueryExtension queryExtension,
         int startIndex, int extent, FetchPredicate fetchPredicate, boolean countAll,
         int timeout)
   {
      boolean distinct = queryExtension == null
            ? false
            : queryExtension.isDistinct() || queryExtension.isEngineDistinct();

      ResultSet resultSet = executePersistentQuery(QueryDescriptor.from(type,
            queryExtension), timeout);
      return new ResultSetIterator(this, type, distinct, resultSet, startIndex, extent,
            fetchPredicate, countAll);
   }

   /**
    * Retrieves a {@link Vector} of instances of <code>type</code>.
    *
    * @see #getIterator(Class)
    */
   public Vector getVector(Class type)
   {
      return (Vector) ClosableIteratorUtils.copyResult(new Vector(), getIterator(type));
   }

   public Vector getVector(Class type, QueryExtension queryExtension)
   {
      return (Vector) ClosableIteratorUtils.copyResult(new Vector(), getIterator(type,
            queryExtension));
   }

   public <T extends Persistent> T findFirst(Class<T> type, QueryExtension queryExtension)
   {
      return findFirst(type, queryExtension, NO_TIMEOUT);
   }

   public <T extends Persistent> T findFirst(Class<T> type, QueryExtension queryExtension, int timeout)
   {
      T persistent = null;

      ResultIterator iterator = getIterator(type, queryExtension, 0, 1, timeout);
      try
      {
         if (iterator.hasNext())
         {
            persistent = (T) iterator.next();
         }
      }
      finally
      {
         iterator.close();
      }

      return persistent;
   }

   public void save()
   {
      save(true);
   }

   public void save(boolean closeConnection)
   {
      if (trace.isDebugEnabled())
      {
         trace.debug("Committing DatabaseDriver.");
      }

      try
      {
         try
         {
            flush();

            if (hasConnection())
            {
               getConnection().commit();
            }

            if (trace.isDebugEnabled())
            {
               trace.debug(this + ", committed.");
            }
         }
         finally
         {
            if (closeConnection && hasConnection())
            {
               returnJDBCConnection();
            }
         }
      }
      catch (SQLException x)
      {
         throw new InternalException("Cannot commit database session.", x);
      }
   }

   public void rollback()
   {
      rollback(true);
   }

   public void rollback(boolean closeConnection)
   {
      try
      {
         try
         {
            fastCloseAndClearPersistenceControllers();

            if (hasConnection())
            {
               getConnection().rollback();
            }

            if (trace.isDebugEnabled())
            {
               trace.debug(this + ", rolled back.");
            }
         }
         finally
         {
            if (closeConnection)
            {
               returnJDBCConnection();
            }
         }
      }
      catch (SQLException x)
      {
         throw new InternalException("Cannot rollback database session.", x);
      }
   }

   /**
    * Writes all changes being cached in the session buffer, releases all
    * resources but does not commit or rollback the transaction.
    */
   public void flush()
   {
      if (isArchiveAuditTrail)
      {
         // readonly
         return;
      }

      if (trace.isDebugEnabled())
      {
         trace.debug(this + ", flushing!");
      }

      final TransientProcessInstanceSupport transientPiSupport = new TransientProcessInstanceSupport(dbDescriptor.supportsSequences());
      
      try
      {

         // notify listeners in advance since they might add new objects to the cache
         // collect the keys to avoid concurrent modification exceptions
         List<Class<?>> types = CollectionUtils.newList(objCacheRegistry.keySet());
         for (Class<?> type : types)
         {
            Map cache = objCacheRegistry.get(type);
            // collect the values to avoid concurrent modification exceptions
            List<PersistenceController> controllers = CollectionUtils.newList(cache.values());
            for (PersistenceController dpc : controllers)
            {
               Persistent persistent = dpc.getPersistent();
               IPersistentListener persistentListener = PersistentListenerUtils.getPersistentListener(persistent);
               if (dpc.isCreated())
               {
                  persistentListener.created(persistent);
               }
               else if (dpc.isModified())
               {
                  persistentListener.updated(persistent);
               }
            }
         }
         // update modified objects

         Map aiCache = (Map) objCacheRegistry.get(ActivityInstanceBean.class);
         if ((null != aiCache) && !aiCache.isEmpty())
         {
            updateWorkItems(aiCache);
            updateActivityInstancesHistory(aiCache);
         }

         // TODO make sure no non-PI related data is lost

         Map<Object, PersistenceController> pis = objCacheRegistry.get(ProcessInstanceBean.class);
         
         boolean supportsAsynchWrite = params.getBoolean("Carnot.Engine.Tuning.SupportAsyncAuditTrailWrite", false);
         supportsAsynchWrite &= supportsAsynchWrite && (null != pis) && !pis.isEmpty();

         if (supportsAsynchWrite)
         {
            for (Iterator i = pis.values().iterator(); i.hasNext();)
            {
               PersistenceController pcPi = (PersistenceController) i.next();
               IProcessInstance pi = (IProcessInstance) pcPi.getPersistent();

               supportsAsynchWrite &= pcPi.isCreated() && pi.isTerminated();
            }
         }
         else if (ProcessInstanceUtils.isTransientPiSupportEnabled())
         {
            final Map<Object, PersistenceController> ais = objCacheRegistry.get(ActivityInstanceBean.class);
            transientPiSupport.init(pis, ais);
         }

         BlobBuilder blobBuilder = null;
         if (supportsAsynchWrite || transientPiSupport.persistentsNeedToBeWrittenToBlob())
         {
            if (supportsAsynchWrite && params.getBoolean("Carnot.Engine.Tuning.SupportAsyncAuditTrailWriteViaJms", true))
            {
               try
               {
                  blobBuilder = new JmsBytesMessageBuilder();
               }
               catch (PublicException e)
               {
                  throw new InternalException("Failed to prepare JMS BLOB creation.", e);
               }
            }
            else
            {
               blobBuilder = new ByteArrayBlobBuilder();
            }

            blobBuilder.init(params);
         }


         Map<Pair<Long, DataCluster>, List<Pair<PersistenceController, DataSlot>>> piToDv = isUsingDataClusters()
               ? CollectionUtils.newMap()
               : Collections.EMPTY_MAP;

         Set dpcDelayedCloseSet = null;
         List<Persistent> persistentToBeInserted = Collections.EMPTY_LIST;
         List<Persistent> persistentToBeUpdated = Collections.EMPTY_LIST;

         for (Class<?> type : objCacheRegistry.keySet())
         {
            Map cache = objCacheRegistry.get(type);
            if ((null == cache) || cache.isEmpty())
            {
               continue;
            }

            DmlManager dmlManager = getDMLManager(type);
            AbstractCache externalCache = CacheHelper.getCache(type);
            if (!persistentToBeInserted.isEmpty())
            {
               persistentToBeInserted.clear();
            }
            if (!persistentToBeUpdated.isEmpty())
            {
               persistentToBeUpdated.clear();
            }
            for (PersistenceController dpc : (Collection<PersistenceController>) cache.values())
            {
               Persistent persistent = dpc.getPersistent();
               if ((dpc.isCreated() || dpc.isModified())
                     && (persistent instanceof LazilyEvaluated))
               {
                  ((LazilyEvaluated) persistent).performLazyEvaluation();
               }
               if (dpc.isCreated())
               {
                  if (Collections.EMPTY_LIST == persistentToBeInserted)
                  {
                     persistentToBeInserted = CollectionUtils.newArrayList(25);
                  }
                  persistentToBeInserted.add(persistent);
                  update(externalCache, persistent);
               }
               else if (dpc.isModified())
               {
                  if (Collections.EMPTY_LIST == persistentToBeUpdated)
                  {
                     persistentToBeUpdated = CollectionUtils.newArrayList(25);
                  }
                  persistentToBeUpdated.add(persistent);
                  update(externalCache, persistent);
               }

               // // DataCluster update - step 1
               if (isUsingDataClusters() && DataValueBean.class.equals(type))
               {
                  if (null == dpcDelayedCloseSet)
                  {
                     dpcDelayedCloseSet = CollectionUtils.newSet();
                  }

                  DataClusterHelper.prepareDataValueUpdate(dpc, piToDv,
                        dpcDelayedCloseSet);
               }
            }

            if (supportsAsynchWrite)
            {
               if (persistentToBeUpdated.isEmpty())
               {
                  if ( !persistentToBeInserted.isEmpty() && (null != blobBuilder))
                  {
                     ProcessBlobWriter.writeInstances(blobBuilder,
                           dmlManager.getTypeDescriptor(), persistentToBeInserted);
                     continue;
                  }
               }
               else
               {
                  throw new InternalException(
                        "Failed to asynchronously complete AuditTrail as the type "
                              + type + " unexpectedly contained modified instances.");
               }
            }
            else if (transientPiSupport.arePisTransient())
            {
               if ( !persistentToBeUpdated.isEmpty())
               {
                  throw new IllegalStateException("Failed to flush transient process as the type '" + type + "' unexpectedly contained modified instances.");
               }

               transientPiSupport.collectPersistentKeysToBeInserted(persistentToBeInserted);
               
               if (transientPiSupport.isCurrentSessionTransient())
               {
                  transientPiSupport.writeToBlobOrDiscard(persistentToBeInserted, blobBuilder, dmlManager.getTypeDescriptor());
                  
                  /* do not write the current entry to the database, but proceed with the next one */
                  continue;
               }
            }

            List<Persistent> persistentAlreadyExists = null;
            if (DataValueBean.class.equals(type))
            {
               // this type supports conversion from failing inserts into updates
               persistentAlreadyExists = CollectionUtils
                     .newArrayList(persistentToBeInserted.size());
            }

            doBatchInsert(persistentToBeInserted, persistentAlreadyExists, dmlManager,
                  dmlManager.getTypeDescriptor());
            if (persistentAlreadyExists != null && !persistentAlreadyExists.isEmpty()
                  && Collections.EMPTY_LIST == persistentToBeUpdated)
            {
               persistentToBeUpdated = CollectionUtils.newArrayList(25);
            convertAlreadyExistingPersistentsIntoUpdateList(persistentAlreadyExists,
                  persistentToBeUpdated, dmlManager, dmlManager.getTypeDescriptor());
            }
            doBatchUpdate(persistentToBeUpdated, dmlManager, dmlManager
                  .getTypeDescriptor());

            // close persistence controllers
            for (int i = 0; i < persistentToBeInserted.size(); ++i)
            {
               Persistent persistent = (Persistent) persistentToBeInserted.get(i);
               if ((null == dpcDelayedCloseSet)
                     || !dpcDelayedCloseSet.contains(persistent.getPersistenceController()))
               {
                  persistent.getPersistenceController().close();
               }
            }

            for (int i = 0; i < persistentToBeUpdated.size(); ++i)
            {
               Persistent persistent = (Persistent) persistentToBeUpdated.get(i);
               if ((null == dpcDelayedCloseSet)
                     || !dpcDelayedCloseSet.contains(persistent.getPersistenceController()))
               {
                  persistent.getPersistenceController().close();
               }
            }
         }

         // DataCluster update - step 2
         if ( !piToDv.isEmpty())
         {
            DataClusterHelper.completeDataValueUpdate(piToDv, this);
         }

         if (null != dpcDelayedCloseSet)
         {
            for (Iterator i = dpcDelayedCloseSet.iterator(); i.hasNext();)
            {
               ((PersistenceController) i.next()).close();
            }
         }

         // delete dead objects
         if ( !piToDv.isEmpty())
         {
            piToDv.clear();
         }
         dpcDelayedCloseSet = null;
         for (Iterator cacheItr = deadObjCacheRegistry.keySet().iterator(); cacheItr.hasNext();)
         {
            final Class type = (Class) cacheItr.next();
            final Map cache = (Map) deadObjCacheRegistry.get(type);
            final AbstractCache externalCache = CacheHelper.getCache(type);

            for (Iterator i = cache.values().iterator(); i.hasNext();)
            {
               PersistenceController cntrl = (PersistenceController) i.next();
               Persistent persistent = cntrl.getPersistent();
               remove(externalCache, persistent);

               if (transientPiSupport.arePisTransient())
               {
                  transientPiSupport.collectPersistentKeyToBeDeleted(persistent);
               }
               
               if (trace.isDebugEnabled())
               {
                  trace.debug(this + ", deleting: " + persistent);
               }

               if (isUsingDataClusters() && ProcessInstanceBean.class.equals(type))
               {
                  long piOid;
                  try
                  {
                     Field pkField = getDMLManager(type).getTypeDescriptor().getPkField();
                     piOid = pkField.getLong(persistent);
                  }
                  catch(IllegalAccessException x)
                  {
                     throw new InternalException("Failed extracting PK value.", x);
                  }

                  long startTime;
                  long stopTime;
                  String statementString;

                  DataCluster[] clusterSetup = getClusterSetup();
                  for (int j = 0; j < clusterSetup.length; ++j)
                  {
                     StringBuffer stmt = new StringBuffer(100);

                     stmt.append("DELETE FROM ");
                     sqlUtils.appendTableRef(stmt, clusterSetup[j], false);
                     stmt.append(" WHERE ").append(clusterSetup[j].getProcessInstanceColumn());
                     stmt.append("=?");

                     PreparedStatement statement = null;
                     try
                     {

                        statementString = stmt.toString();
                        statement = getConnection().prepareStatement(statementString);
                        if (trace.isDebugEnabled())
                        {
                           trace.debug("SQL: "+statementString);
                        }
                        statement.setLong(1, piOid);
                        startTime = System.currentTimeMillis();
                        statement.executeUpdate();
                        stopTime = System.currentTimeMillis();
                     }
                     finally
                     {
                        QueryUtils.closeStatement(statement);
                     }

                     monitorSqlExecution(statementString, startTime, stopTime);
                  }
               }

               // DataCluster delete - step 1
               if (isUsingDataClusters() && DataValueBean.class.equals(type))
               {
                  if (null == dpcDelayedCloseSet)
                  {
                     dpcDelayedCloseSet = CollectionUtils.newSet();
                  }

                  DataClusterHelper.prepareDataValueDelete(cntrl, piToDv,
                        dpcDelayedCloseSet);
               }

               if ((null == dpcDelayedCloseSet) || !dpcDelayedCloseSet.contains(cntrl))
               {
                  if ( !((cntrl instanceof DefaultPersistenceController) && ((DefaultPersistenceController) cntrl).isTransientlyDeleted()))
                  {
                     delete(type, persistent);
                  }
               }
            }
         }

         // DataCluster delete - step 2
         if ( !piToDv.isEmpty())
         {
            DataClusterHelper.completeDataValueDelete(piToDv, this);
         }

         if (null != dpcDelayedCloseSet)
         {
            for (Iterator i = dpcDelayedCloseSet.iterator(); i.hasNext();)
            {
               PersistenceController cntrl = (PersistenceController) i.next();
               Persistent persistent = cntrl.getPersistent();
               Class type = persistent.getClass();
               delete(type, persistent);
            }
         }

         if (transientPiSupport.arePisTransient())
         {
            transientPiSupport.cleanUpInMemStorage();
         }
         
         if (null != blobBuilder)
         {
            try
            {
               blobBuilder.persistAndClose();

               if (trace.isDebugEnabled())
               {
                  trace.debug("Persisted processes to BLOB.");
               }

               if (transientPiSupport.isCurrentSessionTransient())
               {
                  if ( !transientPiSupport.areAllPisCompleted())
                  {
                     transientPiSupport.writeToInMemStorage(blobBuilder);
                  }
                  else if (transientPiSupport.isDeferredPersist())
                  {
                     writeIntoAuditTrail(blobBuilder);
                  }
               }
               else if (blobBuilder instanceof ByteArrayBlobBuilder)
               {
                  writeIntoAuditTrail(blobBuilder);
               }
            }
            catch (PublicException e)
            {
               throw new InternalException(
                     "Failed to persist processes to BLOB: " + e.getMessage(), e);
            }
         }

         // TODO (ellipsis) verify no harm is being done by skipping explicit clearing
         fastCloseAndClearPersistenceControllers();

         // execute bulk deletes;

         for (Iterator i = bulkDeleteStatements.iterator(); i.hasNext();)
         {
            BulkDeleteStatement bulkDeleteStatement = (BulkDeleteStatement) i.next();
            executeDelete(bulkDeleteStatement.getStatementString(), bulkDeleteStatement.getBindValueList(), bulkDeleteStatement.getType());
         }
         
         BpmRuntimeEnvironment rtEnv = PropertyLayerProviderInterceptor.getCurrent();
         if (rtEnv != null && rtEnv.isDeploymentBeanCreated()) 
         {
            ModelManagerFactory.getCurrent().resetLastDeployment();
         }

         
      }
      catch (SQLException x)
      {
         ExceptionUtils.logAllBatchExceptions(x);
         throw new InternalException("Error during flush.", x);
      }
   }

   private void writeIntoAuditTrail(final BlobBuilder blobBuilder)
   {
      BlobReader blobReader = new ByteArrayBlobReader(((ByteArrayBlobBuilder) blobBuilder).getBlob());

      blobReader.init(params);
      blobReader.nextBlob();

      ProcessBlobAuditTrailPersistor persistor = new ProcessBlobAuditTrailPersistor();
      persistor.persistBlob(blobReader);

      // TODO configure
      persistor.writeIntoAuditTrail(this, 1);

      blobReader.close();
   }
   
   private void remove(AbstractCache externalCache, Persistent persistent)
   {
      if (externalCache != null)
      {
         externalCache.remove(persistent);
      }
   }

   private void update(AbstractCache externalCache, Persistent persistent)
   {
      if (externalCache != null)
      {
         externalCache.set(persistent, true);
      }
   }
   
   /**
    * Converts persistent objects from "to be inserted" (contained in list persistentAlreadyExists)
    * into "to be updated". For that the already existing objects will be fetched (as these
    * have different OIDs, they will be hold as different objects in cache) and the necessary
    * data is copied to them.
    *
    * @param persistentAlreadyExists persistent objects which where planned to be inserted but already exist (Threw UniqueConstraintVialoationException on insert).
    * @param persistentToBeUpdated list of persistent objects to be updated.
    * @param dmlManager
    * @param typeDescriptor type descriptor for the persistent objects.
    * @throws SQLException
    */
   private void convertAlreadyExistingPersistentsIntoUpdateList(List<Persistent> persistentAlreadyExists,
         List<Persistent> persistentToBeUpdated, DmlManager dmlManager,
         TypeDescriptor typeDescriptor) throws SQLException
   {
      if (persistentAlreadyExists != null && !persistentAlreadyExists.isEmpty())
      {
         if (typeDescriptor.getType().equals(DataValueBean.class))
         {
            ModelManager modelManager = ModelManagerFactory.getCurrent();
            Map<Pair<Long, Long>, DataValueBean> lookupMap = CollectionUtils.newHashMap();
            OrTerm orTerm = new OrTerm();
            for (Persistent persistent : persistentAlreadyExists)
            {
               DataValueBean dataValue = (DataValueBean) persistent;
               final long piOid = dataValue.getProcessInstance().getOID();
               long dataRtOid = modelManager.getRuntimeOid(dataValue.getData());

               // put into map for later lookup of dataValue
               lookupMap.put(new Pair(piOid, dataRtOid), dataValue);

               // add to predicate
               orTerm.add(Predicates.andTerm( //
                     Predicates.isEqual(DataValueBean.FR__PROCESS_INSTANCE, piOid), //
                     Predicates.isEqual(DataValueBean.FR__DATA, dataRtOid)));
            }

            QueryDescriptor queryDescriptor = QueryDescriptor.from(DataValueBean.class)
                  .where(orTerm);

            ResultIterator dvIter = getIterator(DataValueBean.class, queryDescriptor
                  .getQueryExtension());
            while (dvIter.hasNext())
            {
               DataValueBean dv = (DataValueBean) dvIter.next();

               long piOid = dv.getProcessInstance().getOID();
               long dataRtOid = modelManager.getRuntimeOid(dv.getData());

               DataValueBean oldDataValue = lookupMap.get(new Pair(piOid, dataRtOid));
               if (oldDataValue != null)
               {
                  dv.setValue(oldDataValue.getValue(), false);
                  if (dv.getPersistenceController().isModified())
                  {
                     persistentToBeUpdated.add(dv);
                  }
               }
               else
               {
                  Assert.lineNeverReached(MessageFormat.format(
                        "Data value object for pi with oid {0} and data runtime oid {1} does not exists.",
                        new Object[] { piOid, dataRtOid }));
               }
            }
         }
         else
         {
            // TODO: currently only data values are handled
            Assert.lineNeverReached("Cannot handle persist type: "
                  + typeDescriptor.getType());
         }
      }
   }

   /**
    * Closes the session of the driver to the database.
    * <p/>
    * Does not close the connectionManager.
    */
   public void disconnect() throws SQLException
   {
      fastCloseAndClearPersistenceControllers();
      returnJDBCConnection();
   }

   /**
    * Creates a persistent vector.
    */
   public PersistentVector createPersistentVector()
   {
      return new DefaultPersistentVector();
   }

   /**
    * Checks, wether an object matching the PK values in the link buffer of
    * <tt>persistent</tt> is already buffered. If so, it is returned.
    * Otherwise, this object is retrieved from the database.
    */
   public Persistent fetchLink(Persistent persistent, String linkName)
   {
      DefaultPersistenceController cntrl = (DefaultPersistenceController)
            persistent.getPersistenceController();
      TypeDescriptor typeDescriptor = tdRegistry.getDescriptor(persistent.getClass());

      final int linkIdx = typeDescriptor.getLinkIdx(linkName);
      final LinkDescriptor link = typeDescriptor.getLink(linkIdx);

      if (null == link)
      {
         throw new InternalException("Unknown link name '" + linkName + "'.");
      }

      Persistent target;
      if (cntrl.isLinkFetched(linkIdx))
      {
         try
         {
            target = (Persistent) link.getField().get(persistent);
         }
         catch (Exception e)
         {
            throw new InternalException("Failed fetching linked target instance", e);
         }
      }
      else
      {
         cntrl.markLinkFetched(linkIdx);

         Number linkOID = (Number) cntrl.getLinkBuffer()[linkIdx];
         if (linkOID == null)
         {
            return null;
         }

         // TODO don't fetch when OID is 0
         target = findByOID(link.getTargetType(), linkOID.longValue());

         if (trace.isDebugEnabled())
         {
            trace.debug(this + ", going to set link '" + linkName + "' using link type "
                  + link.getTargetType() + ".");
         }

         try
         {
            link.getField().set(persistent, target);
         }
         catch (Exception x)
         {
            throw new InternalException("Failed linking target instance.", x);
         }

         if (trace.isDebugEnabled())
         {
            trace.debug(this + ", link " + linkName + " set.");
         }
      }

      return target;
   }

   public boolean hasConnection()
   {
      return null != jdbcConnection;
   }

   /**
    * Gets the current JDBC connectionManager or retrieves a new
    * connectionManager form the pool if the current JDBC connectionManager is
    * null.
    * <p/>
    * This method is also used to recreate a connectionManager after abort.
    * <p/>
    * Access to the variable <tt>jdbcConnection</tt> must be .
    */
   public Connection getConnection() throws SQLException
   {
      if (null == jdbcConnection)
      {
         this.threadBinding = Thread.currentThread().hashCode();

         Connection rawConnection = dataSource.getConnection();

         this.jdbcConnection = isInterceptingJdbcCalls()
               ? JdbcProxy.newInstance(rawConnection)
               : rawConnection;

         if (params.getBoolean(name
               + SessionProperties.DS_ASSERT_READ_COMMITTED_SUFFIX, true)
               && Connection.TRANSACTION_READ_COMMITTED != jdbcConnection
                     .getTransactionIsolation())
         {
            throw new PublicException("Invalid TX isolation level "
                  + jdbcConnection.getTransactionIsolation()
                  + ", requiring TRANSACTION_READ_COMMITTED ("
                  + Connection.TRANSACTION_READ_COMMITTED + ")");
         }

         if (jdbcConnection.getAutoCommit()
               && params.getBoolean(
                     name + SessionProperties.DS_FIX_AUTO_COMMIT_SUFFIX, false))
         {
            jdbcConnection.setAutoCommit(false);
         }
         if (trace.isDebugEnabled())
         {
            trace.debug(this + ", started using " + LogUtils.instanceInfo(jdbcConnection));
            trace.debug("Isolation is: " + jdbcConnection.getTransactionIsolation());
            trace.debug("Autocommit = " + jdbcConnection.getAutoCommit());
         }

         // inform all interested parties about connection retrieval
         for (int i = 0; i < connectionHooks.size(); ++i)
         {
            ((ConnectionHook) connectionHooks.get(i)).onGetConnection(jdbcConnection);
         }
      }
      else
      {
         if (Thread.currentThread().hashCode() != threadBinding)
         {
            throw new InternalException("Another thread with hashcode "
                  + Thread.currentThread().hashCode()
                  + " has tried to obtain session bound to thread hashcode "
                  + threadBinding);
         }

         if (trace.isDebugEnabled())
         {
            trace.debug(this + ",       reusing " + LogUtils.instanceInfo(jdbcConnection));
         }
      }

      return jdbcConnection;
   }

   private int executeDelete(Class type, PredicateTerm predicate, Joins joins,
         boolean useBulkDeleteStatements)
   {
      return executeDelete(type, predicate, joins, useBulkDeleteStatements, false);
   }

   private int executeDelete(Class type, PredicateTerm predicate, Joins joins,
         boolean useBulkDeleteStatements, boolean useLockTable)
   {
      List bindValueList = isUsingPreparedStatements(type) ? new ArrayList() : null;
      int processedRows = 0;

      final DmlManager dmlManager = getDMLManager(type);

      DeleteDescriptor delete = useLockTable
            ? DeleteDescriptor.fromLockTable(type)
            : DeleteDescriptor.from(type);
      if (null != joins)
      {
         delete.getQueryExtension().addJoins(joins);
      }
      if (null != predicate)
      {
         delete.where(predicate);
      }

      String statementString = dmlManager.prepareDeleteStatement(delete, bindValueList,
            useLockTable);

      if (useBulkDeleteStatements)
      {
         bulkDeleteStatements.add(new BulkDeleteStatement(type, statementString, bindValueList));
      }
      else
      {
         processedRows = executeDelete(statementString, bindValueList, type);
      }

      return processedRows;
   }

   public int executeDelete(DeleteDescriptor delete)
   {
      int processedRows = 0;

      List bindValueList = isUsingPreparedStatements(delete.getType()) ? new ArrayList() : null;

      final DmlManager dmlManager = getDMLManager(delete.getType());

      String statementString = dmlManager.prepareDeleteStatement(delete, bindValueList,
            delete.isAffectingLockTable());

      processedRows = executeDelete(statementString, bindValueList, delete.getType());

      return processedRows;
   }

   private int executeDelete(String deleteStatement, List bindValueList, Class type)
   {
      int processedRows = 0;
      Statement statement = null;

      try
      {
         long startTime;
         long stopTime;

         if (isUsingPreparedStatements(type))
         {
            Assert.isNotNull(bindValueList,
                  "Execution of prepared delete statement needs valid bind value list.");

            statement = createAndBindPreparedStatement(deleteStatement, bindValueList);

            startTime = System.currentTimeMillis();
            processedRows = ((PreparedStatement) statement).executeUpdate();
            stopTime = System.currentTimeMillis();
         }
         else
         {
            statement = getConnection().createStatement();

            startTime = System.currentTimeMillis();
            processedRows = statement.executeUpdate(deleteStatement);
            stopTime = System.currentTimeMillis();
         }

         monitorSqlExecution(deleteStatement, startTime, stopTime);
      }
      catch (SQLException x)
      {
         trace.warn(x);
         throw new PublicException(x);
      }
      finally
      {
         QueryUtils.closeStatement(statement);
      }

      return processedRows;
   }

   private ResultSet executeLockQuery(Class type, long oid) throws SQLException
   {
      List bindValueList = isUsingPreparedStatements(type) ? new ArrayList() : null;
      DmlManager dmlManager = getDMLManager(type);

      String statementString = dmlManager.getLockRowStatementString(oid,
            isUsingLockTables(), bindValueList);

      boolean useQueryTimeout = getDBDescriptor().useQueryTimeout();
      ResultSet resultSet = null;

      long startTime;
      long stopTime;
      if (isUsingPreparedStatements(type))
      {
         PreparedStatement statement = null;

         try
         {
            try
            {
               statement = createAndBindPreparedStatement(statementString, bindValueList);
            }
            catch (SQLException x)
            {
               throw new InternalException(
                     "Failed creating statement for object locking.", x);
            }

            if (useQueryTimeout)
            {
               statement.setQueryTimeout(1);
            }

            startTime = System.currentTimeMillis();
            resultSet = ManagedResultSet.createManager(statement, statement
                  .executeQuery());
            stopTime = System.currentTimeMillis();
         }
         catch (RuntimeException x)
         {
            QueryUtils.closeStatement(statement);
            throw x;
         }
         catch (SQLException x)
         {
            QueryUtils.closeStatement(statement);
            throw x;
         }
      }
      else
      {
         Statement statement = null;

         try
         {
            try
            {
               statement = getConnection().createStatement();
            }
            catch (SQLException x)
            {
               throw new InternalException(
                     "Failed creating statement for object locking.", x);
            }

            if (useQueryTimeout)
            {
               statement.setQueryTimeout(1);
            }

            startTime = System.currentTimeMillis();
            resultSet = ManagedResultSet.createManager(statement, statement
                  .executeQuery(statementString));
            stopTime = System.currentTimeMillis();
         }
         catch (RuntimeException x)
         {
            QueryUtils.closeStatement(statement);
            throw x;
         }
         catch (SQLException x)
         {
            QueryUtils.closeStatement(statement);
            throw x;
         }
      }

      monitorSqlExecution(statementString, startTime, stopTime);

      return resultSet;
   }

   private int executeLockUpdate(Class type, long oid) throws SQLException
   {
      List bindValueList = isUsingPreparedStatements(type) ? new ArrayList() : null;
      DmlManager dmlManager = getDMLManager(type);

      String statementString = dmlManager.getLockRowStatementString(oid,
            isUsingLockTables(), bindValueList);

      boolean useQueryTimeout = getDBDescriptor().useQueryTimeout();
      int processedRows = 0;
      Statement statement = null;

      try
      {
         long startTime;
         long stopTime;
         if (isUsingPreparedStatements(type))
         {
            try
            {
               statement = createAndBindPreparedStatement(statementString, bindValueList);
            }
            catch (SQLException x)
            {
               throw new InternalException(
                     "Failed creating statement for object locking.", x);
            }

            if (useQueryTimeout)
            {
               statement.setQueryTimeout(1);
            }

            startTime = System.currentTimeMillis();
            processedRows = ((PreparedStatement) statement).executeUpdate();
            stopTime = System.currentTimeMillis();
         }
         else
         {
            try
            {
               statement = getConnection().createStatement();
            }
            catch (SQLException x)
            {
               throw new InternalException(
                     "Failed creating statement for object locking.", x);
            }

            if (useQueryTimeout)
            {
               statement.setQueryTimeout(1);
            }

            startTime = System.currentTimeMillis();
            processedRows = statement.executeUpdate(statementString);
            stopTime = System.currentTimeMillis();
         }

         monitorSqlExecution(statementString, startTime, stopTime);

         return processedRows;
      }
      finally
      {
         QueryUtils.closeStatement(statement);
      }
   }


   public ResultSet executeQuery(Class type, QueryExtension queryExtension)
   {
      return executeQuery(type, queryExtension, NO_TIMEOUT);
   }

   /**
    * Fires a query statement directly in the transactional context of this
    * session.
    *
    * @param type The primary type the query statement works on
    * @param queryExtension A query extension
    * @return A result set which must be closed by the caller
    */
   public ResultSet executeQuery(Class type, QueryExtension queryExtension, int timeout)
   {
      return executeQuery(QueryDescriptor.from(type, queryExtension), timeout);
   }

   public ResultSet executeQuery(QueryDescriptor queryDescr)
   {
      return executeQuery(queryDescr, NO_TIMEOUT);
   }

   public ResultSet executeQuery(QueryDescriptor queryDescr, int timeout)
   {
      List bindValueList = isUsingPreparedStatements(queryDescr.getType()) ? new ArrayList() : null;
      ResultSet  resultSet = null;

      try
      {
         DmlManager dmlManager = getDMLManager(queryDescr.getType());
         String sqlString = dmlManager.prepareSelectStatement(queryDescr, true,
               bindValueList, isUsingMixedPreparedStatements());

         final long startTime;
         final long stopTime;

         if (isUsingPreparedStatements(queryDescr.getType()))
         {
            PreparedStatement stmt = createAndBindPreparedStatement(sqlString,
                  bindValueList);

            try
            {
               if (timeout > NO_TIMEOUT)
               {
                  stmt.setQueryTimeout(timeout);
               }

               startTime = System.currentTimeMillis();
               resultSet = ManagedResultSet.createManager(stmt, stmt.executeQuery());
               stopTime = System.currentTimeMillis();
            }
            catch (SQLException e)
            {
               RuntimeLog.SQL.warn(MessageFormat.format("Failed query: {0}",
                     new Object[] { sqlString }));
               trace.warn("Failed executing query.", e);
               QueryUtils.closeStatement(stmt);
               throw new PublicException(e);
            }
         }
         else
         {
            Statement stmt = getConnection().createStatement();

            try
            {
               if (timeout > NO_TIMEOUT)
               {
                  stmt.setQueryTimeout(timeout);
               }

               startTime = System.currentTimeMillis();

               resultSet = ManagedResultSet.createManager(stmt,
                     stmt.executeQuery(sqlString));
               stopTime = System.currentTimeMillis();
            }
            catch (SQLException e)
            {
               RuntimeLog.SQL.warn(MessageFormat.format("Failed query: {0}",
                     new Object[] { sqlString }));
               trace.warn("Failed executing query.", e);
               QueryUtils.closeStatement(stmt);
               throw new PublicException(e);
            }
         }

         monitorSqlExecution(sqlString, startTime, stopTime);
      }
      catch (SQLException e)
      {
         trace.warn("Failed executing query.", e);
         QueryUtils.closeResultSet(resultSet);
         throw new PublicException(e);
      }

      return resultSet;
   }

   /**
    * Wrapper method for {@link #executeQuery(Class, QueryExtension, int)} which
    * extents the query in order to reduce later lookup of dependent persisten objects.
    *
    * @param type The primary type the query statement works on
    * @param queryExtension A query extension
    * @return A result set which must be closed by the caller
    */
   public ResultSet executePersistentQuery(QueryDescriptor query, int timeout)
   {
      ResultSet resultSet = null;

      List<Column> additionalSelection = CollectionUtils.newArrayList();
      Map<ITableDescriptor, Join> descrToJoinAssociator = CollectionUtils.newHashMap();
      Join piDvHelperJoin = null;

      Serializable withDescrRaw = query.getQueryExtension().getHints()
            .get(IDescriptorProvider.PRP_PROPVIDE_DESCRIPTORS);
      boolean withDescr = false;
      if (withDescrRaw instanceof Boolean)
      {
         withDescr = ((Boolean) withDescrRaw).booleanValue();
      }

      boolean reuseFilterJoins = params.getBoolean(
            KernelTweakingProperties.DESCRIPTOR_PREFETCH_REUSE_FILTER_JOINS, false);

      if (withDescr && reuseFilterJoins)
      {
         BpmRuntimeEnvironment rtEnv = PropertyLayerProviderInterceptor.getCurrent();
         Set<Long> prefetchedDataRtOids = CollectionUtils.newHashSet();
         if (rtEnv != null)
         {
            rtEnv.setProperty(KernelTweakingProperties.DESCRIPTOR_PREFETCHED_RTOID_SET,
                  prefetchedDataRtOids);
         }

         processDataValueFilterJoins(query, prefetchedDataRtOids, additionalSelection,
               descrToJoinAssociator);

         if ( !descrToJoinAssociator.isEmpty())
         {
            // get the first join as all DVs / data cluster table will join to the same table
            Entry<ITableDescriptor, Join> entry = descrToJoinAssociator.entrySet().iterator().next();
            Join join = entry.getValue();
            Join dependency = join.getDependency();

            piDvHelperJoin = new Join(ProcessInstanceBean.class, "DV_F_PIS");
            piDvHelperJoin.setRequired(false);
            if(dependency != null)
            {
               piDvHelperJoin.on(dependency.fieldRef(ProcessInstanceScopeBean.FIELD__SCOPE_PROCESS_INSTANCE), ProcessInstanceBean.FIELD__OID);
               piDvHelperJoin.setDependency(dependency);
            }
            else
            {
               piDvHelperJoin.on(query.fieldRef(ProcessInstanceBean.FIELD__SCOPE_PROCESS_INSTANCE), ProcessInstanceBean.FIELD__OID);
            }
         }
      }

      if (isUsingEagerLinkFetching() || !descrToJoinAssociator.isEmpty())
      {
         int linkCounter = 0;

         Joins linkJoins = new Joins();
         List linkToJoinAssociator = new ArrayList(query.getLinks().size());

         if (isUsingEagerLinkFetching())
         {
            for (Iterator i = query.getLinks().iterator(); i.hasNext();)
            {
               LinkDescriptor linkDescriptor = (LinkDescriptor) i.next();

               if (linkDescriptor.isEagerFetchable())
               {
                  Class targetType = linkDescriptor.getTargetType();

                  String eagerLinkFetchAlias = EAGER_LINK_FETCH_ALIAS_PREFIX
                        + linkCounter;
                  Join elfJoin = new Join(targetType, eagerLinkFetchAlias).on(
                        query.fieldRef(linkDescriptor.getField().getName()),
                        linkDescriptor.getFkField().getName());
                  elfJoin.setRequired(linkDescriptor.isMandatory());

                  // Checks if there already exists an equal join in the given query extension.
                  for (Iterator j = query.getQueryExtension().getJoins().iterator(); j
                        .hasNext();)
                  {
                     final Join currentJoin = (Join) j.next();

                     if (elfJoin.equals(currentJoin))
                     {
                        elfJoin = currentJoin;
                        break;
                     }
                  }

                  if ( !linkJoins.contains(elfJoin))
                  {
                     // Store the newly created elfJoin for later modification of queryExtension

                     linkJoins.add(elfJoin);
                     ++linkCounter;
                  }

                  // extend the selection list
                  linkToJoinAssociator.add(new Pair(linkDescriptor, elfJoin));
                  additionalSelection.addAll(SqlUtils
                        .getDefaultSelectFieldList(new TableAliasDecorator(elfJoin
                              .getTableAlias(), tdRegistry.getDescriptor(targetType))));
               }
            }
         }

         if ( !additionalSelection.isEmpty())
         {
            List<Column> newSelection = CollectionUtils.<Column> newArrayList(SqlUtils
                  .getDefaultSelectFieldList(query));
            if (piDvHelperJoin != null)
            {
               newSelection.addAll(SqlUtils
                     .getDefaultSelectFieldList(new TableAliasDecorator(piDvHelperJoin
                           .getTableAlias(), tdRegistry
                           .getDescriptor(ProcessInstanceBean.class))));
            }
            newSelection.addAll(additionalSelection);

            // replace original query extension by a local shallow copy
            query = QueryDescriptor.shallowCopy(query);

            query.getQueryExtension().setSelection(
                  (Column[]) newSelection.toArray(new Column[newSelection.size()]));

            if(piDvHelperJoin != null)
            {
               query.getQueryExtension().getJoins().add(piDvHelperJoin);
            }

            // if any then add joins for eager link fetching
            query.getQueryExtension().getJoins().add(linkJoins);
         }

         // execute extended query
         resultSet = executeQuery(query, timeout);

         if ( !additionalSelection.isEmpty())
         {
            // Since there exist some additional persistent objects to be fetched eagerly
            // the original result set has to be wrapped by an MultiPersistenResultSet.

            Column[] selectionList = query.getQueryExtension().getSelection();
            ResultSet baseResultSet = resultSet;
            resultSet = MultiplePersistentResultSet.createPersistentProjector(
                  query.getType(), query, baseResultSet, selectionList);

            // first: handle additional scope PI
            if (piDvHelperJoin != null)
            {
               AliasProjectionResultSet projector = AliasProjectionResultSet
                     .createAliasProjector(ProcessInstanceBean.class, piDvHelperJoin,
                           baseResultSet, selectionList);
               ((MultiplePersistentResultSet) resultSet).add(projector);
            }

            // second: handle existing data values
            for (ITableDescriptor tableDescr : descrToJoinAssociator.keySet())
            {
               AliasProjectionResultSet projector = AliasProjectionResultSet
                     .createAliasProjector(DataValueBean.class, tableDescr,
                           baseResultSet, selectionList);
               ((MultiplePersistentResultSet) resultSet).add(projector);
            }

            // last but not least: handle existing ELF values
            for (Iterator i = linkToJoinAssociator.iterator(); i.hasNext();)
            {
               Pair linkToJoinAssociation = (Pair) i.next();
               LinkDescriptor linkDescriptor = (LinkDescriptor) linkToJoinAssociation
                     .getFirst();
               Join join = (Join) linkToJoinAssociation.getSecond();

               AliasProjectionResultSet projector = AliasProjectionResultSet
                     .createAliasProjector(linkDescriptor.getTargetType(), join,
                           baseResultSet, selectionList);
               ((MultiplePersistentResultSet) resultSet).add(projector);
            }
         }
      }
      else
      {
         resultSet = executeQuery(query, timeout);
      }

      return resultSet;
   }

   /**
    * Fires an UPDATE statement directly in the transactional context of this session.
    *
    * @param updateDescriptor A description of the UPDATE to be performed
    *
    * @return The number of rows updates.
    */
   /*public int executeUpdate(UpdateDescriptor updateDescriptor)
   {
      return executeUpdate(updateDescriptor, NO_TIMEOUT);
   }*/

   /**
    * Fires an UPDATE statement directly in the transactional context of this session.
    *
    * @param updateDescriptor A description of the UPDATE to be performed
    *
    * @return The number of rows updates.
    */
   /*public int executeUpdate(UpdateDescriptor updateDescriptor, int timeout)
   {
      int nRows = 0;

      List bindValueList = isUsingPreparedStatements(updateDescriptor.getType()) ? new ArrayList() : null;

      try
      {
         // TODO how about lock table maintenance

         // TODO build INSERT statement

         DmlManager dmlManager = getDMLManager(updateDescriptor.getType());
         String sqlString = dmlManager.prepareUpdateStatement(updateDescriptor,
               bindValueList, false);

         final long startTime;
         final long stopTime;

         Statement stmt = null;
         try
         {
            if (isUsingPreparedStatements(updateDescriptor.getType()))
            {
               stmt = createAndBindPreparedStatement(sqlString, bindValueList);

               try
               {
                  if (timeout > NO_TIMEOUT)
                  {
                     stmt.setQueryTimeout(timeout);
                  }

                  startTime = System.currentTimeMillis();
                  nRows = 0;//((PreparedStatement) stmt).executeUpdate();
                  stopTime = System.currentTimeMillis();
               }
               catch (SQLException e)
               {
                  RuntimeLog.SQL.debug(MessageFormat.format("Failed SQL insert: {0}",
                        new Object[] {sqlString}));
                  trace.warn("Failed executing SQL insert.", e);
                  throw new PublicException(e);
               }
            }
            else
            {
               stmt = getConnection().createStatement();

               try
               {
                  if (timeout > NO_TIMEOUT)
                  {
                     stmt.setQueryTimeout(timeout);
                  }

                  startTime = System.currentTimeMillis();
                  nRows = 0;//stmt.executeUpdate(sqlString);
                  stopTime = System.currentTimeMillis();
               }
               catch (SQLException e)
               {
                  RuntimeLog.SQL.debug(MessageFormat.format("Failed SQL update: {0}",
                        new Object[] {sqlString}));
                  trace.warn("Failed executing SQL update.", e);
                  throw new PublicException(e);
               }
            }
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }

         monitorSqlExecution(sqlString, startTime, stopTime);
      }
      catch (SQLException e)
      {
         trace.warn("Failed executing query.", e);
         throw new PublicException(e);
      }

      return nRows;
   }*/

   /**
    * Fires an INSERT statement directly in the transactional context of this session.
    *
    * @param insertDescriptor A description of the INSERT to be performed
    *
    * @return The number of rows inserted.
    */
   public int executeInsert(InsertDescriptor insertDescriptor)
   {
      return executeInsert(insertDescriptor, NO_TIMEOUT);
   }

   /**
    * Fires an INSERT statement directly in the transactional context of this session.
    *
    * @param insertDescriptor A description of the INSERT to be performed
    *
    * @return The number of rows inserted.
    */
   public int executeInsert(InsertDescriptor insertDescriptor, int timeout)
   {
      int nRows = 0;

      List bindValueList = isUsingPreparedStatements(insertDescriptor.getType()) ? new ArrayList() : null;

      try
      {
         // TODO how about lock table maintenance

         // TODO build INSERT statement

         DmlManager dmlManager = getDMLManager(insertDescriptor.getType());
         String sqlString = dmlManager.prepareInsertStatement(insertDescriptor,
               bindValueList, false);

         final long startTime;
         final long stopTime;

         Statement stmt = null;
         try
         {
            if (isUsingPreparedStatements(insertDescriptor.getType()))
            {
               stmt = createAndBindPreparedStatement(sqlString, bindValueList);

               try
               {
                  if (timeout > NO_TIMEOUT)
                  {
                     stmt.setQueryTimeout(timeout);
                  }

                  startTime = System.currentTimeMillis();
                  nRows = ((PreparedStatement) stmt).executeUpdate();
                  stopTime = System.currentTimeMillis();
               }
               catch (SQLException e)
               {
                  RuntimeLog.SQL.warn(MessageFormat.format("Failed SQL insert: {0}",
                        new Object[] {sqlString}));
                  trace.warn("Failed executing SQL insert.", e);
                  throw new PublicException(e);
               }
            }
            else
            {
               stmt = getConnection().createStatement();

               try
               {
                  if (timeout > NO_TIMEOUT)
                  {
                     stmt.setQueryTimeout(timeout);
                  }

                  startTime = System.currentTimeMillis();
                  nRows = stmt.executeUpdate(sqlString);
                  stopTime = System.currentTimeMillis();
               }
               catch (SQLException e)
               {
                  RuntimeLog.SQL.warn(MessageFormat.format("Failed SQL insert: {0}",
                        new Object[] {sqlString}));
                  trace.warn("Failed executing SQL insert.", e);
                  throw new PublicException(e);
               }
            }
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }

         monitorSqlExecution(sqlString, startTime, stopTime);
      }
      catch (SQLException e)
      {
         trace.warn("Failed executing query.", e);
         throw new PublicException(e);
      }

      return nRows;
   }

   /**
    * This method creates a <code>PreparedStatement</code> from a given <code>statementString</code>
    * and binds the values given in <code>bindValueList</code> to this PreparedStatement.
    *
    * @param statementString Query in SQL-syntax
    * @param bindValueList List of type List<Pair<Class, Object>> with typed values
    *
    * @return The PreparedStatement with bound values
    *
    * @throws SQLException
    */
   private PreparedStatement createAndBindPreparedStatement(String statementString,
         List bindValueList) throws SQLException
   {
      PreparedStatement preparedStmt = getConnection().prepareStatement(statementString);

      int position = 0;
      for (Iterator i = bindValueList.iterator(); i.hasNext();)
      {
         Pair pair = (Pair) i.next();

         ++position;

         DmlManager.setSQLValue(preparedStmt, position, (Class) pair.getFirst(),
               pair.getSecond(), dbDescriptor);
      }

      return preparedStmt;
   }

   public void delete(Class type, Persistent persistent)
   {
      DmlManager dmlManager = getDMLManager(type);
      Field[] pkFields = dmlManager.getTypeDescriptor().getPkFields();

      try
      {
         AndTerm pkPredicate = new AndTerm();
         for (int i = 0; i < pkFields.length; i++ )
         {
            Assert.condition(Long.class.isAssignableFrom(pkFields[i].getType())
                  || Long.TYPE.isAssignableFrom(pkFields[i].getType()),
                  "PK field must be numeric.");

            pkPredicate.add(Predicates.isEqual(dmlManager.getTypeDescriptor().fieldRef(
                  pkFields[i].getName()),
                  ((Number) pkFields[i].get(persistent)).longValue()));
         }
         delete(type, pkPredicate, false);
      }
      catch (IllegalAccessException e)
      {
         throw new InternalException(e);
      }
   }

   public void delete(Class type, PredicateTerm predicate, boolean delay)
   {
      delete(type, predicate, NO_JOINS, delay);
   }

   public void delete(Class type, PredicateTerm predicate, Join join, boolean delay)
   {
      Joins joins = null;

      if (null != join)
      {
         joins = new Joins();
         joins.add(join);
      }

      delete(type, predicate, joins, delay);
   }

   public void delete(Class type, PredicateTerm predicate, Joins joins, boolean delay)
   {
      if (isArchiveAuditTrail)
      {
         // readonly
         throw new PublicException("Archive AuditTrail does not allow changes.");
      }

      DmlManager dmlManager = getDMLManager(type);

      final TypeDescriptor typeDescriptor = tdRegistry.getDescriptor(type);

      if (isUsingLockTables() && dmlManager.getTypeDescriptor().isDistinctLockTableName())
      {
         final Field[] pkFields = typeDescriptor.getPkFields();
         AndTerm deletePredicate = null;

         if (null != predicate)
         {
            deletePredicate = new AndTerm();

            for (int i = 0; i < pkFields.length; i++ )
            {
               QueryDescriptor subQuery = QueryDescriptor
                     .from(type, "o")
                     .select(pkFields[i].getName())
                     .where(predicate);

               if (null != joins)
               {
                  subQuery.getQueryExtension().addJoins(joins);
               }

               ITableDescriptor tdLockTable = typeDescriptor.getLockTableDescriptor();
               deletePredicate.add(Predicates.inList(
                     tdLockTable.fieldRef(pkFields[i].getName()), subQuery));
            }
         }

         executeDelete(type, deletePredicate, null, delay, true);
      }


      // Deleting DataValueBeans needs no special handling because everything is done
      // by deletion of ProcessInstanceBeans (DataClusters)

      if (isUsingDataClusters() && ProcessInstanceBean.class.equals(type)
            && (null == predicate))
      {
         DataCluster[] clusterSetup = getClusterSetup();
         for (int j = 0; j < clusterSetup.length; ++j)
         {
            StringBuffer stmt = new StringBuffer(100);
            stmt.append("DELETE FROM ");
            sqlUtils.appendTableRef(stmt, clusterSetup[j], false);

            PreparedStatement statement = null;
            final String sqlString = stmt.toString();
            try
            {
               statement = getConnection().prepareStatement(sqlString);

               long startTime = System.currentTimeMillis();
               statement.executeUpdate();
               monitorSqlExecution(sqlString, startTime, System.currentTimeMillis());
            }
            catch(SQLException x)
            {
               RuntimeLog.SQL.warn(MessageFormat.format("Failed update: {0}",
                     new Object[] { sqlString }));
               trace.warn(x);
               throw new PublicException(x);
            }
            finally
            {
               QueryUtils.closeStatement(statement);
            }
         }
      }

      executeDelete(type, predicate, joins, delay);
   }

   /**
    * Deletes all instances previously created
    * for this class.
    */
   public void deleteAllInstances(Class type, boolean delay)
   {
      delete(type, null, delay);
   }

   public void lock(Class type, long oid) throws ConcurrencyException
   {
      DmlManager dmlManager = getDMLManager(type);

      try
      {
         if (dmlManager.isLockRowStatementSQLQuery())
         {
            ResultSet rs = null;
            try
            {
               rs = executeLockQuery(type, oid);

               if ( !rs.next())
               {
                  // TODO (sb): with certain databases (e.g. Oracle) this could also
                  // mean that the locking table is not consistent and does not contain
                  // data for this oid. But how to find out...
                  // see CRNT-22509
                  BpmRuntimeError error = BpmRuntimeError.BPMRT_LOCK_CONFLICT.raise(
                        new Long(oid), type);

                  trace.info(error.toString());
                  throw new ConcurrencyException(error);
               }
            }
            finally
            {
               QueryUtils.closeResultSet(rs);
            }
         }
         else
         {
            int processedRows = executeLockUpdate(type, oid);

            if (processedRows == 0)
            {
               try
               {
                  if (isUsingLockTables()
                        && dmlManager.getTypeDescriptor().isDistinctLockTableName()
                        && exists(type, QueryExtension.where(Predicates.isEqual(
                              dmlManager.getTypeDescriptor().fieldRef(IdentifiablePersistentBean.FIELD__OID), oid))))
                  {
                     String msg = "Locking table may not be consistent for object of type "
                           + type + " with oid = " + oid + ".";

                     trace.warn(msg);
                  }
               }
               catch (Throwable e)
               {
                  trace.warn("Cannot investigate consistency of locking table for "
                        + "object of type " + type + " with oid = " + oid + ".", e);
               }

               BpmRuntimeError error = BpmRuntimeError.BPMRT_LOCK_CONFLICT.raise(
                     new Long(oid), type);

               trace.info(error.toString());
               throw new ConcurrencyException(error);
            }
         }

         if (trace.isDebugEnabled())
         {
            trace.debug("Object of type " + type + " with oid " + oid + " locked.");
         }
      }
      catch (SQLException x)
      {
         if (DBMSKey.DERBY.equals(dbDescriptor.getDbmsKey())
               && (x.getSQLState().equals("40001")))
         {
            throw new PublicException("Derby session rolled back.", x);
         }
         else
         {
            BpmRuntimeError error = BpmRuntimeError.BPMRT_LOCK_CONFLICT.raise(
                  new Long(oid), type);
            throw new ConcurrencyException(error, x);
         }
      }
   }

   public boolean isSynchronized(Persistent persistent)
   {
      TypeDescriptor typeManager = tdRegistry.getDescriptor(persistent.getClass());
      if (typeManager.getLoader() == null)
      {
         return true;
      }
      Set typeCache = (null != synchronizationCache)
            ? (Set) synchronizationCache.get(typeManager.getType())
            : null;
      return (null != typeCache)
            && typeCache.contains(typeManager.getIdentityKey(persistent));
   }

   public Persistent findByOID(Class type, long oid)
   {
      if (type.equals(DepartmentBean.class))
      {
         oid = (oid + 1) - 1;
      }
      TypeDescriptor typeManager = tdRegistry.getDescriptor(type);
      DmlManager dmlManager = getDMLManager(type);

      if ( !((1 == typeManager.getPkFields().length)
            && (Long.class.isAssignableFrom(typeManager.getPkFields()[0].getType())
                  || Long.TYPE.isAssignableFrom(typeManager.getPkFields()[0].getType()))))
      {
         Assert.condition(false, "Invalid argument: type " + type
               + " has more than one PK field.");
      }
      
      Object identityKey = typeManager.getIdentityKey(Long.valueOf(oid));
      Persistent persistent = retrieveFromCache(type, identityKey);
      if (persistent != null)
      {
         Map deadCache = (Map) deadObjCacheRegistry.get(type);
         if ((null != deadCache) && deadCache.containsKey(Long.valueOf(oid)))
         {
            return null;
         }
         return persistent;
      }

      if (ProcessInstanceUtils.isTransientPiSupportEnabled() && isTransientPersistentCandidate(typeManager))
      {
         final Persistent transientPersistent = TransientProcessInstanceSupport.findAndReattach(oid, type, this);
         if (transientPersistent != null)
         {
            return transientPersistent;
         }
      }

      AbstractCache cache = CacheHelper.getCache(type);
      if (cache != null)
      {
         persistent = (Persistent) cache.get(oid);
         if (persistent != null)
         {
            PersistenceController controller = persistent.getPersistenceController();
            if (controller == null)
            {
               controller = dmlManager.createPersistenceController(this, persistent);
            }
            addToPersistenceControllers(identityKey, controller);
            return persistent;
         }
      }

      String sql = null;
      ResultSet resultSet = null;
      Persistent result = null;
      try
      {
         Field[] pkFields = typeManager.getPkFields();

         if (1 != pkFields.length)
         {
            Assert.condition(false, "FindByOid does not support type "
                  + typeManager.getType() + " as it has more than one PK field.");
         }

         QueryDescriptor query = QueryDescriptor
               .from(type);

         query.where(Predicates.isEqual(query.fieldRef(pkFields[0].getName()), oid));

         resultSet = executePersistentQuery(query
               .where(Predicates.isEqual(query.fieldRef(pkFields[0].getName()), oid)), Session.NO_TIMEOUT);

         if (resultSet.next())
         {
            result = createObjectFromRow(type, resultSet, null, NO_FETCH_PREDICATE);
            if (typeManager.getLoader() != null)
            {
               typeManager.getLoader().load(result);
            }
         }
      }
      catch (Exception x)
      {
         throw new InternalException("Failed finding object with pk " + oid + "."
               + " Statement: " + sql, x);
      }
      finally
      {
         QueryUtils.closeResultSet(resultSet);
      }
      return result;
   }

   private boolean isTransientPersistentCandidate(final TypeDescriptor typeDesc)
   {
      return typeDesc.isTryDeferredInsert();
   }
   
   /**
    * Creates an object of type <tt>type</tt> and populates its attributed with
    * the current row values in the result set <tt>resultSet</tt>.
    */
   public Persistent createObjectFromRow(Class type, ResultSet resultSet,
         List createdObjects, FetchPredicate predicate)
   {
      Persistent object = null;

      if ((null == predicate) || predicate.accept(resultSet))
      {
         Object identityKey = getDMLManager(type).getIdentityKey(resultSet);

         Persistent cachedObject = retrieveFromCache(type, identityKey);

         if (cachedObject != null)
         {
            Map deadCache = (Map) deadObjCacheRegistry.get(type);
            if ((null == deadCache) || !deadCache.containsKey(identityKey))
            {
               object = cachedObject;

               if (resultSet instanceof MultiplePersistentResultSet)
               {
                  MultiplePersistentResultSet mpResultSet = (MultiplePersistentResultSet) resultSet;
                  if (mpResultSet.isForceLoadingEnabled())
                  {
                     loadAdditionalObjectsFromRow(mpResultSet, createdObjects);
                  }
               }
            }
         }
         else
         {
            object = createObjectFromRow(type, resultSet, createdObjects);

            if (resultSet instanceof MultiplePersistentResultSet)
            {
               MultiplePersistentResultSet mpResultSet = (MultiplePersistentResultSet) resultSet;
               loadAdditionalObjectsFromRow(mpResultSet, createdObjects);
            }
         }
      }

      return object;
   }

   private void loadAdditionalObjectsFromRow(MultiplePersistentResultSet mpResultSet,
         List createdObjects)
   {
      try
      {
         while (mpResultSet.hasNextSecondary())
         {
            mpResultSet.nextSecondary();

            DmlManager dmlManager = getDMLManager(mpResultSet.getPersistentType());
            TypeDescriptor typeDescriptor = dmlManager.getTypeDescriptor();

            Field[] pkFields = typeDescriptor.getPkFields();
            Object[] joinedPkValues = new Object[pkFields.length];
            for (int i = 0; i < pkFields.length; i++ )
            {
               joinedPkValues[i] = DmlManager.getJavaValue(
                     pkFields[i].getType(),
                     typeDescriptor.getPersistentField(i).getLength(),
                     mpResultSet,
                     1 + typeDescriptor.getFieldColumnIndex(pkFields[i]),
                     false, false);
            }

            boolean nullPk = false;
            for (int i = 0; i < joinedPkValues.length; i++ )
            {
               nullPk |= (null == joinedPkValues[i]);
            }

            if ( !nullPk
                  && !existsInCache(mpResultSet.getPersistentType(),
                        typeDescriptor.getIdentityKey(joinedPkValues)))
            {
               createObjectFromRow(mpResultSet.getPersistentType(), mpResultSet
                     , createdObjects);
            }
         }
      }
      finally
      {
         mpResultSet.activatePrimary();
      }
   }

   private Persistent createObjectFromRow(Class type, ResultSet resultSet, List createdObjects)
   {
      DmlManager dmlManager = getDMLManager(type);
      TypeDescriptor typeDescriptor = dmlManager.getTypeDescriptor();
      PersistenceController cntrl = dmlManager.createObjectFromRow(this, resultSet);
      Persistent result = cntrl.getPersistent();

      addToPersistenceControllers(typeDescriptor.getIdentityKey(result), cntrl);

      if (createdObjects != null)
      {
//         trace.info("Adding object " + result);
         createdObjects.add(result);
      }

/*      if (typeDescriptor.getLoader() != null)
      {
         typeDescriptor.getLoader().load(result);
      }*/

      AbstractCache externalCache = CacheHelper.getCache(type);
      if (externalCache != null)
      {
         if (!externalCache.isCached(result))
         {
            externalCache.set(result, false);
         }
      }

      return result;
   }

   private Persistent retrieveFromCache(Class type, Object identityKey)
   {
      Persistent po = null;

      Map cache = (Map) objCacheRegistry.get(type);
      if (null != cache)
      {
         PersistenceController cntrl = (PersistenceController) cache.get(identityKey);

         if (null != cntrl)
         {
            if (trace.isDebugEnabled())
            {
               trace.debug(this + ", found " + cntrl.getPersistent() + " in cache.");
            }

            po = cntrl.getPersistent();
         }
      }
      return po;
   }

   public boolean existsInCache(Class type, Object identityKey)
   {
      Map cache = (Map) objCacheRegistry.get(type);
      return cache != null && cache.containsKey(identityKey);
   }

   @Override
   public <T> Iterator<T> getSessionCacheIterator(final Class<T> type, final FilterOperation<T> op)
   {
      final Collection<PersistenceController> cache = getCache(type);
      
      final Set<T> persistents = new HashSet<T>();
      for (final PersistenceController p : cache)
      {
         final T t = (T) p.getPersistent();
         final FilterResult result = op.filter(t);
         if (result == FilterResult.ADD)
         {
            persistents.add(t);
         }
      }
      
      return persistents.iterator();
   }
   
   public void setSynchronized(Persistent persistent)
   {
      TypeDescriptor typeDescr = tdRegistry.getDescriptor(persistent.getClass());

      if (null == synchronizationCache)
      {
         this.synchronizationCache = CollectionUtils.newMap();
      }

      Set typeCache = (Set) synchronizationCache.get(typeDescr.getType());
      if (null == typeCache)
      {
         typeCache = new HashSet();
         synchronizationCache.put(typeDescr.getType(), typeCache);
      }
      typeCache.add(typeDescr.getIdentityKey(persistent));
   }

   /**
    * Returns the active connectionManager to the connectionManager pool.
    * <p/>
    * Access to the variable <tt>jdbcConnection</tt> must be .
    */
   private void returnJDBCConnection() throws SQLException
   {
      if (jdbcConnection != null)
      {
         if (trace.isDebugEnabled())
         {
            trace.debug(this + ", stopped using " + LogUtils.instanceInfo(jdbcConnection));
         }

         // inform all interested parties about connection closing
         for (int i = 0; i < connectionHooks.size(); ++i)
         {
            ((ConnectionHook) connectionHooks.get(i)).onCloseConnection(jdbcConnection);
         }

         jdbcConnection.close();

         if (trace.isDebugEnabled())
         {
            trace.debug(this + " has returned JDBC connection " + jdbcConnection + ".");
         }

         jdbcConnection = null;
      }
   }

   /**
    *  possibly closing persistence controllers is not necessary because
    * the hashmap is cleaned  up anyway.
    */
   public void closeAndClearPersistenceControllers()
   {
      for (Iterator cacheItr = objCacheRegistry.values().iterator(); cacheItr.hasNext();)
      {
         Map cache = (Map) cacheItr.next();
         for (Iterator i = cache.values().iterator(); i.hasNext();)
         {
            ((PersistenceController) i.next()).close();
         }
         disconnectPersistenceControllers(cache);
      }
      for (Iterator cacheItr = deadObjCacheRegistry.values().iterator(); cacheItr.hasNext();)
      {
         disconnectPersistenceControllers((Map) cacheItr.next());
      }

      if ((null != synchronizationCache) && !synchronizationCache.isEmpty())
      {
         synchronizationCache.clear();
      }
   }

   /**
    *  possibly closing persistence controllers is not necessary because
    * the hashmap is cleaned  up anyway.
    */
   public void fastCloseAndClearPersistenceControllers()
   {
      this.objCacheRegistry = Collections.EMPTY_MAP;
      this.deadObjCacheRegistry = Collections.EMPTY_MAP;

      this.synchronizationCache = null;
   }

   /**
    * Disconnects the persistence controller of a persistent object from the session
    * and from the persistent, so that the persistent object has to rebind to
    * a new session, if persistence is required
    *
    * @param persistenceControllers the bundle of persistenceControllers to clear
    */
   private void disconnectPersistenceControllers(Map persistenceControllers)
   {
      for (Iterator i = persistenceControllers.values().iterator(); i.hasNext();)
      {
         PersistenceController controller = (PersistenceController) i.next();
         controller.getPersistent().disconnectPersistenceController();
      }
      persistenceControllers.clear();
   }

   public String toString()
   {
      return "Session #" + hashCode();
   }

   public boolean fetchVector(Persistent persistent, String vectorName)
   {
      try
      {
         DefaultPersistenceController cntrl = (DefaultPersistenceController)
               persistent.getPersistenceController();
         TypeDescriptor typeDescriptor = tdRegistry.getDescriptor(persistent.getClass());
         int vectorIdx = typeDescriptor.getPersistentVectorIdx(vectorName);
         PersistentVectorDescriptor descriptor = typeDescriptor.getPersistentVector(vectorIdx);
         if (descriptor == null)
         {
            throw new InternalException("Vector '" + vectorName + "' unknown.");
         }

         if (cntrl.isVectorFetched(vectorIdx))
         {
            return false;
         }
         cntrl.markVectorFetched(vectorIdx);
         TypeDescriptor otherType = tdRegistry.getDescriptor(descriptor.getType());
         Field[] pkFields = typeDescriptor.getPkFields();
         AndTerm predicate = new AndTerm();
         for (int i = 0; i < pkFields.length; i++ )
         {
            final Object fkValue = pkFields[i].get(persistent);
            if (fkValue instanceof Number)
            {
               predicate.add(Predicates.isEqual(otherType.fieldRef(descriptor.getOtherRole()),
                     ((Number) fkValue).longValue()));
            }
            else
            {
               predicate.add(Predicates.isEqual(otherType.fieldRef(descriptor.getOtherRole()),
                     String.valueOf(fkValue)));
            }
         }
         ResultIterator items = getIterator(descriptor.getType(),
               QueryExtension.where(predicate));
         try
         {
            DmlManager dmlManager = getDMLManager(persistent.getClass());
            dmlManager.setVector(persistent, vectorName, items);
         }
         finally
         {
            items.close();
         }
      }
      catch (Exception x)
      {
         throw new InternalException(x);
      }
      return true;
   }

   public void reloadObject(Persistent persistent) throws PhantomException
   {
      try
      {
         DmlManager dmlManager = getDMLManager(persistent.getClass());

         int retries = 10;
         while (0 < retries)
         {
            try
            {
               dmlManager.reloadObjectFromRow(this, persistent);
               break;
            }
            catch (ConcurrencyException e)
            {
               trace.info("Failed reloading object. Trying again " + retries + " times");

               --retries;
               if (0 < retries)
               {
                  Thread.sleep(100);
               }
               else
               {
                  throw e;
               }
            }
         }
      }
      catch (PhantomException e)
      {
         throw e;
      }
      catch (Exception x)
      {
         throw new InternalException("Failed to reload object.", x);
      }
   }

   public void reloadAttribute(Persistent persistent, String attributeName)
         throws PhantomException
   {
      try
      {
         DmlManager dmlManager = getDMLManager(persistent.getClass());

         int retries = 10;
         while (0 < retries)
         {
            try
            {
               dmlManager.reloadAttributeFromRow(this, persistent, attributeName);
               break;
            }
            catch (ConcurrencyException e)
            {
               trace.info("Failed reloading attribute. Trying again " + retries
                     + " times", e);

               --retries;
               if (0 < retries)
               {
                  Thread.sleep(100);
               }
               else
               {
                  throw e;
               }
            }
         }
      }
      catch (PhantomException e)
      {
         throw e;
      }
      catch (Exception x)
      {
         throw new InternalException("Failed to reload object.", x);
      }
   }

   public DBDescriptor getDBDescriptor()
   {
      return dbDescriptor;
   }

   public void markDeleted(DefaultPersistenceController cntrl)
   {
      markDeleted(cntrl, false);
   }

   public void markDeleted(DefaultPersistenceController cntrl, boolean writeThrough)
   {
      if (isArchiveAuditTrail)
      {
         // readonly
         throw new PublicException("Archive AuditTrail does not allow changes.");
      }

      Persistent persistent = cntrl.getPersistent();
      TypeDescriptor typeDescriptor = tdRegistry.getDescriptor(persistent.getClass());

      addToDeadPersistenceControllers(typeDescriptor.getIdentityKey(persistent), cntrl);

      if (writeThrough)
      {
         delete(persistent.getClass(), persistent);
      }
   }

   public void markModified(DefaultPersistenceController cntrl)
   {
      if (isArchiveAuditTrail)
      {
         // readonly
         throw new PublicException("Archive AuditTrail does not allow changes.");
      }

      // @todo (france, ub): analyze this. This leads e.g. to random deadlocks on DB2

      // Fetch all links - workaround to protect against overwriting by fetchLink()

      Persistent persistent = cntrl.getPersistent();
      ITypeDescriptor typeDescriptor = tdRegistry.getDescriptor(persistent.getClass());

      for (Iterator i = typeDescriptor.getLinks().iterator(); i.hasNext();)
      {
         LinkDescriptor descriptor = (LinkDescriptor) i.next();
         fetchLink(persistent, descriptor.getField().getName());
      }
   }

   /**
    * @param persistentClass class the SQL call is to be performed on, can be null if unknown
    * @return
    */
   public boolean isUsingPreparedStatements(Class persistentClass)
   {
      // TODO (ab) either look if there is a CLOB column or use an annotation
      if (ClobDataBean.class.equals(persistentClass))
      {
         // operations on HugeStringHolder must be performed using
         // prepared statements only
         return true;
      }
      else
      {
         return isUsingMixedPreparedStatements() ? true : params.getBoolean(
               SessionProperties.DS_NAME_AUDIT_TRAIL.equals(name)
               ? KEY_AUDIT_TRAIL_USE_PREPARED_STATEMENTS
               : name + SessionProperties.DS_USE_PREPARED_STATEMENTS_SUFFIX, false);
      }
   }

   public boolean isUsingMixedPreparedStatements()
   {
      String property = SessionProperties.DS_NAME_AUDIT_TRAIL.equals(name)
         ? KEY_AUDIT_TRAIL_USE_PREPARED_STATEMENTS
         : name + SessionProperties.DS_USE_PREPARED_STATEMENTS_SUFFIX;
      String option = params.getString(property);
      return StringUtils.isNotEmpty(option) &&
           SessionProperties.OPT_MIXED_PREPARED_STATEMENTS.equals(option.toLowerCase().trim());
   }

   /**
    * This method tells whether distinct locking tables should be used for locking
    * operations in this session.
    *
    * @return <code>true</code> if lock tables are used.
    */
   public boolean isUsingLockTables()
   {
      boolean useLockTablesDefault = dbDescriptor.getUseLockTablesDefault();
      return params.getBoolean(name + SessionProperties.DS_USE_LOCK_TABLES_SUFFIX,
            useLockTablesDefault);
   }

   public boolean isUsingEagerLinkFetching()
   {
      return params.getBoolean(
            name + SessionProperties.DS_USE_EAGER_LINK_FETCH_SUFFIX, false);
   }

   /**
    * This method tells whether jdbc calls on {@link Connection}, {@link Statement},
    * {@link PreparedStatement} and {@link ResultSet} are intercepted.
    *
    * @return <code>true</code> if jdbc call interception takes place.
    */
   public boolean isInterceptingJdbcCalls()
   {
      return params.getBoolean(
            name + SessionProperties.DS_INTERCEPT_JDBC_CALLS_SUFFIX, false);
   }

   /**
    * This method tells whether method call monitoring on jdbc calls takes place
    * in case that transaction state allows only certain methods to be executed.
    *
    * @return
    */
   public boolean isMonitoringOnJdbcInterception()
   {
      return isInterceptingJdbcCalls() && params.getBoolean(
            name + SessionProperties.DS_MONITOR_ON_JDBC_INTERCEPTION_SUFFIX, false);
   }

   /**
    * SQL execution is monitored. Statements are recorded with their execution
    * time for later tracing. Slow statements will be traced at once.
    *
    * @param sqlString the SQL string.
    * @param startTime the start time of execution in milliseconds.
    * @param stopTime the stop time of execution in milliseconds.
    */
   public void monitorSqlExecution(String sqlString, final long startTime,
         final long stopTime)
   {
      final long diffTime = stopTime - startTime;
      RuntimeLogUtils.getSqlTimeRecorder(params).record(sqlString, diffTime);
      if (diffTime >= params.getLong(
            KernelTweakingProperties.SLOW_STATEMENT_TRACING_THRESHOLD, Long.MAX_VALUE))
      {
         trace.warn("Slow query: " + diffTime + "ms\n" + sqlString);
      }
   }

   private void updateWorkItems(Map aiCache)
   {
      for (Iterator aiItr = aiCache.values().iterator(); aiItr.hasNext();)
      {
         PersistenceController dpc = (PersistenceController) aiItr.next();

         if (dpc.isModified() || dpc.isCreated())
         {
            ActivityInstanceBean ai = (ActivityInstanceBean) dpc.getPersistent();

            // support polymorphism wrt. activity definition
            IActivityExecutionStrategy aeStrategy = ActivityExecutionUtils.getExecutionStrategy(ai.getActivity());
            if (null != aeStrategy)
            {
               aeStrategy.updateWorkItem(ai);

               continue;
            }

            // TODO refactor default behavior to be accessible to custom execution strategies

            if (ai.getActivity().isInteractive())
            {
               // TODO lock AI before any change to workitems to be safe from concurrency issues?
               if ((null != ai.getOriginalState()) || (null != ai.getOriginalPerformer())
                     || ai.getPersistenceController().getModifiedFields().contains(ActivityInstanceBean.FIELD__CRITICALITY))
               {
                  if (MonitoringUtils.hasWorklistMonitors())
                  {
                     // notify worklist monitors of previous and current performers
                     IParticipant previousPerformer = (null != ai.getOriginalPerformer())
                           ? PerformerUtils.decodePerformer(ai.getOriginalPerformer(),
                                 (IModel) ai.getActivity().getModel())
                           : null;
                     IParticipant currentPerformer = ai.getPerformer();

                     if (previousPerformer != currentPerformer)
                     {
                        if (trace.isDebugEnabled())
                        {
                           trace.debug("Sending worklist monitor notifications for " + ai);
                        }

                        if (null != previousPerformer)
                        {
                           MonitoringUtils.worklistMonitors().removedFromWorklist(
                                 previousPerformer, ai);
                        }
                        if (null != currentPerformer)
                        {
                           MonitoringUtils.worklistMonitors().addedToWorklist(
                                 currentPerformer, ai);
                        }
                     }
                  }

                  final ActivityInstanceState wiState = null != ai.getOriginalState()
                        ? ai.getOriginalState()
                        : ai.getState();

                  if (ActivityInstanceState.Suspended.equals(ai.getState())
                        || ActivityInstanceState.Application.equals(ai.getState()))
                  {
                     if (ActivityInstanceState.Suspended.equals(wiState)
                           || ActivityInstanceState.Application.equals(wiState))
                     {
                        // update existing workitem

                        // TODO do not load, but update from transiently created instance
                        WorkItemBean wi = WorkItemBean.findByOID(ai.getOID());
                        wi.update(ai);
                     }
                     else
                     {
                        // insert new workitem

                        // TODO try not to load PI here
                        new WorkItemBean(ai, ai.getProcessInstance());
                     }
                  }
                  else if (ActivityInstanceState.Suspended.equals(wiState)
                        || ActivityInstanceState.Application.equals(wiState))
                  {
                     // delete existing workitem
                     delete(WorkItemBean.class, Predicates.isEqual(
                           WorkItemBean.FR__ACTIVITY_INSTANCE, ai.getOID()), false);
                  }
               }
            }
         }
      }
   }

   private void updateActivityInstancesHistory(Map aiCache)
   {
      final ChangeLogDigester digester = PropertyLayerProviderInterceptor.getCurrent()
            .getChangeLogDigester();

      for (Iterator aiItr = aiCache.values().iterator(); aiItr.hasNext();)
      {
         PersistenceController dpc = (PersistenceController) aiItr.next();
         ActivityInstanceBean ai = (ActivityInstanceBean) dpc.getPersistent();

         List historicStates = ai.getHistoricStates();
         if ( !historicStates.isEmpty())
         {
            // append current state with open interval
            historicStates.add(new ChangeLogDigester.HistoricState(
                  ai.getLastModificationTime(), ai.getState(), ai.getPerformer(), ai.getCurrentDepartment()));

            // apply (optional) post processing
            historicStates = digester.digestChangeLog(ai, historicStates);

            Date predecessor = null;
            if ( !historicStates.isEmpty())
            {
               // persist digested change log
               PerformerUtils.EncodedPerformer performedOnBehalfOf = null;

               // first state may be a follow up to the last persisted one
               ChangeLogDigester.HistoricState initialState = (ChangeLogDigester.HistoricState) historicStates.get(0);
               if (initialState.isUpdatedRecord())
               {
                  ActivityInstanceHistoryBean updatedRecord = (ActivityInstanceHistoryBean) findFirst(
                        ActivityInstanceHistoryBean.class,
                        QueryExtension.where(Predicates.andTerm(
                              Predicates.isEqual(
                                    ActivityInstanceHistoryBean.FR__ACTIVITY_INSTANCE,
                                    ai.getOID()),
                              Predicates.isEqual(
                                    ActivityInstanceHistoryBean.FR__FROM,
                                    initialState.getFrom().getTime()))));

                  if (null != updatedRecord)
                  {
                     updatedRecord.setUntil(initialState.getUntil());
                     predecessor = initialState.getUntil();

                     performedOnBehalfOf = updatedRecord.getEncodedOnBehalfOf();
                  }
                  else
                  {
                     // Throwing an exception is not allowed here because the entries
                     // are written only since version 4.5. The upgrade job had not all
                     // informations to generate a consistent set of history entries for
                     // previously existing AIs.
                     trace.info("Missing history entry for " + ai
                           + " with from timestamp " + initialState.getFrom().getTime());
                  }
               }

               for (int i = 0; i < historicStates.size(); ++i)
               {
                  ChangeLogDigester.HistoricState state = (ChangeLogDigester.HistoricState) historicStates.get(i);

                  if ( !state.isUpdatedRecord())
                  {
                     // TODO propagate onBehalfOf

                     IParticipant performer = state.getPerformer();
                     IDepartment department = state.getDepartment();
                     if (null != performer && !(performer instanceof IUser))
                     {
                        // replace with new non-user performer
                        performedOnBehalfOf = PerformerUtils.encodeParticipant(performer,
                              department);
                     }

                     new ActivityInstanceHistoryBean(ai, state.getFrom(), state
                           .getUntil(), state.getState(), performer, department,
                           performedOnBehalfOf);
                     if ((null != predecessor) && !predecessor.equals(state.getFrom()))
                     {
                        trace.error("Broken AI history link: " + ai.getOID() + ", " + predecessor + " vs. " + state.getFrom().getTime());
                     }
                     predecessor = state.getUntil();
                  }
               }
            }
         }
      }
   }

   /**
    * This method adds a provided connection hook. All added hooks
    * will be called arbitrarily.
    *
    * @param hook A connection hook
    * @return true if the specified hook has not already been added.
    */
   private boolean addConnectionHook(ConnectionHook hook)
   {
      if (Collections.EMPTY_LIST.equals(connectionHooks))
      {
         this.connectionHooks = new ArrayList();
      }
      return connectionHooks.add(hook);
   }

   private void processDataValueFilterJoins(QueryDescriptor query,
         Set<Long> prefetchedDataRtOids, List<Column> additionalSelection,
         Map<ITableDescriptor, Join> descrToJoinAssociator)
   {
      for (final Join join : query.getQueryExtension().getJoins())
      {
         ITableDescriptor tableDescriptor = join.getRhsTableDescriptor();
         if (tableDescriptor instanceof DataCluster)
         {
            DataCluster cluster = (DataCluster) tableDescriptor;

            ModelManager modelManager = ModelManagerFactory.getCurrent();

            int usedSlotCnt = 0;
            for (DataSlot slot : cluster.getAllSlots())
            {
               // only apply slots for this cluster if it is not for structured data values
               if (StringUtils.isEmpty(slot.getAttributeName()))
               {
                  List<IModel> models = modelManager.getModels();
                  IData data = null;
                  for (IModel model : models)
                  {
                     data = model.findData(slot.getDataId());
                     if (data != null)
                     {
                        break;
                     }
                  }
                  usedSlotCnt++;

                  if (data == null)
                  {
                     trace.warn("Data with ID "
                           + slot.getDataId()
                           + " referenced by data cluster does not exist. Will be skipped for prefetching.");
                     break;
                  }

                  TableAliasDecorator typeDescr = new TableAliasDecorator(
                        join.getTableAlias() + "_S" + usedSlotCnt, null)
                  {
                     @Override
                     public String getTableName()
                     {
                        return join.getTableName();
                     }
                  };

                  // currently the following order of columns has to be used:
                  // dv.oid, dv.model, dv.data, dv.string_value, dv.number_value, dv.type_key, dv.processInstance

                  // oid
                  FieldRef oid = new FieldRef(join, slot.getOidColumn());
                  additionalSelection.add(Functions.constantExpression(oid.toString(),
                        typeDescr.getTableAlias() + "." + DataValueBean.FIELD__OID));

                  // model: valid for PI, AI and WI queries
                  additionalSelection.add(Functions.constantExpression(
                        query.fieldRef(DataValueBean.FIELD__MODEL).toString(),
                        typeDescr.getTableAlias() + "." + DataValueBean.FIELD__MODEL));

                  // data
                  long dataRtOid = modelManager.getRuntimeOid(data);
                  additionalSelection.add(Functions.constantExpression(
                        Long.toString(dataRtOid), typeDescr.getTableAlias() + "."
                              + DataValueBean.FIELD__DATA));

                  // record that dataRtOid for later optimization
                  prefetchedDataRtOids.add(dataRtOid);

                  // string_value
                  if (StringUtils.isNotEmpty(slot.getSValueColumn()))
                  {
                     FieldRef stringValue = new FieldRef(join, slot.getSValueColumn());
                     additionalSelection.add(Functions.constantExpression(
                           stringValue.toString(), typeDescr.getTableAlias() + "."
                                 + DataValueBean.FIELD__STRING_VALUE));
                  }
                  else
                  {
                     additionalSelection.add(Functions.constantExpression("null",
                           typeDescr.getTableAlias() + "."
                                 + DataValueBean.FIELD__STRING_VALUE));
                  }

                  // number_value
                  if (StringUtils.isNotEmpty(slot.getNValueColumn()))
                  {
                     FieldRef numberValue = new FieldRef(join, slot.getNValueColumn());
                     additionalSelection.add(Functions.constantExpression(
                           numberValue.toString(), typeDescr.getTableAlias() + "."
                                 + DataValueBean.FIELD__NUMBER_VALUE));
                  }
                  else
                  {
                     additionalSelection.add(Functions.constantExpression("0",
                           typeDescr.getTableAlias() + "."
                                 + DataValueBean.FIELD__NUMBER_VALUE));
                  }

                  // dv.type_key
                  FieldRef typeKey = new FieldRef(join, slot.getTypeColumn());
                  additionalSelection.add(Functions.constantExpression(
                        typeKey.toString(), typeDescr.getTableAlias() + "."
                              + DataValueBean.FIELD__TYPE_KEY));

                  // dv.processInstance
                  FieldRef processInstance = new FieldRef(join,
                        cluster.getProcessInstanceColumn());
                  additionalSelection.add(Functions.constantExpression(
                        processInstance.toString(), typeDescr.getTableAlias() + "."
                              + DataValueBean.FIELD__PROCESS_INSTANCE));

                  // add cluster join for each slot which will be fetched
                  descrToJoinAssociator.put(typeDescr, join);
               }
            }
         }
         else if (tableDescriptor instanceof TypeDescriptor)
         {
            TypeDescriptor typeDescriptor = (TypeDescriptor) tableDescriptor;
            TableAliasDecorator typeDescr = new TableAliasDecorator(
                  join.getTableAlias(), typeDescriptor);
            if (DataValueBean.class.equals(typeDescriptor.getType()))
            {
               additionalSelection.addAll(SqlUtils
                     .getDefaultSelectFieldList(typeDescr));
               descrToJoinAssociator.put(typeDescr, join);

               List<PredicateTerm> parts = join.getRestriction().getParts();
               addDataRtOids(prefetchedDataRtOids, parts);
            }
         }
      }
   }

   private void addDataRtOids(Set<Long> prefetchedDataRtOids, List<PredicateTerm> parts)
   {
      for (PredicateTerm predicateTerm : parts)
      {
         if (predicateTerm instanceof ComparisonTerm)
         {
            ComparisonTerm comp = (ComparisonTerm) predicateTerm;
            if (DataValueBean.FIELD__DATA.equals(comp.getLhsField().fieldName)
                  && (Operator.IN.equals(comp.getOperator()) || Operator.IS_EQUAL
                        .equals(comp.getOperator())))
            {
               Object value = comp.getValueExpr();
               if (value instanceof List)
               {
                  prefetchedDataRtOids.addAll((List<Long>) comp.getValueExpr());
               }
               else if (value instanceof Long)
               {
                  prefetchedDataRtOids.add((Long) comp.getValueExpr());
               }
            }
         }
         else if (predicateTerm instanceof MultiPartPredicateTerm)
         {
            MultiPartPredicateTerm term = (MultiPartPredicateTerm) predicateTerm;
            addDataRtOids(prefetchedDataRtOids, term.getParts());
         }
      }
   }

   private static final class SessionCacheComparator implements Comparator
   {
      public int compare(Object lhs, Object rhs)
      {
         if ((lhs instanceof Long) && rhs instanceof Long)
         {
            return ((Long) lhs).compareTo((Long)rhs);
         }
         else if ((lhs instanceof Object[]) && rhs instanceof Object[])
         {
            Object[] lhsArray = (Object[]) lhs;
            Object[] rhsArray = (Object[]) rhs;

            int result = 0;
            for (int i = 0; (0 == result) && i < lhsArray.length; i++ )
            {
               result = compare(lhsArray[i], rhsArray[i]);
            }

            return result;
         }
         else
         {
            return ((Comparable) lhs).compareTo(rhs);
         }
      }
   }

   public static interface TypeDescriptorRegistryProvider
   {
      TypeDescriptorRegistry getTypeDescriptorRegistry();
   }

   public static interface DmlManagerRegistryProvider
   {
      DmlManagerRegistry getDmlManagerRegistry(String sessionName, SqlUtils sqlUtils,
            DBDescriptor dbDescriptor, TypeDescriptorRegistry tdRegistry);
   }

   public static final class RuntimeDmlManagerProvider
         implements DmlManagerRegistryProvider
   {
      private static final String KEY_GLOBAL_DML_MANAGER_CACHE_PREFIX = RuntimeDmlManagerProvider.class.getName()
            + ".GlobalCache.";

      private static final String KEY_AUDIT_TRAIL_DML_MANAGER_CACHE = KEY_GLOBAL_DML_MANAGER_CACHE_PREFIX
            + SessionProperties.DS_NAME_AUDIT_TRAIL;

      public DmlManagerRegistry getDmlManagerRegistry(final String sessionName,
            final SqlUtils sqlUtils, final DBDescriptor dbDescriptor,
            final TypeDescriptorRegistry tdRegistry)
      {
         final GlobalParameters globals = GlobalParameters.globals();

         final String keyDmlManagerCache = SessionProperties.DS_NAME_AUDIT_TRAIL.equals(sessionName)
               ? KEY_AUDIT_TRAIL_DML_MANAGER_CACHE
               : KEY_GLOBAL_DML_MANAGER_CACHE_PREFIX + sessionName;

         DmlManagerRegistry registry = (DmlManagerRegistry) globals.get(keyDmlManagerCache);
         if (null == registry)
         {
            registry = (DmlManagerRegistry) globals.getOrInitialize(
                  keyDmlManagerCache, new ValueProvider()
                  {
                     public Object getValue()
                     {
                        DmlManagerRegistry registry = new DmlManagerRegistry();

                        registry.registerDefaultRuntimeClasses(dbDescriptor, sqlUtils,
                              tdRegistry);

                        return registry;
                     }
                  });
         }

         return registry;
      }
   }

}

