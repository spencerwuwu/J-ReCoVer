// https://searchcode.com/api/result/74234598/

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapred.JobClient.RawSplit;
import org.apache.hadoop.mapred.SortedRanges.Range;
import org.apache.hadoop.mapred.TaskStatus.Phase;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.util.StringUtils;

/*************************************************************
 * TaskInProgress maintains all the info needed for a
 * Task in the lifetime of its owning Job.  A given Task
 * might be speculatively executed or reexecuted, so we
 * need a level of indirection above the running-id itself.
 * <br>
 * A given TaskInProgress contains multiple taskids,
 * 0 or more of which might be executing at any one time.
 * (That's what allows speculative execution.)  A taskid
 * is now *never* recycled.  A TIP allocates enough taskids
 * to account for all the speculation and failures it will
 * ever have to handle.  Once those are up, the TIP is dead.
 * **************************************************************
 */
class TaskInProgress {
  static final int MAX_TASK_EXECS = 1;
  int maxTaskAttempts = 4;    
  long speculativeLag;
  double maxProgressRateForSpeculation;
  private boolean speculativeForced = false;
  private boolean useProcessingRateForSpeculation = false;
  private static final int NUM_ATTEMPTS_PER_RESTART = 1000;

  public static final Log LOG = LogFactory.getLog(TaskInProgress.class);

  // Defines the TIP
  private String jobFile = null;
  private RawSplit rawSplit;
  private int numMaps;
  private int partition;
  private TaskID id;
  private JobInProgressTraits job;
  private final int numSlotsRequired;

  // Status of the TIP
  private int successEventNumber = -1;
  private int numTaskFailures = 0;
  private int numKilledTasks = 0;
  private double progress = 0;
  private double progressRate;
  private Phase processingPhase;
  private ProcessingRates processingRates = new ProcessingRates(0, 0, 0, 0);
  private String state = "";
  private long startTime = 0;
  private long lastDispatchTime = 0; // most recent time task given to TT
  private long execStartTime = 0;
  private long execFinishTime = 0;
  private int completes = 0;
  private boolean failed = false;
  private boolean killed = false;
  private long maxSkipRecords = 0;
  private FailedRanges failedRanges = new FailedRanges();
  private volatile boolean skipping = false;
  private boolean jobCleanup = false; 
  private boolean jobSetup = false;
   
  // The 'next' usable taskid of this tip
  int nextTaskId = 0;
    
  // The taskid that took this TIP to SUCCESS
  private TaskAttemptID successfulTaskId;

  // The first taskid of this tip
  private TaskAttemptID firstTaskId;
  
  // The taskid of speculative task
  private TaskAttemptID speculativeTaskId;
  
  // Map from task Id -> TaskTracker Id, contains tasks that are
  // currently runnings
  private TreeMap<TaskAttemptID, String> activeTasks = new TreeMap<TaskAttemptID, String>();
  // All attempt Ids of this TIP
  private TreeSet<TaskAttemptID> tasks = new TreeSet<TaskAttemptID>();
  private JobConf conf;
  private Map<TaskAttemptID,List<String>> taskDiagnosticData =
    new TreeMap<TaskAttemptID,List<String>>();
  /**
   * Map from taskId -> TaskStatus
   */
  TreeMap<TaskAttemptID,TaskStatus> taskStatuses = 
    new TreeMap<TaskAttemptID,TaskStatus>();

  // Map from taskId -> TaskTracker Id, 
  // contains cleanup attempts and where they ran, if any
  private TreeMap<TaskAttemptID, String> cleanupTasks =
    new TreeMap<TaskAttemptID, String>();

  private TreeSet<String> machinesWhereFailed = new TreeSet<String>();
  private TreeSet<TaskAttemptID> tasksReportedClosed = new TreeSet<TaskAttemptID>();
  
  //list of tasks to kill, <taskid> -> <shouldFail> 
  private TreeMap<TaskAttemptID, Boolean> tasksToKill = new TreeMap<TaskAttemptID, Boolean>();
  
  //task to commit, <taskattemptid>  
  private TaskAttemptID taskToCommit;
  
  private volatile Counters counters = new Counters();
  
  private HashMap<TaskAttemptID, Long> dispatchTimeMap = 
    new HashMap<TaskAttemptID, Long>();
  
  // Whether to use the input record processing rate when speculating maps
  // based on the processing rate. Has no effect if speculating based on the
  // progress rate.
  public static final String USE_MAP_RECORDS_PROCESSING_RATE = 
      "mapreduce.job.speculative.use.map.record.rate";
  
  /**
   * Private helper class to pass around / store processing rates more easily
   */
  private final static class ProcessingRates {
    private double mapRate = 0;
    private double copyRate = 0;
    private double sortRate = 0;
    private double reduceRate = 0;
    
    public ProcessingRates(double mapRate, double copyRate, double sortRate,
        double reduceRate) {
      this.mapRate = mapRate;
      this.copyRate = copyRate;
      this.sortRate = sortRate;
      this.reduceRate = reduceRate;
    }
    
    public ProcessingRates(ProcessingRates p) {
      this.mapRate = p.mapRate;
      this.copyRate = p.copyRate;
      this.sortRate = p.sortRate;
      this.reduceRate = p.reduceRate;
    }
    
    public double getRate(Phase p) {
      if (p == Phase.MAP) {
        return this.mapRate;
      } else if (p == Phase.SHUFFLE) {
        return this.copyRate;
      } else if (p == Phase.SORT) {
        return this.sortRate;
      } else if (p == Phase.REDUCE) {
        return this.reduceRate;
      } else {
        throw new RuntimeException("Invalid phase " + p);
      }
    }
  }
  
  /**
   * Constructor for MapTask
   */
  public TaskInProgress(JobID jobid, String jobFile, 
                        RawSplit rawSplit, 
                        JobConf conf, 
                        JobInProgressTraits job, int partition,
                        int numSlotsRequired) {
    this.jobFile = jobFile;
    this.rawSplit = rawSplit;
    this.job = job;
    this.conf = conf;
    this.partition = partition;
    this.maxSkipRecords = SkipBadRecords.getMapperMaxSkipRecords(conf);
    this.numSlotsRequired = numSlotsRequired;
    setMaxTaskAttempts();
    init(jobid);
  }
        
