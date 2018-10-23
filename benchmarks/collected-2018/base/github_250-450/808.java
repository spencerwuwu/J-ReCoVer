// https://searchcode.com/api/result/74214741/

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
package org.eclipse.stardust.engine.core.runtime.beans;

import java.util.Collections;
import java.util.List;

import org.eclipse.stardust.common.config.Parameters;
import org.eclipse.stardust.common.error.ConcurrencyException;
import org.eclipse.stardust.common.error.InternalException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.runtime.ProcessInstanceState;
import org.eclipse.stardust.engine.core.persistence.PhantomException;
import org.eclipse.stardust.engine.core.persistence.jdbc.IdentifiablePersistentBean;
import org.eclipse.stardust.engine.core.runtime.audittrail.management.ExecutionPlan;
import org.eclipse.stardust.engine.core.runtime.audittrail.management.ProcessInstanceUtils;
import org.eclipse.stardust.engine.core.runtime.beans.interceptors.PropertyLayerProviderInterceptor;
import org.eclipse.stardust.engine.core.runtime.beans.removethis.KernelTweakingProperties;
import org.eclipse.stardust.engine.core.runtime.beans.tokencache.ISecondLevelTokenCache;
import org.eclipse.stardust.engine.core.runtime.beans.tokencache.TokenManagerRegistry;
import org.eclipse.stardust.engine.core.runtime.removethis.EngineProperties;

/**
 *
 * @author ubirkemeyer
 * @version $Revision$
 */
public class ProcessCompletionJanitor extends SecurityContextAwareAction
{
   public static final Logger trace = LogManager.getLogger(ProcessCompletionJanitor.class);

   private long processInstanceOID;
   private long count;
   private boolean hasParent;
   private boolean synchronous;

   public ProcessCompletionJanitor(JanitorCarrier carrier, boolean hasParent)
   {
      super(carrier);
      this.processInstanceOID = carrier.getProcessInstance();
      this.hasParent = hasParent;
      synchronous = true;
   }

   public ProcessCompletionJanitor(JanitorCarrier carrier)
   {
      super(carrier);
      this.processInstanceOID = carrier.getProcessInstance();
      this.count = carrier.getCount();
      synchronous = false;
   }

   void complete(IProcessInstance pi)
   {
      ((ProcessInstanceBean) pi).complete();

      // cleanup secondlevelcache
      TokenManagerRegistry.instance().removeSecondLevelCache(pi);

      ProcessInstanceUtils.cleanupProcessInstance(pi);
      
      if (!hasParent)
      {
         IActivityInstance activityInstance = pi.getStartingActivityInstance();
         BpmRuntimeEnvironment rtEnv = PropertyLayerProviderInterceptor.getCurrent();
         ExecutionPlan plan = rtEnv.getExecutionPlan();
         if (plan != null && !plan.isTerminated())
         {
            plan.checkNextStep(activityInstance);
         }
         if (activityInstance != null)
         {
            activityInstance.lock();
            try
            {
               ((IdentifiablePersistentBean) activityInstance).reloadAttribute(ActivityInstanceBean.FIELD__STATE);
               if (!activityInstance.isTerminated() && !activityInstance.isAborting())
               {
                  activityInstance.activate();
                  // do out data mappings if necessary
                  ActivityThread.schedule(null, null, activityInstance, true,
                        null, Collections.EMPTY_MAP, false);
               }
               else
               {
                  if (trace.isDebugEnabled())
                  {
                     trace.debug("Skipping recovery of concurrently completed starting "
                           + "activity" + activityInstance);
                  }
               }
            }
            catch (PhantomException e)
            {
               throw new InternalException(e);
            }
         }
         else
         {
            // No startingActivityInstance found, but this could be a spawned sub process.
            // Lookup parent, complete parent process if possible.
            if (pi.getRootProcessInstanceOID() != pi.getOID())
            {
               IProcessInstance parentProcessInstance = ProcessInstanceHierarchyBean.findParentForSubProcessInstanceOid(pi.getOID());
               if (parentProcessInstance != null)
               {
                  ProcessCompletionJanitor processCompletionJanitor = new ProcessCompletionJanitor(
                        new JanitorCarrier(parentProcessInstance.getOID()), hasParent);
                  processCompletionJanitor.execute();
               }
            }
         }
      }
   }

   public Object execute()
   {
      return execute(false);
   }
   
