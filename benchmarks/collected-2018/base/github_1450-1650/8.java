// https://searchcode.com/api/result/110750176/

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.mapred;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.server.jobtracker.TaskTracker;

/**
 * A {@link TaskScheduler} that implements the requirements in HADOOP-3421
 * and provides a HOD-less way to share large clusters. This scheduler 
 * provides the following features: 
 *  * support for queues, where a job is submitted to a queue. 
 *  * Queues are assigned a fraction of the capacity of the grid (their
 *  'capacity') in the sense that a certain capacity of resources 
 *  will be at their disposal. All jobs submitted to the queues of an Org 
 *  will have access to the capacity to the Org.
 *  * Free resources can be allocated to any queue beyond its 
 *  capacity.
 *  * Queues optionally support job priorities (disabled by default). 
 *  * Within a queue, jobs with higher priority will have access to the 
 *  queue's resources before jobs with lower priority. However, once a job 
 *  is running, it will not be preempted for a higher priority job.
 *  * In order to prevent one or more users from monopolizing its resources, 
 *  each queue enforces a limit on the percentage of resources allocated to a 
 *  user at any given time, if there is competition for them.
 *  
 */
class CapacityTaskScheduler extends TaskScheduler {


  /***********************************************************************
   * Keeping track of scheduling information for queues
   * 
   * We need to maintain scheduling information relevant to a queue (its 
   * name, capacity, etc), along with information specific to 
   * each kind of task, Map or Reduce (num of running tasks, pending 
   * tasks etc). 
   * 
   * This scheduling information is used to decide how to allocate
   * tasks, redistribute capacity, etc.
   *  
   * A QueueSchedulingInfo(QSI) object represents scheduling information for
   * a queue. A TaskSchedulingInfo (TSI) object represents scheduling 
   * information for a particular kind of task (Map or Reduce).
   *   
   **********************************************************************/

  private static class TaskSchedulingInfo {

    /** 
     * the actual capacity, which depends on how many slots are available
     * in the cluster at any given time. 
     */
    private int capacity = 0;
    // number of running tasks
    int numRunningTasks = 0;
    // number of slots occupied by running tasks
    int numSlotsOccupied = 0;

    //the actual maximum capacity which depends on how many slots are available
    //in cluster at any given time.
    private int maxCapacity = -1;

    /**
     * for each user, we need to keep track of number of slots occupied by
     * running tasks
     */
    Map<String, Integer> numSlotsOccupiedByUser = 
      new HashMap<String, Integer>();

    /**
     * reset the variables associated with tasks
     */
    void resetTaskVars() {
      numRunningTasks = 0;
      numSlotsOccupied = 0;
      for (String s: numSlotsOccupiedByUser.keySet()) {
        numSlotsOccupiedByUser.put(s, Integer.valueOf(0));
      }
    }


    /**
     * Returns the actual capacity.
     * capacity.
     *
     * @return
     */
    int getCapacity() {
      return capacity;
    }

    /**
     * Mutator method for capacity
     *
     * @param capacity
     */
    void setCapacity(int capacity) {
        this.capacity = capacity;
    }


    /**
     * return information about the tasks
     */
    @Override
    public String toString() {
      float occupiedSlotsAsPercent =
          getCapacity() != 0 ?
            ((float) numSlotsOccupied * 100 / getCapacity()) : 0;
      StringBuffer sb = new StringBuffer();
      
      sb.append("Capacity: " + capacity + " slots\n");
      
      if(getMaxCapacity() >= 0) {
        sb.append("Maximum capacity: " + getMaxCapacity() +" slots\n");
      }
      sb.append(String.format("Used capacity: %d (%.1f%% of Capacity)\n",
          Integer.valueOf(numSlotsOccupied), Float
              .valueOf(occupiedSlotsAsPercent)));
      sb.append(String.format("Running tasks: %d\n", Integer
          .valueOf(numRunningTasks)));
      // include info on active users
      if (numSlotsOccupied != 0) {
        sb.append("Active users:\n");
        for (Map.Entry<String, Integer> entry : numSlotsOccupiedByUser
            .entrySet()) {
          if ((entry.getValue() == null) || (entry.getValue().intValue() <= 0)) {
            // user has no tasks running
            continue;
          }
          sb.append("User '" + entry.getKey() + "': ");
          int numSlotsOccupiedByThisUser = entry.getValue().intValue();
          float p =
              (float) numSlotsOccupiedByThisUser * 100 / numSlotsOccupied;
          sb.append(String.format("%d (%.1f%% of used capacity)\n", Long
              .valueOf(numSlotsOccupiedByThisUser), Float.valueOf(p)));
        }
      }
      return sb.toString();
    }

    int getMaxCapacity() {
      return maxCapacity;
    }

    void setMaxCapacity(int maxCapacity) {
      this.maxCapacity = maxCapacity;
    }
  }
  
  private static class QueueSchedulingInfo {
    String queueName;

    /**
     * capacity(%) is set in the config
     */
    float capacityPercent = 0;
    
    
  /**
   * maxCapacityPercent(%) is set in config as
   * mapred.capacity-scheduler.queue.<queue-name>.maximum-capacity
   * maximum-capacity percent defines a limit beyond which a queue
   * cannot expand. Remember this limit is dynamic and changes w.r.t
   * cluster size.
   */
    float maxCapacityPercent = -1;
    /** 
     * to handle user limits, we need to know how many users have jobs in 
     * the queue.
     */  
    Map<String, Integer> numJobsByUser = new HashMap<String, Integer>();
      
    /**
     * min value of user limit (same for all users)
     */
    int ulMin;
    
    /**
     * We keep track of the JobQueuesManager only for reporting purposes 
     * (in toString()). 
     */
    private JobQueuesManager jobQueuesManager;
    
    /**
     * We keep a TaskSchedulingInfo object for each kind of task we support
     */
    TaskSchedulingInfo mapTSI;
    TaskSchedulingInfo reduceTSI;
    
    public QueueSchedulingInfo(
      String queueName, float capacityPercent,
      float maxCapacityPercent, int ulMin, JobQueuesManager jobQueuesManager
    ) {
      this.queueName = new String(queueName);
      this.capacityPercent = capacityPercent;
      this.maxCapacityPercent = maxCapacityPercent;
      this.ulMin = ulMin;
      this.jobQueuesManager = jobQueuesManager;
      this.mapTSI = new TaskSchedulingInfo();
      this.reduceTSI = new TaskSchedulingInfo();
    }
    
    /**
     * return information about the queue
     *
     * @return a String representing the information about the queue.
     */
    @Override
    public String toString(){
      // We print out the queue information first, followed by info
      // on map and reduce tasks and job info
      StringBuffer sb = new StringBuffer();
      sb.append("Queue configuration\n");
      sb.append("Capacity Percentage: ");
      sb.append(capacityPercent);
      sb.append("%\n");
      sb.append(String.format("User Limit: %d%s\n",ulMin, "%"));
      sb.append(String.format("Priority Supported: %s\n",
          (jobQueuesManager.doesQueueSupportPriorities(queueName))?
              "YES":"NO"));
      sb.append("-------------\n");

      sb.append("Map tasks\n");
      sb.append(mapTSI.toString());
      sb.append("-------------\n");
      sb.append("Reduce tasks\n");
      sb.append(reduceTSI.toString());
      sb.append("-------------\n");
      
      sb.append("Job info\n");
      sb.append(String.format("Number of Waiting Jobs: %d\n", 
          jobQueuesManager.getWaitingJobCount(queueName)));
      sb.append(String.format("Number of users who have submitted jobs: %d\n", 
          numJobsByUser.size()));
      return sb.toString();
    }
  }