  /**
   * Constructor for ReduceTask
   */
  public TaskInProgress(JobID jobid, String jobFile, 
                        int numMaps, 
                        int partition, JobConf conf,
                        JobInProgressTraits job, int numSlotsRequired) {
    this.jobFile = jobFile;
    this.numMaps = numMaps;
    this.partition = partition;
    this.job = job;
    this.conf = conf;
    this.maxSkipRecords = SkipBadRecords.getReducerMaxSkipGroups(conf);
    this.numSlotsRequired = numSlotsRequired;
    setMaxTaskAttempts();
    init(jobid);
  }
  
  /**
   * Set the max number of attempts before we declare a TIP as "failed"
   */
  private void setMaxTaskAttempts() {
    if (isMapTask()) {
      this.maxTaskAttempts = conf.getMaxMapAttempts();
    } else {
      this.maxTaskAttempts = conf.getMaxReduceAttempts();
    }
  }
    
  /**
   * Return the index of the tip within the job, so 
   * "task_200707121733_1313_0002_m_012345" would return 12345;
   * @return int the tip index
   */
  public int idWithinJob() {
    return partition;
  }    

  public boolean isJobCleanupTask() {
   return jobCleanup;
  }
  
  public void setJobCleanupTask() {
    jobCleanup = true;
  }

  public boolean isJobSetupTask() {
    return jobSetup;
  }
	  
  public void setJobSetupTask() {
    jobSetup = true;
  }

  public boolean isOnlyCommitPending() {
    for (TaskStatus t : taskStatuses.values()) {
      if (t.getRunState() == TaskStatus.State.COMMIT_PENDING) {
        return true;
      }
    }
    return false;
  }
 
  public boolean isCommitPending(TaskAttemptID taskId) {
    TaskStatus t = taskStatuses.get(taskId);
    if (t == null) {
      return false;
    }
    return t.getRunState() ==  TaskStatus.State.COMMIT_PENDING;
  }
  
  /**
   * @return true if using processing rate to determine whether the task should
   * be speculated
   */
  public boolean isUsingProcessingRateForSpeculation() {
    return useProcessingRateForSpeculation;
  }
  
  /**
   * Initialization common to Map and Reduce
   */
  void init(JobID jobId) {
    this.startTime = JobTracker.getClock().getTime();
    this.id = new TaskID(jobId, isMapTask(), partition);
    this.skipping = startSkipping();
    long speculativeDuration;
    if (isMapTask()) {
      this.speculativeLag = conf.getMapSpeculativeLag();
      speculativeDuration = conf.getMapSpeculativeDuration();
    } else {
      this.speculativeLag = conf.getReduceSpeculativeLag();
      speculativeDuration = conf.getReduceSpeculativeDuration();
    }

    // speculate only if 1/(1000 * progress_rate) > speculativeDuration
    // ie. :
    // speculate only if progress_rate < 1/(1000 * speculativeDuration)

    if (speculativeDuration > 0) {
      this.maxProgressRateForSpeculation = 1.0/(1000.0*speculativeDuration);
    } else {
      // disable this check for durations <= 0
      this.maxProgressRateForSpeculation = -1.0;
    }
    
    this.useProcessingRateForSpeculation = 
        conf.getBoolean("mapreduce.job.speculative.using.processing.rate", 
            false);
  }

  ////////////////////////////////////
  // Accessors, info, profiles, etc.
  ////////////////////////////////////

  
  /**
   * Return the dispatch time
   */
  public long getDispatchTime(TaskAttemptID taskid){
    Long l = dispatchTimeMap.get(taskid);
    if (l != null) {
      return l.longValue();
    }
    return 0;
  }

  public long getLastDispatchTime(){
    return this.lastDispatchTime;
  }
  
  /**
   * Set the dispatch time
   */
  public void setDispatchTime(TaskAttemptID taskid, long disTime){
    dispatchTimeMap.put(taskid, disTime);
    this.lastDispatchTime = disTime;
  }
  /**
   * Return the start time
   */
  public long getStartTime() {
    return startTime;
  }
  
  /**
   * Return the exec start time
   */
  public long getExecStartTime() {
    return execStartTime;
  }
  
  /**
   * Set the exec start time
   */
  public void setExecStartTime(long startTime) {
    execStartTime = startTime;
  }
  
  /**
   * Return the exec finish time
   */
  public long getExecFinishTime() {
    return execFinishTime;
  }

  /**
   * Set the exec finish time
   */
  public void setExecFinishTime(long finishTime) {
    execFinishTime = finishTime;
    JobHistory.Task.logUpdates(id, execFinishTime); // log the update
  }
  
  /**
   * Return the parent job
   */
  public JobInProgressTraits getJob() {
    return job;
  }
  /**
   * Return an ID for this task, not its component taskid-threads
   */
  public TaskID getTIPId() {
    return this.id;
  }
  /**
   * Whether this is a map task
   */
  public boolean isMapTask() {
    return rawSplit != null;
  }
    
  /**
   * Returns the type of the {@link TaskAttemptID} passed. 
   * The type of an attempt is determined by the nature of the task and not its 
   * id. 
   * For example,
   * - Attempt 'attempt_123_01_m_01_0' might be a job-setup task even though it 
   *   has a _m_ in its id. Hence the task type of this attempt is JOB_SETUP 
   *   instead of MAP.
   * - Similarly reduce attempt 'attempt_123_01_r_01_0' might have failed and is
   *   now supposed to do the task-level cleanup. In such a case this attempt 
   *   will be of type TASK_CLEANUP instead of REDUCE.
   */
  TaskType getAttemptType (TaskAttemptID id) {
    if (isCleanupAttempt(id)) {
      return TaskType.TASK_CLEANUP;
    } else if (isJobSetupTask()) {
      return TaskType.JOB_SETUP;
    } else if (isJobCleanupTask()) {
      return TaskType.JOB_CLEANUP;
    } else if (isMapTask()) {
      return TaskType.MAP;
    } else {
      return TaskType.REDUCE;
    }
  }
  
  /**
   * Is the Task associated with taskid is the first attempt of the tip? 
   * @param taskId
   * @return Returns true if the Task is the first attempt of the tip
   */  
  public boolean isFirstAttempt(TaskAttemptID taskId) {
    return firstTaskId == null ? false : firstTaskId.equals(taskId); 
  }

  /**
   * Is the Task associated with taskid is the speculative attempt of the tip? 
   * @param taskId
   * @return Returns true if the Task is the speculative attempt of the tip
   */  
  public boolean isSpeculativeAttempt(TaskAttemptID taskId) {
    return speculativeTaskId == null ? false : 
    					speculativeTaskId.equals(taskId); 
  }
  
  /**
   * Is this tip currently running any tasks?
   * @return true if any tasks are running
   */
  public boolean isRunning() {
    return !activeTasks.isEmpty();
  }

