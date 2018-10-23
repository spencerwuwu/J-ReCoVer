// https://searchcode.com/api/result/74204755/

/*******************************************************************************
 * Copyright (c) 2011 SunGard CSA LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SunGard CSA LLC - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.stardust.engine.cli.sysconsole;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.eclipse.stardust.common.Assert;
import org.eclipse.stardust.common.CollectionUtils;
import org.eclipse.stardust.common.DateUtils;
import org.eclipse.stardust.common.Functor;
import org.eclipse.stardust.common.Pair;
import org.eclipse.stardust.common.StringUtils;
import org.eclipse.stardust.common.TransformingIterator;
import org.eclipse.stardust.common.Unknown;
import org.eclipse.stardust.common.config.Parameters;
import org.eclipse.stardust.common.error.ErrorCase;
import org.eclipse.stardust.common.error.InternalException;
import org.eclipse.stardust.common.error.PublicException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.model.IData;
import org.eclipse.stardust.engine.api.model.IModel;
import org.eclipse.stardust.engine.api.model.PredefinedConstants;
import org.eclipse.stardust.engine.api.runtime.BpmRuntimeError;
import org.eclipse.stardust.engine.api.runtime.IllegalOperationException;
import org.eclipse.stardust.engine.api.runtime.ProcessInstanceState;
import org.eclipse.stardust.engine.core.compatibility.extensions.dms.data.DocumentSetStorageBean;
import org.eclipse.stardust.engine.core.compatibility.extensions.dms.data.DocumentStorageBean;
import org.eclipse.stardust.engine.core.model.utils.ModelElementList;
import org.eclipse.stardust.engine.core.persistence.AndTerm;
import org.eclipse.stardust.engine.core.persistence.Column;
import org.eclipse.stardust.engine.core.persistence.ComparisonTerm;
import org.eclipse.stardust.engine.core.persistence.DeleteDescriptor;
import org.eclipse.stardust.engine.core.persistence.FieldRef;
import org.eclipse.stardust.engine.core.persistence.Functions;
import org.eclipse.stardust.engine.core.persistence.InsertDescriptor;
import org.eclipse.stardust.engine.core.persistence.Join;
import org.eclipse.stardust.engine.core.persistence.JoinElement;
import org.eclipse.stardust.engine.core.persistence.Joins;
import org.eclipse.stardust.engine.core.persistence.MultiPartPredicateTerm;
import org.eclipse.stardust.engine.core.persistence.Operator;
import org.eclipse.stardust.engine.core.persistence.OrTerm;
import org.eclipse.stardust.engine.core.persistence.PredicateTerm;
import org.eclipse.stardust.engine.core.persistence.Predicates;
import org.eclipse.stardust.engine.core.persistence.QueryDescriptor;
import org.eclipse.stardust.engine.core.persistence.ResultIterator;
import org.eclipse.stardust.engine.core.persistence.jdbc.DBDescriptor;
import org.eclipse.stardust.engine.core.persistence.jdbc.DBMSKey;
import org.eclipse.stardust.engine.core.persistence.jdbc.FieldDescriptor;
import org.eclipse.stardust.engine.core.persistence.jdbc.ITypeDescriptor;
import org.eclipse.stardust.engine.core.persistence.jdbc.IdentifiablePersistentBean;
import org.eclipse.stardust.engine.core.persistence.jdbc.LinkDescriptor;
import org.eclipse.stardust.engine.core.persistence.jdbc.QueryUtils;
import org.eclipse.stardust.engine.core.persistence.jdbc.Session;
import org.eclipse.stardust.engine.core.persistence.jdbc.SessionFactory;
import org.eclipse.stardust.engine.core.persistence.jdbc.SessionProperties;
import org.eclipse.stardust.engine.core.persistence.jdbc.TypeDescriptor;
import org.eclipse.stardust.engine.core.runtime.beans.*;
import org.eclipse.stardust.engine.core.runtime.beans.removethis.KernelTweakingProperties;
import org.eclipse.stardust.engine.core.runtime.setup.DataCluster;
import org.eclipse.stardust.engine.core.runtime.setup.DataSlot;
import org.eclipse.stardust.engine.core.runtime.setup.RuntimeSetup;
import org.eclipse.stardust.engine.core.spi.extensions.runtime.Event;
import org.eclipse.stardust.engine.core.struct.StructuredTypeRtUtils;
import org.eclipse.stardust.engine.core.struct.beans.StructuredDataBean;
import org.eclipse.stardust.engine.core.struct.beans.StructuredDataValueBean;


/**
 * @author ubirkemeyer, rsauer
 * @version $Revision$
 */

// todo: use everywhere qualified user schema
public class Archiver
{
   private static final String FIELD_PARTITION = "partition";
   private static final String SPACE = " ";
   private static final String DOT = ".";

   private static final int SQL_IN_CHUNK_SIZE = 1000;

   public static final String OPTION_DRY_RUN = "org.eclipse.stardust.engine.cli.sysconsole.dryRun";
   public static final String OPTION_STMT_BATCH_SIZE = "org.eclipse.stardust.engine.cli.sysconsole.stmtBatchSize";

   private static final Logger trace = LogManager.getLogger(Archiver.class);

   private static final String ALIAS_SRC = "o";
   private static final String ALIAS_ARCHIVE = "arch";

   private static final PredicateTerm PT_TERMINATED_PI = Predicates.inList(
         ProcessInstanceBean.FR__STATE,
         new int[] { ProcessInstanceState.ABORTED, ProcessInstanceState.COMPLETED });

   private static final PredicateTerm PT_ALIVE_PI = Predicates.notInList(
         ProcessInstanceBean.FR__STATE,
         new int[] { ProcessInstanceState.ABORTED, ProcessInstanceState.COMPLETED });

   private static final ComparisonTerm PT_EVENT_BINDING_PI = Predicates.isEqual(
         EventBindingBean.FR__TYPE, Event.PROCESS_INSTANCE);

   private static final ComparisonTerm PT_EVENT_BINDING_AI = Predicates.isEqual(
         EventBindingBean.FR__TYPE, Event.ACTIVITY_INSTANCE);

   private static final ComparisonTerm PT_STRING_DATA_IS_MODEL_RECORD = Predicates.isEqual(
         LargeStringHolder.FR__DATA_TYPE, TypeDescriptor.getTableName(ModelPersistorBean.class));

   private static final ComparisonTerm PT_STRING_DATA_IS_DATA_VALUE_RECORD = Predicates.isEqual(
         LargeStringHolder.FR__DATA_TYPE, TypeDescriptor.getTableName(DataValueBean.class));

   private static final ComparisonTerm PT_STRING_DATA_IS_DMS_DOC_VALUE_RECORD = Predicates
         .inList(LargeStringHolder.FR__DATA_TYPE, new String[] {
               TypeDescriptor.getTableName(DocumentStorageBean.class),
               TypeDescriptor.getTableName(DocumentSetStorageBean.class) });

   private static final int PK_OID = 0;
   private static final int PK_MODEL = 1;

   private final boolean archive;

   private final String srcSchema;
   private final String archiveSchema;

   private final long txBatchSizeLimit;
   private final int stmtBatchSizeLimit;

   private final Short partitionOid;

   private final Session session;

   private final Set ignoredRootPiOids = new HashSet();

   public Archiver(boolean archive, String archiveSchema, long txBatchSize,
         boolean force, String partitionId)
   {
      this.archive = archive;
      this.archiveSchema = archiveSchema;

      this.txBatchSizeLimit = txBatchSize;
      this.stmtBatchSizeLimit = Parameters.instance().getInteger(OPTION_STMT_BATCH_SIZE,
            100);

      try
      {
         this.session = (Session) SessionFactory.getSession(SessionFactory.AUDIT_TRAIL);

         // obtain connection to test session validity
         session.getConnection();
      }
      catch (SQLException e)
      {
         final String message = "Failed obtaining JDBC connection to audit trail db";
         trace.warn(message, e);
         throw new PublicException(message);
      }

      this.srcSchema = !StringUtils.isEmpty(session.getSchemaName())
            ? session.getSchemaName()
            : Parameters.instance().getString(
                  SessionFactory.AUDIT_TRAIL + SessionProperties.DS_USER_SUFFIX);

      Statement stmt = null;
      ResultSet rsPartition = null;
      try
      {
         DBDescriptor dbDescriptor = session.getDBDescriptor();
         stmt = session.getConnection().createStatement();
         rsPartition = stmt.executeQuery("SELECT " + IdentifiablePersistentBean.FIELD__OID
               + "  FROM " + getQualifiedName(srcSchema, dbDescriptor.quoteIdentifier(AuditTrailPartitionBean.TABLE_NAME))
               + " WHERE " + AuditTrailPartitionBean.FIELD__ID + "='" + partitionId + "'");
         if (rsPartition.next())
         {
            this.partitionOid = new Short(rsPartition.getShort(1));
         }
         else
         {
            throw new PublicException(("Invalid partition ID '" + partitionId + "'."));
         }
      }
      catch (SQLException sqle)
      {
         final String message = "Failed resolving partition ID '" + partitionId + "'.";
         trace.warn(message, sqle);
         throw new PublicException(message);
      }
      finally
      {
         QueryUtils.closeStatementAndResultSet(stmt, rsPartition);
      }
   }

   public void archiveDeadModels(long interval)
   {
      List/*<ModelHelper>*/ deadModels = findDeadModels();
      if ( !deadModels.isEmpty())
      {
         trace.info("Found dead models: "
               + StringUtils.join(new TransformingIterator(deadModels.iterator(),
                     new Functor()
                     {
                        public Object execute(Object source)
                        {
                           return new Long(((ModelHelper) source).getOid());
                        }
                     }), ", ") + ".");

         Set<Long> terminatedDeadModels = new TreeSet<Long>();
         for (Iterator i = deadModels.iterator(); i.hasNext();)
         {
            ModelHelper model = (ModelHelper) i.next();
            final long nonterminatedInstances = getAliveProcessInstancesCount(model.getOid());
            if (0 == nonterminatedInstances)
            {
               terminatedDeadModels.add(model.getOid());
            }
            else
            {
               trace.info("Ignoring dead model with OID " + model.getOid() + " as of "
                     + nonterminatedInstances + " nonterminated process instances.");
            }
         }

         for (Long modelOid : terminatedDeadModels)
         {
            archiveDeadModel(modelOid, interval, terminatedDeadModels);
         }
         for (Long modelOid : terminatedDeadModels)
         {
            if (canDeleteModel(modelOid, terminatedDeadModels))
            {
               deleteModel(modelOid);
      }
         }
      }
      else
      {
         trace.info("No dead models found.");
      }
   }

   public void archiveDeadModel(long modelOid, long interval)
   {
      Set<Long> terminatedDeadModels = Collections.singleton(modelOid);
      
      archiveDeadModel(modelOid, interval, terminatedDeadModels);
      
      if (canDeleteModel(modelOid, terminatedDeadModels))
      {
         deleteModel(modelOid);
   }
   }
   
   public void archiveDeadModel(long modelOid, long interval, Set<Long> terminatedDeadModels)
   {
      // check for nonterminated process instances
      final long nAliveProcesses = getAliveProcessInstancesCount(modelOid);

      if (archive)
      {
         if (0 < nAliveProcesses)
         {
            throw new PublicException("Cannot archive models with nonterminated "
                  + "process instances (found " + nAliveProcesses
                  + " nonterminated process instances).");
         }
      }
      else
      {
         if (0 < nAliveProcesses)
         {
            throw new PublicException("Unable to delete closure of model with OID "
                  + modelOid + " as of " + nAliveProcesses
                  + " nonterminated process instances.");
         }
      }

      archiveProcesses(modelOid, null, interval);
      }

   public void archiveDeadProcesses(Date before, long interval)
   {
      // perform archive/delete operation on process instances
      archiveProcesses(null, before, interval);

      // report models now being without process instances
      List/*<Long>*/ unusedModelsOids = findUnusedModels();

      if (!unusedModelsOids.isEmpty())
      {
         trace.info("Found models with no process instances in audittrail (OIDs): "
               + StringUtils.join(unusedModelsOids.iterator(), ", "));
      }
   }

   public void archiveDeadProcesses(List inputRootPiOids)
   {
      // evaluate process instance links
      Set<Long> evaluatedRootOids = evaluateLinkedProcessInstances(inputRootPiOids, null);

      if (evaluatedRootOids.isEmpty())
      {
         trace.info(MessageFormat.format(
               "Cannot {0} process instances (no valid process instances to archive).",
               new Object[] {archive ? "archive" : "delete"}));

      }
      else
      {
         List<Long> rootPiOids = new ArrayList<Long>(evaluatedRootOids);

         // check for nonterminated process instances
         final long nAliveProcesses = getAliveProcessInstancesCount(null, rootPiOids);

         if (0 < nAliveProcesses)
         {
            throw new PublicException(
                  MessageFormat.format(
                        "Cannot {0} process instances (found {1} nonterminated process instances).",
                        new Object[] {
                              archive ? "archive" : "delete", new Long(nAliveProcesses)}));
         }

         // check if PIs are strictly terminated root PIs
         QueryDescriptor qFindRootPis = QueryDescriptor.from(ProcessInstanceBean.class)
               .select(ProcessInstanceBean.FIELD__OID,
                     ProcessInstanceBean.FIELD__ROOT_PROCESS_INSTANCE)
               .where(splitUpOidsSubList(rootPiOids, ProcessInstanceBean.FR__OID));
         Set nonrootPiOids = new TreeSet();
         ResultSet rsCheckPreconditions = session.executeQuery(qFindRootPis,
               Session.NO_TIMEOUT);
         try
         {
            while (rsCheckPreconditions.next())
            {
               long piOid = rsCheckPreconditions.getLong(1);
               long rootPiOid = rsCheckPreconditions.getLong(2);
               if (piOid != rootPiOid)
               {
                  nonrootPiOids.add(new Long(piOid));
               }
            }
         }
         catch (SQLException sqle)
         {
            throw new PublicException("Failed verifying preconditions.", sqle);
         }
         finally
         {
            QueryUtils.closeResultSet(rsCheckPreconditions);
         }

         if ( !nonrootPiOids.isEmpty())
         {
            ErrorCase errorCase;
            if (archive)
            {
               errorCase = BpmRuntimeError.ATDB_ARCHIVE_UNABLE_TO_ARCHIVE_NON_ROOT_PI.raise(nonrootPiOids);
            }
            else
            {
               errorCase = BpmRuntimeError.ATDB_ARCHIVE_UNABLE_TO_DELETE_NON_ROOT_PI.raise(nonrootPiOids);
            }

            new IllegalOperationException(errorCase);
         }

         if (archive)
         {
         // process instance links can include processes of models other than specified by modelOid
            List<Long> findModels = findModels(rootPiOids);
            for (Long toSyncModelOid : findModels)
            {
               if (toSyncModelOid != null)
               {
                  synchronizeMasterTables(toSyncModelOid);
               }
            }
         }

         doArchiveProcesses(rootPiOids);

         // report models now being without process instances
         List/* <Long> */unusedModelsOids = findUnusedModels();

         if ( !unusedModelsOids.isEmpty())
         {
            trace.info("Found models with no process instances in audittrail (OIDs): "
                  + StringUtils.join(unusedModelsOids.iterator(), ", "));
         }
      }
   }