   public Object execute(boolean forceSynchronous)
   {
      boolean performed = false;
      ProcessInstanceBean pi = ProcessInstanceBean.findByOID(processInstanceOID);

      if (!pi.isTerminated() && !pi.isAborting())
      {
         try
         {
            // TODO reload state attribute to reduce need to lock in case of concurrent
            // completion

            pi.lock();
            try
            {
               ProcessInstanceState state = pi.getState();
               ((IdentifiablePersistentBean) pi).reloadAttribute(ProcessInstanceBean.FIELD__STATE);

               if (!pi.isTerminated() && !pi.isAborting())
               {
                  // (fh) restore original state
                  if (!state.equals(pi.getState()))
                  {
                     if (trace.isDebugEnabled())
                     {
                        trace.debug("Restoring state for Process Instance = "
                              + processInstanceOID + ": " + state + "<--"
                              + pi.getState());
                     }
                     pi.restoreState(state);
                  }
                  
                  if (!forceSynchronous && synchronous
                        && Parameters.instance().getBoolean(
                              KernelTweakingProperties.ASYNC_PROCESS_COMPLETION, false))
                  {
                     trace.info("Scheduling a new janitor for " + processInstanceOID
                           + " due to configuration to force asynchronous process completion.");
                     scheduleJanitor(new JanitorCarrier(processInstanceOID, count));
                  }
                  else
                  {
                     if (count <= 0)
                     {
                        int timeout = Parameters.instance().getInteger(
                              KernelTweakingProperties.PROCESS_COMPLETION_TOKEN_COUNT_TIMEOUT, 1);

                        ISecondLevelTokenCache secondLevelTokenCache = TokenManagerRegistry.instance().getSecondLevelCache(pi);

                        boolean canComplete = false;
                        if (secondLevelTokenCache.hasCompleteInformation())
                        {
                           // in single-node scenario, rely on secondlevel cache for unconsumed queries
                           if (secondLevelTokenCache.getUnconsumedTokenCount() == 0)
                           {
                              canComplete = true;
                           }
                        }
                        else
                        {
                           long unconsumed = TransitionTokenBean.countUnconsumedForProcessInstance(pi, timeout);
                           /*Iterator<TransitionTokenBean> itr = TransitionTokenBean.findUnconsumedForProcessInstance(pi.getOID());
                           while (itr.hasNext())
                           {
                              TransitionTokenBean token = itr.next();
                              System.out.println(token);
                           }*/
                           if (unconsumed == 0 || synchronous && (unconsumed + count == 0))
                           {
                              canComplete = true;
                           }
                        }

                        if (canComplete)
                        {
                           List<IProcessInstance> childProcessInstances = ProcessInstanceHierarchyBean.findChildren(pi);

                           for (IProcessInstance childProcessInstance : childProcessInstances)
                           {
                              if ( !childProcessInstance.isTerminated()
                                    && !childProcessInstance.isAborting())
                              {
                                 canComplete = false;
                              }
                           }
                        }

                        if (canComplete)
                        {
                           complete(pi);
                           performed = true;
                        }
                     }

                     if (!performed && ProcessInstanceState.Interrupted.equals(pi.getState()))
                     {
                        if (forceSynchronous)
                        {
                           throw new InternalException("Cannot complete process synchronously: " + processInstanceOID);
                        }
                        scheduleJanitor(new InterruptionJanitorCarrier(processInstanceOID));
                     }
                  }
               }
            }
            catch (PhantomException e)
            {
               throw new InternalException(e);
            }
         }
         catch (ConcurrencyException e)
         {
            if (forceSynchronous)
            {
               throw e;
            }
            trace.info("Cannot run janitor for " + processInstanceOID
                  + " due to a locking conflict, scheduling a new one.");
            scheduleJanitor(new JanitorCarrier(processInstanceOID, count));
         }
      }

      return performed ? Boolean.TRUE : Boolean.FALSE;
   }

   private void scheduleJanitor(ActionCarrier carrier)
   {
      ForkingServiceFactory factory = (ForkingServiceFactory)
            Parameters.instance().get(EngineProperties.FORKING_SERVICE_HOME);
      ForkingService service = null;
      try
      {
         service = factory.get();
         service.fork(carrier, true);
      }
      finally
      {
         factory.release(service);
      }
   }

   public void incrementCount(long increment)
   {
      count += increment;
   }

   public String toString()
   {
      return "Process completion janitor, pi = " + processInstanceOID;
   }
}