  /**
   * Is this attempt currently running ?
   * @param  taskId task attempt id.
   * @return true if attempt taskId is running
   */
  boolean isAttemptRunning(TaskAttemptID taskId) {
    return activeTasks.containsKey(taskId);
  }
    
  TaskAttemptID getSuccessfulTaskid() {
    return successfulTaskId;
  }
  
  private void setSuccessfulTaskid(TaskAttemptID successfulTaskId) {
    this.successfulTaskId = successfulTaskId; 
  }
  
  private void resetSuccessfulTaskid() {
    this.successfulTaskId = null; 
  }
  
  /**
   * Is this tip complete?
   * 
   * @return <code>true</code> if the tip is complete, else <code>false</code>
   */
  public synchronized boolean isComplete() {
    return (completes > 0);
  }

  /**
   * Is the given taskid the one that took this tip to completion?
   * 
   * @param taskid taskid of attempt to check for completion
   * @return <code>true</code> if taskid is complete, else <code>false</code>
   */
  public boolean isComplete(TaskAttemptID taskid) {
    return ((completes > 0) 
            && taskid.equals(getSuccessfulTaskid()));
  }

  /**
   * Is the tip a failure?
   * 
   * @return <code>true</code> if tip has failed, else <code>false</code>
   */
  public boolean isFailed() {
    return failed;
  }

  /**
   * Number of times the TaskInProgress has failed.
   */
  public int numTaskFailures() {
    return numTaskFailures;
  }

  /**
   * Number of times the TaskInProgress has been killed by the framework.
   */
  public int numKilledTasks() {
    return numKilledTasks;
  }

  /**
   * Get the overall progress (from 0 to 1.0) for this TIP
   */
  public double getProgress() {
    return progress;
  }

  /**
   * Get the last known progress rate for this task
   */
  public double getProgressRate() {
    return progressRate;
  }
  /**
   * Get the processing rate for this task (e.g. bytes/ms in reduce)
   */
  public double getProcessingRate(TaskStatus.Phase phase) {
    // we don't have processing rate information for the starting and cleaning
    // up phase
    if (phase != TaskStatus.Phase.MAP && 
        phase != TaskStatus.Phase.SHUFFLE &&
        phase != TaskStatus.Phase.SORT &&
        phase != TaskStatus.Phase.REDUCE) {
      return 0;
    }
    return processingRates.getRate(getProcessingPhase());
  }
  /**
   * Get the phase of processing
   */
  public Phase getProcessingPhase() {
    return processingPhase;
  }
  
  /**
   * Get the task's counters
   */
  public Counters getCounters() {
    return counters;
  }

  /**
   * Returns whether a component task-thread should be 
   * closed because the containing JobInProgress has completed
   * or the task is killed by the user
   */
  public boolean shouldClose(TaskAttemptID taskid) {
    /**
     * If the task hasn't been closed yet, and it belongs to a completed
     * TaskInProgress close it.
     * 
     * However, for completed map tasks we do not close the task which
     * actually was the one responsible for _completing_ the TaskInProgress. 
     */

    if (tasksReportedClosed.contains(taskid)) {
      if (tasksToKill.keySet().contains(taskid))
        return true;
      else
        return false;
    }

    boolean close = false;
    TaskStatus ts = taskStatuses.get(taskid);

    if ((ts != null) &&
        ((this.failed) ||
        ((job.getStatus().getRunState() != JobStatus.RUNNING &&
         (job.getStatus().getRunState() != JobStatus.PREP))))) {
      tasksReportedClosed.add(taskid);
      close = true;
    } else if ((completes > 0) && // isComplete() is synchronized!
               !(isMapTask() && !jobSetup && 
                 !jobCleanup && isComplete(taskid))) {
      tasksReportedClosed.add(taskid);
      close = true; 
    } else if (isCommitPending(taskid) && !shouldCommit(taskid)) {
      tasksReportedClosed.add(taskid);
      close = true; 
    } else {
      close = tasksToKill.keySet().contains(taskid);
    }   
    return close;
  }

  /**
   * Commit this task attempt for the tip. 
   * @param taskid
   */
  public void doCommit(TaskAttemptID taskid) {
    taskToCommit = taskid;
  }

  /**
   * Returns whether the task attempt should be committed or not 
   */
  public boolean shouldCommit(TaskAttemptID taskid) {
    return !isComplete() && isCommitPending(taskid) && 
           taskToCommit.equals(taskid);
  }

  /**
   * Creates a "status report" for this task.  Includes the
   * task ID and overall status, plus reports for all the
   * component task-threads that have ever been started.
   */
  synchronized TaskReport generateSingleReport() {
    ArrayList<String> diagnostics = new ArrayList<String>();
    for (List<String> l : taskDiagnosticData.values()) {
      diagnostics.addAll(l);
    }
    TIPStatus currentStatus = null;
    if (isRunning() && !isComplete()) {
      currentStatus = TIPStatus.RUNNING;
    } else if (isComplete()) {
      currentStatus = TIPStatus.COMPLETE;
    } else if (wasKilled()) {
      currentStatus = TIPStatus.KILLED;
    } else if (isFailed()) {
      currentStatus = TIPStatus.FAILED;
    } else if (!(isComplete() || isRunning() || wasKilled())) {
      currentStatus = TIPStatus.PENDING;
    }
    
    TaskReport report = new TaskReport
      (getTIPId(), (float)progress, state,
       diagnostics.toArray(new String[diagnostics.size()]),
       currentStatus, execStartTime, execFinishTime, counters);
    if (currentStatus == TIPStatus.RUNNING) {
      report.setRunningTaskAttempts(activeTasks.keySet());
    } else if (currentStatus == TIPStatus.COMPLETE) {
      report.setSuccessfulAttempt(getSuccessfulTaskid());
    }
    return report;
  }

  /**
   * Get the diagnostic messages for a given task within this tip.
   * 
   * @param taskId the id of the required task
   * @return the list of diagnostics for that task
   */
  synchronized List<String> getDiagnosticInfo(TaskAttemptID taskId) {
    return taskDiagnosticData.get(taskId);
  }
    
  ////////////////////////////////////////////////
  // Update methods, usually invoked by the owning
  // job.
  ////////////////////////////////////////////////
  