  /** quick way to get qsi object given a queue name */
  private Map<String, QueueSchedulingInfo> queueInfoMap = 
    new HashMap<String, QueueSchedulingInfo>();
  
  /**
   * This class captures scheduling information we want to display or log.
   */
  private static class SchedulingDisplayInfo {
    private String queueName;
    CapacityTaskScheduler scheduler;
    
    SchedulingDisplayInfo(String queueName, CapacityTaskScheduler scheduler) { 
      this.queueName = queueName;
      this.scheduler = scheduler;
    }
    
    @Override
    public String toString(){
      // note that we do not call updateQSIObjects() here for performance
      // reasons. This means that the data we print out may be slightly
      // stale. This data is updated whenever assignTasks() is called
      // If this doesn't happen, the data gets stale. If we see
      // this often, we may need to detect this situation and call 
      // updateQSIObjects(), or just call it each time. 
      return scheduler.getDisplayInfo(queueName);
    }
  }

  // this class encapsulates the result of a task lookup
  private static class TaskLookupResult {

    static enum LookUpStatus {
      TASK_FOUND,
      NO_TASK_FOUND,
      TASK_FAILING_MEMORY_REQUIREMENT,
    }
    // constant TaskLookupResult objects. Should not be accessed directly.
    private static final TaskLookupResult NoTaskLookupResult = 
      new TaskLookupResult(null, TaskLookupResult.LookUpStatus.NO_TASK_FOUND);
    private static final TaskLookupResult MemFailedLookupResult = 
      new TaskLookupResult(null, 
          TaskLookupResult.LookUpStatus.TASK_FAILING_MEMORY_REQUIREMENT);

    private LookUpStatus lookUpStatus;
    private Task task;

    // should not call this constructor directly. use static factory methods.
    private TaskLookupResult(Task t, LookUpStatus lUStatus) {
      this.task = t;
      this.lookUpStatus = lUStatus;
    }
    
    static TaskLookupResult getTaskFoundResult(Task t) {
      return new TaskLookupResult(t, LookUpStatus.TASK_FOUND);
    }
    static TaskLookupResult getNoTaskFoundResult() {
      return NoTaskLookupResult;
    }
    static TaskLookupResult getMemFailedResult() {
      return MemFailedLookupResult;
    }
    

    Task getTask() {
      return task;
    }

    LookUpStatus getLookUpStatus() {
      return lookUpStatus;
    }
  }

  /** 
   * This class handles the scheduling algorithms. 
   * The algos are the same for both Map and Reduce tasks. 
   * There may be slight variations later, in which case we can make this
   * an abstract base class and have derived classes for Map and Reduce.  
   */
  private static abstract class TaskSchedulingMgr {

    /** our TaskScheduler object */
    protected CapacityTaskScheduler scheduler;
    protected TaskType type = null;

    abstract Task obtainNewTask(TaskTrackerStatus taskTracker, 
        JobInProgress job) throws IOException;

    int getSlotsOccupied(JobInProgress job) {
      return (getNumReservedTaskTrackers(job) + getRunningTasks(job)) * 
             getSlotsPerTask(job);
    }

    abstract int getClusterCapacity();
    abstract int getSlotsPerTask(JobInProgress job);
    abstract int getRunningTasks(JobInProgress job);
    abstract int getPendingTasks(JobInProgress job);
    abstract TaskSchedulingInfo getTSI(QueueSchedulingInfo qsi);
    abstract int getNumReservedTaskTrackers(JobInProgress job);
    
    /**
     * To check if job has a speculative task on the particular tracker.
     * 
     * @param job job to check for speculative tasks.
     * @param tts task tracker on which speculative task would run.
     * @return true if there is a speculative task to run on the tracker.
     */
    abstract boolean hasSpeculativeTask(JobInProgress job, 
        TaskTrackerStatus tts);

    /**
     * Check if the given job has sufficient reserved tasktrackers for all its
     * pending tasks.
     * 
     * @param job job to check for sufficient reserved tasktrackers 
     * @return <code>true</code> if the job has reserved tasktrackers,
     *         else <code>false</code>
     */
    boolean hasSufficientReservedTaskTrackers(JobInProgress job) {
      return getNumReservedTaskTrackers(job) >= getPendingTasks(job);
    }
    
    /**
     * List of QSIs for assigning tasks.
     * Queues are ordered by a ratio of (# of running tasks)/capacity, which
     * indicates how much 'free space' the queue has, or how much it is over
     * capacity. This ordered list is iterated over, when assigning tasks.
     */  
    private List<QueueSchedulingInfo> qsiForAssigningTasks = 
      new ArrayList<QueueSchedulingInfo>();

    /**
     * Comparator to sort queues.
     * For maps, we need to sort on QueueSchedulingInfo.mapTSI. For 
     * reducers, we use reduceTSI. So we'll need separate comparators.  
     */ 
    private static abstract class QueueComparator 
      implements Comparator<QueueSchedulingInfo> {
      abstract TaskSchedulingInfo getTSI(QueueSchedulingInfo qsi);
      public int compare(QueueSchedulingInfo q1, QueueSchedulingInfo q2) {
        TaskSchedulingInfo t1 = getTSI(q1);
        TaskSchedulingInfo t2 = getTSI(q2);
        // look at how much capacity they've filled. Treat a queue with
        // capacity=0 equivalent to a queue running at capacity
        double r1 = (0 == t1.getCapacity())? 1.0f:
          (double)t1.numSlotsOccupied/(double) t1.getCapacity();
        double r2 = (0 == t2.getCapacity())? 1.0f:
          (double)t2.numSlotsOccupied/(double) t2.getCapacity();
        if (r1<r2) return -1;
        else if (r1>r2) return 1;
        else return 0;
      }
    }
    // subclass for map and reduce comparators
    private static final class MapQueueComparator extends QueueComparator {
      TaskSchedulingInfo getTSI(QueueSchedulingInfo qsi) {
        return qsi.mapTSI;
      }
    }
    private static final class ReduceQueueComparator extends QueueComparator {
      TaskSchedulingInfo getTSI(QueueSchedulingInfo qsi) {
        return qsi.reduceTSI;
      }
    }
    // these are our comparator instances
    protected final static MapQueueComparator mapComparator = new MapQueueComparator();
    protected final static ReduceQueueComparator reduceComparator = new ReduceQueueComparator();
    // and this is the comparator to use
    protected QueueComparator queueComparator;

    // Returns queues sorted according to the QueueComparator.
    // Mainly for testing purposes.
    String[] getOrderedQueues() {
      List<String> queues = new ArrayList<String>(qsiForAssigningTasks.size());
      for (QueueSchedulingInfo qsi : qsiForAssigningTasks) {
        queues.add(qsi.queueName);
      }
      return queues.toArray(new String[queues.size()]);
    }