   public void archiveLogEntries(Date before, long interval)
   {
      try
      {
         PredicateTerm referencePredicate = Predicates.andTerm(Predicates.isEqual(
               LogEntryBean.FR__PROCESS_INSTANCE, 0), Predicates.isEqual(
               LogEntryBean.FR__ACTIVITY_INSTANCE, 0));
         PredicateTerm partitionPredicate = Predicates.isEqual(
               LogEntryBean.FR__PARTITION, partitionOid.shortValue());
         long tsStop = (null == before) ? Long.MAX_VALUE : before.getTime();
         do
         {
            long tsStart = findMinValue(srcSchema, LogEntryBean.class,
                  LogEntryBean.FR__STAMP, Predicates.andTerm(partitionPredicate,
                        referencePredicate));
            if ((tsStart <= 0) || (tsStart > tsStop))
            {
               // there are no log entries left
               break;
            }

            if (archive)
            {
               backupLogEntries(Math.min(tsStart + interval, tsStop), Predicates.andTerm(
                     partitionPredicate, referencePredicate));
            }
            deleteLogEntries(Math.min(tsStart + interval, tsStop), Predicates.andTerm(
                  partitionPredicate, referencePredicate));
         }
         while (true);
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // ignore
         }
         throw new PublicException("Failed archiving log entries.", e);
      }
   }

   private int backupLogEntries(long tsBefore, PredicateTerm restriction)
   {
      QueryDescriptor fSelect = QueryDescriptor.from(srcSchema, LogEntryBean.class,
            ALIAS_SRC);

      PredicateTerm fSelectPredicate = Predicates.andTerm(Predicates.lessOrEqual(
            LogEntryBean.FR__STAMP, tsBefore), restriction);

      try
      {
         return session.executeInsert(InsertDescriptor.into(archiveSchema,
               LogEntryBean.class).fullselect(fSelect.where(fSelectPredicate)));
      }
      catch (Exception e)
      {
         throw new PublicException("Failed archiving entries from "
               + fSelect.getTableName()
               + " included in transitive closure of already archived log entries.", e);
      }
   }

   public void archiveUserSessions(Date before, long interval)
   {
      try
      {
         Joins joins = createUserSessionJoins();

         long txBatchSize = 0;
         long processedEntries = 0;
         long tsStop = (null == before) ? Long.MAX_VALUE : before.getTime();
         do
         {
            long tsStart = findMinValue(srcSchema, UserSessionBean.class,
                  UserSessionBean.FR__EXPIRATION_TIME, joins,
                  Predicates.isEqual(UserRealmBean.FR__PARTITION, partitionOid.shortValue()));
            long tsBefore = Math.min(tsStart + interval, tsStop);
            if ((tsStart <= 0) || (tsStart > tsStop))
            {
               // there are no user sessions left
               if(txBatchSize > 0)
               {
                  commit();

                  trace.info(MessageFormat.format(
                        "{0} {1} user session records expired before {2} from audittrail.",
                        new Object[] {
                              archive ? "Archived" : "Deleted",
                                    new Long(processedEntries),
                                    DateUtils.getNoninteractiveDateFormat()
                                          .format(new Date(tsBefore)) }));
               }
               break;
            }

            if (archive)
            {
               txBatchSize += backupUserSessions(tsBefore);
            }
            int deletedUserSessions = deleteUserSessions(tsBefore);
            txBatchSize += deletedUserSessions;
            processedEntries += deletedUserSessions;

            if (txBatchSize >= txBatchSizeLimit)
            {
               commit();

               trace.info(MessageFormat.format(
                     "{0} {1} user session records expired before {2} from audittrail.",
                     new Object[] {
                           archive ? "Archived" : "Deleted",
                                 new Long(processedEntries),
                                 DateUtils.getNoninteractiveDateFormat()
                                       .format(new Date(tsBefore)) }));

               txBatchSize = 0;
               processedEntries = 0;
            }
         }
         while (true);
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // ignore
         }
         throw new PublicException("Failed deleting user sessions.", e);
      }
   }

   private Joins createUserSessionJoins()
   {
      Joins joins = new Joins();

      Join ubJoin = new Join(UserBean.class).on(UserSessionBean.FR__USER,
            UserBean.FIELD__OID);
      joins.add(ubJoin);

      Join urJoin = new Join(UserRealmBean.class).on(UserBean.FR__REALM,
            UserRealmBean.FIELD__OID);
      urJoin.setDependency(ubJoin);
      joins.add(urJoin);

      return joins;
   }

   public void archiveDeadData(String[] ids, Date before, long interval)
   {
      if (archive)
      {
         throw new PublicException("Data can only be deleted, standalone archiving is" +
               " not supported.");
      }

      Map<String, List<String>> values = extractID(ids);

      // check
      findAllDataIds(values);

      for(Map.Entry<String, List<String>> entry : values.entrySet())
      {
         String modelID = entry.getKey();
         List<String> value = entry.getValue();

         if(modelID.equals(Integer.toString(PredefinedConstants.ALL_MODELS)))
         {
            modelID = null;
         }

         QueryDescriptor query = QueryDescriptor.from(srcSchema, DataValueBean.class)
               .select(Functions.min(ProcessInstanceBean.FR__TERMINATION_TIME));

         query.innerJoin(srcSchema, AuditTrailDataBean.class, "dd").on(
               DataValueBean.FR__DATA, AuditTrailDataBean.FIELD__OID).andOn(
               DataValueBean.FR__MODEL, AuditTrailDataBean.FIELD__MODEL);

         if(modelID != null)
         {
            query.innerJoin(srcSchema, ModelPersistorBean.class, "m").on(
                  AuditTrailDataBean.FR__MODEL, ModelPersistorBean.FIELD__OID).where(
                        Predicates.andTerm(
                  Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid
                        .shortValue()),
                        Predicates.isEqual(ModelPersistorBean.FR__ID, modelID)));

         }
         else
         {
            query.innerJoin(srcSchema, ModelPersistorBean.class, "m").on(
                  AuditTrailDataBean.FR__MODEL, ModelPersistorBean.FIELD__OID).where(
                  Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid
                        .shortValue()));
         }

         query.innerJoin(srcSchema, ProcessInstanceScopeBean.class).on(
               DataValueBean.FR__PROCESS_INSTANCE,
               ProcessInstanceScopeBean.FIELD__SCOPE_PROCESS_INSTANCE);

         query.innerJoin(srcSchema, ProcessInstanceBean.class).on(
               ProcessInstanceScopeBean.FR__ROOT_PROCESS_INSTANCE,
               ProcessInstanceBean.FIELD__OID);

         long tsStop = (null == before) ? Long.MAX_VALUE : before.getTime();
         do
         {
            long tsStart;

            try
            {
               ResultSet rs = null;
               try
               {
                  rs = session.executeQuery(query.where(Predicates.andTerm(Predicates
                        .inList(AuditTrailDataBean.FR__ID, value), PT_TERMINATED_PI)));

                  tsStart = rs.next() ? rs.getLong(1) : -1;
               }
               finally
               {
                  QueryUtils.closeResultSet(rs);
               }
            }
            catch (Exception e)
            {
               throw new PublicException("Failed to find starting time.", e);
            }

            if (tsStart <= 0 || tsStart > tsStop)
            {
               // there are no processes left
               break;
            }

            archiveData(modelID, value, Math.min(tsStart + interval, tsStop));
         }
         while (true);
      }
   }

   /**
    * Resolve full PI closure of root PIs.
    *
    * @param piRootOids List of root process instance oids.
    * @return Set of dull process instance closure.
    */
   private Set resolvePiClosure(List piRootOids)
   {
      if (piRootOids.isEmpty())
      {
         return Collections.EMPTY_SET;
      }

      QueryDescriptor qPiClosure = QueryDescriptor.from(ProcessInstanceBean.class)
            .select(ProcessInstanceBean.FR__OID) //
            .where(splitUpOidsSubList(piRootOids,
                  ProcessInstanceBean.FR__ROOT_PROCESS_INSTANCE));

      final Set resolvedPiOids = new TreeSet();

      ResultSet rsPiOids = session.executeQuery(qPiClosure, Session.NO_TIMEOUT);
      try
      {
         while (rsPiOids.next())
         {
            resolvedPiOids.add(new Long(rsPiOids.getLong(1)));
         }
      }
      catch (SQLException sqle)
      {
         throw new PublicException("Failed resolving process instance closure.", sqle);
      }
      finally
      {
         QueryUtils.closeResultSet(rsPiOids);
      }

      return Collections.unmodifiableSet(resolvedPiOids);
   }

   private void commit()
   {
      if (Parameters.instance().getBoolean(OPTION_DRY_RUN, false))
      {
         trace.info("Skipping commit as dryRun option is set.");
      }
      else
      {
         session.save(false);
      }
   }

   private void archiveProcesses(Long modelOid, Date before, long interval)
   {
      try
      {
         AndTerm predicate = new AndTerm();
         predicate.add(PT_TERMINATED_PI);
         predicate.add(Predicates.isEqual(ProcessInstanceBean.FR__OID,
               ProcessInstanceBean.FR__ROOT_PROCESS_INSTANCE));

         if (modelOid != null)
         {
            predicate.add(Predicates.isEqual(ProcessInstanceBean.FR__MODEL,
                  modelOid.longValue()));
         }
         else
         {
            predicate.add(Predicates.inList(ProcessInstanceBean.FR__MODEL,
                  QueryDescriptor
                        .from(srcSchema, ModelPersistorBean.class)
                        .selectDistinct(ModelPersistorBean.FIELD__OID)
                        .where(Predicates
                              .isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()))));
         }

         if (null != before)
         {
            predicate.add(Predicates.lessOrEqual(ProcessInstanceBean.FR__TERMINATION_TIME,
                  before.getTime()));
         }

         // TODO
/*
         if (archive)
         {
            buffer.append(" AND " + piPk.getName() + " NOT IN (SELECT " + piPk.getName()
               + " FROM ").append(getArchiveObjName(piTd.getTableName())).append(")");
         }
*/

         long tsStop = (null == before) ? Long.MAX_VALUE : before.getTime();
         do
         {
            PredicateTerm usedPredicate = predicate;
            if ( !ignoredRootPiOids.isEmpty())
            {
               usedPredicate = Predicates.andTerm(predicate,
                     splitUpOidsSubList(new ArrayList(ignoredRootPiOids),
                     ProcessInstanceBean.FR__OID, Operator.NOT_IN));
            }

            long tsStart = findMinValue(srcSchema, ProcessInstanceBean.class,
                  ProcessInstanceBean.FR__TERMINATION_TIME, usedPredicate);
            if ((tsStart <= 0) || (tsStart > tsStop))
            {
               // TODO if the loop is not run at least once, how about model/utility table synchronization ?
               // there are no processes left
               break;
            }
            archiveProcesses(modelOid, new Date(Math.min(tsStart + interval, tsStop)));
         }
         while (true);
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // ignore
         }
         throw new PublicException("Failed archiving processes", e);
      }
   }

   private void archiveProcesses(Long modelOid, Date before)
   {
      String verbPre = archive ? "move" : "delete";
      String verbPost = archive ? "Moved" : "Deleted";
      String targetDbName = archive ? "audittrail archive" : "audittrail";

      // TODO
      boolean paranoid = false;

      try
      {
         List<Long> rootPiOids = findCandidateRootProcessInstances(modelOid, before,
               paranoid);

         rootPiOids = new ArrayList(evaluateLinkedProcessInstances(rootPiOids, before));

         if (rootPiOids.isEmpty())
         {
            trace.info(MessageFormat.format(
                  "Cannot {0} process instances (no valid process instances to archive).",
                  new Object[] {archive ? "archive" : "delete"}));

         }
         else
         {

            if (null != before)
            {
               trace.info(MessageFormat.format(
                     "About to {0} {1} root process instances terminated before "
                           + DateUtils.getNoninteractiveDateFormat().format(before)
                           + " into the {2}.", new Object[] {
                           verbPre, new Integer(rootPiOids.size()), targetDbName}));
            }
            else
            {
               trace.info(MessageFormat.format(
                     "About to {0} {1} terminated root process instances into the {2}.",
                     new Object[] {verbPre, new Integer(rootPiOids.size()), targetDbName}));
            }

            archiveModels(modelOid);

            // synchronize model and utility table to ensure referential integrity for
            // archived process instances
            if (archive)
            {
               // process instance links can include processes of models other than specified by modelOid
               List<Long> findModels = findModels(rootPiOids);
               for (Long toSyncModelOid : findModels)
               {
                  if (toSyncModelOid != null)
                  {
                     synchronizeMasterTables(toSyncModelOid);
                  }
               }
            }

            long nProcessInstances = doArchiveProcesses(rootPiOids);

            if (null != before)
            {
               trace.info(MessageFormat.format(
                     "{0} {1} process instances terminated before "
                           + DateUtils.getNoninteractiveDateFormat().format(before)
                           + " into the {2}.", new Object[] {
                           verbPost, new Long(nProcessInstances), targetDbName}));
            }
            else
            {
               trace.info(MessageFormat.format(
                     "{0} {1} terminated process instances into the {2}.", new Object[] {
                           verbPost, new Long(nProcessInstances), targetDbName}));
            }
         }
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // ignore
         }
         throw new PublicException("Failed archiving processes", e);
      }
   }

   /**
    * Handles conditions introduced by process instance links.
    * filters invalid rootOids (has linked rootPI: not terminated, not terminated <code>before</code>)
    * adds valid linked rootOids.
    */
   private Set<Long> evaluateLinkedProcessInstances(final List<Long> rootPiOids, final Date before)
   {
      Set<Long> resultRootPiOids = new TreeSet<Long>(rootPiOids);

      PiLinkVisitationContext visitationContext = new PiLinkVisitationContext();

      // only evaluate pi root oids which have links
      List<Long> rootPiOidsWithLinks = findRootPiOidsHavingLinks(rootPiOids);

      for (Long oid : rootPiOidsWithLinks)
      {
         boolean hasActiveLinkHierarchy = hasActiveLinkHierarchy(resultRootPiOids, oid, before, visitationContext);
         if (hasActiveLinkHierarchy)
         {
            resultRootPiOids.remove(oid);
            ignoredRootPiOids.add(oid);
            // log ignored pi oids
            trace.info("Root process instance '" + oid
                  + "' will be ignored. Links to active processes exist. ");
         }
      }


      return resultRootPiOids;
   }

   private List<Long> findRootPiOidsHavingLinks(List<Long> rootPiOids)
   {
      QueryDescriptor qFindLinkedPis = QueryDescriptor.from(ProcessInstanceBean.class)
            .selectDistinct(ProcessInstanceBean.FIELD__ROOT_PROCESS_INSTANCE)
            .where(splitUpOidsSubList(rootPiOids, ProcessInstanceBean.FR__ROOT_PROCESS_INSTANCE));

      qFindLinkedPis.innerJoin(ProcessInstanceLinkBean.class, "PIL_PI")
            .on(ProcessInstanceBean.FR__OID,
                  ProcessInstanceLinkBean.FIELD__PROCESS_INSTANCE)
            .orOn(ProcessInstanceBean.FR__OID,
                  ProcessInstanceLinkBean.FIELD__LINKED_PROCESS_INSTANCE);

      ResultSet rsLinkedPis = session.executeQuery(qFindLinkedPis, Session.NO_TIMEOUT);

      List<Long> rootPiOidsWithLink = new LinkedList<Long>();
      try
      {
         while (rsLinkedPis.next())
         {
            long piOid = rsLinkedPis.getLong(1);
            rootPiOidsWithLink.add(piOid);
         }
      }
      catch (SQLException sqle)
      {
         throw new PublicException("Failed verifying preconditions.", sqle);
      }
      finally
      {
         QueryUtils.closeResultSet(rsLinkedPis);
      }
      return rootPiOidsWithLink;
   }

   /**
    * Traverses process instance hierarchies via links to check if there is any active
    * linked root processes or linked root processes that are terminated after the given before date.
    *
    * Also records the modelOids of visited process instances in the visitation context.
    */
   private boolean hasActiveLinkHierarchy(Set<Long> resultRootPiOids,
         Long currentRootOid, Date before, PiLinkVisitationContext visitationContext)
   {

      Boolean hasActiveLinkHierarchy = false;
      if ( !visitationContext.getVisitedRootPIs().keySet().contains(currentRootOid))
      {

         // find PI oids which have a link to the currentRootOid.
         QueryDescriptor qFindLinkedPis = QueryDescriptor.from(ProcessInstanceBean.class)
               .select(ProcessInstanceBean.FIELD__OID)
               .where(
                     Predicates.isEqual(ProcessInstanceBean.FR__ROOT_PROCESS_INSTANCE,
                           currentRootOid));

         qFindLinkedPis.innerJoin(ProcessInstanceLinkBean.class, "PIL_PI")
               .on(ProcessInstanceBean.FR__OID,
                     ProcessInstanceLinkBean.FIELD__PROCESS_INSTANCE)
               .orOn(ProcessInstanceBean.FR__OID,
                     ProcessInstanceLinkBean.FIELD__LINKED_PROCESS_INSTANCE);

         ResultSet rsLinkedPis = session.executeQuery(qFindLinkedPis, Session.NO_TIMEOUT);
         Set<Long> piOidsWithLinkToRoot = new HashSet<Long>();
         try
         {
            while (rsLinkedPis.next())
            {
               long piOid = rsLinkedPis.getLong(1);
               piOidsWithLinkToRoot.add(piOid);
            }
         }
         catch (SQLException sqle)
         {
            throw new PublicException("Failed verifying preconditions.", sqle);
         }
         finally
         {
            QueryUtils.closeResultSet(rsLinkedPis);
         }

         // for each PI that has a link, retrieve link and check linked PI hierarchies for
         // active root PIs.
         for (Long linkedPiOid : piOidsWithLinkToRoot)
         {
            if (trace.isDebugEnabled())
            {
               trace.debug("Analyzing PIs linked to/from terminated root process instances: "
                     + piOidsWithLinkToRoot);
            }
            ResultIterator<IProcessInstanceLink> links = ProcessInstanceLinkBean.findAllForProcessInstance(linkedPiOid);
            while (links.hasNext())
            {
               IProcessInstanceLink link = links.next();
               IProcessInstance processInstanceRoot = link.getProcessInstance()
                     .getRootProcessInstance();
               IProcessInstance linkedProcessInstanceRoot = link.getLinkedProcessInstance()
                     .getRootProcessInstance();

               // mark as currently evaluating
               visitationContext.getVisitedRootPIs().put(currentRootOid, null);
               if (trace.isDebugEnabled())
               {
                  trace.debug(currentRootOid + " is being visited.");
               }

               IProcessInstance current = null;
               IProcessInstance target = null;
               if (processInstanceRoot.getOID() == currentRootOid)
               {
                  // the link is FROM the rootPI hierarchy TO another hierarchy.
                  current = processInstanceRoot;
                  target = linkedProcessInstanceRoot;
               }
               else
               {
                  // the link is TO the rootPI hierarchy FROM another hierarchy.
                  current = linkedProcessInstanceRoot;
                  target = processInstanceRoot;
               }

               if ( !hasActiveLinkHierarchy)
               {
                  if ( !target.isTerminated() || before != null
                        && target.getTerminationTime() != null
                        && before.before(target.getTerminationTime()))
                  {
                     hasActiveLinkHierarchy = true;
                  }
                  else
                  {
                     hasActiveLinkHierarchy |= hasActiveLinkHierarchy(resultRootPiOids,
                           target.getOID(), before, visitationContext);
                  }
               }

               // save evaluated value
               visitationContext.getVisitedRootPIs().put(currentRootOid,
                     hasActiveLinkHierarchy);
               if (trace.isDebugEnabled())
               {
                  trace.debug(currentRootOid + " created : " + hasActiveLinkHierarchy);
               }

               if ( !hasActiveLinkHierarchy)
               {
                  // add visited linked root PIs which are sure to be in a linked
                  // terminated PI hierarchy.
                  if ( !resultRootPiOids.contains(current.getOID()))
                  {
                     resultRootPiOids.add(current.getOID());
                     trace.info("Found and added new linked root process instance '"
                           + current.getOID() + "'.");

                  }
                  if ( !resultRootPiOids.contains(target.getOID()))
                  {
                     resultRootPiOids.add(target.getOID());
                     trace.info("Found and added new linked root process instance '"
                           + target.getOID() + "'.");
                  }
               }
            }
         }
      }
      else
      {
         Boolean visitedLinkValue = visitationContext.getVisitedRootPIs().get(
               currentRootOid);
         if (visitedLinkValue == null)
         {
            // link is currently being evaluated.
            // this can happen if the links are cyclic or multiple links lead to the
            // same process instance rootOid.
            // no further evaluation of this link is disallowed to prevent endless
            // loops (cyclic) or are not simply not needed (multiple links).
            if (trace.isDebugEnabled())
            {
               trace.debug(currentRootOid + " exists : ignoring multiple link or cycle.");
            }
         }
         else
         {
            // link is evaluated
            hasActiveLinkHierarchy |= visitedLinkValue;

            if (trace.isDebugEnabled())
            {
               trace.debug(currentRootOid + " exists : " + hasActiveLinkHierarchy);
            }
         }
      }

      return hasActiveLinkHierarchy;
   }

   /**
    * This visitation context keeps track of visited nodes (rootPis) and the result of the
    * visitation to prevent cyclic transitions and reevaluation of existing results.
    */
   private class PiLinkVisitationContext
   {
      private Map<Long,Boolean> visitedRootPIs = new HashMap<Long, Boolean>();

      /**
       * @return map of visited root PI oids as key with value null if currently visiting or Boolean
       *         result of a completed evaluation.
       */
      public Map<Long, Boolean> getVisitedRootPIs()
      {
         return visitedRootPIs;
      }

   }

   private List<Long> findCandidateRootProcessInstances(Long modelOid, Date before,
         boolean paranoid)
   {
      // TODO reimplement with maximum size threshold

      final Set<Long> result = new TreeSet<Long>();

      try
      {
         QueryDescriptor query = QueryDescriptor
               .from(srcSchema, ProcessInstanceBean.class)
               .select(ProcessInstanceBean.FIELD__OID);

         AndTerm predicate = new AndTerm();
         predicate.add(PT_TERMINATED_PI);
         predicate.add(Predicates.isEqual(ProcessInstanceBean.FR__OID,
               ProcessInstanceBean.FR__ROOT_PROCESS_INSTANCE));


         if (modelOid != null)
         {
            predicate.add(Predicates.isEqual(
                  query.fieldRef(ProcessInstanceBean.FIELD__MODEL), modelOid.longValue()));
         }
         else
         {
            predicate.add(Predicates.inList(query.fieldRef(ProcessInstanceBean.FIELD__MODEL),
                  QueryDescriptor
                        .from(srcSchema, ModelPersistorBean.class)
                        .selectDistinct(ModelPersistorBean.FIELD__OID)
                        .where(Predicates
                              .isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()))));
         }

         if (null != before)
         {
            predicate.add(Predicates.lessOrEqual(
                  query.fieldRef(ProcessInstanceBean.FIELD__TERMINATION_TIME),
                  before.getTime()));
         }

         if ( !ignoredRootPiOids.isEmpty())
         {
            predicate.add(splitUpOidsSubList(new ArrayList(ignoredRootPiOids),
                  query.fieldRef(ProcessInstanceBean.FIELD__OID), Operator.NOT_IN));
         }

         if (paranoid)
         {
            predicate.add(Predicates.notInList(ProcessInstanceBean.FR__OID,
                  QueryDescriptor
                        .from(archiveSchema, ProcessInstanceBean.class, ALIAS_ARCHIVE)
                        .select(ProcessInstanceBean.FIELD__OID)));
         }

         ResultSet rs = null;

         // find all root process instances terminated before timestamp.
         try
         {
            rs = session.executeQuery(query.where(predicate));
            while (rs.next())
            {
               result.add(new Long(rs.getLong(1)));
            }
         }
         finally
         {
            QueryUtils.closeResultSet(rs);
         }
      }
      catch (Exception e)
      {
         throw new PublicException("Failed to backup processes"
            + (modelOid == null ? "" : " for model with OID " + modelOid)
            + (before == null ? "" : " terminated before " + before), e);
      }

      return new ArrayList<Long>(result);
   }

   private void synchronizeMasterTables(Long modelOid)
   {
      // synchronize model and utility table to ensure referential integrity for
      // archived process instances
      if (archive)
      {
         trace.info("About to synchronize model and utility tables.");

         Join srcPartition = new Join(srcSchema, AuditTrailPartitionBean.class, "s_prt") //
               .on(AuditTrailPartitionBean.FR__OID,
                     AuditTrailPartitionBean.FIELD__OID);
         srcPartition.where(Predicates.isEqual(
               srcPartition.fieldRef(AuditTrailPartitionBean.FIELD__OID),
               partitionOid.shortValue()));
         synchronizePkStableTables(AuditTrailPartitionBean.class, null, srcPartition);

         synchronizeUtilityTableArchive();
         synchronizeModelTableArchive(modelOid);
         synchronizeOrganizationalTableArchive(modelOid);
         synchronizePreferencesTableArchive();
         synchronizeModelTables(modelOid);
         synchronizeProcessInstanceLinkTypeArchive();
         commit();

         trace.info("Synchronized model and utility tables.");
      }
   }

   public long doArchiveProcesses(Collection rootPiOids)
   {
      long nProcessInstances = 0;

      int txBatchSize = 0;
      List stmtBatchRootPiOids = new ArrayList(stmtBatchSizeLimit);

      while ( !rootPiOids.isEmpty())
      {
         stmtBatchRootPiOids.clear();

         // build batch of root PI OIDs
         for (Iterator i = rootPiOids.iterator(); i.hasNext()
               && (stmtBatchRootPiOids.size() < stmtBatchSizeLimit) && (txBatchSize < txBatchSizeLimit);)
         {
            stmtBatchRootPiOids.add(i.next());
            ++txBatchSize;
         }

         // operate on current batch of root PI closure
         ArrayList stmtBatchPiClosure = new ArrayList(
               resolvePiClosure(stmtBatchRootPiOids));

         // reduce batch of root PI OIDs for those which have "shared" data
         // with still running asynch sub processes (workaround for bug CRNT-12508 wrt. archiving).
         List rootPiOidsWithSharedData = getRootPiPOidsSharingDataWithRunningAsynchPis(
               stmtBatchRootPiOids, stmtBatchPiClosure);
         if ( !rootPiOidsWithSharedData.isEmpty())
         {
            // Further iterations shall ignore root PIs which "share" data with currently running root PIs.
            ignoredRootPiOids.addAll(rootPiOidsWithSharedData);

            // log ignored pi oids
            logInfo("The following root process instances with shared data will be ignored: ", rootPiOidsWithSharedData.iterator());

            List tmpBatchRootPiOids = new ArrayList(stmtBatchRootPiOids);
            tmpBatchRootPiOids.removeAll(rootPiOidsWithSharedData);
            // re-evaluate PI closure due to changed RootPIs
            stmtBatchPiClosure = new ArrayList(resolvePiClosure(tmpBatchRootPiOids));
         }

         if ( !stmtBatchPiClosure.isEmpty())
         {
            if (archive)
            {
               doCopyProcessInstances(stmtBatchPiClosure);
            }
            doDeleteProcessInstances(stmtBatchPiClosure);
         }

         nProcessInstances += stmtBatchPiClosure.size();

         // forget about PIs from current batch, as they are either archived or deleted
         // (or ignored for this archiver run).
         rootPiOids.removeAll(stmtBatchRootPiOids);

         // commit, if TX batch size limit was reached or all PIs were archived
         if (rootPiOids.isEmpty() || (txBatchSize >= txBatchSizeLimit))
         {
            commit();

            txBatchSize = 0;
         }
      }
      return nProcessInstances;
   }

   private void logInfo(String message, Iterator<Long> iterator)
   {
      StringBuffer ignoredOidsMsg = new StringBuffer();
      ignoredOidsMsg.append(message);
      String DELIM = "";
      while(iterator.hasNext())
      {
         Long piOid = iterator.next();
         ignoredOidsMsg.append(DELIM).append(piOid);
         if(DELIM.length() == 0)
         {
            DELIM = ", ";
         }
      }
      trace.info(ignoredOidsMsg);
   }

   private List getRootPiPOidsSharingDataWithRunningAsynchPis(List stmtBatchRootPiOids,
         List stmtBatchPiClosure)
   {
      String propertySharedDataExist = SchemaHelper
            .getAuditTrailProperty(KernelTweakingProperties.INFINITY_DMS_SHARED_DATA_EXIST);
      boolean applySharedDataFix = StringUtils.isNotEmpty(propertySharedDataExist)
            && Boolean.parseBoolean(propertySharedDataExist);
      if (!applySharedDataFix)
      {
         return Collections.EMPTY_LIST;
      }
      Set docDataOids = findAllDocumentDataOids();
      if (docDataOids.isEmpty())
      {
         return Collections.EMPTY_LIST;
      }

      /*
         SELECT DISTINCT spi.ROOTPROCESSINSTANCE
         from
            data_value dv
            INNER JOIN process_instance spi ON (dv.processInstance = spi.oid)
            left outer join data_value dv2 on (dv.NUMBER_VALUE = dv2.NUMBER_VALUE and dv.DATA = dv2.DATA and dv.MODEL = dv2.MODEL)
            left outer join PROCESS_INSTANCE spi2 on (dv2.PROCESSINSTANCE = spi2.OID)
         where
            dv.DATA in (10, 12) and -- set RT-OIDs of document/document set datas
            dv.PROCESSINSTANCE in (1, 2) and -- all PI-OIDs of Root-PI-Closure; only ScopePIs will select data values
            dv.TYPE_KEY != -1 and -- no null values
            spi2.TERMINATIONTIME = 0  -- count for all Scope-PIs which are not terminated.
       */

      QueryDescriptor q = QueryDescriptor.from(DataValueBean.class, "dv");

      Join spiJoin = new Join(ProcessInstanceBean.class, "spi")
            .on(DataValueBean.FR__PROCESS_INSTANCE, ProcessInstanceBean.FIELD__OID);

      Join dv2Join = new Join(DataValueBean.class, "dv2")
            .on(DataValueBean.FR__NUMBER_VALUE, DataValueBean.FIELD__NUMBER_VALUE)
            .andOn(DataValueBean.FR__DATA, DataValueBean.FIELD__DATA)
            .andOn(DataValueBean.FR__MODEL, DataValueBean.FIELD__MODEL);
      dv2Join.setRequired(false);

      Join spi2Join = new Join(ProcessInstanceBean.class, "spi2")
            .on(dv2Join.fieldRef(DataValueBean.FIELD__PROCESS_INSTANCE), ProcessInstanceBean.FIELD__OID);
      spi2Join.setRequired(false);
      spi2Join.setDependency(dv2Join);

      q.getQueryExtension().addJoin(spiJoin).addJoin(dv2Join).addJoin(spi2Join);

      q.getQueryExtension().setDistinct(true);
      q.getQueryExtension().setSelection( new Column[] { spiJoin.fieldRef(
            ProcessInstanceBean.FIELD__ROOT_PROCESS_INSTANCE) });

      q.where(Predicates.andTerm(
            splitUpOidsSubList(new ArrayList(docDataOids), DataValueBean.FR__DATA),
            splitUpOidsSubList(stmtBatchPiClosure, DataValueBean.FR__PROCESS_INSTANCE),
            Predicates.notEqual(DataValueBean.FR__TYPE_KEY, -1),
            Predicates.isEqual(spi2Join.fieldRef(ProcessInstanceBean.FIELD__TERMINATION_TIME), 0)));

      List stillRunningRootPiOids = new ArrayList();
      ResultSet rs = null;
      try
      {
         rs = session.executeQuery(q);
         while (rs.next())
         {
            stillRunningRootPiOids.add(new Long(rs.getLong(1)));
         }
      }
      catch (SQLException e)
      {
         throw new PublicException(e);
      }
      finally
      {
         QueryUtils.closeResultSet(rs);
      }

      return stillRunningRootPiOids;
   }

   private int doCopyProcessInstances(List piOids)
   {
      // copy processes instances
      int nProcesses = backupPiParts(piOids, ProcessInstanceBean.class,
            ProcessInstanceBean.FIELD__OID);

      // backup process instances closure
      backupPiParts(piOids, LogEntryBean.class, LogEntryBean.FIELD__PROCESS_INSTANCE);

      backupPiParts(piOids, ProcessInstanceProperty.class,
            ProcessInstanceProperty.FIELD__OBJECT_OID);

      backupPiParts(piOids, EventBindingBean.class, EventBindingBean.FIELD__OBJECT_OID,
            PT_EVENT_BINDING_PI);

      // TODO (ab) SPI
      Set /*<Long>*/ structuredDataOids = findAllStructuredDataOids();
      if (structuredDataOids.size() != 0)
      {
         backupPiParts(piOids, StructuredDataValueBean.class, StructuredDataValueBean.FIELD__PROCESS_INSTANCE);

         backup2ndLevelPiParts(piOids,  LargeStringHolder.class,
               LargeStringHolder.FIELD__OBJECTID, StructuredDataValueBean.class,
               StructuredDataValueBean.FIELD__PROCESS_INSTANCE, Predicates.isEqual(
                     LargeStringHolder.FR__DATA_TYPE, TypeDescriptor.getTableName(StructuredDataValueBean.class)));

         backup2ndLevelPiParts(piOids, ClobDataBean.class,
               ClobDataBean.FIELD__OID, DataValueBean.class,
               DataValueBean.FIELD__NUMBER_VALUE,
               DataValueBean.FIELD__PROCESS_INSTANCE, Predicates.inList(
                     DataValueBean.FR__DATA, structuredDataOids.iterator()));
      }

      // TODO (sb) SPI
      Set /*<Long>*/docDataOids = findAllDocumentDataOids();
      if (docDataOids.size() != 0)
      {
         backup2ndLevelPiParts(piOids, LargeStringHolder.class,
               LargeStringHolder.FIELD__OBJECTID, DataValueBean.class,
               DataValueBean.FIELD__NUMBER_VALUE,
               DataValueBean.FIELD__PROCESS_INSTANCE,
               PT_STRING_DATA_IS_DMS_DOC_VALUE_RECORD, true /*work around many data values referencing single string_data entry*/);
      }

      backupPiParts(piOids, DataValueBean.class, DataValueBean.FIELD__PROCESS_INSTANCE);

      backupDvParts(piOids, LargeStringHolder.class, LargeStringHolder.FIELD__OBJECTID,
            PT_STRING_DATA_IS_DATA_VALUE_RECORD);

      backupPiParts(piOids, ActivityInstanceBean.class,
            ActivityInstanceBean.FIELD__PROCESS_INSTANCE);

      backupAiParts(piOids, ActivityInstanceProperty.class,
            ActivityInstanceProperty.FIELD__OBJECT_OID);

      backupAiParts(piOids, ActivityInstanceHistoryBean.class,
            ActivityInstanceHistoryBean.FIELD__ACTIVITY_INSTANCE);

      backupAiParts(piOids, EventBindingBean.class, EventBindingBean.FIELD__OBJECT_OID,
            PT_EVENT_BINDING_AI);

      backupAiParts(piOids, ActivityInstanceLogBean.class,
            ActivityInstanceLogBean.FIELD__ACTIVITY_INSTANCE);

      backupAiParts(piOids, LogEntryBean.class, LogEntryBean.FIELD__ACTIVITY_INSTANCE);

      backupAiParts(piOids, WorkItemBean.class, WorkItemBean.FIELD__ACTIVITY_INSTANCE);

      backupPiParts(piOids, TransitionInstanceBean.class,
            TransitionInstanceBean.FIELD__PROCESS_INSTANCE);

      backupPiParts(piOids, TransitionTokenBean.class,
            TransitionTokenBean.FIELD__PROCESS_INSTANCE);

      backupPiParts(piOids, ProcessInstanceHierarchyBean.class,
            ProcessInstanceHierarchyBean.FIELD__PROCESS_INSTANCE);

      backupPiParts(piOids, ProcessInstanceScopeBean.class,
            ProcessInstanceScopeBean.FIELD__PROCESS_INSTANCE);

      backupPiParts(piOids, ProcessInstanceLinkBean.class,
            ProcessInstanceLinkBean.FIELD__PROCESS_INSTANCE);

      backupDepParts();

      // TODO how about data clusters?

      return nProcesses;
   }

   private void deleteModel(long modelOid)
   {
      try
      {
         AdminServiceUtils.deleteModelRuntimePart(modelOid, session, true);
         AdminServiceUtils.deleteModelModelingPart(modelOid, session);

         ModelPersistorBean model = ModelPersistorBean.findByModelOID(modelOid);
         model.delete();

         commit();

         trace.info("Deleted model with OID " + modelOid + " from audittrail");
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // this is fatal, but should never happen.
         }
         throw new PublicException("Failed deleting model with OID " + modelOid, e);
      }
   }

   private long getAliveProcessInstancesCount(long modelOid)
   {
      return getAliveProcessInstancesCount(new Long(modelOid), null);
   }

   private long getAliveProcessInstancesCount(Long modelOid, List piOids)
   {
      long nonterminatedInstances = 0;

      try
      {
         ResultSet rs = null;
         try
         {
            PredicateTerm predicate = PT_ALIVE_PI;
            if (null != modelOid)
            {
               predicate = Predicates.andTerm( //
                     Predicates.isEqual( //
                           ProcessInstanceBean.FR__MODEL, modelOid.longValue()),
                     predicate);
            }
            if (null != piOids)
            {
               predicate = Predicates.andTerm( //
                     predicate,
                     Predicates.inList( //
                           ProcessInstanceBean.FR__OID, piOids));
            }

            rs = session.executeQuery(QueryDescriptor
                  .from(srcSchema, ProcessInstanceBean.class)
                  .select(Functions.rowCount())
                  .where(predicate));

            if (rs.next())
            {
               nonterminatedInstances = rs.getLong(1);
            }
            else
            {
               throw new PublicException("Failed retrieving number of nonterminated"
                     + " process instances for model with OID " + modelOid);
            }
         }
         finally
         {
            QueryUtils.closeResultSet(rs);
         }
      }
      catch (Exception e)
      {
         throw new PublicException("Failed retrieving number of nonterminated process "
               + "instances for model with OID " + modelOid, e);
      }

      return nonterminatedInstances;
   }

   private List/*<ModelHelper>*/ findModels(PredicateTerm predicate)
   {
      List unsortedModels = new ArrayList();
      try
      {
         QueryDescriptor query = QueryDescriptor
               .from(srcSchema, ModelPersistorBean.class)
               .select(
                     new FieldRef[] {
                           ModelPersistorBean.FR__OID,
                           ModelPersistorBean.FR__VALID_FROM,
                           ModelPersistorBean.FR__VALID_TO,
                           ModelPersistorBean.FR__PREDECESSOR,
                           ModelPersistorBean.FR__DISABLED});

         if (null != predicate)
         {
            query.setPredicateTerm(predicate);
         }

         ResultSet rs = null;
         try
         {
            rs = session.executeQuery(query);

            while (rs.next())
            {
               long oid = rs.getLong(1);
               long validFrom = rs.getLong(2);
               long validTo = rs.getLong(3);
               long predecessor = rs.getLong(4);
               long disabled = rs.getLong(5);

               ModelHelper helper = new ModelHelper(oid, validFrom, validTo,
                     (0 != disabled), predecessor);
               unsortedModels.add(helper);
            }
         }
         finally
         {
            QueryUtils.closeResultSet(rs);
         }
      }
      catch (Exception e)
      {
         throw new PublicException("Failed retrieving models.", e);
      }

      return unsortedModels;
   }

   private List/*<ModelHelper>*/findDeadModels()
   {
      // get all model information
      List<ModelHelper> unsortedModels = findModels(Predicates.isEqual(
            ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()));

      List<ModelHelper> deadModels = new ArrayList<ModelHelper>();

      // don't add models that are not valid yet
      // don't add models that have active or interrupted processes
      long now = Calendar.getInstance().getTime().getTime();
      for (ModelHelper model : unsortedModels)
      {
         if (model.getValidFrom() < now && getAliveProcessInstancesCount(model.getOid()) == 0)
         {
            deadModels.add(model);
         }
      }

      return deadModels;
   }

   private void archiveData(String modelId, List<String> ids, long tsBefore)
   {
      final int nData;

      try
      {
         // building common subselect, qualifying records to be deleted
         QueryDescriptor subSel = QueryDescriptor
               .from(srcSchema, DataValueBean.class)
               .select(DataValueBean.FR__OID);

         subSel.innerJoin(srcSchema, AuditTrailDataBean.class)
               .on(DataValueBean.FR__DATA, AuditTrailDataBean.FIELD__OID)
               .andOn(DataValueBean.FR__MODEL, AuditTrailDataBean.FIELD__MODEL);

         if(modelId != null)
         {
            subSel.innerJoin(srcSchema, ModelPersistorBean.class, "m")
            .on(AuditTrailDataBean.FR__MODEL, ModelPersistorBean.FIELD__OID)
            .where(Predicates.andTerm(
                  Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()),
                  Predicates.isEqual(ModelPersistorBean.FR__ID, modelId)));
         }
         else
         {
            subSel.innerJoin(srcSchema, ModelPersistorBean.class, "m")
            .on(AuditTrailDataBean.FR__MODEL, ModelPersistorBean.FIELD__OID)
            .where(Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()));
         }

         subSel.innerJoin(srcSchema, ProcessInstanceScopeBean.class)
               .on(DataValueBean.FR__PROCESS_INSTANCE, ProcessInstanceScopeBean.FIELD__SCOPE_PROCESS_INSTANCE);

         subSel.innerJoin(srcSchema, ProcessInstanceBean.class)
               .on(ProcessInstanceScopeBean.FR__ROOT_PROCESS_INSTANCE, ProcessInstanceBean.FIELD__OID);

         subSel.setPredicateTerm(Predicates.andTerm(
               Predicates.inList(AuditTrailDataBean.FR__ID, ids),
               PT_TERMINATED_PI,
               Predicates.lessOrEqual(ProcessInstanceBean.FR__TERMINATION_TIME, tsBefore)));

         // deleting overflow records for data values
         if (trace.isDebugEnabled())
         {
            trace.debug("Deleting overflow records for data " + stringLiteralList(ids) + ".");
         }

         session.executeDelete(DeleteDescriptor
               .from(srcSchema, LargeStringHolder.class)
               .where(Predicates.andTerm(
                     PT_STRING_DATA_IS_DATA_VALUE_RECORD,
                     Predicates.inList(LargeStringHolder.FR__OBJECTID, subSel))));

         // TODO: Delete "overflow" records for struct data and document/document sets.

         // deleting data values
         if (trace.isDebugEnabled())
         {
            trace.debug("Deleting data values for data " + stringLiteralList(ids) + ".");
         }

         DeleteDescriptor dvDelete = DeleteDescriptor
               .from(srcSchema, DataValueBean.class);

         dvDelete.innerJoin(srcSchema, AuditTrailDataBean.class)
               .on(DataValueBean.FR__DATA, AuditTrailDataBean.FIELD__OID)
               .andOn(DataValueBean.FR__MODEL, AuditTrailDataBean.FIELD__MODEL);

         if(modelId != null)
         {
            dvDelete.innerJoin(srcSchema, ModelPersistorBean.class, "m")
            .on(AuditTrailDataBean.FR__MODEL, ModelPersistorBean.FIELD__OID)
            .where(Predicates.andTerm(
                  Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()),
                  Predicates.isEqual(ModelPersistorBean.FR__ID, modelId)));
         }
         else
         {
            dvDelete.innerJoin(srcSchema, ModelPersistorBean.class, "m")
            .on(AuditTrailDataBean.FR__MODEL, ModelPersistorBean.FIELD__OID)
            .where(Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()));

         }

         dvDelete.innerJoin(srcSchema, ProcessInstanceScopeBean.class)
               .on(DataValueBean.FR__PROCESS_INSTANCE, ProcessInstanceScopeBean.FIELD__SCOPE_PROCESS_INSTANCE);

         dvDelete.innerJoin(srcSchema, ProcessInstanceBean.class)
               .on(ProcessInstanceScopeBean.FR__ROOT_PROCESS_INSTANCE, ProcessInstanceBean.FIELD__OID);

         nData = session.executeDelete(dvDelete
               .where(Predicates.andTerm(
                     Predicates.inList(AuditTrailDataBean.FR__ID, ids),
                     PT_TERMINATED_PI,
                     Predicates.lessOrEqual(ProcessInstanceBean.FR__TERMINATION_TIME, tsBefore))));