  /**
   * Save diagnostic information for a given task.
   * 
   * @param taskId id of the task 
   * @param diagInfo diagnostic information for the task
   */
  public void addDiagnosticInfo(TaskAttemptID taskId, String diagInfo) {
    List<String> diagHistory = taskDiagnosticData.get(taskId);
    if (diagHistory == null) {
      diagHistory = new ArrayList<String>();
      taskDiagnosticData.put(taskId, diagHistory);
    }
    diagHistory.add(diagInfo);
  }
  
  /**
   * A status message from a client has arrived.
   * It updates the status of a single component-thread-task,
   * which might result in an overall TaskInProgress status update.
   * @return has the task changed its state noticeably?
   */
  synchronized boolean updateStatus(TaskStatus status) {
    TaskAttemptID taskid = status.getTaskID();
    String taskTracker = status.getTaskTracker();
    String diagInfo = status.getDiagnosticInfo();
    TaskStatus oldStatus = taskStatuses.get(taskid);
    boolean changed = true;
    if (diagInfo != null && diagInfo.length() > 0) {
      long runTime = status.getRunTime();
      LOG.info("Error from " + taskid + " on " + taskTracker + " runTime(msec) "
        + runTime + ": " + diagInfo);
      addDiagnosticInfo(taskid, diagInfo);
    }
    
    if(skipping) {
      failedRanges.updateState(status);
    }
    
    if (oldStatus != null) {
      TaskStatus.State oldState = oldStatus.getRunState();
      TaskStatus.State newState = status.getRunState();
          
      // We should never recieve a duplicate success/failure/killed
      // status update for the same taskid! This is a safety check, 
      // and is addressed better at the TaskTracker to ensure this.
      // @see {@link TaskTracker.transmitHeartbeat()}
      if ((newState != TaskStatus.State.RUNNING && 
           newState != TaskStatus.State.COMMIT_PENDING && 
           newState != TaskStatus.State.FAILED_UNCLEAN && 
           newState != TaskStatus.State.KILLED_UNCLEAN && 
           newState != TaskStatus.State.UNASSIGNED) && 
          (oldState == newState)) {
        LOG.warn("Recieved duplicate status update of '" + newState + 
                 "' for '" + taskid + "' of TIP '" + getTIPId() + "'" +
                 "oldTT=" + oldStatus.getTaskTracker() + 
                 " while newTT=" + status.getTaskTracker());
        return false;
      }

      // The task is not allowed to move from completed back to running.
      // We have seen out of order status messagesmoving tasks from complete
      // to running. This is a spot fix, but it should be addressed more
      // globally.
      if ((newState == TaskStatus.State.RUNNING || 
          newState == TaskStatus.State.UNASSIGNED) &&
          (oldState == TaskStatus.State.FAILED || 
           oldState == TaskStatus.State.KILLED || 
           oldState == TaskStatus.State.FAILED_UNCLEAN || 
           oldState == TaskStatus.State.KILLED_UNCLEAN || 
           oldState == TaskStatus.State.SUCCEEDED ||
           oldState == TaskStatus.State.COMMIT_PENDING)) {
        return false;
      }
      
      //Do not accept any status once the task is marked FAILED/KILLED
      //This is to handle the case of the JobTracker timing out a task
      //due to launch delay, but the TT comes back with any state or 
      //TT got expired
      if (oldState == TaskStatus.State.FAILED ||
          oldState == TaskStatus.State.KILLED) {
        tasksToKill.put(taskid, true);
        return false;	  
      }
          
      changed = oldState != newState;
    }
    // if task is a cleanup attempt, do not replace the complete status,
    // update only specific fields.
    // For example, startTime should not be updated, 
    // but finishTime has to be updated.
    if (!isCleanupAttempt(taskid)) {
      taskStatuses.put(taskid, status);
      //we don't want to include setup tasks in the task execution stats
      if (!isJobSetupTask() && !isJobCleanupTask() && ((isMapTask() && job.hasSpeculativeMaps()) || 
          (!isMapTask() && job.hasSpeculativeReduces()))) {
        processingPhase = status.getPhase();
        updateProgressRate(JobTracker.getClock().getTime());
        if (useProcessingRateForSpeculation) {
          updateProcessingRate(JobTracker.getClock().getTime());
        }
      }
    } else {
      taskStatuses.get(taskid).statusUpdate(status.getRunState(),
        status.getProgress(), status.getStateString(), status.getPhase(),
        status.getFinishTime());
    }

    // Recompute progress
    recomputeProgress();
    return changed;
  }

  /**
   * Indicate that one of the taskids in this TaskInProgress
   * has failed.
   */
  public void incompleteSubTask(TaskAttemptID taskid, 
                                JobStatus jobStatus) {
    //
    // Note the failure and its location
    //
    TaskStatus status = taskStatuses.get(taskid);
    String trackerName;
    String trackerHostName = null;
    TaskStatus.State taskState = TaskStatus.State.FAILED;
    if (status != null) {
      trackerName = status.getTaskTracker();
      trackerHostName = 
        JobInProgressTraits.convertTrackerNameToHostName(trackerName);
      // Check if the user manually KILLED/FAILED this task-attempt...
      Boolean shouldFail = tasksToKill.remove(taskid);
      if (shouldFail != null) {
        if (status.getRunState() == TaskStatus.State.FAILED ||
            status.getRunState() == TaskStatus.State.KILLED) {
          taskState = (shouldFail) ? TaskStatus.State.FAILED :
                                     TaskStatus.State.KILLED;
        } else {
          taskState = (shouldFail) ? TaskStatus.State.FAILED_UNCLEAN :
                                     TaskStatus.State.KILLED_UNCLEAN;
          
        }
        status.setRunState(taskState);
        addDiagnosticInfo(taskid, "Task has been " + taskState + " by the user" );
      }
 
      taskState = status.getRunState();
      if (taskState != TaskStatus.State.FAILED && 
          taskState != TaskStatus.State.KILLED &&
          taskState != TaskStatus.State.FAILED_UNCLEAN &&
          taskState != TaskStatus.State.KILLED_UNCLEAN) {
        LOG.info("Task '" + taskid + "' running on '" + trackerName + 
                "' in state: '" + taskState + "' being failed!");
        status.setRunState(TaskStatus.State.FAILED);
        taskState = TaskStatus.State.FAILED;
      }

      // tasktracker went down and failed time was not reported. 
      if (0 == status.getFinishTime()){
        status.setFinishTime(JobTracker.getClock().getTime());
      }
    }

    this.activeTasks.remove(taskid);
    
    // Since we do not fail completed reduces (whose outputs go to hdfs), we 
    // should note this failure only for completed maps, only if this taskid;
    // completed this map. however if the job is done, there is no need to 
    // manipulate completed maps
    if (this.isMapTask() && !jobSetup && !jobCleanup && isComplete(taskid) && 
        jobStatus.getRunState() != JobStatus.SUCCEEDED) {
      this.completes--;
      
      // Reset the successfulTaskId since we don't have a SUCCESSFUL task now
      resetSuccessfulTaskid();
    }

    // Note that there can be failures of tasks that are hosted on a machine 
    // that has not yet registered with restarted jobtracker
    // recalculate the counts only if its a genuine failure
    if (tasks.contains(taskid)) {
      if (taskState == TaskStatus.State.FAILED) {
        numTaskFailures++;
        machinesWhereFailed.add(trackerHostName);
        if(maxSkipRecords>0) {
          //skipping feature enabled
          LOG.debug("TaskInProgress adding" + status.getNextRecordRange());
          failedRanges.add(status.getNextRecordRange());
          skipping = startSkipping();
        }

      } else if (taskState == TaskStatus.State.KILLED) {
        numKilledTasks++;
      }
    }

    if (numTaskFailures >= maxTaskAttempts) {
      LOG.info("TaskInProgress " + getTIPId() + " has failed " + numTaskFailures + " times.");
      kill();
    }
  }
  
