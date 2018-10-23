// https://searchcode.com/api/result/74214471/

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
package org.eclipse.stardust.engine.core.runtime.beans;

import java.text.MessageFormat;
import java.util.*;

import org.eclipse.stardust.common.Procedure;
import org.eclipse.stardust.common.config.Parameters;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.common.rt.IJobManager;
import org.eclipse.stardust.engine.api.runtime.AdministrationService;
import org.eclipse.stardust.engine.api.runtime.LogCode;
import org.eclipse.stardust.engine.core.extensions.conditions.timer.TimeStampEmitter;
import org.eclipse.stardust.engine.core.persistence.ClosableIterator;
import org.eclipse.stardust.engine.core.runtime.logging.RuntimeLog;
import org.eclipse.stardust.engine.core.runtime.removethis.EngineProperties;
import org.eclipse.stardust.engine.core.spi.extensions.runtime.Event;
import org.eclipse.stardust.engine.core.spi.extensions.runtime.PullEventEmitter;


/**
 * @author ubirkemeyer
 * @version $Revision$
 */
public class EventDaemon implements IDaemon
{
   private static final Logger trace = LogManager.getLogger(EventDaemon.class);
   public static final Logger daemonLogger = RuntimeLog.DAEMON;   
   
   public static final String ID = AdministrationService.EVENT_DAEMON;

   public EventDaemon()
   {
   }

   // @todo (france, ub): exception handling is lacking
   public ExecutionResult execute(long batchSize)
   {
      long nEvents = 0;

      Date now = new Date();
      if (!isEventDaemonDisabled(now))
      {
         ForkingServiceFactory factory = (ForkingServiceFactory) Parameters.instance()
               .get(EngineProperties.FORKING_SERVICE_HOME);
         final IJobManager jobManager = factory.getJobManager();
         try
         {
            final long currentTime = now.getTime();
            DependentObjectsCache modelCache = ModelManagerFactory.getCurrent().getDependentCache();
            
            boolean timerEventsEvaluated = false;
            
            Iterator emitters = modelCache.getEmitters();
            while ((nEvents < batchSize) && emitters.hasNext())
            {
               PullEventEmitter emitter = (PullEventEmitter) emitters.next();
               
               // evaluate timestamp events only once (CRNT-3702)
               if (emitter instanceof TimeStampEmitter)
               {
                  if (timerEventsEvaluated)
                  {
                     continue;
                  }
                  timerEventsEvaluated = true;
               }
               
               ClosableIterator events = null;
               try
               {
                  // find events due to be handled
                  events = emitter.execute(currentTime);
                  
                  while ((nEvents < batchSize) && events.hasNext())
                  {
                     ++nEvents;
                     
                     final Event event = (Event) events.next();
                     
                     try
                     {
                        // perform event handling in separate TX to make sure failure in
                        // processing one does not disturb any other (CRNT-5683)
                        jobManager.performSynchronousJob(new Procedure()
                        {
                           protected void invoke()
                           {
                              daemonLogger.info("Event Daemon, process event '" + event.toString() + "'.");                              
                              EventUtils.processPullEvent(event);
                           }
                        });
                     }
                     catch (Exception e)
                     {
                        // TODO eventually disable event or reduce priority of event to
                        // keep future noise down
                        
                        AuditTrailLogger.getInstance(LogCode.EVENT,
                              EventUtils.getEventSourceInstance(event)).warn(
                                    MessageFormat.format("Failed processing event {0}.",
                                          new Object[] {event}), e);
                     }
                  }
               }
               finally
               {
                  if (null != events)
                  {
                     events.close();
                  }
               }
            }
         }
         finally
         {
            factory.release(jobManager);
         }
      }
      
      return (nEvents >= batchSize) ? IDaemon.WORK_PENDING : IDaemon.WORK_DONE;
   }

   private boolean isEventDaemonDisabled(Date date)
   {
      final String daysExpr = Parameters.instance().getString(
            ID + "." + EngineProperties.DISABLED_DAYS_OF_WEEK, "");

      Set days = new HashSet();
      try
      {
         StringTokenizer tokenizer = new StringTokenizer(daysExpr, ",");
         while (tokenizer.hasMoreTokens())
         {
            days.add(new Integer(tokenizer.nextToken().trim()));
         }
      }
      catch (Exception e)
      {
         trace.warn("Failed parsing " + ID + "." + EngineProperties.DISABLED_DAYS_OF_WEEK
               + " expression: '" + daysExpr + "'");
      }

      Calendar cal = Calendar.getInstance();
      cal.setTime(date);

      int dow = cal.get(Calendar.DAY_OF_WEEK);
      boolean disabled = days.contains(new Integer(dow));

      if (trace.isDebugEnabled() && disabled)
      {
         trace.debug("Pull events disabled on day of week: " + dow);
      }
      return disabled;
   }

   public String getType()
   {
      return ID;
   }
}