// ????
         // deleting appropriate slots in data cluster tables
         final DataCluster[] dClusters = RuntimeSetup.instance().getDataClusterSetup();
         for (int idx = 0; idx < dClusters.length; ++idx)
         {
            synchronizeDataCluster(dClusters[idx], ids);
         }
// ###
// ???? call to referencing models


         commit();

         trace.info(MessageFormat.format("Deleted {0} values for data {1}.",
               new Object[] {new Long(nData), stringLiteralList(ids)}));
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // ignore
         }
         throw new PublicException("Failed deleting data " + stringLiteralList(ids)
               + " for terminated processes.", e);
      }
   }

   private void deleteLogEntries(long tsBefore, PredicateTerm restriction)
   {
      DeleteDescriptor delete = DeleteDescriptor.from(srcSchema, LogEntryBean.class);

      PredicateTerm fSelectPredicate = Predicates.andTerm(Predicates.lessOrEqual(
            LogEntryBean.FR__STAMP, tsBefore), restriction);

      try
      {
         long nRecords = session.executeDelete(delete.where(fSelectPredicate));

         trace.info("Deleted " + nRecords + " log records created before "
               + DateUtils.getNoninteractiveDateFormat().format(new Date(tsBefore))
               + " from audittrail.");

         commit();
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception e1)
         {
            // ignore
         }
         throw new PublicException(MessageFormat.format(
               "Failed deleting log entries created before {}.", new Object[] {new Date(
                     tsBefore)}), e);
      }
   }

   private int backupUserSessions(long tsBefore)
   {
      try
      {
         Joins joins = createUserSessionJoins();

         // This left outer join is needed to simulate a "not exists (...)" in order
         // to prevent inserting of duplicate entries. See where clause for predicate.
         Join ausJoin = new Join(archiveSchema, UserSessionBean.class, "aus")
               .on(UserSessionBean.FR__OID, UserSessionBean.FIELD__OID);
         ausJoin.setRequired(false);
         joins.add(ausJoin);

         QueryDescriptor select = QueryDescriptor
               .from(srcSchema, UserSessionBean.class)
               .where(Predicates.andTerm(
                     Predicates.isEqual(UserRealmBean.FR__PARTITION, partitionOid.shortValue()),
                     Predicates.lessOrEqual(UserSessionBean.FR__EXPIRATION_TIME, tsBefore),
                     // is null on data joined by outer join prevents duplicates.
                     Predicates.isNull(ausJoin.fieldRef(UserSessionBean.FIELD__OID))));
         select.getQueryExtension().addJoins(joins);

         InsertDescriptor insert = InsertDescriptor.into(archiveSchema,
               UserSessionBean.class);
         insert.setFullselect(select);

         int nRecords = session.executeInsert(insert);

         return nRecords;
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception rollbackExc)
         {
            trace.error("Exception on rollback", rollbackExc);
         }

         throw new PublicException(MessageFormat.format(
               "Failed inserting user session entries expired before {0}.",
               new Object[] { new Date(tsBefore) }), e);
      }
   }

   private int deleteUserSessions(long tsBefore)
   {
      try
      {
         DeleteDescriptor delete = DeleteDescriptor
               .from(srcSchema, UserSessionBean.class);

         Joins joins = createUserSessionJoins();
         delete.getQueryExtension().addJoins(joins);

         int nRecords = session.executeDelete(delete
               .where(Predicates.andTerm(
                     Predicates.isEqual(UserRealmBean.FR__PARTITION, partitionOid.shortValue()),
                     Predicates.lessOrEqual(UserSessionBean.FR__EXPIRATION_TIME, tsBefore))));

         return nRecords;
      }
      catch (Exception e)
      {
         try
         {
            session.rollback(false);
         }
         catch (Exception rollbackExc)
         {
            trace.error("Exception on rollback", rollbackExc);
         }

         throw new PublicException(MessageFormat.format(
               "Failed deleting user session entries expired before {0}.",
               new Object[] { new Date(tsBefore) }), e);
      }
   }

   private long findMinValue(String schema, Class type, FieldRef field,
         PredicateTerm predicate)
   {
      return findMinValue(schema, type, field, new Joins(), predicate);
   }

   private long findMinValue(String schema, Class type, FieldRef field, Joins joins,
         PredicateTerm predicate)
   {
      ResultSet rs = null;
      try
      {
         QueryDescriptor query = QueryDescriptor
               .from(schema, type)
               .select(Functions.min(field))
               .where(predicate);

         if (null != joins && !joins.isEmpty())
         {
            query.getQueryExtension().addJoins(joins);
         }

         rs = session.executeQuery(query);

         return rs.next() ? rs.getLong(1) : -1;
      }
      catch (Exception e)
      {
         throw new PublicException(MessageFormat.format(
               "Failed finding minimum value for attribute ''{0}''.",
               new Object[] {field.fieldName}), e);
      }
      finally
      {
         QueryUtils.closeResultSet(rs);
      }
   }

   private long findMaxValue(String schema, Class type, FieldRef field,
         PredicateTerm predicate)
   {
      ResultSet rs = null;
      try
      {
         QueryDescriptor query = QueryDescriptor
               .from(schema, type, ALIAS_SRC)
               .select(Functions.max(field))
               .where(predicate);

         rs = session.executeQuery(query);

         return rs.next() ? rs.getLong(1) : -1;
      }
      catch (Exception e)
      {
         throw new PublicException(MessageFormat.format(
               "Failed finding maximum value for attribute ''{0}''.",
               new Object[] {field.fieldName}), e);
      }
      finally
      {
         QueryUtils.closeResultSet(rs);
      }
   }

   private List/*<Long>*/ findUnusedModels()
   {
      List result = new ArrayList();

      QueryDescriptor query = QueryDescriptor
            .from(srcSchema, ModelPersistorBean.class)
            .select(ModelPersistorBean.FR__OID);

      ResultSet rs = null;
      try
      {
         QueryDescriptor piSubQuery = QueryDescriptor
               .from(srcSchema, ProcessInstanceBean.class)
               .selectDistinct(ProcessInstanceBean.FIELD__MODEL);

         rs = session.executeQuery(query
               .where(Predicates
                     .notInList(ModelPersistorBean.FR__OID, piSubQuery)));

         while (rs.next())
         {
            result.add(new Long(rs.getLong(1)));
         }
      }
      catch (Exception e)
      {
         throw new PublicException("Failed finding unused models.", e);
      }
      finally
      {
         QueryUtils.closeResultSet(rs);
      }

      return result;
   }

   /**
    * finds distinct modelOids for given rootPiOids including modelOids of subprocesses.
    */
   private List<Long> findModels(List<Long> rootPiOids)
   {
      List<Long> result = new ArrayList<Long>();

      QueryDescriptor query = QueryDescriptor
            .from(srcSchema, ModelPersistorBean.class)
            .select(ModelPersistorBean.FR__OID);

      ResultSet rs = null;
      try
      {
         QueryDescriptor piSubQuery = QueryDescriptor
               .from(srcSchema, ProcessInstanceBean.class)
               .selectDistinct(ProcessInstanceBean.FIELD__MODEL)
               .where(splitUpOidsSubList(rootPiOids,
                     ProcessInstanceBean.FR__ROOT_PROCESS_INSTANCE));

         rs = session.executeQuery(query
               .where(Predicates
                     .inList(ModelPersistorBean.FR__OID, piSubQuery)));

         while (rs.next())
         {
            result.add(new Long(rs.getLong(1)));
         }
      }
      catch (Exception e)
      {
         throw new PublicException("Failed finding models.", e);
      }
      finally
      {
         QueryUtils.closeResultSet(rs);
      }

      return result;
   }

   private void synchronizeUtilityTableArchive()
   {
      Statement stmt = null;

      try
      {
         // TODO (kafka) are global properties with OID -1 correctly treated?
         // rsauer: yes, as property does not have a model association
         final ComparisonTerm partitionPredicate = Predicates.inList(
               PropertyPersistor.FR__PARTITION, new long[] { -1, partitionOid });
         synchronizePkInstableTables(PropertyPersistor.class, null, null, partitionPredicate);

         // deleting runtime config property, if existent, as it currently only controls
         // data cluster deployments, which are not supported in archive audit trails
         session.executeDelete(DeleteDescriptor.from(archiveSchema,
               PropertyPersistor.class).where( //
               Predicates.andTerm( //
                     Predicates.isEqual( //
                           PropertyPersistor.FR__NAME, //
                           RuntimeSetup.RUNTIME_SETUP_PROPERTY_CLUSTER_DEFINITION),
                     partitionPredicate)));

         stmt = session.getConnection().createStatement();

         long maxOid = findMaxValue(archiveSchema, PropertyPersistor.class,
               PropertyPersistor.FR__OID, null);

         // TODO
         // install archive audittrail property to enable readonly operation of the engine
         TypeDescriptor propertyTd = TypeDescriptor.get(PropertyPersistor.class);
         DBDescriptor dbDescriptor = session.getDBDescriptor();
         String syncSql = "INSERT INTO " + getArchiveObjName(propertyTd.getTableName())
               + " (" + getFieldNames(dbDescriptor, propertyTd) + ") "
               + "VALUES ("
               + maxOid + 1 + ", '" + Constants.CARNOT_ARCHIVE_AUDITTRAIL + "', 'true', 'DEFAULT', 0"
               + ", -1)";  // TODO (kafka): in future properties can belong to partitions.

         if (trace.isDebugEnabled())
         {
            trace.debug(syncSql);
         }

         stmt.executeUpdate(syncSql);
      }
      catch (SQLException e)
      {
         throw new PublicException("Failed synchronizing archived utility tables", e);
      }
      finally
      {
         QueryUtils.closeStatement(stmt);
      }
   }

   private void synchronizeModelTableArchive(Long modelOid)
   {
      try
      {
         synchronizePkStableTables(ModelPersistorBean.class, modelOid);

         // synchronize model definition fragments in string data table, constrained to
         // partition via model OID
         synchronizePkInstableTables(LargeStringHolder.class,
               LargeStringHolder.FIELD__OBJECTID, modelOid,
               PT_STRING_DATA_IS_MODEL_RECORD);

         synchronizePkStableTables(AuditTrailProcessDefinitionBean.class, modelOid);
         synchronizePkStableTables(AuditTrailDataBean.class, modelOid);
         synchronizePkStableTables(StructuredDataBean.class, modelOid);
         synchronizePkStableTables(AuditTrailActivityBean.class, modelOid);
         synchronizePkStableTables(AuditTrailTransitionBean.class, modelOid);
         synchronizePkStableTables(AuditTrailParticipantBean.class, modelOid);
         synchronizePkStableTables(AuditTrailEventHandlerBean.class, modelOid);
         synchronizePkStableTables(AuditTrailTriggerBean.class, modelOid);

// ModelRefBean
// ModelDeploymentBean

      }
      catch (Exception e)
      {
         throw new PublicException("Failed synchronizing model table archive", e);
      }
   }

   private void synchronizeOrganizationalTableArchive(Long modelOid)
   {
      try
      {
         synchronizePkStableTables(UserDomainBean.class, null);

         // join domain to reduce domain hierarchy to partition
         synchronizePkStableTables(UserDomainHierarchyBean.class, null,
               new Join(srcSchema, UserDomainBean.class, "o_ud") //
                     .on(UserDomainHierarchyBean.FR__SUPERDOMAIN, UserDomainBean.FIELD__OID) //
                     .where(Predicates.isEqual(UserDomainBean.FR__PARTITION, partitionOid.shortValue())));

         synchronizePkStableTables(UserRealmBean.class, null);

         // join realm to reduce users to partition
         synchronizePkStableTables(UserBean.class, null,
               new Join(srcSchema, UserRealmBean.class, "o_ur") //
                     .on(UserBean.FR__REALM, UserRealmBean.FIELD__OID) //
                     .where(Predicates.isEqual(UserRealmBean.FR__PARTITION, partitionOid.shortValue())));

         // join realm to reduce users properties to partition (need joins twice, once per schema)
         Join srcUJoin = new Join(srcSchema, UserBean.class, "o_usr") //
               .on(UserProperty.FR__OBJECT_OID, UserBean.FIELD__OID);
         Join srcUrJoin = new Join(srcSchema, UserRealmBean.class, "o_ur") //
               .on(srcUJoin.fieldRef(UserBean.FIELD__REALM), UserRealmBean.FIELD__OID);
         srcUrJoin.where(Predicates.isEqual(srcUrJoin.fieldRef(UserRealmBean.FIELD__PARTITION), partitionOid.shortValue()));
         srcUrJoin.setDependency(srcUJoin);

         Join tgtUJoin = new Join(archiveSchema, UserBean.class, "bo_usr") //
               .on(UserProperty.FR__OBJECT_OID, UserBean.FIELD__OID);
         Join tgtUrJoin = new Join(archiveSchema, UserRealmBean.class, "bo_ur") //
               .on(tgtUJoin.fieldRef(UserBean.FIELD__REALM), UserRealmBean.FIELD__OID);
         tgtUrJoin.where(Predicates.isEqual(tgtUrJoin.fieldRef(UserRealmBean.FIELD__PARTITION), partitionOid.shortValue()));
         tgtUrJoin.setDependency(tgtUJoin);

         Joins srcJoins = new Joins();
         srcJoins.add(srcUJoin);
         srcJoins.add(srcUrJoin);
         Joins tgtJoins = new Joins();
         tgtJoins.add(tgtUJoin);
         tgtJoins.add(tgtUrJoin);
         synchronizePkInstableTables(UserProperty.class, null, null, null, srcJoins, tgtJoins);

         synchronizeUserParticipantLinkTable(modelOid);

         synchronizePkStableTables(UserSessionBean.class, null);

         synchronizePkStableTables(UserGroupBean.class, null);

         // join user group to reduce user group properties to partition
         Join srcUgJoin = new Join(srcSchema, UserGroupBean.class, "o_ug") //
               .on(UserGroupProperty.FR__OBJECT_OID, UserGroupBean.FIELD__OID);
         Join tgtUgJoin = new Join(archiveSchema, UserGroupBean.class, "bo_ug") //
               .on(UserGroupProperty.FR__OBJECT_OID, UserGroupBean.FIELD__OID);
         synchronizePkInstableTables(UserGroupProperty.class, null, null, null,
               srcUgJoin.where(Predicates.isEqual(srcUgJoin.fieldRef(UserGroupBean.FIELD__PARTITION), partitionOid.shortValue())),
               tgtUgJoin.where(Predicates.isEqual(tgtUgJoin.fieldRef(UserGroupBean.FIELD__PARTITION), partitionOid.shortValue())));

         // join user group to reduce links to partition
         srcUgJoin = new Join(srcSchema, UserGroupBean.class, "o_ug") //
               .on(UserUserGroupLink.FR__USER_GROUP, UserGroupBean.FIELD__OID);
         tgtUgJoin = new Join(archiveSchema, UserGroupBean.class, "bo_ug") //
               .on(UserUserGroupLink.FR__USER_GROUP, UserGroupBean.FIELD__OID);
         synchronizePkInstableTables(UserUserGroupLink.class, null, null, null,
               srcUgJoin.where(Predicates.isEqual(srcUgJoin.fieldRef(UserGroupBean.FIELD__PARTITION), partitionOid.shortValue())),
               tgtUgJoin.where(Predicates.isEqual(tgtUgJoin.fieldRef(UserGroupBean.FIELD__PARTITION), partitionOid.shortValue())));
      }
      catch (Exception e)
      {
         throw new PublicException("Failed synchronizing organizational table archive", e);
      }
   }

   private void synchronizeUserParticipantLinkTable(Long modelOid) throws SQLException
   {
      Join sourceParticipantsJoin = new Join(srcSchema, AuditTrailParticipantBean.class, "p")
         .on(UserParticipantLink.FR__PARTICIPANT, AuditTrailParticipantBean.FIELD__OID);
      Join sourceModelsJoin = new Join(srcSchema, ModelPersistorBean.class, "m")
         .on(sourceParticipantsJoin.fieldRef(AuditTrailParticipantBean.FIELD__MODEL), ModelPersistorBean.FIELD__OID);
      Joins sourceJoins = new Joins().add(sourceParticipantsJoin).add(sourceModelsJoin);

      Join targetParticipantsJoin = new Join(archiveSchema, AuditTrailParticipantBean.class, "p")
         .on(UserParticipantLink.FR__PARTICIPANT, AuditTrailParticipantBean.FIELD__OID);
      Join targetModelsJoin = new Join(archiveSchema, ModelPersistorBean.class, "m")
         .on(targetParticipantsJoin.fieldRef(AuditTrailParticipantBean.FIELD__MODEL), ModelPersistorBean.FIELD__OID);
      Joins targetJoins = new Joins().add(targetParticipantsJoin).add(targetModelsJoin);

      if (modelOid == null)
      {
         PredicateTerm sourcePredicate = Predicates.isEqual(
               sourceModelsJoin.fieldRef(ModelPersistorBean.FIELD__PARTITION), partitionOid.shortValue());

         PredicateTerm targetPredicate = Predicates.isEqual(
               targetModelsJoin.fieldRef(ModelPersistorBean.FIELD__PARTITION), partitionOid.shortValue());

         synchronizePkInstableTables(UserParticipantLink.class, null, null, sourcePredicate, targetPredicate, sourceJoins, targetJoins);
      }
      else
      {
         Join sourceModels2Join = new Join(srcSchema, ModelPersistorBean.class, "m2")
            .on(sourceModelsJoin.fieldRef(ModelPersistorBean.FIELD__ID), ModelPersistorBean.FIELD__ID);
         sourceJoins.add(sourceModels2Join);
         PredicateTerm sourcePredicate = Predicates.andTerm(
            Predicates.isEqual(sourceModelsJoin.fieldRef(ModelPersistorBean.FIELD__PARTITION), partitionOid.shortValue()),
            Predicates.isEqual(sourceModels2Join.fieldRef(ModelPersistorBean.FIELD__OID), modelOid));

         Join targetModels2Join = new Join(archiveSchema, ModelPersistorBean.class, "m2")
            .on(targetModelsJoin.fieldRef(ModelPersistorBean.FIELD__ID), ModelPersistorBean.FIELD__ID);
         targetJoins.add(targetModels2Join);
         PredicateTerm targetPredicate = Predicates.andTerm(
            Predicates.isEqual(targetModelsJoin.fieldRef(ModelPersistorBean.FIELD__PARTITION), partitionOid.shortValue()),
            Predicates.isEqual(targetModels2Join.fieldRef(ModelPersistorBean.FIELD__OID), modelOid));

         synchronizePkInstableTables(UserParticipantLink.class, null, null, sourcePredicate, targetPredicate, sourceJoins, targetJoins);
      }
   }

   private int doDeleteProcessInstances(List piOids)
   {
      if(piOids.isEmpty())
      {
         return 0;
      }
      // TODO rsauer: delegate to ProcessInstanceUtils
      deletePiParts(piOids, TransitionTokenBean.class,
            TransitionTokenBean.FR__PROCESS_INSTANCE);

      deletePiParts(piOids, TransitionInstanceBean.class,
            TransitionInstanceBean.FR__PROCESS_INSTANCE);

      deleteAiParts(piOids, LogEntryBean.class, LogEntryBean.FR__ACTIVITY_INSTANCE);

      deleteAiParts(piOids, ActivityInstanceLogBean.class,
            ActivityInstanceLogBean.FR__ACTIVITY_INSTANCE);

      deleteAiParts(piOids, ActivityInstanceHistoryBean.class,
            ActivityInstanceHistoryBean.FR__ACTIVITY_INSTANCE);

      deleteAiParts(piOids, EventBindingBean.class, EventBindingBean.FR__OBJECT_OID,
            PT_EVENT_BINDING_AI);

      deleteAiParts(piOids, ActivityInstanceProperty.class,
            ActivityInstanceProperty.FR__OBJECT_OID);

      deleteAiParts(piOids, WorkItemBean.class, WorkItemBean.FR__ACTIVITY_INSTANCE);

      deletePiParts(piOids, ActivityInstanceBean.class,
            ActivityInstanceBean.FR__PROCESS_INSTANCE);

      // TODO (ab) SPI
      Set /*<Long>*/ structuredDataOids = findAllStructuredDataOids();
      if (structuredDataOids.size() != 0)
      {
         delete2ndLevelPiParts(piOids, LargeStringHolder.class,
               LargeStringHolder.FR__OBJECTID, StructuredDataValueBean.class,
               StructuredDataValueBean.FR__PROCESS_INSTANCE, Predicates.isEqual(
                     LargeStringHolder.FR__DATA_TYPE,
                     TypeDescriptor.getTableName(StructuredDataValueBean.class)));

         deletePiParts(piOids, StructuredDataValueBean.class, StructuredDataValueBean.FR__PROCESS_INSTANCE);

         delete2ndLevelPiParts(piOids, ClobDataBean.class,
               ClobDataBean.FR__OID, DataValueBean.class,
               DataValueBean.FIELD__NUMBER_VALUE,
               DataValueBean.FR__PROCESS_INSTANCE, Predicates.inList(
                     DataValueBean.FR__DATA, structuredDataOids.iterator()), session);
      }

      // TODO (sb) SPI
      Set /*<Long>*/docDataOids = findAllDocumentDataOids();
      if (docDataOids.size() != 0)
      {
         delete2ndLevelPiParts(piOids, LargeStringHolder.class,
               LargeStringHolder.FR__OBJECTID, DataValueBean.class,
               DataValueBean.FIELD__NUMBER_VALUE, DataValueBean.FR__PROCESS_INSTANCE,
               PT_STRING_DATA_IS_DMS_DOC_VALUE_RECORD, session);
      }

      deleteDvParts(piOids, LargeStringHolder.class, LargeStringHolder.FR__OBJECTID,
            PT_STRING_DATA_IS_DATA_VALUE_RECORD);

      deletePiParts(piOids, DataValueBean.class, DataValueBean.FR__PROCESS_INSTANCE);

      deletePiParts(piOids, LogEntryBean.class, LogEntryBean.FR__PROCESS_INSTANCE);

      deletePiParts(piOids, EventBindingBean.class, EventBindingBean.FR__OBJECT_OID,
            PT_EVENT_BINDING_PI);

      deletePiParts(piOids, ProcessInstanceProperty.class,
            ProcessInstanceProperty.FR__OBJECT_OID);

      deletePiParts(piOids, ProcessInstanceHierarchyBean.class,
            ProcessInstanceHierarchyBean.FR__PROCESS_INSTANCE);

      deletePiParts(piOids, ProcessInstanceScopeBean.class,
            ProcessInstanceScopeBean.FR__PROCESS_INSTANCE);

      deletePiParts(piOids, ProcessInstanceLinkBean.class,
            ProcessInstanceLinkBean.FR__PROCESS_INSTANCE);

      // finally deleting rows from data clusters
      final DataCluster[] dClusters = RuntimeSetup.instance().getDataClusterSetup();

      for (int idx = 0; idx < dClusters.length; ++idx)
      {
         final DataCluster dCluster = dClusters[idx];

         Statement stmt = null;
         try
         {
            stmt = session.getConnection().createStatement();
            DBDescriptor dbDescriptor = session.getDBDescriptor();
            StringBuffer buffer = new StringBuffer(100 + piOids.size() * 10);
            buffer.append("DELETE FROM ").append(getSrcObjName(dbDescriptor.quoteIdentifier(dCluster.getTableName())))
                  .append(" WHERE ").append(getInClause(dCluster.getProcessInstanceColumn(), piOids));

            if (trace.isDebugEnabled())
            {
               trace.debug(buffer);
            }
            stmt.executeUpdate(buffer.toString());
         }
         catch (SQLException e)
         {
            throw new PublicException(MessageFormat.format(
                  "Failed deleting entries from data cluster table ''{0}''. Reason: {1}.",
                  new Object[] {getSrcObjName(dCluster.getTableName()), e.getMessage()}), e);
         }
         finally
         {
            QueryUtils.closeStatement(stmt);
         }
      }

      return deletePiParts(piOids, ProcessInstanceBean.class,
            ProcessInstanceBean.FR__OID, null);
   }

   protected static String getInClause(String columnName, List inEntries)
   {
      StringBuffer buffer = new StringBuffer();
      if(!CollectionUtils.isEmpty(inEntries))
      {
         int fromIdx = 0;
         int toIdx = inEntries.size() < SQL_IN_CHUNK_SIZE ? inEntries.size() : SQL_IN_CHUNK_SIZE;
         List entries = inEntries.subList(fromIdx, toIdx);
         String TOKEN = columnName + " IN (";
         while(!entries.isEmpty())
         {
            buffer.append(TOKEN).append(StringUtils.join(entries.iterator(), ", ")).append(")");
            TOKEN = " OR " + columnName + " IN (";
            fromIdx = fromIdx + SQL_IN_CHUNK_SIZE;
            toIdx = inEntries.size() >= toIdx + SQL_IN_CHUNK_SIZE ? toIdx + SQL_IN_CHUNK_SIZE : inEntries.size();
            if(fromIdx < toIdx)
            {
               entries = inEntries.subList(fromIdx, toIdx);
            }
            else
            {
               entries = Collections.EMPTY_LIST;
            }
         }
      }
      return buffer.toString();
   }

   private static Set<Long> findAllStructuredDataOids()
   {
      return findAllDataOids(new Functor()
      {
         public Object execute(Object source)
         {
            IData data = (IData) source;
            String dataTypeId = data.getType().getId();

            boolean result =
               StructuredTypeRtUtils.isStructuredType(dataTypeId) ||
               StructuredTypeRtUtils.isDmsType(dataTypeId);

            return result ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   public static Set<Long> findAllDocumentDataOids()
   {
      return findAllDataOids(new Functor()
      {
         public Object execute(Object source)
         {
            IData data = (IData) source;
            String evalClassName = (String) data.getType().getAttribute(
                  PredefinedConstants.EVALUATOR_CLASS_ATT);
            boolean result =
               org.eclipse.stardust.engine.core.compatibility.extensions.dms.data.DocumentEvaluator.class.getName().equals(evalClassName) ||
               org.eclipse.stardust.engine.core.compatibility.extensions.dms.data.DocumentSetEvaluator.class.getName().equals(evalClassName);

            return result ? Boolean.TRUE : Boolean.FALSE;
         }
      });
   }

   private static Set<Long> findAllDataOids(Functor functor)
   {
      Set<Long> dataOids = new TreeSet();
      for (Iterator modelItr = ModelManagerFactory.getCurrent().getAllModels(); modelItr
            .hasNext();)
      {
         IModel model = (IModel) modelItr.next();
         ModelElementList allData = model.getData();
         for (int i = 0, len = allData.size(); i < len; i++)
         {
            IData data = (IData) allData.get(i);
            final Boolean match = (Boolean) functor.execute(data);
            if (match.booleanValue())
            {
               dataOids.add(ModelManagerFactory.getCurrent().getRuntimeOid(data));
            }
         }
      }
      return dataOids;
   }

   private static void findAllDataIds(Map<String, List<String>> values) throws PublicException
   {
      for(Map.Entry<String, List<String>> entry : values.entrySet())
      {
         String modelID = entry.getKey();
         List<String> value = entry.getValue();

         Set<String> dataIds = CollectionUtils.newSet();
         List<IModel> models = null;
         if(modelID.equals(Integer.toString(PredefinedConstants.ALL_MODELS)))
         {
            models = ModelManagerFactory.getCurrent().getModels();
         }
         else
         {
            models = ModelManagerFactory.getCurrent().getModelsForId(modelID);
            if(models.isEmpty())
            {
               throw new PublicException(
                     "No model with id '" + modelID + "'.");
            }
         }

         for (IModel model : models)
         {
            ModelElementList dataList = model.getData();
            for (int i = 0; i < dataList.size(); i++)
            {
               IData data = (IData) dataList.get(i);
               dataIds.add(data.getId());
            }
         }

         for(String dataID : value)
         {
            if (!dataIds.contains(dataID))
            {
               throw new PublicException(
                     "Cannot delete data for nonexisting data id '" + dataID + "'.");
            }
         }
      }
   }

   private void synchronizeDataCluster(DataCluster dataCluster, List<String> dataIds)
   {
      final String dcTableName = getSrcObjName(dataCluster.getTableName());

      TypeDescriptor dvType = TypeDescriptor.get(DataValueBean.class);
      final String dvTableName = getSrcObjName(dvType.getTableName());
      final String pkDV = dvType.getPkFields()[PK_OID].getName();

      TypeDescriptor mType = TypeDescriptor.get(ModelPersistorBean.class);
      final String mTableName = getSrcObjName(mType.getTableName());

      Statement stmt = null;

      Set ids = (null != dataIds)
            ? new HashSet(dataIds)
            : Collections.EMPTY_SET;

      try
      {
         stmt = session.getConnection().createStatement();
         // TODO (ab) in the case of structured data, several DataSlots can exist for one data,
         // it would be more efficient to make one update for all DataSlots of one data
         for (DataSlot dataSlot : dataCluster.getAllSlots())
         {
            if (ids.contains(dataSlot.getQualifiedDataId()))
            {
               StringBuffer buffer = new StringBuffer(400);
               buffer.append("UPDATE ").append(dcTableName)
                     .append(" SET ")
                     .append(dataSlot.getOidColumn()).append("=NULL")
                     .append(", ").append(dataSlot.getTypeColumn()).append("=NULL");

               if ( !StringUtils.isEmpty(dataSlot.getNValueColumn()))
               {
                  buffer.append(", ").append(dataSlot.getNValueColumn()).append("=NULL");
               }
               if ( !StringUtils.isEmpty(dataSlot.getSValueColumn()))
               {
                  buffer.append(", ").append(dataSlot.getSValueColumn()).append("=NULL");
               }

               buffer.append(" WHERE NOT EXISTS (SELECT 'x'")//
                     .append("  FROM ").append(dvTableName).append(" dv")//
                     .append(", ").append(mTableName).append(" m")//
                     .append(" WHERE dv.").append(pkDV).append(" = ")//
                     .append(dcTableName).append(".").append(dataSlot.getOidColumn())//
                     .append(" AND m.").append(ModelPersistorBean.FIELD__ID).append(" = ")
                     .append("'").append(dataSlot.getModelId()).append("'")//
                     .append(" AND dv.").append(DataValueBean.FIELD__MODEL).append(" = m.").append(ModelPersistorBean.FIELD__OID)
                     .append(" )");

               if (trace.isDebugEnabled())
               {
                  trace.debug(buffer.toString());
               }
               stmt.executeUpdate(buffer.toString());
            }
         }
      }
      catch (SQLException x)
      {
         String message = "Couldn't synchronize data cluster table '" + dcTableName
               + "'." + " Reason: " + x.getMessage();
         trace.error(message, x);
         throw new PublicException(message, x);
      }
      finally
      {
         QueryUtils.closeStatement(stmt);
      }
   }

   private int deletePiParts(List piOids, Class partType, FieldRef fkPiField)
   {
      return deletePiParts(piOids, partType, fkPiField, null);
   }

   private int deletePiParts(List piOids, Class partType, FieldRef fkPiField,
         PredicateTerm restriction)
   {
      PredicateTerm piPredicate = splitUpOidsSubList(piOids, fkPiField);

      PredicateTerm predicate = (null != restriction)
            ? Predicates.andTerm(piPredicate, restriction)
            : piPredicate;

      // delete lock rows

      TypeDescriptor tdType = TypeDescriptor.get(partType);
      if (isLockingEnabled() && tdType.isDistinctLockTableName())
      {
         Assert.condition(1 == tdType.getPkFields().length,
               "Lock-tables are not supported for types with compound PKs.");

         DeleteDescriptor delete = DeleteDescriptor
               .fromLockTable(srcSchema, partType);

         String partOid = tdType.getPkFields()[PK_OID].getName();
         PredicateTerm lockRowsPredicate = Predicates
               .inList(delete.fieldRef(partOid), QueryDescriptor
                     .from(partType)
                     .select(partOid)
                     .where(predicate));

         session.executeDelete(delete
               .where(lockRowsPredicate));
      }

      // delete data rows

      DeleteDescriptor delete = DeleteDescriptor
            .from(srcSchema, partType)
            .where(predicate);

      return session.executeDelete(delete);
   }

   private void deleteAiParts(List piOids, Class partType, FieldRef fkAiField)
   {
      deleteAiParts(piOids, partType, fkAiField, null);
   }

   private void deleteAiParts(List piOids, Class partType, FieldRef fkAiField,
         PredicateTerm restriction)
   {
      delete2ndLevelPiParts(piOids, partType, fkAiField, ActivityInstanceBean.class,
            ActivityInstanceBean.FR__PROCESS_INSTANCE, restriction);
   }

   private void deleteDvParts(List piOids, Class partType, FieldRef fkDvField,
         PredicateTerm restriction)
   {
      delete2ndLevelPiParts(piOids, partType, fkDvField, DataValueBean.class,
            DataValueBean.FR__PROCESS_INSTANCE, restriction);
   }

   private int delete2ndLevelPiParts(List piOids, Class partType, FieldRef fkPiPartField,
         Class piPartType, FieldRef piOidField, PredicateTerm restriction)
   {
      TypeDescriptor tdPiPart = TypeDescriptor.get(piPartType);
      return delete2ndLevelPiParts(piOids, partType, fkPiPartField, piPartType, tdPiPart.getPkFields()[0].getName(),  piOidField, restriction, session);
   }

   private int delete2ndLevelPiParts(List piOids, Class partType, FieldRef fkPiPartField,
         Class piPartType, String piPartPkName, FieldRef piOidField, PredicateTerm restriction, Session session)
   {

      PredicateTerm predicate = Predicates.andTerm(
            splitUpOidsSubList(piOids, piOidField), (null != restriction)
                  ? restriction
                  : Predicates.TRUE);

      // delete lock rows
      TypeDescriptor tdType = TypeDescriptor.get(partType);

      if (isLockingEnabled() && tdType.isDistinctLockTableName())
      {
         Assert.condition(1 == tdType.getPkFields().length,
               "Lock-tables are not supported for types with compound PKs.");

         String partOid = tdType.getPkFields()[PK_OID].getName();

         QueryDescriptor lckSubselect = QueryDescriptor
               .from(srcSchema, partType)
               .select(partOid);

         lckSubselect.innerJoin(srcSchema, piPartType)
               .on(fkPiPartField, piPartPkName);

         DeleteDescriptor delete = DeleteDescriptor.fromLockTable(srcSchema, partType);
         delete.where(Predicates.inList(delete.fieldRef(partOid), lckSubselect.where(predicate)));

         session.executeDelete(delete);
      }

      // delete data rows

      DeleteDescriptor delete = DeleteDescriptor.from(srcSchema, partType);

      delete.innerJoin(srcSchema, piPartType)
            .on(fkPiPartField, piPartPkName);

      return session.executeDelete(delete
            .where(predicate));
   }

   private boolean isLockingEnabled()
   {
      return session.isUsingLockTables();
   }

   private int backupPiParts(List piOids, Class partType,
         String fkPiFieldName)
   {
      return backupPiParts(piOids, partType, fkPiFieldName, null);
   }

   private int backupPiParts(List piOids, Class partType,
         String fkPiFieldName, PredicateTerm restriction)
   {
      QueryDescriptor fSelect = QueryDescriptor
            .from(srcSchema, partType, ALIAS_SRC);

      // restricting to explicit list of PI OIDs
      final PredicateTerm piOidPredicate = splitUpOidsSubList(piOids, fSelect
            .fieldRef(fkPiFieldName));

      // merging additional filter, if needed
      final PredicateTerm fSelectPredicate = (null == restriction)
            ? piOidPredicate
            : Predicates.andTerm(restriction, piOidPredicate);

      try
      {
         return session.executeInsert(InsertDescriptor
               .into(archiveSchema, partType)
               .fullselect(fSelect
                     .where(fSelectPredicate)));
      }
      catch (Exception e)
      {
         throw new PublicException(
               "Failed archiving entries from "
                     + fSelect.getTableName()
                     + " included in transitive closure of already archived process instances.",
               e);
      }
   }

   public static PredicateTerm splitUpOidsSubList(List piOids, FieldRef fieldRef)
   {
      return splitUpOidsSubList(piOids, fieldRef, Operator.IN);
   }

   private static PredicateTerm splitUpOidsSubList(List piOids, FieldRef fieldRef,
         Operator.Binary op)
   {
      final PredicateTerm piOidPredicate;
      List<List<Long>> splitList = CollectionUtils.split(piOids, SQL_IN_CHUNK_SIZE);
      if (splitList.size() == 1)
      {
         if (Operator.NOT_IN.equals(op))
         {
            piOidPredicate = Predicates.notInList(fieldRef, piOids);
         }
         else // if(Operator.IN.equals(op))
         {
            piOidPredicate = Predicates.inList(fieldRef, piOids);
         }
      }
      else
      {
         MultiPartPredicateTerm mpTerm;
         if (Operator.NOT_IN.equals(op))
         {
            mpTerm = new AndTerm();
         }
         else // if(Operator.IN.equals(op))
         {
            mpTerm = new OrTerm();
         }

         for (List<Long> list : splitList)
         {
            if (Operator.NOT_IN.equals(op))
            {
               mpTerm.add(Predicates.notInList(fieldRef, list));
            }
            else // if(Operator.IN.equals(op))
            {
               mpTerm.add(Predicates.inList(fieldRef, list));
            }
         }

         piOidPredicate = mpTerm;
      }
      return piOidPredicate;
   }

   private void backupAiParts(List piOids, Class targetType, String fkPiPartFieldName)
   {
      backupAiParts(piOids, targetType, fkPiPartFieldName, null);
   }

   private void backupAiParts(List piOids, Class targetType, String fkPiPartFieldName,
         PredicateTerm restriction)
   {
      backup2ndLevelPiParts(piOids, targetType, fkPiPartFieldName,
            ActivityInstanceBean.class, ActivityInstanceBean.FIELD__PROCESS_INSTANCE,
            restriction);
   }

   private void backupDvParts(List piOids, Class targetType, String fkPiPartFieldName,
         PredicateTerm restriction)
   {
      backup2ndLevelPiParts(piOids, targetType, fkPiPartFieldName, DataValueBean.class,
            ActivityInstanceBean.FIELD__PROCESS_INSTANCE, restriction);
   }

   private void backupDepParts()
   {
      Join depJoin = new Join(archiveSchema, DepartmentBean.class, "depJoin").on(
            DepartmentBean.FR__OID, DepartmentBean.FIELD__OID);
      depJoin.setRequired(false);
      QueryDescriptor select = QueryDescriptor.from(srcSchema, DepartmentBean.class)
            .where(Predicates.isNull(depJoin.fieldRef(DepartmentBean.FIELD__OID)));
      select.getQueryExtension().addJoin(depJoin);
      InsertDescriptor insert = InsertDescriptor
            .into(archiveSchema, DepartmentBean.class);
      insert.setFullselect(select);
      session.executeInsert(insert);

      Join dhJoin = new Join(archiveSchema, DepartmentHierarchyBean.class, "dhJoin").on(
            DepartmentHierarchyBean.FR__SUBDEPARTMENT, DepartmentHierarchyBean.FIELD__SUBDEPARTMENT);
      dhJoin.setRequired(false);
      QueryDescriptor dhSelect = QueryDescriptor.from(srcSchema,
            DepartmentHierarchyBean.class).where(
            Predicates.isNull(dhJoin.fieldRef(DepartmentHierarchyBean.FIELD__SUBDEPARTMENT)));
      dhSelect.getQueryExtension().addJoin(dhJoin);
      InsertDescriptor dhInsert = InsertDescriptor.into(archiveSchema,
            DepartmentHierarchyBean.class);
      dhInsert.setFullselect(dhSelect);
      session.executeInsert(dhInsert);
   }

   private void backup2ndLevelPiParts(List piOids, Class partType,
         String fkPiPartFieldName, Class piPartType, String fkPiFieldName, PredicateTerm restriction)
   {
      backup2ndLevelPiParts(piOids, partType, fkPiPartFieldName, piPartType, IdentifiablePersistentBean.FIELD__OID, fkPiFieldName, restriction);
   }

   private void backup2ndLevelPiParts(List piOids, Class partType,
         String fkPiPartFieldName, Class piPartType, String piPartTypeFieldName,
         String fkPiFieldName, PredicateTerm restriction)
   {
      backup2ndLevelPiParts(piOids, partType, fkPiPartFieldName, piPartType,
            piPartTypeFieldName, fkPiFieldName, restriction, false);
   }

   private void backup2ndLevelPiParts(List piOids, Class partType,
         String fkPiPartFieldName, Class piPartType, String piPartTypeFieldName,
         String fkPiFieldName, PredicateTerm restriction, boolean distinctSelect)
   {
      // fullselect from 2nd level part
      QueryDescriptor fSelect = QueryDescriptor.from(srcSchema, partType, ALIAS_SRC);
      fSelect.getQueryExtension().setDistinct(distinctSelect);

      // join PI part

      Join piPartJoin = fSelect
            .innerJoin(piPartType)
            .on(fSelect.fieldRef(fkPiPartFieldName), piPartTypeFieldName);

      // restricting to explicit list of PI OIDs
      final PredicateTerm piOidPredicate = splitUpOidsSubList(piOids, piPartJoin
            .fieldRef(fkPiFieldName));

      // merging additional filter, if needed
      final PredicateTerm fSelectPredicate = (null == restriction)
            ? piOidPredicate
            : Predicates.andTerm(restriction, piOidPredicate);

      try
      {
         session.executeInsert(InsertDescriptor
               .into(archiveSchema, partType)
               .fullselect(fSelect
                     .where(fSelectPredicate)));
      }
      catch (Exception e)
      {
         throw new PublicException(
               "Failed archiving entries from "
                     + fSelect.getTableName()
                     + " included in transitive closure of already archived process instances.",
               e);
      }
   }

   private String getSrcObjName(String objectName)
   {
      return getSrcObjName(objectName, null);
   }

   private String getSrcObjName(String objectName, String alias)
   {
      return getQualifiedTableRef(srcSchema, objectName, alias);
   }

   private String getArchiveObjName(String objectName)
   {
      return getArchiveObjName(objectName, null);
   }

   private String getArchiveObjName(String objectName, String alias)
   {
      return getQualifiedTableRef(archiveSchema, objectName, alias);
   }

   private String getQualifiedTableRef(String schemaName, String objectName, String alias)
   {
      StringBuffer result = new StringBuffer(100);
      result.append(getQualifiedName(schemaName, objectName));
      if ( !StringUtils.isEmpty(alias))
      {
         result.append(" ").append(alias);
      }
      return result.toString();
   }

   private void synchronizePkStableTables(Class type, Long modelOid)
   {
      synchronizePkStableTables(type, modelOid, (Joins) null);
   }

   private void synchronizePkStableTables(Class type, Long modelOid, Join join)
   {
      Joins joins = new Joins();
      if (null != join)
      {
         joins.add(join);
      }
      synchronizePkStableTables(type, modelOid, joins);
   }

   private void synchronizePkStableTables(Class type, Long modelOid, Joins joins)
   {
      TypeDescriptor tdType = TypeDescriptor.get(type);
      DBDescriptor dbDescriptor = session.getDBDescriptor();

      String pkOID = tdType.getPkFields()[PK_OID].getName();
      String pkModel = tdType.getPkFields().length > 1 ? tdType.getPkFields()[PK_MODEL].getName() : null;
      String fieldPartition = tdType.hasField(FIELD_PARTITION) ? dbDescriptor.quoteIdentifier(FIELD_PARTITION) : null;
      String tableName = dbDescriptor.quoteIdentifier(tdType.getTableName());

      if (null == joins)
      {
         joins = new Joins();
      }

      StringBuffer updBuf = new StringBuffer(1000);
      updBuf.append("UPDATE ");

      String updTabReference = "bo";
      String updTabRefPrefix = "bo.";
      if (DBMSKey.SYBASE.equals(dbDescriptor.getDbmsKey())
            || DBMSKey.MSSQL8.equals(dbDescriptor.getDbmsKey()))
      {
         updTabReference = getArchiveObjName(tableName);
         updTabRefPrefix = updTabReference + DOT;
         updBuf.append(updTabReference);
      }
      else
      {
         updBuf.append(getArchiveObjName(tableName)).append(SPACE).append(updTabReference);
      }

      updBuf.append(  " SET ");
      boolean update = false;
      if (dbDescriptor.supportsMultiColumnUpdates())
      {
         update = true;

         updBuf.append("(").append(getFieldNames(dbDescriptor, tdType)).append(") = ")
               .append("(SELECT ").append(getFieldNames(dbDescriptor, tdType))
               .append(" FROM ").append(getSrcObjName(tableName)).append(" o ")
               .append(" WHERE ").append(updTabRefPrefix).append(pkOID).append(" = o.").append(pkOID);
         if (pkModel != null)
         {
            updBuf.append(" AND o.").append(pkModel).append(" = ").append((modelOid == null
                  ? updTabRefPrefix + pkModel
                  : modelOid.toString()));
         }
         // TODO (kafka, rsauer) remove predicate, as it is implicitly satisfied by OID predicate?
         if(null != fieldPartition)
         {
            updBuf.append(" AND o.").append(fieldPartition).append(" = ").append(updTabRefPrefix).append(fieldPartition);
         }
         updBuf.append(")");
      }
      else
      {
         String joinToken = "";

         for (Iterator i = tdType.getPersistentFields().iterator(); i.hasNext();)
         {
            FieldDescriptor field = (FieldDescriptor) i.next();
            if ( !tdType.isPkField(field.getField()))
            {
               update = true;

               updBuf.append(joinToken);
               joinToken = ", ";

               appendPkStableUpdateClause(modelOid, pkOID, fieldPartition, pkModel,
                     tableName, updBuf,
                     updTabRefPrefix + dbDescriptor.quoteIdentifier(field.getField().getName()),
                     "o." + dbDescriptor.quoteIdentifier(field.getField().getName()),
                     updTabReference);
            }
         }
         for (Iterator i = tdType.getLinks().iterator(); i.hasNext();)
         {
            LinkDescriptor link = (LinkDescriptor) i.next();

            updBuf.append(joinToken);
            joinToken = ", ";

            appendPkStableUpdateClause(modelOid, pkOID, fieldPartition, pkModel,
                  tableName, updBuf,
                  updTabRefPrefix + dbDescriptor.quoteIdentifier(link.getField().getName()),
                  "o." + dbDescriptor.quoteIdentifier(link.getField().getName()),
                  updTabReference);
         }
      }

      updBuf.append(" WHERE EXISTS (")
            .append(    "SELECT * FROM ").append(getSrcObjName(tableName)).append(" o ");

      applyJoinsOnFromClause(dbDescriptor, joins, updBuf);

      updBuf.append(     " WHERE o.").append(pkOID).append(" = ").append(updTabRefPrefix).append(pkOID);
      if (pkModel != null)
      {
         updBuf.append(         " AND o." + pkModel + " = " + (modelOid == null ? updTabRefPrefix + pkModel : modelOid.toString()));
         if (null == modelOid)
         {
            updBuf.append(" AND o." + pkModel + " IN (SELECT m." + ModelPersistorBean.FIELD__OID
                  + "  FROM " + getSrcObjName(ModelPersistorBean.TABLE_NAME, "m")
                  + " WHERE m." + dbDescriptor.quoteIdentifier(ModelPersistorBean.FIELD__PARTITION) + " = " + partitionOid.toString() + ")");
         }
      }
      if(null != fieldPartition)
      {
         // TODO (kafka, rsauer) remove o.partition=bo.partition predicate, as it is implicitly satisfied by OID predicate?
         updBuf.append(" AND o." + fieldPartition + " = " + updTabRefPrefix + fieldPartition)
               .append(" AND o." + fieldPartition + " = " + partitionOid.toString());
      }

      applyJoinsOnWhereClause(dbDescriptor, joins, updBuf, true);

      updBuf.append(")");

      if (modelOid != null)
      {
         // for ModelPersistorBean, the modelOid is the OID.
         updBuf.append(" AND ").append((pkModel == null ? pkOID : pkModel) + " = " + modelOid);
      }

      StringBuffer insBuf = new StringBuffer(1000);

      insBuf.append("INSERT INTO ").append(getArchiveObjName(tableName))
            .append(" (").append(getFieldNames(dbDescriptor, tdType)).append(") ")
            .append("SELECT ").append(getFieldNames(dbDescriptor, tdType, "o"))
            .append(  " FROM ").append(getSrcObjName(tableName)).append(" o ");

      applyJoinsOnFromClause(dbDescriptor, joins, insBuf);

      insBuf.append(  " WHERE ");

      boolean useAnd = applyJoinsOnWhereClause(dbDescriptor, joins, insBuf, false);

      if (modelOid != null)
      {
         // for ModelPersistorBean, the modelOid is the OID.
         insBuf.append(useAnd ? " AND " : "");
         insBuf.append(" o.").append((pkModel == null ? pkOID : pkModel) + " = " + modelOid);
         useAnd = true;
      }
      else  if ((pkModel != null) && (null == modelOid))
      {
         insBuf.append(useAnd ? " AND " : "");
         insBuf.append(" o." + pkModel + " IN (SELECT m." + ModelPersistorBean.FIELD__OID
               + "  FROM " + getSrcObjName(ModelPersistorBean.TABLE_NAME, "m")
               + " WHERE m." + dbDescriptor.quoteIdentifier(ModelPersistorBean.FIELD__PARTITION) + " = " + partitionOid.toString() + ")");
         useAnd = true;
      }
      if(null != fieldPartition)
      {
         insBuf.append(useAnd ? " AND " : "");
         insBuf.append(" o." + fieldPartition + " = " + partitionOid.toString());
         useAnd = true;
      }
      insBuf.append(useAnd ? " AND " : "");
      insBuf.append(" NOT EXISTS (")
            .append(    "SELECT * FROM ").append(getArchiveObjName(tableName)).append(" bo ")
            .append(       " WHERE o.").append(pkOID).append(" = bo.").append(pkOID);
      if (pkModel != null)
      {
         if(modelOid == null)
         {
            insBuf.append(" AND o." + pkModel + " = " + (modelOid == null ? "bo." + pkModel : modelOid.toString()));
         }
         else
         {
            insBuf.append(" AND bo." + pkModel + " = " + modelOid.toString());
         }
      }
      if(null != fieldPartition)
      {
         insBuf.append(" AND bo." + fieldPartition + " = " + partitionOid.toString());
      }

      insBuf.append(")");

      Statement stmt = null;
      try
      {
         stmt = session.getConnection().createStatement();

         int match = 0;
         if(update)
         {
            if (trace.isDebugEnabled())
            {
               trace.debug(updBuf.toString());
            }
            match = stmt.executeUpdate(updBuf.toString());
         }

         if (trace.isDebugEnabled())
         {
            trace.debug(insBuf.toString());
         }
         if(match == 0)
         {
            stmt.executeUpdate(insBuf.toString());
         }
      }
      catch (SQLException e)
      {
         throw new PublicException("Failed synchronizing PK-stable table "
               + tdType.getTableName(), e);
      }
      finally
      {
         QueryUtils.closeStatement(stmt);
      }
   }

   private void synchronizePreferencesTableArchive()
   {
      TypeDescriptor tdType = TypeDescriptor.get(PreferencesBean.class);
      DBDescriptor dbDescriptor = session.getDBDescriptor();

      // Fill composite primary key list
      List<String> pkList = new LinkedList<String>();
      final Field[] pkFields = tdType.getPkFields();
      for (int i = 0; i<pkFields.length; i++)
      {
         pkList.add(pkFields[i].getName());
      }

      String fieldPartition = tdType.hasField(FIELD_PARTITION) ? dbDescriptor.quoteIdentifier(FIELD_PARTITION) : null;
      String tableName = dbDescriptor.quoteIdentifier(tdType.getTableName());

      StringBuffer updBuf = new StringBuffer(1000);
      updBuf.append("UPDATE ");

      String updTabReference = "bo";
      String updTabRefPrefix = "bo.";
      if (DBMSKey.SYBASE.equals(dbDescriptor.getDbmsKey())
            || DBMSKey.MSSQL8.equals(dbDescriptor.getDbmsKey()))
      {
         updTabReference = getArchiveObjName(tableName);
         updTabRefPrefix = updTabReference + DOT;
         updBuf.append(updTabReference);
      }
      else
      {
         updBuf.append(getArchiveObjName(tableName)).append(SPACE).append(updTabReference);
      }

      updBuf.append(  " SET ");

      if (dbDescriptor.supportsMultiColumnUpdates())
      {
         updBuf.append("(").append(getFieldNames(dbDescriptor, tdType)).append(") = ")
               .append("(SELECT ").append(getFieldNames(dbDescriptor, tdType))
               .append(" FROM ").append(getSrcObjName(tableName)).append(" o ")
               .append(" WHERE ").append(composePkListClause(pkList, updTabRefPrefix));

         // TODO (kafka, rsauer) remove predicate, as it is implicitly satisfied by OID predicate?
         if(null != fieldPartition)
         {
            updBuf.append(" AND o.").append(fieldPartition).append(" = ").append(updTabRefPrefix).append(fieldPartition);
         }
         updBuf.append(")");
      }
      else
      {
         String joinToken = "";

         for (Iterator i = tdType.getPersistentFields().iterator(); i.hasNext();)
         {
            FieldDescriptor field = (FieldDescriptor) i.next();
            if ( !tdType.isPkField(field.getField()))
            {
               updBuf.append(joinToken);
               joinToken = ", ";

               appendPkStableUpdateClause(pkList, fieldPartition,
                     tableName, updBuf,
                     updTabRefPrefix + dbDescriptor.quoteIdentifier(field.getField().getName()),
                     "o." + dbDescriptor.quoteIdentifier(field.getField().getName()),
                     updTabReference);
            }
         }
         for (Iterator i = tdType.getLinks().iterator(); i.hasNext();)
         {
            LinkDescriptor link = (LinkDescriptor) i.next();

            updBuf.append(joinToken);
            joinToken = ", ";

            appendPkStableUpdateClause(pkList, fieldPartition,
                  tableName, updBuf,
                  updTabRefPrefix + dbDescriptor.quoteIdentifier(link.getField().getName()),
                  "o." + dbDescriptor.quoteIdentifier(link.getField().getName()),
                  updTabReference);
         }
      }

      updBuf.append(" WHERE EXISTS (")
            .append(    "SELECT * FROM ").append(getSrcObjName(tableName)).append(" o ");

      updBuf.append(     " WHERE ").append(composePkListClause(pkList, updTabRefPrefix));

      if(null != fieldPartition)
      {
         // TODO (kafka, rsauer) remove o.partition=bo.partition predicate, as it is implicitly satisfied by OID predicate?
         updBuf.append(" AND o." + fieldPartition + " = " + updTabRefPrefix + fieldPartition)
               .append(" AND o." + fieldPartition + " = " + partitionOid.toString());
      }

      updBuf.append(")");

      StringBuffer insBuf = new StringBuffer(1000);

      insBuf.append("INSERT INTO ").append(getArchiveObjName(tableName))
            .append(" (").append(getFieldNames(dbDescriptor, tdType)).append(") ")
            .append("SELECT ").append(getFieldNames(dbDescriptor, tdType, "o"))
            .append(  " FROM ").append(getSrcObjName(tableName)).append(" o ");

      insBuf.append(  " WHERE ");
      boolean useAnd = false;
      if(null != fieldPartition)
      {
         insBuf.append(useAnd ? " AND " : "");
         insBuf.append(" o." + fieldPartition + " = " + partitionOid.toString());
         useAnd = true;
      }
      insBuf.append(useAnd ? " AND " : "");

      String arcAlias = "";
      if ( !DBMSKey.SYBASE.equals(dbDescriptor.getDbmsKey())
            && !DBMSKey.MSSQL8.equals(dbDescriptor.getDbmsKey()))
      {
         arcAlias = SPACE + updTabReference;
      }

      insBuf.append(" NOT EXISTS (")
            .append(    "SELECT * FROM ").append(getArchiveObjName(tableName)).append(arcAlias)
            .append(       " WHERE").append(composePkListClause(pkList, updTabRefPrefix));

      if (null != fieldPartition)
      {
         insBuf.append(" AND " + updTabRefPrefix + fieldPartition + " = "
               + partitionOid.toString());
      }

      insBuf.append(")");

      Statement stmt = null;
      try
      {
         stmt = session.getConnection().createStatement();

         if (trace.isDebugEnabled())
         {
            trace.debug(updBuf.toString());
         }
         stmt.executeUpdate(updBuf.toString());

         if (trace.isDebugEnabled())
         {
            trace.debug(insBuf.toString());
         }
         stmt.executeUpdate(insBuf.toString());
      }
      catch (SQLException e)
      {
         throw new PublicException("Failed synchronizing PK-stable table "
               + tdType.getTableName(), e);
      }
      finally
      {
         QueryUtils.closeStatement(stmt);
      }
   }

   private void appendPkStableUpdateClause(List<String> pkList, String fieldPartition,
         String tableName, StringBuffer updBuf, String tgtFieldList,
         String srcFieldList, String updTabReference)
   {
      String updTabRefPrefix = updTabReference + DOT;
      updBuf.append(tgtFieldList).append(" = ")
            .append("(SELECT ").append(srcFieldList)
            .append(" FROM ").append(getSrcObjName(tableName)).append(" o")
            .append(" WHERE ").append(composePkListClause(pkList, updTabRefPrefix));

      // TODO (kafka, rsauer) remove predicate, as it is implicitly satisfied by OID predicate?
      if (null != fieldPartition)
      {
         updBuf.append(" AND o." + fieldPartition + " = " + updTabRefPrefix + fieldPartition);
      }
      updBuf.append(")");
   }

   private String composePkListClause(List<String> pkList, String updTabRefPrefix)
   {
      StringBuffer buf = new StringBuffer(200);

      boolean addAnd = false;
      for (String pk : pkList)
      {
         String and = addAnd ? " AND" : "";

         buf.append(and)
               .append(" o.")
               .append(pk)
               .append(" = ")
               .append(updTabRefPrefix)
               .append(pk);
         addAnd = true;
      }
      return buf.toString();
   }

   private void appendPkStableUpdateClause(Long modelOid, String pkOID, String fieldPartition,
         String pkModel, String tableName, StringBuffer updBuf, String tgtFieldList,
         String srcFieldList, String updTabReference)
   {
      String updTabRefPrefix = updTabReference + DOT;
      updBuf.append(tgtFieldList).append(" = ")
            .append("(SELECT ").append(srcFieldList)
            .append(" FROM ").append(getSrcObjName(tableName)).append(" o")
            .append(" WHERE ").append(updTabRefPrefix).append(pkOID).append(" = o.").append(pkOID);
      if (pkModel != null)
      {
         updBuf.append(" AND o.").append(pkModel).append(" = ").append((modelOid == null
               ? updTabRefPrefix + pkModel
               : modelOid.toString()));
      }
      // TODO (kafka, rsauer) remove predicate, as it is implicitly satisfied by OID predicate?
      if (null != fieldPartition)
      {
         updBuf.append(" AND o." + fieldPartition + " = " + updTabRefPrefix + fieldPartition);
      }
      updBuf.append(")");
   }

   /**
    * @param joins
    * @param buffer
    */
   private void applyJoinsOnFromClause(DBDescriptor dbDescriptor, Joins joins,
         StringBuffer buffer)
   {
      for (Iterator i = joins.iterator(); i.hasNext();)
      {
         Join join = (Join) i.next();
         buffer.append(", ")
               .append(getQualifiedName(join.getSchemaName(), dbDescriptor.quoteIdentifier(join.getTableName())))
               .append(" ").append(join.getTableAlias());
      }
   }

   /**
    * @param joins
    * @param buffer
    */
   private boolean applyJoinsOnWhereClause(DBDescriptor dbDescriptor, Joins joins,
         StringBuffer buffer, boolean startWithAnd)
   {
      final String AND = " AND ";
      String concat = startWithAnd ? AND : "";
      boolean result = false;

      for (Iterator i = joins.iterator(); i.hasNext();)
      {
         Join join = (Join) i.next();
         result = true;

         final List<JoinElement> joinConditions = join.getJoinConditions();
         Assert.condition(joinConditions.size() == 1,
               "Joins with exact one join condition supported!");

         Pair joinCondition = (Pair) joinConditions.get(0).getJoinCondition();
         buffer.append(concat);
         concat = AND;
         buffer.append(" o").append(".").append(dbDescriptor.quoteIdentifier(((FieldRef)joinCondition.getFirst()).fieldName))
               .append(" = ")
               .append(join.getTableAlias()).append(".")
               .append(dbDescriptor.quoteIdentifier(((FieldRef)joinCondition.getSecond()).fieldName));

         AndTerm restriction = join.getRestriction();
         if (null != restriction && !restriction.getParts().isEmpty())
         {
            final List parts = restriction.getParts();
            Assert.condition( joinConditions.size() == 1,
                  "Restrictions with one part are supported!");

            Object rawPredicate = parts.get(0);
            if (rawPredicate instanceof ComparisonTerm)
            {
               ComparisonTerm predicate = (ComparisonTerm) rawPredicate;

               if (predicate.getOperator().isUnary())
               {
                  Assert.lineNeverReached("Unary operators are not yet supported!");
               }
               else if (predicate.getOperator().isBinary())
               {
                  buffer.append(concat)
                        .append(join.getTableAlias()).append(".")
                        .append(dbDescriptor.quoteIdentifier(predicate.getLhsField().fieldName))
                        .append(predicate.getOperator().getId())
                        // TODO (kafka): Do evaluate valueExpresion by its type.
                        .append(predicate.getValueExpr().toString());
               }
               else if (predicate.getOperator().isTernary())
               {
                  Assert.lineNeverReached("Ternary operators are not yet supported!");
               }
            }
            else
            {
               Assert.lineNeverReached("ComparisonTerms only are supported!");
            }
         }
      }

      return result;
   }

   private void synchronizePkInstableTables(Class type, String modelColumnName,
         Long modelOid, PredicateTerm predicate) throws SQLException
   {
      synchronizePkInstableTables(type, modelColumnName, modelOid, predicate,
            (Joins) null, (Joins) null);
   }

   private void synchronizePkInstableTables(Class type, String modelColumnName,
         Long modelOid, PredicateTerm predicate, Join srcJoin, Join tgtJoin) throws SQLException
   {
      Joins srcJoins = new Joins();
      if (null != srcJoin)
      {
         srcJoins.add(srcJoin);
      }
      Joins tgtJoins = new Joins();
      if (null != tgtJoin)
      {
         tgtJoins.add(tgtJoin);
      }
      synchronizePkInstableTables(type, modelColumnName, modelOid, predicate, srcJoins,
            tgtJoins);
   }

   private void synchronizePkInstableTables(Class type, String modelColumnName,
         Long modelOid, PredicateTerm predicate, Joins srcJoins, Joins tgtJoins) throws SQLException
   {
      synchronizePkInstableTables(type, modelColumnName, modelOid, predicate, predicate, srcJoins, tgtJoins);
   }

   private void synchronizePkInstableTables(Class type, String modelColumnName,
         Long modelOid, PredicateTerm predicate, PredicateTerm tgtPredicate, Joins srcJoins, Joins tgtJoins) throws SQLException
   {
      if (null == srcJoins)
      {
         srcJoins = new Joins();
      }
      if (null == tgtJoins)
      {
         tgtJoins = new Joins();
      }

      //joins = Joins.copyWithSchema(archiveSchema, joins);

      DeleteDescriptor delete = DeleteDescriptor.from(archiveSchema, type);
      delete.getQueryExtension().addJoins(tgtJoins);

      AndTerm delPredicate = new AndTerm();
      if (null != tgtPredicate)
      {
         delPredicate.add(tgtPredicate);
      }

      if (!StringUtils.isEmpty(modelColumnName))
      {
         if (null != modelOid)
         {
            delPredicate.add(Predicates.isEqual(delete.fieldRef(modelColumnName),
                  modelOid.longValue()));
         }
         else
         {
            delPredicate.add(Predicates.inList(delete.fieldRef(modelColumnName),
                  QueryDescriptor
                        .from(srcSchema, ModelPersistorBean.class)
                        .selectDistinct(ModelPersistorBean.FIELD__OID)
                        .where(Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()))));
         }
      }

      DeleteDescriptor deleteDescriptor = delete.where(delPredicate);
      deleteDescriptor.getQueryExtension().setDistinct(true);
      session.executeDelete(deleteDescriptor);

      QueryDescriptor srcQuery = QueryDescriptor.from(srcSchema, type, ALIAS_SRC);
      srcQuery.getQueryExtension().addJoins(srcJoins);

      AndTerm srcPredicate = new AndTerm();
      if (null != predicate)
      {
         srcPredicate.add(predicate);
      }
      if ( !StringUtils.isEmpty(modelColumnName))
      {
         if ( null != modelOid)
         {
            srcPredicate.add(Predicates.isEqual(srcQuery.fieldRef(modelColumnName),
                  modelOid.longValue()));
         }
         else
         {
            srcPredicate.add(Predicates.inList(srcQuery.fieldRef(modelColumnName),
                  QueryDescriptor
                        .from(srcSchema, ModelPersistorBean.class)
                        .selectDistinct(ModelPersistorBean.FIELD__OID)
                        .where(Predicates.isEqual(ModelPersistorBean.FR__PARTITION, partitionOid.shortValue()))));
         }
      }

      QueryDescriptor queryDescriptor = srcQuery.where(srcPredicate);
      queryDescriptor.getQueryExtension().setDistinct(true);
      InsertDescriptor insert = InsertDescriptor
            .into(archiveSchema, type)
            .fullselect(queryDescriptor);

      session.executeInsert(insert);
   }

   private static String getFieldNames(DBDescriptor dbDescriptor, ITypeDescriptor type)
   {
      return getFieldNames(dbDescriptor, type, null);
   }

   private static String getFieldNames(DBDescriptor dbDescriptor, ITypeDescriptor type, String alias)
   {
      return StringUtils.join(
            StringUtils.join(new TransformingIterator(type.getPersistentFields()
                     .iterator(), new FieldNameFunctor(alias, dbDescriptor)), ", "),
            StringUtils.join(new TransformingIterator(type.getLinks()
                  .iterator(), new FieldNameFunctor(alias, dbDescriptor)), ", "), ", ");
   }

   private static String getQualifiedName(String qualifier, String objectName)
   {
      StringBuffer result = new StringBuffer(100);
      if ( !StringUtils.isEmpty(qualifier))
      {
         result.append(qualifier).append(".");
      }
      result.append(objectName);

      return result.toString();
   }

   private static String stringLiteralList(List<String> literals)
   {
      return StringUtils.join(new TransformingIterator(
            literals.iterator(), new Functor()
            {
               public Object execute(Object source)
               {
                  return "'" + source + "'";
               }
            }), ", ");
   }

   private static final class FieldNameFunctor implements Functor
   {
      private final String prefix;
      private final DBDescriptor dbDescriptor;

      public FieldNameFunctor(String alias, DBDescriptor dbDescriptor)
      {
         this.prefix = StringUtils.isEmpty(alias) ? "" : (alias + ".");
         this.dbDescriptor = dbDescriptor;
      }

      public Object execute(Object source)
      {
         if (source instanceof FieldDescriptor)
         {
            return prefix
                  + dbDescriptor.quoteIdentifier(((FieldDescriptor) source).getField()
                        .getName());
         }
         else if (source instanceof LinkDescriptor)
         {
            return prefix
                  + dbDescriptor.quoteIdentifier(((LinkDescriptor) source).getField()
                        .getName());
         }
         else
         {
            throw new InternalException("Invalid persistent attribute descriptor: "
                  + source);
         }
      }
   }

   private static final class ModelHelper
   {
      private final long oid;
      private final long validFrom;

      public ModelHelper(long oid, long validFrom, long validTo, boolean disabled,
            long predecessor)
      {
         this.oid = oid;
         // validFrom is 1 Jan 1970 when not set.
         this.validFrom = validFrom == Unknown.LONG || validFrom == 0
               ? Long.MIN_VALUE : validFrom;
      }

      public long getOid()
      {
         return oid;
      }

      public long getValidFrom()
      {
         return validFrom;
      }

   }

   // check all if qualified
   private Map<String, List<String>> extractID(String[] ids) throws PublicException
   {
      List<IModel> models = ModelManagerFactory.getCurrent().getModels();
      String modelId = null;
      boolean multiModel = false;
      for (IModel model : models)
      {
         String nextId = model.getId();
         if(modelId != null)
         {
            if(!modelId.equals(nextId))
            {
               multiModel = true;
               break;
            }
         }
         modelId = nextId;
      }

      Map<String, List<String>> values = new HashMap<String, List<String>>();

      for (int i = 0; i < ids.length; i++)
      {
         String id = ids[i];
         String namespace = null;
         if (id.startsWith("{"))
         {
            QName qname = QName.valueOf(id);
            namespace = qname.getNamespaceURI();
            id = qname.getLocalPart();
         }
         else if(multiModel)
         {
            throw new PublicException("Id:" + id + " - Qualified Id needed as we have different model id.");
         }

         if (namespace == null)
         {
            namespace = Integer.toString(PredefinedConstants.ALL_MODELS);
         }

         List entries = values.get(namespace);
         if(entries == null)
         {
            entries = new ArrayList<String>();
         }
         entries.add(id);
         values.put(namespace, entries);
      }

      return values;
   }

   /**
    * @param modelOid
    * @param terminatedDeadModels modelOids of models having no alive process instances and are taking part in the archiving or delete operation.
    * @return
    */
   private boolean canDeleteModel(long modelOid, Set<Long> terminatedDeadModels)
   {
      ModelManager modelManager = ModelManagerFactory.getCurrent();
      IModel model = modelManager.findModel(modelOid);

      if (model != null)
      {
         if (modelManager.isActive(model))
         {
            return false;
         }
         else if (PredefinedConstants.PREDEFINED_MODEL_ID.equals(model.getId()))
         {
            return false;
         }
      }

      for (Iterator<IModel> i = modelManager.getAllModels(); i.hasNext();)
      {
         IModel usingModel = i.next();
         List<IModel> usedModels = ModelRefBean.getUsedModels(usingModel);
         for (Iterator<IModel> j = usedModels.iterator(); j.hasNext();)
         {
            IModel usedModel = j.next();
            if (model.getOID() != usingModel.getOID())
            {
               if (model.getOID() == usedModel.getOID())
               {
                  // refering model
                  if (!terminatedDeadModels.contains(Integer.valueOf(usingModel.getModelOID()).longValue()))
                  {
                     return false;
                  }

                  // precondition: no circular references (cycles avoided by design)
                  boolean canDeleteUsedModel = canDeleteModel(usingModel.getModelOID(), terminatedDeadModels);
                  if (!canDeleteUsedModel)
                  {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   private void synchronizeModelTables(Long modelOid)
   {
      synchronizePkStableTables(ModelRefBean.class, modelOid);
      synchronizePkStableTables(ModelDeploymentBean.class, modelOid);
   }

   private void synchronizeProcessInstanceLinkTypeArchive()
   {
      synchronizePkStableTables(ProcessInstanceLinkTypeBean.class, null);
   }

   private void archiveModels(Long modelOid)
   {
      if (modelOid != null)
      {
         List<Long> references = getReferences(modelOid);
         for (Long oid : references)
         {
            synchronizePkStableTables(ModelPersistorBean.class, oid);
            synchronizePkStableTables(ModelDeploymentBean.class, oid);
            try
            {
               synchronizePkInstableTables(LargeStringHolder.class,
                     LargeStringHolder.FIELD__OBJECTID, oid,
                     PT_STRING_DATA_IS_MODEL_RECORD);
            }
            catch (Exception e)
            {
               throw new PublicException("Failed synchronizing string_data table archive", e);
            }
         }
         commit();
      }
   }

   private List<Long> getReferences(long modelOid)
   {
      ModelManager modelManager = ModelManagerFactory.getCurrent();
      IModel model = modelManager.findModel(modelOid);

      List<Long> referingModels = new ArrayList<Long>();

      List<IModel> usedModels = ModelRefBean.getUsedModels(model);
      for (Iterator<IModel> j = usedModels.iterator(); j.hasNext();)
      {
         IModel usedModel = j.next();
         if (model.getOID() != usedModel.getOID())
         {
            referingModels.add(new Long(usedModel.getModelOID()));
         }
      }

      return referingModels;
   }
}