    TaskSchedulingMgr(CapacityTaskScheduler sched) {
      scheduler = sched;
    }
    
    // let the scheduling mgr know which queues are in the system
    void initialize(Map<String, QueueSchedulingInfo> qsiMap) { 
      // add all the qsi objects to our list and sort
      qsiForAssigningTasks.addAll(qsiMap.values());
      Collections.sort(qsiForAssigningTasks, queueComparator);
    }
    
    private synchronized void updateCollectionOfQSIs() {
      Collections.sort(qsiForAssigningTasks, queueComparator);
    }

    /**
     * Ceil of result of dividing two integers.
     * 
     * This is *not* a utility method. 
     * Neither <code>a</code> or <code>b</code> should be negative.
     *  
     * @param a
     * @param b
     * @return ceil of the result of a/b
     */
    private int divideAndCeil(int a, int b) {
      if (b == 0) {
        LOG.info("divideAndCeil called with a=" + a + " b=" + b);
        return 0;
      }
      return (a + (b - 1)) / b;
    }
    
    private boolean isUserOverLimit(JobInProgress j, QueueSchedulingInfo qsi) {
      // what is our current capacity? It is equal to the queue-capacity if
      // we're running below capacity. If we're running over capacity, then its
      // #running plus slotPerTask of the job (which is the number of extra
      // slots we're getting).
      int currentCapacity;
      TaskSchedulingInfo tsi = getTSI(qsi);
      if (tsi.numSlotsOccupied < tsi.getCapacity()) {
        currentCapacity = tsi.getCapacity();
      }
      else {
        currentCapacity = tsi.numSlotsOccupied + getSlotsPerTask(j);
      }
      int limit = 
        Math.max(divideAndCeil(currentCapacity, qsi.numJobsByUser.size()), 
                 divideAndCeil(qsi.ulMin*currentCapacity, 100));
      String user = j.getProfile().getUser();
      if (tsi.numSlotsOccupiedByUser.get(user) >= limit) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("User " + user + " is over limit, num slots occupied=" + 
                    tsi.numSlotsOccupiedByUser.get(user) + ", limit=" + limit);
        }
        return true;
      }
      else {
        return false;
      }
    }

    /*
     * This is the central scheduling method. 
     * It tries to get a task from jobs in a single queue. 
     * Always return a TaskLookupResult object. Don't return null. 
     */
    private TaskLookupResult getTaskFromQueue(TaskTracker taskTracker,
                                              QueueSchedulingInfo qsi)
    throws IOException {
      TaskTrackerStatus taskTrackerStatus = taskTracker.getStatus();
      // we only look at jobs in the running queues, as these are the ones
      // who have been potentially initialized

      for (JobInProgress j : 
        scheduler.jobQueuesManager.getRunningJobQueue(qsi.queueName)) {
        // only look at jobs that can be run. We ignore jobs that haven't 
        // initialized, or have completed but haven't been removed from the 
        // running queue. 
        if (j.getStatus().getRunState() != JobStatus.RUNNING) {
          continue;
        }
        //Check if queue is over maximum-capacity
        if(this.areTasksInQueueOverMaxCapacity(qsi,j.getNumSlotsPerTask(type))) {
          continue;
        }
        // check if the job's user is over limit
        if (isUserOverLimit(j, qsi)) {
          continue;
        } 
        //If this job meets memory requirements. Ask the JobInProgress for
        //a task to be scheduled on the task tracker.
        //if we find a job then we pass it on.
        if (scheduler.memoryMatcher.matchesMemoryRequirements(j, type,
                                                              taskTrackerStatus)) {
          // We found a suitable job. Get task from it.
          Task t = obtainNewTask(taskTrackerStatus, j);
          //if there is a task return it immediately.
          if (t != null) {
            // we're successful in getting a task
            return TaskLookupResult.getTaskFoundResult(t);
          } else {
            //skip to the next job in the queue.
            if (LOG.isDebugEnabled()) {
              LOG.debug("Job " + j.getJobID().toString()
                  + " returned no tasks of type " + type);
            }
            continue;
          }
        } else {
          // if memory requirements don't match then we check if the job has
          // pending tasks and has insufficient number of 'reserved'
          // tasktrackers to cover all pending tasks. If so we reserve the
          // current tasktracker for this job so that high memory jobs are not
          // starved
          if ((getPendingTasks(j) != 0 && !hasSufficientReservedTaskTrackers(j))) {
            // Reserve all available slots on this tasktracker
            LOG.info(j.getJobID() + ": Reserving "
                + taskTracker.getTrackerName()
                + " since memory-requirements don't match");
            taskTracker.reserveSlots(type, j, taskTracker
                .getAvailableSlots(type));

            // Block
            return TaskLookupResult.getMemFailedResult();
          }
        }//end of memory check block
        // if we're here, this job has no task to run. Look at the next job.
      }//end of for loop

      // if we're here, we haven't found any task to run among all jobs in 
      // the queue. This could be because there is nothing to run, or that 
      // the user limit for some user is too strict, i.e., there's at least 
      // one user who doesn't have enough tasks to satisfy his limit. If 
      // it's the latter case, re-look at jobs without considering user 
      // limits, and get a task from the first eligible job; however
      // we do not 'reserve' slots on tasktrackers anymore since the user is 
      // already over the limit
      // Note: some of the code from above is repeated here. This is on 
      // purpose as it improves overall readability.  
      // Note: we walk through jobs again. Some of these jobs, which weren't
      // considered in the first pass, shouldn't be considered here again, 
      // but we still check for their viability to keep the code simple. In
      // some cases, for high mem jobs that have nothing to run, we call 
      // obtainNewTask() unnecessarily. Should this be a problem, we can 
      // create a list of jobs to look at (those whose users were over 
      // limit) in the first pass and walk through that list only. 
      for (JobInProgress j : 
        scheduler.jobQueuesManager.getRunningJobQueue(qsi.queueName)) {
        if (j.getStatus().getRunState() != JobStatus.RUNNING) {
          continue;
        }
        //Check if queue is over maximum-capacity
        if (this.areTasksInQueueOverMaxCapacity(
          qsi, j.getNumSlotsPerTask(type))) {
          continue;
        }
        
        if (scheduler.memoryMatcher.matchesMemoryRequirements(j, type,
            taskTrackerStatus)) {
          // We found a suitable job. Get task from it.
          Task t = obtainNewTask(taskTrackerStatus, j);
          //if there is a task return it immediately.
          if (t != null) {
            // we're successful in getting a task
            return TaskLookupResult.getTaskFoundResult(t);
          } else {
            //skip to the next job in the queue.
            continue;
          }
        } else {
          //if memory requirements don't match then we check if the 
          //job has either pending or speculative task. If the job
          //has pending or speculative task we block till this job
          //tasks get scheduled, so that high memory jobs are not 
          //starved
          if (getPendingTasks(j) != 0 || hasSpeculativeTask(j, taskTrackerStatus)) {
            return TaskLookupResult.getMemFailedResult();
          } 
        }//end of memory check block
      }//end of for loop

      // found nothing for this queue, look at the next one.
      if (LOG.isDebugEnabled()) {
        String msg = "Found no task from the queue " + qsi.queueName;
        LOG.debug(msg);
      }
      return TaskLookupResult.getNoTaskFoundResult();
    }

    // Always return a TaskLookupResult object. Don't return null. 
    // The caller is responsible for ensuring that the QSI objects and the 
    // collections are up-to-date.
    private TaskLookupResult assignTasks(TaskTracker taskTracker) 
    throws IOException {
      TaskTrackerStatus taskTrackerStatus = taskTracker.getStatus();

      printQSIs();

      // Check if this tasktracker has been reserved for a job...
      JobInProgress job = taskTracker.getJobForFallowSlot(type);
      if (job != null) {
        int availableSlots = taskTracker.getAvailableSlots(type);
        if (LOG.isDebugEnabled()) {
          LOG.debug(job.getJobID() + ": Checking 'reserved' tasktracker " + 
                    taskTracker.getTrackerName() + " with " + availableSlots + 
                    " '" + type + "' slots");
        }

        if (availableSlots >= job.getNumSlotsPerTask(type)) {
          // Unreserve 
          taskTracker.unreserveSlots(type, job);
          
          // We found a suitable job. Get task from it.
          Task t = obtainNewTask(taskTrackerStatus, job);
          //if there is a task return it immediately.
          if (t != null) {
            if (LOG.isDebugEnabled()) {
              LOG.info(job.getJobID() + ": Got " + t.getTaskID() + 
                       " for reserved tasktracker " + 
                       taskTracker.getTrackerName());
            }
            // we're successful in getting a task
            return TaskLookupResult.getTaskFoundResult(t);
          } 
        } else {
          // Re-reserve the current tasktracker
          taskTracker.reserveSlots(type, job, availableSlots);
          
          if (LOG.isDebugEnabled()) {
            LOG.debug(job.getJobID() + ": Re-reserving " + 
                      taskTracker.getTrackerName());
          }

          return TaskLookupResult.getMemFailedResult(); 
        }
      }
      
      
      for (QueueSchedulingInfo qsi : qsiForAssigningTasks) {
        // we may have queues with capacity=0. We shouldn't look at jobs from 
        // these queues
        if (0 == getTSI(qsi).getCapacity()) {
          continue;
        }

        //This call is for optimization if we are already over the
        //maximum-capacity we avoid traversing the queues.
        if(this.areTasksInQueueOverMaxCapacity(qsi,1)) {
          continue;
        }
        TaskLookupResult tlr = getTaskFromQueue(taskTracker, qsi);
        TaskLookupResult.LookUpStatus lookUpStatus = tlr.getLookUpStatus();

        if (lookUpStatus == TaskLookupResult.LookUpStatus.NO_TASK_FOUND) {
          continue; // Look in other queues.
        }

        // if we find a task, return
        if (lookUpStatus == TaskLookupResult.LookUpStatus.TASK_FOUND) {
          return tlr;
        }
        // if there was a memory mismatch, return
        else if (lookUpStatus == 
          TaskLookupResult.LookUpStatus.TASK_FAILING_MEMORY_REQUIREMENT) {
            return tlr;
        }
      }

      // nothing to give
      return TaskLookupResult.getNoTaskFoundResult();
    }


    /**
     * Check if maximum-capacity is set for this queue.
     * If set and greater than 0 ,
     * check if numofslotsoccupied+numSlotsPerTask is greater than
     * maximum-capacity , if yes , implies this queue is over limit.
     *
     * Incase noOfSlotsOccupied is less than maximum-capacity ,but ,
     * numOfSlotsOccupied + noSlotsPerTask is more than maximum-capacity we
     * still dont assign the task . This may lead to under utilization of very
     * small set of slots. But this is ok , as we strictly respect the
     * maximum-capacity limit.
     * 
     * @param qsi
     * @return true if queue is over limit.
     */

    private boolean areTasksInQueueOverMaxCapacity(
      QueueSchedulingInfo qsi, int numSlotsPerTask) {
      TaskSchedulingInfo tsi = getTSI(qsi);
      if (tsi.getMaxCapacity() >= 0) {
        if ((tsi.numSlotsOccupied + numSlotsPerTask) > tsi.getMaxCapacity()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(
              "Queue " + qsi.queueName + " " + "has reached its  max " + type +
                "Capacity");
            LOG.debug("Current running tasks " + tsi.getCapacity());

          }
          return true;
        }
      }
      return false;
    }

    // for debugging.
    private void printQSIs() {
      if (LOG.isDebugEnabled()) {
        StringBuffer s = new StringBuffer();
        for (QueueSchedulingInfo qsi : qsiForAssigningTasks) {
          TaskSchedulingInfo tsi = getTSI(qsi);
          Collection<JobInProgress> runJobs =
            scheduler.jobQueuesManager.getRunningJobQueue(qsi.queueName);
          s.append(
            String.format(
              " Queue '%s'(%s): runningTasks=%d, "
                + "occupiedSlots=%d, capacity=%d, runJobs=%d  maxCapacity=%d ",
              qsi.queueName,
              this.type, Integer.valueOf(tsi.numRunningTasks), Integer
                .valueOf(tsi.numSlotsOccupied), Integer
                .valueOf(tsi.getCapacity()), Integer.valueOf(runJobs.size()),
              Integer.valueOf(tsi.getMaxCapacity())));
        }
        LOG.debug(s);
      }
    }
    
    /**
     * Check if one of the tasks have a speculative task to execute on the 
     * particular task tracker.
     * 
     * @param tips tasks of a job
     * @param progress percentage progress of the job
     * @param tts task tracker status for which we are asking speculative tip
     * @return true if job has a speculative task to run on particular TT.
     */
    boolean hasSpeculativeTask(TaskInProgress[] tips, float progress, 
        TaskTrackerStatus tts) {
      long currentTime = System.currentTimeMillis();
      for(TaskInProgress tip : tips)  {
        if(tip.isRunning() 
            && !(tip.hasRunOnMachine(tts.getHost(), tts.getTrackerName())) 
            && tip.hasSpeculativeTask(currentTime, progress)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * The scheduling algorithms for map tasks. 
   */
  private static class MapSchedulingMgr extends TaskSchedulingMgr {

    MapSchedulingMgr(CapacityTaskScheduler schedulr) {
      super(schedulr);
      type = TaskType.MAP;
      queueComparator = mapComparator;
    }

    @Override
    Task obtainNewTask(TaskTrackerStatus taskTracker, JobInProgress job) 
    throws IOException {
      ClusterStatus clusterStatus = 
        scheduler.taskTrackerManager.getClusterStatus();
      int numTaskTrackers = clusterStatus.getTaskTrackers();
      return job.obtainNewMapTask(taskTracker, numTaskTrackers, 
          scheduler.taskTrackerManager.getNumberOfUniqueHosts());
    }

    @Override
    int getClusterCapacity() {
      return scheduler.taskTrackerManager.getClusterStatus().getMaxMapTasks();
    }

    @Override
    int getRunningTasks(JobInProgress job) {
      return job.runningMaps();
    }

    @Override
    int getPendingTasks(JobInProgress job) {
      return job.pendingMaps();
    }

    @Override
    int getSlotsPerTask(JobInProgress job) {
      return job.getNumSlotsPerTask(TaskType.MAP);
    }

    @Override
    TaskSchedulingInfo getTSI(QueueSchedulingInfo qsi) {
      return qsi.mapTSI;
    }

    int getNumReservedTaskTrackers(JobInProgress job) {
      return job.getNumReservedTaskTrackersForMaps();
    }

    @Override
    boolean hasSpeculativeTask(JobInProgress job, TaskTrackerStatus tts) {
      //Check if job supports speculative map execution first then 
      //check if job has speculative maps.
      return (job.getMapSpeculativeExecution())&& (
          hasSpeculativeTask(job.getTasks(TaskType.MAP), 
              job.getStatus().mapProgress(), tts));
    }

  }

  /**
   * The scheduling algorithms for reduce tasks. 
   */
  private static class ReduceSchedulingMgr extends TaskSchedulingMgr {

    ReduceSchedulingMgr(CapacityTaskScheduler schedulr) {
      super(schedulr);
      type = TaskType.REDUCE;
      queueComparator = reduceComparator;
    }

    @Override
    Task obtainNewTask(TaskTrackerStatus taskTracker, JobInProgress job) 
    throws IOException {
      ClusterStatus clusterStatus = 
        scheduler.taskTrackerManager.getClusterStatus();
      int numTaskTrackers = clusterStatus.getTaskTrackers();
      return job.obtainNewReduceTask(taskTracker, numTaskTrackers, 
          scheduler.taskTrackerManager.getNumberOfUniqueHosts());
    }

    @Override
    int getClusterCapacity() {
      return scheduler.taskTrackerManager.getClusterStatus()
          .getMaxReduceTasks();
    }

    @Override
    int getRunningTasks(JobInProgress job) {
      return job.runningReduces();
    }

    @Override
    int getPendingTasks(JobInProgress job) {
      return job.pendingReduces();
    }

    @Override
    int getSlotsPerTask(JobInProgress job) {
      return job.getNumSlotsPerTask(TaskType.REDUCE);    
    }

    @Override
    TaskSchedulingInfo getTSI(QueueSchedulingInfo qsi) {
      return qsi.reduceTSI;
    }

    int getNumReservedTaskTrackers(JobInProgress job) {
      return job.getNumReservedTaskTrackersForReduces();
    }

    @Override
    boolean hasSpeculativeTask(JobInProgress job, TaskTrackerStatus tts) {
      //check if the job supports reduce speculative execution first then
      //check if the job has speculative tasks.
      return (job.getReduceSpeculativeExecution()) && (
          hasSpeculativeTask(job.getTasks(TaskType.REDUCE), 
              job.getStatus().reduceProgress(), tts));
    }

  }
  
  /** the scheduling mgrs for Map and Reduce tasks */ 
  protected TaskSchedulingMgr mapScheduler = new MapSchedulingMgr(this);
  protected TaskSchedulingMgr reduceScheduler = new ReduceSchedulingMgr(this);

  MemoryMatcher memoryMatcher = new MemoryMatcher(this);

  /** we keep track of the number of map/reduce slots we saw last */
  private int prevMapClusterCapacity = 0;
  private int prevReduceClusterCapacity = 0;
  
    
  static final Log LOG = LogFactory.getLog(CapacityTaskScheduler.class);
  protected JobQueuesManager jobQueuesManager;
  protected CapacitySchedulerConf schedConf;
  /** whether scheduler has started or not */
  private boolean started = false;

  /**
   * A clock class - can be mocked out for testing.
   */
  static class Clock {
    long getTime() {
      return System.currentTimeMillis();
    }
  }

  private Clock clock;
  private JobInitializationPoller initializationPoller;

  private long memSizeForMapSlotOnJT;
  private long memSizeForReduceSlotOnJT;
  private long limitMaxMemForMapTasks;
  private long limitMaxMemForReduceTasks;
  private boolean assignMultipleTasks = true;

  public CapacityTaskScheduler() {
    this(new Clock());
  }
  
  // for testing
  public CapacityTaskScheduler(Clock clock) {
    this.jobQueuesManager = new JobQueuesManager(this);
    this.clock = clock;
  }
  
  /** mostly for testing purposes */
  public void setResourceManagerConf(CapacitySchedulerConf conf) {
    this.schedConf = conf;
  }

  private void initializeMemoryRelatedConf() {
    //handling @deprecated
    if (conf.get(
      CapacitySchedulerConf.DEFAULT_PERCENTAGE_OF_PMEM_IN_VMEM_PROPERTY) !=
      null) {
      LOG.warn(
        JobConf.deprecatedString(
          CapacitySchedulerConf.DEFAULT_PERCENTAGE_OF_PMEM_IN_VMEM_PROPERTY));
    }

    //handling @deprecated
    if (conf.get(CapacitySchedulerConf.UPPER_LIMIT_ON_TASK_PMEM_PROPERTY) !=
      null) {
      LOG.warn(
        JobConf.deprecatedString(
          CapacitySchedulerConf.UPPER_LIMIT_ON_TASK_PMEM_PROPERTY));
    }

    if (conf.get(JobConf.MAPRED_TASK_DEFAULT_MAXVMEM_PROPERTY) != null) {
      LOG.warn(
        JobConf.deprecatedString(
          JobConf.MAPRED_TASK_DEFAULT_MAXVMEM_PROPERTY));
    }

    memSizeForMapSlotOnJT =
        JobConf.normalizeMemoryConfigValue(conf.getLong(
            JobTracker.MAPRED_CLUSTER_MAP_MEMORY_MB_PROPERTY,
            JobConf.DISABLED_MEMORY_LIMIT));
    memSizeForReduceSlotOnJT =
        JobConf.normalizeMemoryConfigValue(conf.getLong(
            JobTracker.MAPRED_CLUSTER_REDUCE_MEMORY_MB_PROPERTY,
            JobConf.DISABLED_MEMORY_LIMIT));

    //handling @deprecated values
    if (conf.get(JobConf.UPPER_LIMIT_ON_TASK_VMEM_PROPERTY) != null) {
      LOG.warn(
        JobConf.deprecatedString(
          JobConf.UPPER_LIMIT_ON_TASK_VMEM_PROPERTY)+
          " instead use " +JobTracker.MAPRED_CLUSTER_MAX_MAP_MEMORY_MB_PROPERTY+
          " and " + JobTracker.MAPRED_CLUSTER_MAX_REDUCE_MEMORY_MB_PROPERTY
      );
      
      limitMaxMemForMapTasks = limitMaxMemForReduceTasks =
        JobConf.normalizeMemoryConfigValue(
          conf.getLong(
            JobConf.UPPER_LIMIT_ON_TASK_VMEM_PROPERTY,
            JobConf.DISABLED_MEMORY_LIMIT));
      if (limitMaxMemForMapTasks != JobConf.DISABLED_MEMORY_LIMIT &&
        limitMaxMemForMapTasks >= 0) {
        limitMaxMemForMapTasks = limitMaxMemForReduceTasks =
          limitMaxMemForMapTasks /
            (1024 * 1024); //Converting old values in bytes to MB
      }
    } else {
      limitMaxMemForMapTasks =
        JobConf.normalizeMemoryConfigValue(
          conf.getLong(
            JobTracker.MAPRED_CLUSTER_MAX_MAP_MEMORY_MB_PROPERTY,
            JobConf.DISABLED_MEMORY_LIMIT));
      limitMaxMemForReduceTasks =
        JobConf.normalizeMemoryConfigValue(
          conf.getLong(
            JobTracker.MAPRED_CLUSTER_MAX_REDUCE_MEMORY_MB_PROPERTY,
            JobConf.DISABLED_MEMORY_LIMIT));
    }
    LOG.info(String.format("Scheduler configured with "
        + "(memSizeForMapSlotOnJT, memSizeForReduceSlotOnJT, "
        + "limitMaxMemForMapTasks, limitMaxMemForReduceTasks)"
        + " (%d,%d,%d,%d)", Long.valueOf(memSizeForMapSlotOnJT), Long
        .valueOf(memSizeForReduceSlotOnJT), Long
        .valueOf(limitMaxMemForMapTasks), Long
        .valueOf(limitMaxMemForReduceTasks)));
  }

  long getMemSizeForMapSlot() {
    return memSizeForMapSlotOnJT;
  }

  long getMemSizeForReduceSlot() {
    return memSizeForReduceSlotOnJT;
  }

  long getLimitMaxMemForMapSlot() {
    return limitMaxMemForMapTasks;
  }

  long getLimitMaxMemForReduceSlot() {
    return limitMaxMemForReduceTasks;
  }

  String[] getOrderedQueues(TaskType type) {
    if (type == TaskType.MAP) {
      return mapScheduler.getOrderedQueues();
    } else if (type == TaskType.REDUCE) {
      return reduceScheduler.getOrderedQueues();
    }
    return null;
  }

  @Override
  public synchronized void start() throws IOException {
    if (started) return;
    super.start();
    // initialize our queues from the config settings
    if (null == schedConf) {
      schedConf = new CapacitySchedulerConf();
    }

    initializeMemoryRelatedConf();
    
    // read queue info from config file
    QueueManager queueManager = taskTrackerManager.getQueueManager();
    Set<String> queues = queueManager.getQueues();
    // Sanity check: there should be at least one queue. 
    if (0 == queues.size()) {
      throw new IllegalStateException("System has no queue configured");
    }

    Set<String> queuesWithoutConfiguredCapacity = new HashSet<String>();
    float totalCapacityPercent = 0.0f;
    for (String queueName: queues) {
      float capacityPercent = schedConf.getCapacity(queueName);
      if (capacityPercent == -1.0) {
        queuesWithoutConfiguredCapacity.add(queueName);
      }else {
        totalCapacityPercent += capacityPercent;
      }

      float maxCapacityPercent = schedConf.getMaxCapacity(queueName);
      int ulMin = schedConf.getMinimumUserLimitPercent(queueName);
      // create our QSI and add to our hashmap
      QueueSchedulingInfo qsi = new QueueSchedulingInfo(
        queueName, capacityPercent, maxCapacityPercent ,ulMin, jobQueuesManager);
      queueInfoMap.put(queueName, qsi);

      // create the queues of job objects
      boolean supportsPrio = schedConf.isPrioritySupported(queueName);
      jobQueuesManager.createQueue(queueName, supportsPrio);
      
      SchedulingDisplayInfo schedulingInfo = 
        new SchedulingDisplayInfo(queueName, this);
      queueManager.setSchedulerInfo(queueName, schedulingInfo);
      
    }
    float remainingQuantityToAllocate = 100 - totalCapacityPercent;
    float quantityToAllocate = 
      remainingQuantityToAllocate/queuesWithoutConfiguredCapacity.size();
    for(String queue: queuesWithoutConfiguredCapacity) {
      QueueSchedulingInfo qsi = queueInfoMap.get(queue); 
      qsi.capacityPercent = quantityToAllocate;
      if(qsi.maxCapacityPercent >= 0) {
        if(qsi.capacityPercent > qsi.maxCapacityPercent) {
          throw new IllegalStateException(
            " Allocated capacity of " + qsi.capacityPercent +
              " to unconfigured queue " + qsi.queueName +
              " is greater than maximum Capacity " + qsi.maxCapacityPercent);
        }
      }
      schedConf.setCapacity(queue, quantityToAllocate);
    }    
    
    if (totalCapacityPercent > 100.0) {
      throw new IllegalArgumentException(
        "Sum of queue capacities over 100% at "
          + totalCapacityPercent);
    }    
    
    // let our mgr objects know about the queues
    mapScheduler.initialize(queueInfoMap);
    reduceScheduler.initialize(queueInfoMap);
    
    // listen to job changes
    taskTrackerManager.addJobInProgressListener(jobQueuesManager);

    //Start thread for initialization
    if (initializationPoller == null) {
      this.initializationPoller = new JobInitializationPoller(
          jobQueuesManager,schedConf,queues, taskTrackerManager);
    }
    initializationPoller.init(queueManager.getQueues(), schedConf);
    initializationPoller.setDaemon(true);
    initializationPoller.start();

    started = true;
    LOG.info("Capacity scheduler initialized " + queues.size() + " queues");  
  }
  
  /** mostly for testing purposes */
  void setInitializationPoller(JobInitializationPoller p) {
    this.initializationPoller = p;
  }
  
  @Override
  public synchronized void terminate() throws IOException {
    if (!started) return;
    if (jobQueuesManager != null) {
      taskTrackerManager.removeJobInProgressListener(
          jobQueuesManager);
    }
    started = false;
    initializationPoller.terminate();
    super.terminate();
  }
  
  @Override
  public synchronized void setConf(Configuration conf) {
    super.setConf(conf);
  }

  /**
   * provided for the test classes
   * lets you update the QSI objects and sorted collections
   */ 
  void updateQSIInfoForTests() {
    ClusterStatus c = taskTrackerManager.getClusterStatus();
    int mapClusterCapacity = c.getMaxMapTasks();
    int reduceClusterCapacity = c.getMaxReduceTasks();
    // update the QSI objects
    updateQSIObjects(mapClusterCapacity, reduceClusterCapacity);
    mapScheduler.updateCollectionOfQSIs();
    reduceScheduler.updateCollectionOfQSIs();
  }

  /**
   * Update individual QSI objects.
   * We don't need exact information for all variables, just enough for us
   * to make scheduling decisions. For example, we don't need an exact count
   * of numRunningTasks. Once we count upto the grid capacity, any
   * number beyond that will make no difference.
   */
  private synchronized void updateQSIObjects(
    int mapClusterCapacity,
      int reduceClusterCapacity) {
    // if # of slots have changed since last time, update.
    // First, compute whether the total number of TT slots have changed
    for (QueueSchedulingInfo qsi: queueInfoMap.values()) {
      // compute new capacities, if TT slots have changed
      if (mapClusterCapacity != prevMapClusterCapacity) {
        qsi.mapTSI.setCapacity(
          (int)
          (qsi.capacityPercent*mapClusterCapacity/100));

        //compute new max map capacities
        if(qsi.maxCapacityPercent > 0) {
          qsi.mapTSI.setMaxCapacity(
            (int) (qsi.maxCapacityPercent * mapClusterCapacity / 100));
        }
      }
      if (reduceClusterCapacity != prevReduceClusterCapacity) {
        qsi.reduceTSI.setCapacity(
          (int)
            (qsi.capacityPercent * reduceClusterCapacity / 100));

        //compute new max reduce capacities
        if (qsi.maxCapacityPercent > 0) {
          qsi.reduceTSI.setMaxCapacity(
            (int) (qsi.maxCapacityPercent * reduceClusterCapacity / 100));
        }
      }

      // reset running/pending tasks, tasks per user
      qsi.mapTSI.resetTaskVars();
      qsi.reduceTSI.resetTaskVars();
      // update stats on running jobs
      for (JobInProgress j:
        jobQueuesManager.getRunningJobQueue(qsi.queueName)) {
        if (j.getStatus().getRunState() != JobStatus.RUNNING) {
          continue;
        }

        int numMapsRunningForThisJob = mapScheduler.getRunningTasks(j);
        int numReducesRunningForThisJob = reduceScheduler.getRunningTasks(j);
        int numRunningMapSlots = 
          numMapsRunningForThisJob * mapScheduler.getSlotsPerTask(j);
        int numRunningReduceSlots =
          numReducesRunningForThisJob * reduceScheduler.getSlotsPerTask(j);
        int numMapSlotsForThisJob = mapScheduler.getSlotsOccupied(j);
        int numReduceSlotsForThisJob = reduceScheduler.getSlotsOccupied(j);
        int numReservedMapSlotsForThisJob = 
          (mapScheduler.getNumReservedTaskTrackers(j) * 
           mapScheduler.getSlotsPerTask(j)); 
        int numReservedReduceSlotsForThisJob = 
          (reduceScheduler.getNumReservedTaskTrackers(j) * 
           reduceScheduler.getSlotsPerTask(j)); 
        j.setSchedulingInfo(getJobQueueSchedInfo(numMapsRunningForThisJob, 
                              numRunningMapSlots,
                              numReservedMapSlotsForThisJob,
                              numReducesRunningForThisJob, 
                              numRunningReduceSlots,
                              numReservedReduceSlotsForThisJob));
        qsi.mapTSI.numRunningTasks += numMapsRunningForThisJob;
        qsi.reduceTSI.numRunningTasks += numReducesRunningForThisJob;
        qsi.mapTSI.numSlotsOccupied += numMapSlotsForThisJob;
        qsi.reduceTSI.numSlotsOccupied += numReduceSlotsForThisJob;
        Integer i =
            qsi.mapTSI.numSlotsOccupiedByUser.get(j.getProfile().getUser());
        qsi.mapTSI.numSlotsOccupiedByUser.put(j.getProfile().getUser(),
            Integer.valueOf(i.intValue() + numMapSlotsForThisJob));
        i = qsi.reduceTSI.numSlotsOccupiedByUser.get(j.getProfile().getUser());
        qsi.reduceTSI.numSlotsOccupiedByUser.put(j.getProfile().getUser(),
            Integer.valueOf(i.intValue() + numReduceSlotsForThisJob));
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("updateQSI: job %s: run(m)=%d, "
              + "occupied(m)=%d, run(r)=%d, occupied(r)=%d, finished(m)=%d,"
              + " finished(r)=%d, failed(m)=%d, failed(r)=%d, "
              + "spec(m)=%d, spec(r)=%d, total(m)=%d, total(r)=%d", j
              .getJobID().toString(), Integer
              .valueOf(numMapsRunningForThisJob), Integer
              .valueOf(numMapSlotsForThisJob), Integer
              .valueOf(numReducesRunningForThisJob), Integer
              .valueOf(numReduceSlotsForThisJob), Integer.valueOf(j
              .finishedMaps()), Integer.valueOf(j.finishedReduces()), Integer
              .valueOf(j.failedMapTasks),
              Integer.valueOf(j.failedReduceTasks), Integer
                  .valueOf(j.speculativeMapTasks), Integer
                  .valueOf(j.speculativeReduceTasks), Integer
                  .valueOf(j.numMapTasks), Integer.valueOf(j.numReduceTasks)));
        }

        /*
         * it's fine walking down the entire list of running jobs - there
         * probably will not be many, plus, we may need to go through the
         * list to compute numSlotsOccupiedByUser. If this is expensive, we
         * can keep a list of running jobs per user. Then we only need to
         * consider the first few jobs per user.
         */
      }
    }

    prevMapClusterCapacity = mapClusterCapacity;
    prevReduceClusterCapacity = reduceClusterCapacity;
  }

  private static final int JOBQUEUE_SCHEDULINGINFO_INITIAL_LENGTH = 175;

  static String getJobQueueSchedInfo(int numMapsRunningForThisJob, 
    int numRunningMapSlots, int numReservedMapSlotsForThisJob, 
    int numReducesRunningForThisJob, int numRunningReduceSlots, 
    int numReservedReduceSlotsForThisJob) {
    StringBuilder sb = new StringBuilder(JOBQUEUE_SCHEDULINGINFO_INITIAL_LENGTH);
    sb.append(numMapsRunningForThisJob).append(" running map tasks using ")
      .append(numRunningMapSlots).append(" map slots. ")
      .append(numReservedMapSlotsForThisJob).append(" additional slots reserved. ")
      .append(numReducesRunningForThisJob).append(" running reduce tasks using ")
      .append(numRunningReduceSlots).append(" reduce slots. ")
      .append(numReservedReduceSlotsForThisJob).append(" additional slots reserved.");
    return sb.toString();
  }

  /**
   * Sets whether the scheduler can assign multiple tasks in a heartbeat
   * or not.
   * 
   * This method is used only for testing purposes.
   * 
   * @param assignMultipleTasks true, to assign multiple tasks per heartbeat
   */
  void setAssignMultipleTasks(boolean assignMultipleTasks) {
    this.assignMultipleTasks = assignMultipleTasks;
  }

  /*
   * The grand plan for assigning a task.
   * 
   * If multiple task assignment is enabled, it tries to get one map and
   * one reduce slot depending on free slots on the TT.
   * 
   * Otherwise, we decide whether a Map or Reduce task should be given to a TT 
   * (if the TT can accept either). 
   * Either way, we first pick a queue. We only look at queues that need 
   * a slot. Among these, we first look at queues whose 
   * (# of running tasks)/capacity is the least.
   * Next, pick a job in a queue. we pick the job at the front of the queue
   * unless its user is over the user limit. 
   * Finally, given a job, pick a task from the job. 
   *  
   */
  @Override
  public synchronized List<Task> assignTasks(TaskTracker taskTracker)
  throws IOException {    
    TaskTrackerStatus taskTrackerStatus = taskTracker.getStatus();
    ClusterStatus c = taskTrackerManager.getClusterStatus();
    int mapClusterCapacity = c.getMaxMapTasks();
    int reduceClusterCapacity = c.getMaxReduceTasks();
    int maxMapSlots = taskTrackerStatus.getMaxMapSlots();
    int currentMapSlots = taskTrackerStatus.countOccupiedMapSlots();
    int maxReduceSlots = taskTrackerStatus.getMaxReduceSlots();
    int currentReduceSlots = taskTrackerStatus.countOccupiedReduceSlots();
    if (LOG.isDebugEnabled()) {
      LOG.debug("TT asking for task, max maps=" + taskTrackerStatus.getMaxMapSlots() + 
        ", run maps=" + taskTrackerStatus.countMapTasks() + ", max reds=" + 
        taskTrackerStatus.getMaxReduceSlots() + ", run reds=" + 
        taskTrackerStatus.countReduceTasks() + ", map cap=" + 
        mapClusterCapacity + ", red cap = " + 
        reduceClusterCapacity);
    }

    /* 
     * update all our QSI objects.
     * This involves updating each qsi structure. This operation depends
     * on the number of running jobs in a queue, and some waiting jobs. If it
     * becomes expensive, do it once every few heartbeats only.
     */ 
    updateQSIObjects(mapClusterCapacity, reduceClusterCapacity);
    List<Task> result = new ArrayList<Task>();
    if (assignMultipleTasks) {
      addReduceTask(taskTracker, result, maxReduceSlots, currentReduceSlots);
      addMapTask(taskTracker, result, maxMapSlots, currentMapSlots);
    } else {
      /* 
       * If TT has Map and Reduce slot free, we need to figure out whether to
       * give it a Map or Reduce task.
       * Number of ways to do this. For now, base decision on how much is needed
       * versus how much is used (default to Map, if equal).
       */
      if ((maxReduceSlots - currentReduceSlots) 
          > (maxMapSlots - currentMapSlots)) {
        addReduceTask(taskTracker, result, maxReduceSlots, currentReduceSlots);
        if (result.size() == 0) {
          addMapTask(taskTracker, result, maxMapSlots, currentMapSlots);
        }
      } else {
        addMapTask(taskTracker, result, maxMapSlots, currentMapSlots);
        if (result.size() == 0) {
          addReduceTask(taskTracker, result, maxReduceSlots, currentReduceSlots);
        }
      }
      if (result.size() == 0) {
        return null;
      }
    }
    return result;
  }

  // Pick a reduce task and add to the list of tasks, if there's space
  // on the TT to run one.
  private void addReduceTask(TaskTracker taskTracker, List<Task> tasks,
                                  int maxReduceSlots, int currentReduceSlots) 
                    throws IOException {
    if (maxReduceSlots > currentReduceSlots) {
      reduceScheduler.updateCollectionOfQSIs();
      TaskLookupResult tlr = reduceScheduler.assignTasks(taskTracker);
      if (TaskLookupResult.LookUpStatus.TASK_FOUND == tlr.getLookUpStatus()) {
        tasks.add(tlr.getTask());
      }
    }
  }
  
  // Pick a map task and add to the list of tasks, if there's space
  // on the TT to run one.
  private void addMapTask(TaskTracker taskTracker, List<Task> tasks, 
                              int maxMapSlots, int currentMapSlots)
                    throws IOException {
    if (maxMapSlots > currentMapSlots) {
      mapScheduler.updateCollectionOfQSIs();
      TaskLookupResult tlr = mapScheduler.assignTasks(taskTracker);
      if (TaskLookupResult.LookUpStatus.TASK_FOUND == tlr.getLookUpStatus()) {
        tasks.add(tlr.getTask());
      }
    }
  }
  
  // called when a job is added
  synchronized void jobAdded(JobInProgress job) throws IOException {
    QueueSchedulingInfo qsi = 
      queueInfoMap.get(job.getProfile().getQueueName());
    // qsi shouldn't be null
    // update user-specific info
    Integer i = qsi.numJobsByUser.get(job.getProfile().getUser());
    if (null == i) {
      i = 1;
      // set the count for running tasks to 0
      qsi.mapTSI.numSlotsOccupiedByUser.put(job.getProfile().getUser(),
          Integer.valueOf(0));
      qsi.reduceTSI.numSlotsOccupiedByUser.put(job.getProfile().getUser(),
          Integer.valueOf(0));
    }
    else {
      i++;
    }
    qsi.numJobsByUser.put(job.getProfile().getUser(), i);
    
    // setup scheduler specific job information
    preInitializeJob(job);
    
    if (LOG.isDebugEnabled()) {
      LOG.debug("Job " + job.getJobID().toString() + " is added under user " 
              + job.getProfile().getUser() + ", user now has " + i + " jobs");
    }
  }

  /**
   * Setup {@link CapacityTaskScheduler} specific information prior to
   * job initialization.
   */
  void preInitializeJob(JobInProgress job) {
    JobConf jobConf = job.getJobConf();
    
    // Compute number of slots required to run a single map/reduce task
    int slotsPerMap = 1;
    int slotsPerReduce = 1;
    if (memoryMatcher.isSchedulingBasedOnMemEnabled()) {
      slotsPerMap = jobConf.computeNumSlotsPerMap(getMemSizeForMapSlot());
     slotsPerReduce = 
       jobConf.computeNumSlotsPerReduce(getMemSizeForReduceSlot());
    }
    job.setNumSlotsPerMap(slotsPerMap);
    job.setNumSlotsPerReduce(slotsPerReduce);
  }
  
  // called when a job completes
  synchronized void jobCompleted(JobInProgress job) {
    QueueSchedulingInfo qsi = 
      queueInfoMap.get(job.getProfile().getQueueName());
    // qsi shouldn't be null
    // update numJobsByUser
    if (LOG.isDebugEnabled()) {
      LOG.debug("Job to be removed for user " + job.getProfile().getUser());
    }
    Integer i = qsi.numJobsByUser.get(job.getProfile().getUser());
    i--;
    if (0 == i.intValue()) {
      qsi.numJobsByUser.remove(job.getProfile().getUser());
      // remove job footprint from our TSIs
      qsi.mapTSI.numSlotsOccupiedByUser.remove(job.getProfile().getUser());
      qsi.reduceTSI.numSlotsOccupiedByUser.remove(job.getProfile().getUser());
      if (LOG.isDebugEnabled()) {
        LOG.debug("No more jobs for user, number of users = " + qsi.numJobsByUser.size());
      }
    }
    else {
      qsi.numJobsByUser.put(job.getProfile().getUser(), i);
      if (LOG.isDebugEnabled()) {
        LOG.debug("User still has " + i + " jobs, number of users = "
                + qsi.numJobsByUser.size());
      }
    }
  }
  
  @Override
  public synchronized Collection<JobInProgress> getJobs(String queueName) {
    Collection<JobInProgress> jobCollection = new ArrayList<JobInProgress>();
    Collection<JobInProgress> runningJobs = 
        jobQueuesManager.getRunningJobQueue(queueName);
    if (runningJobs != null) {
      jobCollection.addAll(runningJobs);
    }
    Collection<JobInProgress> waitingJobs = 
      jobQueuesManager.getWaitingJobs(queueName);
    Collection<JobInProgress> tempCollection = new ArrayList<JobInProgress>();
    if(waitingJobs != null) {
      tempCollection.addAll(waitingJobs);
    }
    tempCollection.removeAll(runningJobs);
    if(!tempCollection.isEmpty()) {
      jobCollection.addAll(tempCollection);
    }
    return jobCollection;
  }
  
  JobInitializationPoller getInitializationPoller() {
    return initializationPoller;
  }

  synchronized String getDisplayInfo(String queueName) {
    QueueSchedulingInfo qsi = queueInfoMap.get(queueName);
    if (null == qsi) { 
      return null;
    }
    return qsi.toString();
  }

}