  /**
   * Get whether to start skipping mode. 
   */
  private boolean startSkipping() {
    if(maxSkipRecords>0 && 
        numTaskFailures>=SkipBadRecords.getAttemptsToStartSkipping(conf)) {
      return true;
    }
    return false;
  }

  /**
   * Finalize the <b>completed</b> task; note that this might not be the first 
   * task-attempt of the {@link TaskInProgress} and hence might be declared 
   * {@link TaskStatus.State.SUCCEEDED} or {@link TaskStatus.State.KILLED}
   * 
   * @param taskId id of the completed task-attempt
   * @param finalTaskState final {@link TaskStatus.State} of the task-attempt
   */
  private void completedTask(TaskAttemptID taskId, TaskStatus.State finalTaskState) {
    TaskStatus status = taskStatuses.get(taskId);
    status.setRunState(finalTaskState);
    activeTasks.remove(taskId);
  }
  
  /**
   * Indicate that one of the taskids in this already-completed
   * TaskInProgress has successfully completed; hence we mark this
   * taskid as {@link TaskStatus.State.KILLED}. 
   */
  void alreadyCompletedTask(TaskAttemptID taskid) {
    // 'KILL' the task 
    completedTask(taskid, TaskStatus.State.KILLED);
    
    // Note the reason for the task being 'KILLED'
    addDiagnosticInfo(taskid, "Already completed TIP");
    
    LOG.info("Already complete TIP " + getTIPId() + 
             " has completed task " + taskid);
  }

  /**
   * Indicate that one of the taskids in this TaskInProgress
   * has successfully completed!
   */
  public void completed(TaskAttemptID taskid) {
    //
    // Record that this taskid is complete
    //
    completedTask(taskid, TaskStatus.State.SUCCEEDED);
        
    // Note the successful taskid
    setSuccessfulTaskid(taskid);
    
    //
    // Now that the TIP is complete, the other speculative 
    // subtasks will be closed when the owning tasktracker 
    // reports in and calls shouldClose() on this object.
    //

    this.completes++;
    this.execFinishTime = JobTracker.getClock().getTime();
    recomputeProgress();
    
  }

  /**
   * Get the split locations 
   */
  public String[] getSplitLocations() {
    if (isMapTask() && !jobSetup && !jobCleanup) {
      return rawSplit.getLocations();
    }
    return new String[0];
  }
  
  /**
   * Get the Status of the tasks managed by this TIP
   */
  public TaskStatus[] getTaskStatuses() {
    return taskStatuses.values().toArray(new TaskStatus[taskStatuses.size()]);
  }

  /**
   * Get all the {@link TaskAttemptID}s in this {@link TaskInProgress}
   */
  TaskAttemptID[] getAllTaskAttemptIDs() {
    return tasks.toArray(new TaskAttemptID[tasks.size()]);
  }
  
  /**
   * Get the status of the specified task
   * @param taskid
   * @return
   */
  public TaskStatus getTaskStatus(TaskAttemptID taskid) {
    return taskStatuses.get(taskid);
  }
  /**
   * The TIP's been ordered kill()ed.
   */
  public void kill() {
    if (isComplete() || failed) {
      return;
    }
    this.failed = true;
    killed = true;
    this.execFinishTime = JobTracker.getClock().getTime();
    recomputeProgress();
  }

  /**
   * Was the task killed?
   * @return true if the task killed
   */
  public boolean wasKilled() {
    return killed;
  }
  
  /**
   * Kill the given task
   */
  boolean killTask(TaskAttemptID taskId, boolean shouldFail, String diagnosticInfo) {
    TaskStatus st = taskStatuses.get(taskId);
    if(st != null && (st.getRunState() == TaskStatus.State.RUNNING
        || st.getRunState() == TaskStatus.State.COMMIT_PENDING ||
        st.inTaskCleanupPhase() ||
        st.getRunState() == TaskStatus.State.UNASSIGNED)
        && tasksToKill.put(taskId, shouldFail) == null ) {
      addDiagnosticInfo(taskId, diagnosticInfo);
      LOG.info(diagnosticInfo);
      return true;
    }
    return false;
  }

  /**
   * This method is called whenever there's a status change
   * for one of the TIP's sub-tasks.  It recomputes the overall 
   * progress for the TIP.  We examine all sub-tasks and find 
   * the one that's most advanced (and non-failed).
   */
  void recomputeProgress() {
    if (isComplete()) {
      this.progress = 1;
      // update the counters and the state
      TaskStatus completedStatus = taskStatuses.get(getSuccessfulTaskid());
      this.counters = completedStatus.getCounters();
      this.state = completedStatus.getStateString();
    } else if (failed) {
      this.progress = 0;
      // reset the counters and the state
      this.state = "";
      this.counters = new Counters();
    } else {
      double bestProgress = 0;
      String bestState = "";
      Counters bestCounters = new Counters();
      for (Iterator<TaskAttemptID> it = taskStatuses.keySet().iterator(); it.hasNext();) {
        TaskAttemptID taskid = it.next();
        TaskStatus status = taskStatuses.get(taskid);
        if (status.getRunState() == TaskStatus.State.SUCCEEDED) {
          bestProgress = 1;
          bestState = status.getStateString();
          bestCounters = status.getCounters();
          break;
        } else if (status.getRunState() == TaskStatus.State.COMMIT_PENDING) {
          //for COMMIT_PENDING, we take the last state that we recorded
          //when the task was RUNNING
          bestProgress = this.progress;
          bestState = this.state;
          bestCounters = this.counters;
        } else if (status.getRunState() == TaskStatus.State.RUNNING) {
          if (status.getProgress() >= bestProgress) {
            bestProgress = status.getProgress();
            bestState = status.getStateString();
            if (status.getIncludeCounters()) {
              bestCounters = status.getCounters();
            } else {
              bestCounters = this.counters;
            }
          }
        }
      }
      this.progress = bestProgress;
      this.state = bestState;
      this.counters = bestCounters;
    }
  }

  /////////////////////////////////////////////////
  // "Action" methods that actually require the TIP
  // to do something.
  /////////////////////////////////////////////////

  /**
   * Return whether this TIP still needs to run
   */
  boolean isRunnable() {
    return !failed && (completes == 0);
  }

  

  /**
   * Can this task be speculated? This requires that it isn't done or almost
   * done and that it isn't already being speculatively executed.
   * 
   * Added for use by queue scheduling algorithms.
   * @param currentTime 
   */
  boolean canBeSpeculated(long currentTime) {
    if (skipping || !isRunnable() || !isRunning() || 
        completes != 0 || isOnlyCommitPending() ||
        activeTasks.size() > MAX_TASK_EXECS) {
      return false;
    }

    if (isSpeculativeForced()) {
      return true;
    }

    // no speculation for first few seconds
    if (currentTime - lastDispatchTime < speculativeLag) {
      return false;
    }

    // if the task is making progress fast enough to complete within
    // the acceptable duration allowed for each task - do not speculate
    if ((maxProgressRateForSpeculation > 0) &&
        (progressRate > maxProgressRateForSpeculation)) {
      return false;
    }

    if (isMapTask() ? job.shouldSpeculateAllRemainingMaps() :
                      job.shouldSpeculateAllRemainingReduces()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Speculate " + getTIPId() +
            " because the job is almost finished");
      }
      return true;
    }

    if (useProcessingRateForSpeculation) {
      return canBeSpeculatedUsingProcessingRate(currentTime);
    } else {
      return canBeSpeculatedUsingProgressRate(currentTime);
    }
  }
  
  boolean canBeSpeculatedUsingProgressRate(long currentTime) {
    
    DataStatistics taskStats = job.getRunningTaskStatistics(isMapTask());

    if (LOG.isDebugEnabled()) {
      LOG.debug("activeTasks.size(): " + activeTasks.size() + " "
          + activeTasks.firstKey() + " task's progressrate: " + 
          progressRate + 
          " taskStats : " + taskStats);
    }
    
    // Find if task should be speculated based on standard deviation
    // the max difference allowed between the tasks's progress rate
    // and the mean progress rate of sibling tasks.

    double maxDiff = (taskStats.std() == 0 ? 
                       taskStats.mean()/3 : 
                        job.getSlowTaskThreshold() * taskStats.std());

    // if stddev > mean - we are stuck. cap the max difference at a 
    // more meaningful number.
    maxDiff = Math.min(maxDiff, taskStats.mean() * job.getStddevMeanRatioMax());
    boolean canBeSpeculated = (taskStats.mean() - progressRate > maxDiff);
    if (canBeSpeculated) {
      LOG.info("Task " + getTIPId() + " can be speculated with progressRate = "
          + progressRate + " and taskStats = " + taskStats);
    }
    return canBeSpeculated;
  }

  
  /**
   * For the map task, using the bytes processed/sec as the processing rate
   * For the reduce task, using different rate for different phase:
   * copy: using the bytes copied/sec as the processing rate
   * sort: using the accumulated progress rate as the processing rate
   * reduce: using the the bytes processed/sec as the processing rate
   * @param currentTime
   * @return
   */
  boolean canBeSpeculatedUsingProcessingRate(long currentTime) {

    TaskStatus.Phase p = getProcessingPhase();
    // check if the task is on one of following four phases
    if ((p != TaskStatus.Phase.MAP) && 
        (p != TaskStatus.Phase.SHUFFLE) &&
        (p != TaskStatus.Phase.SORT) &&
        (p != TaskStatus.Phase.REDUCE)) {
      return false;
    }
    
    DataStatistics taskStats = job.getRunningTaskStatistics(p);
    if (LOG.isDebugEnabled()) {
      LOG.debug("TaskID: " + this.id + "processing phase is " + p +
          " and processing rate for this phase is " + 
          getProcessingRate(p));
    }
    // Find if task should be speculated based on standard deviation
    // the max difference allowed between the tasks's progress rate
    // and the mean progress rate of sibling tasks.
    
    double maxDiff = (taskStats.std() == 0 ? 
        taskStats.mean()/3 : 
          job.getSlowTaskThreshold() * taskStats.std());
    
    // if stddev > mean - we are stuck. cap the max difference at a 
    // more meaningful number.
    maxDiff = Math.min(maxDiff, taskStats.mean() * job.getStddevMeanRatioMax());

    return (taskStats.mean() - processingRates.getRate(p) > maxDiff);
  }
    
  /**
   * Return a Task that can be sent to a TaskTracker for execution.
   */
  public Task getTaskToRun(String taskTracker) {

    // Create the 'taskid'; do not count the 'killed' tasks against the job!
    TaskAttemptID taskid = null;
    if (nextTaskId < (MAX_TASK_EXECS + maxTaskAttempts + numKilledTasks)) {
      // Make sure that the attempts are unqiue across restarts
      int attemptId = job.getNumRestarts() * NUM_ATTEMPTS_PER_RESTART + nextTaskId;
      taskid = new TaskAttemptID( id, attemptId);
      ++nextTaskId;
    } else {
      LOG.warn("Exceeded limit of " + (MAX_TASK_EXECS + maxTaskAttempts) +
              " (plus " + numKilledTasks + " killed)"  + 
              " attempts for the tip '" + getTIPId() + "'");
      return null;
    }
    //keep track of the last time we started an attempt at this TIP
    //used to calculate the progress rate of this TIP
    setDispatchTime(taskid, JobTracker.getClock().getTime());
    if (0 == execStartTime){
      // assume task starts running now
      execStartTime = JobTracker.getClock().getTime();
    }
    return addRunningTask(taskid, taskTracker);
  }
  
  public Task addRunningTask(TaskAttemptID taskid, String taskTracker) {
    return addRunningTask(taskid, taskTracker, false);
  }
  
  /**
   * Adds a previously running task to this tip. This is used in case of 
   * jobtracker restarts.
   */
  public Task addRunningTask(TaskAttemptID taskid, 
                             String taskTracker,
                             boolean taskCleanup) {
    // 1 slot is enough for taskCleanup task
    int numSlotsNeeded = taskCleanup ? 1 : numSlotsRequired;
    // create the task
    Task t = null;
    if (isMapTask()) {
      LOG.debug("attempt " + numTaskFailures + " sending skippedRecords "
          + failedRanges.getIndicesCount());
      String splitClass = null;
      BytesWritable split;
      if (!jobSetup && !jobCleanup) {
        splitClass = rawSplit.getClassName();
        split = rawSplit.getBytes();
      } else {
        split = new BytesWritable();
      }
      t = new MapTask(jobFile, taskid, partition, splitClass, split, 
                      numSlotsNeeded, job.getUser());
    } else {
      t = new ReduceTask(jobFile, taskid, partition, numMaps, 
                         numSlotsNeeded, job.getUser());
    }
    if (jobCleanup) {
      t.setJobCleanupTask();
    }
    if (jobSetup) {
      t.setJobSetupTask();
    }
    if (taskCleanup) {
      t.setTaskCleanupTask();
      t.setState(taskStatuses.get(taskid).getRunState());
      cleanupTasks.put(taskid, taskTracker);
    }
    t.setConf(conf);
    LOG.debug("Launching task with skipRanges:"+failedRanges.getSkipRanges());
    t.setSkipRanges(failedRanges.getSkipRanges());
    t.setSkipping(skipping);
    if(failedRanges.isTestAttempt()) {
      t.setWriteSkipRecs(false);
    }

    if (activeTasks.size() >= 1) {
    	speculativeTaskId = taskid;
    } else {
    	speculativeTaskId = null;
    }
    activeTasks.put(taskid, taskTracker);
    tasks.add(taskid);

    // Ask JobTracker to note that the task exists
    // jobtracker.createTaskEntry(taskid, taskTracker, this);

    /*
      // code to find call paths to createTaskEntry
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      boolean found = false;
      for (StackTraceElement s: stackTraceElements) {
      if (s.getMethodName().indexOf("heartbeat") != -1 ||
      s.getMethodName().indexOf("findTask") != -1 ||
      s.getMethodName().indexOf("createAndAddAttempt") != -1 ||
      s.getMethodName().indexOf("processTaskAttempt") != -1) {
      found = true;
      break;
      }
      }

      if (!found) {
      RuntimeException e = new RuntimeException ("calling addRunningTask from outside heartbeat");
      LOG.info(StringUtils.stringifyException(e));
      throw (e);
      }
    */

    // check and set the first attempt
    if (firstTaskId == null) {
      firstTaskId = taskid;
    }
    return t;
  }

  boolean isRunningTask(TaskAttemptID taskid) {
    TaskStatus status = taskStatuses.get(taskid);
    return status != null && status.getRunState() == TaskStatus.State.RUNNING;
  }
  
  boolean isCleanupAttempt(TaskAttemptID taskid) {
    return cleanupTasks.containsKey(taskid);
  }
  
  String machineWhereCleanupRan(TaskAttemptID taskid) {
    return cleanupTasks.get(taskid);
  }
  
  String machineWhereTaskRan(TaskAttemptID taskid) {
    return taskStatuses.get(taskid).getTaskTracker();
  }
    
  boolean wasKilled(TaskAttemptID taskid) {
    return tasksToKill.containsKey(taskid);
  }
  
  /**
   * Has this task already failed on this machine?
   * @param trackerHost The task tracker hostname
   * @return Has it failed?
   */
  public boolean hasFailedOnMachine(String trackerHost) {
    return machinesWhereFailed.contains(trackerHost);
  }
    
  /**
   * Was this task ever scheduled to run on this machine?
   * @param trackerHost The task tracker hostname 
   * @param trackerName The tracker name
   * @return Was task scheduled on the tracker?
   */
  public boolean hasRunOnMachine(String trackerHost, String trackerName) {
    return this.activeTasks.values().contains(trackerName) || 
      hasFailedOnMachine(trackerHost);
  }
  /**
   * Get the number of machines where this task has failed.
   * @return the size of the failed machine set
   */
  public int getNumberOfFailedMachines() {
    return machinesWhereFailed.size();
  }
    
  /**
   * Get the id of this map or reduce task.
   * @return The index of this tip in the maps/reduces lists.
   */
  public int getIdWithinJob() {
    return partition;
  }
    
  /**
   * Set the event number that was raised for this tip
   */
  public void setSuccessEventNumber(int eventNumber) {
    successEventNumber = eventNumber;
  }
       
  /**
   * Get the event number that was raised for this tip
   */
  public int getSuccessEventNumber() {
    return successEventNumber;
  }
  
  /** 
   * Gets the Node list of input split locations sorted in rack order.
   */ 
  public String getSplitNodes() {
    if (!isMapTask() || jobSetup || jobCleanup) {
      return "";
    }
    String[] nodes = rawSplit.getLocations();
    if (nodes == null || nodes.length == 0) {
      return "";
    }
    StringBuffer ret = new StringBuffer(nodes[0]);
    for(int i = 1; i < nodes.length;i++) {
      ret.append(",");
      ret.append(nodes[i]);
    }
    return ret.toString();
  }

  public long getMapInputSize() {
    if(isMapTask() && !jobSetup && !jobCleanup) {
      return rawSplit.getDataLength();
    } else {
      return 0;
    }
  }
  
  public void clearSplit() {
    rawSplit.clearBytes();
  }


  /**
   * update progress rate for a task
   * 
   * The assumption is that the JIP lock is held entering this routine.
   * So it's left unsynchronized. Currently the only places it's called
   * from are TIP.updateStatus and JIP.refreshCandidate*
   */
  public void updateProgressRate(long currentTime) {

    double bestProgressRate = 0;

    for (TaskStatus ts : taskStatuses.values()){
      if (ts.getRunState() == TaskStatus.State.RUNNING  || 
          ts.getRunState() == TaskStatus.State.SUCCEEDED ||
          ts.getRunState() == TaskStatus.State.COMMIT_PENDING) {

        double tsProgressRate = ts.getProgress()/Math.max(1,
            currentTime - getDispatchTime(ts.getTaskID()));
        if (tsProgressRate > bestProgressRate){
          bestProgressRate = tsProgressRate;
        }
      }
    }

    DataStatistics taskStats = job.getRunningTaskStatistics(isMapTask());
    taskStats.updateStatistics(progressRate, bestProgressRate);

    progressRate = bestProgressRate;
  }

  /**
   * Update the processing rate for this task. (e.g. bytes/ms in reduce phase)
   * @param currentTime
   */
  public void updateProcessingRate(long currentTime) {

    double bestMapRate = processingRates.getRate(Phase.MAP);
    double bestShuffleRate = processingRates.getRate(Phase.SHUFFLE);
    double bestSortRate = processingRates.getRate(Phase.SORT);
    double bestReduceRate = processingRates.getRate(Phase.REDUCE);
    // Find the best processing rates. There could be a running task and the
    // speculated task. (should be verified)
    for (TaskStatus ts : taskStatuses.values()){
      // There could be failed/killed/etc tasks - filter those out as they could
      // have had a high processing rate that should no longer be considered.
      if (ts.getRunState() == TaskStatus.State.RUNNING  || 
          ts.getRunState() == TaskStatus.State.SUCCEEDED ||
          ts.getRunState() == TaskStatus.State.COMMIT_PENDING) {
        double mapRate = 0;
        // Since we are not sure if map byte processing rate, or the map
        // record processing rate is better for speculation, offer an option
        if (conf.getBoolean(USE_MAP_RECORDS_PROCESSING_RATE, false)) {
          mapRate = ts.getMapRecordProcessingRate(currentTime);
        } else {
          mapRate = ts.getMapByteProcessingRate(currentTime);
        }
        double shuffleRate = ts.getCopyProcessingRate(currentTime);
        double sortRate = ts.getSortProcessingRate(currentTime);
        double reduceRate = ts.getReduceProcessingRate(currentTime);
        
        if (mapRate > bestMapRate) {
          bestMapRate = mapRate;
        }
        if (shuffleRate > bestShuffleRate) {
          bestShuffleRate = shuffleRate;
        }
        if (sortRate > bestSortRate) {
          bestSortRate = sortRate;
        }
        if (reduceRate > bestReduceRate) {
          bestReduceRate = reduceRate;
        }
      }
    }
    
    ProcessingRates updatedRates = new ProcessingRates(bestMapRate, 
        bestShuffleRate, bestSortRate, bestReduceRate);
    
    // Update the statistics for the job
    updateJobStats(Phase.MAP, processingRates, updatedRates);
    updateJobStats(Phase.SHUFFLE, processingRates, updatedRates);
    updateJobStats(Phase.SORT, processingRates, updatedRates);
    updateJobStats(Phase.REDUCE, processingRates, updatedRates);
    
    processingRates = updatedRates;
  }   
  
  /**
   * Helper function that updates the processing rates stats for this job. Only
   * updates the rate in the corresponding phase.
   * @param phase
   * @param oldRates
   * @param newRates
   */
  private void updateJobStats(Phase phase, ProcessingRates oldRates, 
      ProcessingRates newRates) {
    DataStatistics stats = job.getRunningTaskStatistics(phase);
    stats.updateStatistics(oldRates.getRate(phase), newRates.getRate(phase));
  }
  
  /**
   * Convert a progress rate to the total duration projected by
   * that progress rate
   */
  private static long progressRateToTotalDuration(double rate) {
    if (rate == 0)
      return Long.MAX_VALUE;

    return (long)(1.0/rate);
  }

  /**
   * This class keeps the records to be skipped during further executions 
   * based on failed records from all the previous attempts.
   * It also narrow down the skip records if it is more than the 
   * acceptable value by dividing the failed range into half. In this case one 
   * half is executed in the next attempt (test attempt). 
   * In the test attempt, only the test range gets executed, others get skipped. 
   * Based on the success/failure of the test attempt, the range is divided 
   * further.
   */
  private class FailedRanges {
    private SortedRanges skipRanges = new SortedRanges();
    private Divide divide;
    
    synchronized SortedRanges getSkipRanges() {
      if(divide!=null) {
        return divide.skipRange;
      }
      return skipRanges;
    }
    
    synchronized boolean isTestAttempt() {
      return divide!=null;
    }
    
    synchronized long getIndicesCount() {
      if(isTestAttempt()) {
        return divide.skipRange.getIndicesCount();
      }
      return skipRanges.getIndicesCount();
    }
    
    synchronized void updateState(TaskStatus status){
      if (isTestAttempt() && 
          (status.getRunState() == TaskStatus.State.SUCCEEDED)) {
        divide.testPassed = true;
        //since it was the test attempt we need to set it to failed
        //as it worked only on the test range
        status.setRunState(TaskStatus.State.FAILED);
        
      }
    }
    
    synchronized void add(Range failedRange) {
      LOG.warn("FailedRange:"+ failedRange);
      if(divide!=null) {
        LOG.warn("FailedRange:"+ failedRange +"  test:"+divide.test +
            "  pass:"+divide.testPassed);
        if(divide.testPassed) {
          //test range passed
          //other range would be bad. test it
          failedRange = divide.other;
        }
        else {
          //test range failed
          //other range would be good.
          failedRange = divide.test;
        }
        //reset
        divide = null;
      }
      
      if(maxSkipRecords==0 || failedRange.getLength()<=maxSkipRecords) {
        skipRanges.add(failedRange);
      } else {
        //start dividing the range to narrow down the skipped
        //records until maxSkipRecords are met OR all attempts
        //get exhausted
        divide = new Divide(failedRange);
      }
    }
    
    class Divide {
      private final SortedRanges skipRange;
      private final Range test;
      private final Range other;
      private boolean testPassed;
      Divide(Range range){
        long half = range.getLength()/2;
        test = new Range(range.getStartIndex(), half);
        other = new Range(test.getEndIndex(), range.getLength()-half);
        //construct the skip range from the skipRanges
        skipRange = new SortedRanges();
        for(Range r : skipRanges.getRanges()) {
          skipRange.add(r);
        }
        skipRange.add(new Range(0,test.getStartIndex()));
        skipRange.add(new Range(test.getEndIndex(), 
            (Long.MAX_VALUE-test.getEndIndex())));
      }
    }
    
  }

  TreeMap<TaskAttemptID, String> getActiveTasks() {
    return activeTasks;
  }

  int getNumSlotsRequired() {
    return numSlotsRequired;
  }

  /**
   * Force speculative execution if speculation is allowed in JobInProgress
   */
  public void setSpeculativeForced(boolean speculativeForced) {
    this.speculativeForced = speculativeForced;
  }

  /**
   * Is forced speculative execution enabled?
   */
  public boolean isSpeculativeForced() {
    return speculativeForced;
  }
}

