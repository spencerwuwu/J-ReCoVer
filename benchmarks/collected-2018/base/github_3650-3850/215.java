// https://searchcode.com/api/result/8194738/

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.CleanupQueue.PathDeletionContext;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.JobCounter;
import org.apache.hadoop.mapreduce.JobSubmissionFiles;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.counters.LimitExceededException;
import org.apache.hadoop.mapreduce.jobhistory.JobFinishedEvent;
import org.apache.hadoop.mapreduce.jobhistory.JobHistory;
import org.apache.hadoop.mapreduce.jobhistory.JobInfoChangeEvent;
import org.apache.hadoop.mapreduce.jobhistory.JobInitedEvent;
import org.apache.hadoop.mapreduce.jobhistory.JobPriorityChangeEvent;
import org.apache.hadoop.mapreduce.jobhistory.JobStatusChangedEvent;
import org.apache.hadoop.mapreduce.jobhistory.JobSubmittedEvent;
import org.apache.hadoop.mapreduce.jobhistory.JobUnsuccessfulCompletionEvent;
import org.apache.hadoop.mapreduce.jobhistory.MapAttemptFinishedEvent;
import org.apache.hadoop.mapreduce.jobhistory.ReduceAttemptFinishedEvent;
import org.apache.hadoop.mapreduce.jobhistory.TaskAttemptStartedEvent;
import org.apache.hadoop.mapreduce.jobhistory.TaskAttemptUnsuccessfulCompletionEvent;
import org.apache.hadoop.mapreduce.jobhistory.TaskFailedEvent;
import org.apache.hadoop.mapreduce.jobhistory.TaskFinishedEvent;
import org.apache.hadoop.mapreduce.jobhistory.TaskStartedEvent;
import org.apache.hadoop.mapreduce.security.TokenCache;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.mapreduce.security.token.DelegationTokenRenewal;
import org.apache.hadoop.mapreduce.security.token.JobTokenIdentifier;
import org.apache.hadoop.mapreduce.server.jobtracker.TaskTracker;
import org.apache.hadoop.mapreduce.split.JobSplit;
import org.apache.hadoop.mapreduce.split.SplitMetaInfoReader;
import org.apache.hadoop.mapreduce.split.JobSplit.TaskSplitMetaInfo;
import org.apache.hadoop.mapreduce.task.JobContextImpl;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.util.StringUtils;

/**
 * JobInProgress maintains all the info for keeping a Job on the straight and
 * narrow. It keeps its JobProfile and its latest JobStatus, plus a set of
 * tables for doing bookkeeping of its Tasks.
 */
@InterfaceAudience.LimitedPrivate({"MapReduce"})
@InterfaceStability.Unstable
public class JobInProgress {
  /**
   * Used when the a kill is issued to a job which is initializing.
   */
  static class KillInterruptedException extends InterruptedException {
   private static final long serialVersionUID = 1L;
    public KillInterruptedException(String msg) {
      super(msg);
    }
  }
  
  static final Log LOG = LogFactory.getLog(JobInProgress.class);
    
  JobProfile profile;
  JobStatus status;
  Path jobFile = null;
  Path localJobFile = null;

  TaskInProgress maps[] = new TaskInProgress[0];
  TaskInProgress reduces[] = new TaskInProgress[0];
  TaskInProgress cleanup[] = new TaskInProgress[0];
  TaskInProgress setup[] = new TaskInProgress[0];
  int numMapTasks = 0;
  int numReduceTasks = 0;
  final long memoryPerMap;
  final long memoryPerReduce;
  volatile int numSlotsPerMap = 1;
  volatile int numSlotsPerReduce = 1;
  final int maxTaskFailuresPerTracker;
  
  // Counters to track currently running/finished/failed Map/Reduce task-attempts
  int runningMapTasks = 0;
  int runningReduceTasks = 0;
  int finishedMapTasks = 0;
  int finishedReduceTasks = 0;
  int failedMapTasks = 0; 
  int failedReduceTasks = 0;
  
  static final float DEFAULT_COMPLETED_MAPS_PERCENT_FOR_REDUCE_SLOWSTART = 0.05f;
  int completedMapsForReduceSlowstart = 0;
  
  // runningMapTasks include speculative tasks, so we need to capture 
  // speculative tasks separately 
  int speculativeMapTasks = 0;
  int speculativeReduceTasks = 0;
  
  int mapFailuresPercent = 0;
  int reduceFailuresPercent = 0;
  int failedMapTIPs = 0;
  int failedReduceTIPs = 0;
  private volatile boolean launchedCleanup = false;
  private volatile boolean launchedSetup = false;
  private volatile boolean jobKilled = false;
  private volatile boolean jobFailed = false;
  private final boolean jobSetupCleanupNeeded;
  private final boolean taskCleanupNeeded;

  JobPriority priority = JobPriority.NORMAL;
  protected JobTracker jobtracker;
  
  protected Credentials tokenStorage;
  
  JobHistory jobHistory;

  // NetworkTopology Node to the set of TIPs
  Map<Node, List<TaskInProgress>> nonRunningMapCache;
  
  // Map of NetworkTopology Node to set of running TIPs
  Map<Node, Set<TaskInProgress>> runningMapCache;

  // A list of non-local non-running maps
  List<TaskInProgress> nonLocalMaps;

  // A set of non-local running maps
  Set<TaskInProgress> nonLocalRunningMaps;

  // A list of non-running reduce TIPs
  List<TaskInProgress> nonRunningReduces;

  // A set of running reduce TIPs
  Set<TaskInProgress> runningReduces;
  
  // A list of cleanup tasks for the map task attempts, to be launched
  List<TaskAttemptID> mapCleanupTasks = new LinkedList<TaskAttemptID>();
  
  // A list of cleanup tasks for the reduce task attempts, to be launched
  List<TaskAttemptID> reduceCleanupTasks = new LinkedList<TaskAttemptID>();

  int maxLevel;

  /**
   * A special value indicating that 
   * {@link #findNewMapTask(TaskTrackerStatus, int, int, int, double)} should
   * schedule any available map tasks for this job, including speculative tasks.
   */
  int anyCacheLevel;
  
  /**
   * A special value indicating that 
   * {@link #findNewMapTask(TaskTrackerStatus, int, int, int, double)} should
   * schedule any only off-switch and speculative map tasks for this job.
   */
  private static final int NON_LOCAL_CACHE_LEVEL = -1;

  private int taskCompletionEventTracker = 0; 
  List<TaskCompletionEvent> taskCompletionEvents;
    
  // The maximum percentage of trackers in cluster added to the 'blacklist'.
  private static final double CLUSTER_BLACKLIST_PERCENT = 0.25;
  
  // The maximum percentage of fetch failures allowed for a map 
  private static final double MAX_ALLOWED_FETCH_FAILURES_PERCENT = 0.5;
  
  // No. of tasktrackers in the cluster
  private volatile int clusterSize = 0;
  
  // The no. of tasktrackers where >= conf.getMaxTaskFailuresPerTracker()
  // tasks have failed
  private volatile int flakyTaskTrackers = 0;
  // Map of trackerHostName -> no. of task failures
  private Map<String, Integer> trackerToFailuresMap = 
    new TreeMap<String, Integer>();
    
  //Confine estimation algorithms to an "oracle" class that JIP queries.
  ResourceEstimator resourceEstimator; 
  
  long startTime;
  long launchTime;
  long finishTime;

  // First *task launch times
  final Map<TaskType, Long> firstTaskLaunchTimes =
      new EnumMap<TaskType, Long>(TaskType.class);
  
  // Indicates how many times the job got restarted
  private final int restartCount;

  JobConf conf;
  protected AtomicBoolean tasksInited = new AtomicBoolean(false);
  private JobInitKillStatus jobInitKillStatus = new JobInitKillStatus();

  LocalFileSystem localFs;
  FileSystem fs;
  String user;
  JobID jobId;
  volatile private boolean hasSpeculativeMaps;
  volatile private boolean hasSpeculativeReduces;
  long inputLength = 0;
  
  Counters jobCounters = new Counters();
  
  // Maximum no. of fetch-failure notifications after which map task is killed
  private static final int MAX_FETCH_FAILURES_NOTIFICATIONS = 3;

  // Don't lower speculativeCap below one TT's worth (for small clusters)
  private static final int MIN_SPEC_CAP = 10;
  
  private static final float MIN_SLOTS_CAP = 0.01f;
  
  // Map of mapTaskId -> no. of fetch failures
  private Map<TaskAttemptID, Integer> mapTaskIdToFetchFailuresMap =
    new TreeMap<TaskAttemptID, Integer>();

  private Object schedulingInfo;
  private String submitHostName;
  private String submitHostAddress;

  //thresholds for speculative execution
  float slowTaskThreshold;
  float speculativeCap;
  float slowNodeThreshold; //standard deviations

  //Statistics are maintained for a couple of things
  //mapTaskStats is used for maintaining statistics about
  //the completion time of map tasks on the trackers. On a per
  //tracker basis, the mean time for task completion is maintained
  private DataStatistics mapTaskStats = new DataStatistics();
  //reduceTaskStats is used for maintaining statistics about
  //the completion time of reduce tasks on the trackers. On a per
  //tracker basis, the mean time for task completion is maintained
  private DataStatistics reduceTaskStats = new DataStatistics();
  //trackerMapStats used to maintain a mapping from the tracker to the
  //the statistics about completion time of map tasks
  private Map<String,DataStatistics> trackerMapStats = 
    new HashMap<String,DataStatistics>();
  //trackerReduceStats used to maintain a mapping from the tracker to the
  //the statistics about completion time of reduce tasks
  private Map<String,DataStatistics> trackerReduceStats = 
    new HashMap<String,DataStatistics>();
  //runningMapStats used to maintain the RUNNING map tasks' statistics 
  private DataStatistics runningMapTaskStats = new DataStatistics();
  //runningReduceStats used to maintain the RUNNING reduce tasks' statistics
  private DataStatistics runningReduceTaskStats = new DataStatistics();
 
  private static class FallowSlotInfo {
    long timestamp;
    int numSlots;
    
    public FallowSlotInfo(long timestamp, int numSlots) {
      this.timestamp = timestamp;
      this.numSlots = numSlots;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    public int getNumSlots() {
      return numSlots;
    }

    public void setNumSlots(int numSlots) {
      this.numSlots = numSlots;
    }
  }
  
  private Map<TaskTracker, FallowSlotInfo> trackersReservedForMaps = 
    new HashMap<TaskTracker, FallowSlotInfo>();
  private Map<TaskTracker, FallowSlotInfo> trackersReservedForReduces = 
    new HashMap<TaskTracker, FallowSlotInfo>();
  private Path jobSubmitDir = null;
  
  /**
   * Create an almost empty JobInProgress, which can be used only for tests
   */
  protected JobInProgress(JobID jobid, JobConf conf, JobTracker tracker) {
    this.conf = conf;
    this.jobId = jobid;
    this.numMapTasks = conf.getNumMapTasks();
    this.numReduceTasks = conf.getNumReduceTasks();
    this.maxLevel = NetworkTopology.DEFAULT_HOST_LEVEL;
    this.anyCacheLevel = this.maxLevel+1;
    this.jobtracker = tracker;
    this.restartCount = 0;
    this.profile = new JobProfile(conf.getUser(), jobid, "", "", 
                                  conf.getJobName(),conf.getQueueName());

    this.memoryPerMap = conf.getMemoryForMapTask();
    this.memoryPerReduce = conf.getMemoryForReduceTask();

    this.maxTaskFailuresPerTracker = conf.getMaxTaskFailuresPerTracker();

    
    hasSpeculativeMaps = conf.getMapSpeculativeExecution();
    hasSpeculativeReduces = conf.getReduceSpeculativeExecution();
    this.nonLocalMaps = new LinkedList<TaskInProgress>();
    this.nonLocalRunningMaps = new LinkedHashSet<TaskInProgress>();
    this.runningMapCache = new IdentityHashMap<Node, Set<TaskInProgress>>();
    this.nonRunningReduces = new LinkedList<TaskInProgress>();    
    this.runningReduces = new LinkedHashSet<TaskInProgress>();
    this.resourceEstimator = new ResourceEstimator(this);
    this.status = new JobStatus(jobid, 0.0f, 0.0f, JobStatus.PREP, 
        this.profile.getUser(), this.profile.getJobName(), 
        this.profile.getJobFile(), "");
    this.jobtracker.getInstrumentation().addPrepJob(conf, jobid);
    this.taskCompletionEvents = new ArrayList<TaskCompletionEvent>
    (numMapTasks + numReduceTasks + 10);
    
    this.slowTaskThreshold = Math.max(0.0f,
        conf.getFloat(MRJobConfig.SPECULATIVE_SLOWTASK_THRESHOLD,1.0f));
    this.speculativeCap = conf.getFloat(
        MRJobConfig.SPECULATIVECAP,0.1f);
    this.slowNodeThreshold = conf.getFloat(
        MRJobConfig.SPECULATIVE_SLOWNODE_THRESHOLD,1.0f);
    this.jobSetupCleanupNeeded = conf.getBoolean(
        MRJobConfig.SETUP_CLEANUP_NEEDED, true);
    this.taskCleanupNeeded = conf.getBoolean(
        MRJobConfig.TASK_CLEANUP_NEEDED, true);
    if (tracker != null) { // Some mock tests have null tracker
      this.jobHistory = tracker.getJobHistory();
    }
    this.tokenStorage = null;
  }
  
  JobInProgress(JobConf conf) {
    restartCount = 0;
    jobSetupCleanupNeeded = false;
    taskCleanupNeeded = true;

    this.memoryPerMap = conf.getMemoryForMapTask();
    this.memoryPerReduce = conf.getMemoryForReduceTask();

    this.maxTaskFailuresPerTracker = conf.getMaxTaskFailuresPerTracker();
  }
  
  /**
   * Create a JobInProgress with the given job file, plus a handle
   * to the tracker.
   */
  public JobInProgress(JobTracker jobtracker, 
                       final JobConf default_conf, int rCount,
                       JobInfo jobInfo,
                       Credentials ts
                      ) throws IOException, InterruptedException {
    try {
      this.restartCount = rCount;
      this.jobId = JobID.downgrade(jobInfo.getJobID());
      String url = "http://" + jobtracker.getJobTrackerMachine() + ":"
          + jobtracker.getInfoPort() + "/jobdetails.jsp?jobid=" + this.jobId;
      this.jobtracker = jobtracker;
      this.jobHistory = jobtracker.getJobHistory();
      this.startTime = System.currentTimeMillis();

      this.localFs = jobtracker.getLocalFileSystem();
      this.tokenStorage = ts;
      // use the user supplied token to add user credentials to the conf
      jobSubmitDir = jobInfo.getJobSubmitDir();
      user = jobInfo.getUser().toString();

      UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
      if (ts != null) {
        for (Token<? extends TokenIdentifier> token : ts.getAllTokens()) {
          ugi.addToken(token);
        }
      }

      fs = ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
        public FileSystem run() throws IOException {
          return jobSubmitDir.getFileSystem(default_conf);
        }
      });
      this.localJobFile = default_conf.getLocalPath(JobTracker.SUBDIR + "/"
          + this.jobId + ".xml");

      jobFile = JobSubmissionFiles.getJobConfPath(jobSubmitDir);
      fs.copyToLocalFile(jobFile, localJobFile);
      conf = new JobConf(localJobFile);
      if (conf.getUser() == null) {
        this.conf.setUser(user);
      }
      if (!conf.getUser().equals(user)) {
        String desc = "The username " + conf.getUser() + " obtained from the "
            + "conf doesn't match the username " + user + " the user "
            + "authenticated as";
        AuditLogger.logFailure(user, Operation.SUBMIT_JOB.name(),
            conf.getUser(), jobId.toString(), desc);
        throw new IOException(desc);
      }

      String userGroups[] = ugi.getGroupNames();
      String primaryGroup = (userGroups.length > 0) ? userGroups[0] : null;
      if (primaryGroup != null) {
        conf.set("group.name", primaryGroup);
      }

      this.priority = conf.getJobPriority();
      this.profile = new JobProfile(conf.getUser(), this.jobId, jobFile
          .toString(), url, conf.getJobName(), conf.getQueueName());
      this.status = new JobStatus(this.jobId, 0.0f, 0.0f, JobStatus.PREP,
          profile.getUser(), profile.getJobName(), profile.getJobFile(),
          profile.getURL().toString());
      this.jobtracker.getInstrumentation().addPrepJob(conf, this.jobId);
      status.setStartTime(startTime);
      this.status.setJobPriority(this.priority);

      this.numMapTasks = conf.getNumMapTasks();
      this.numReduceTasks = conf.getNumReduceTasks();

      this.memoryPerMap = conf.getMemoryForMapTask();
      this.memoryPerReduce = conf.getMemoryForReduceTask();

      this.taskCompletionEvents = new ArrayList<TaskCompletionEvent>(
          numMapTasks + numReduceTasks + 10);
      JobContext jobContext = new JobContextImpl(conf, jobId);
      this.jobSetupCleanupNeeded = jobContext.getJobSetupCleanupNeeded();
      this.taskCleanupNeeded = jobContext.getTaskCleanupNeeded();

      // Construct the jobACLs
      status.setJobACLs(jobtracker.getJobACLsManager().constructJobACLs(conf));

      this.mapFailuresPercent = conf.getMaxMapTaskFailuresPercent();
      this.reduceFailuresPercent = conf.getMaxReduceTaskFailuresPercent();

      this.maxTaskFailuresPerTracker = conf.getMaxTaskFailuresPerTracker();

      hasSpeculativeMaps = conf.getMapSpeculativeExecution();
      hasSpeculativeReduces = conf.getReduceSpeculativeExecution();
      this.maxLevel = jobtracker.getNumTaskCacheLevels();
      this.anyCacheLevel = this.maxLevel + 1;
      this.nonLocalMaps = new LinkedList<TaskInProgress>();
      this.nonLocalRunningMaps = new LinkedHashSet<TaskInProgress>();
      this.runningMapCache = new IdentityHashMap<Node, Set<TaskInProgress>>();
      this.nonRunningReduces = new LinkedList<TaskInProgress>();
      this.runningReduces = new LinkedHashSet<TaskInProgress>();
      this.resourceEstimator = new ResourceEstimator(this);
      this.submitHostName = conf.getJobSubmitHostName();
      this.submitHostAddress = conf.getJobSubmitHostAddress();

      this.slowTaskThreshold = Math.max(0.0f, conf.getFloat(
          MRJobConfig.SPECULATIVE_SLOWTASK_THRESHOLD, 1.0f));
      this.speculativeCap = conf.getFloat(MRJobConfig.SPECULATIVECAP, 0.1f);
      this.slowNodeThreshold = conf.getFloat(
          MRJobConfig.SPECULATIVE_SLOWNODE_THRESHOLD, 1.0f);
      // register job's tokens for renewal
      DelegationTokenRenewal.registerDelegationTokensForRenewal(jobInfo
          .getJobID(), ts, jobtracker.getConf());
    } finally {
      // close all FileSystems that was created above for the current user
      // At this point, this constructor is called in the context of an RPC, and
      // hence the "current user" is actually referring to the kerberos
      // authenticated user (if security is ON).
      FileSystem.closeAllForUGI(UserGroupInformation.getCurrentUser());
    }
  }
    
  private void printCache (Map<Node, List<TaskInProgress>> cache) {
    LOG.info("The taskcache info:");
    for (Map.Entry<Node, List<TaskInProgress>> n : cache.entrySet()) {
      List <TaskInProgress> tips = n.getValue();
      LOG.info("Cached TIPs on node: " + n.getKey());
      for (TaskInProgress tip : tips) {
        LOG.info("tip : " + tip.getTIPId());
      }
    }
  }
  
  Map<Node, List<TaskInProgress>> createCache(
                         TaskSplitMetaInfo[] splits, int maxLevel) {
    Map<Node, List<TaskInProgress>> cache = 
      new IdentityHashMap<Node, List<TaskInProgress>>(maxLevel);
    
    for (int i = 0; i < splits.length; i++) {
      String[] splitLocations = splits[i].getLocations();
      if (splitLocations.length == 0) {
        nonLocalMaps.add(maps[i]);
        continue;
      }

      for(String host: splitLocations) {
        Node node = jobtracker.resolveAndAddToTopology(host);
        LOG.info("tip:" + maps[i].getTIPId() + " has split on node:" + node);
        for (int j = 0; j < maxLevel; j++) {
          List<TaskInProgress> hostMaps = cache.get(node);
          if (hostMaps == null) {
            hostMaps = new ArrayList<TaskInProgress>();
            cache.put(node, hostMaps);
            hostMaps.add(maps[i]);
          }
          //check whether the hostMaps already contains an entry for a TIP
          //This will be true for nodes that are racks and multiple nodes in
          //the rack contain the input for a tip. Note that if it already
          //exists in the hostMaps, it must be the last element there since
          //we process one TIP at a time sequentially in the split-size order
          if (hostMaps.get(hostMaps.size() - 1) != maps[i]) {
            hostMaps.add(maps[i]);
          }
          node = node.getParent();
        }
      }
    }
    return cache;
  }
  
  /**
   * Check if the job has been initialized.
   * @return <code>true</code> if the job has been initialized, 
   *         <code>false</code> otherwise
   */
  public boolean inited() {
    return tasksInited.get();
  }
  
  /**
   * Get the user for the job
   */
  public String getUser() {
    return user;
  }

  boolean getMapSpeculativeExecution() {
    return hasSpeculativeMaps;
  }
  
  boolean getReduceSpeculativeExecution() {
    return hasSpeculativeReduces;
  }
  
  long getMemoryForMapTask() {
    return memoryPerMap;
  }
  
  long getMemoryForReduceTask() {
    return memoryPerReduce;
  }
  
  /**
   * Get the number of slots required to run a single map task-attempt.
   * @return the number of slots required to run a single map task-attempt
   */
  int getNumSlotsPerMap() {
    return numSlotsPerMap;
  }

  /**
   * Set the number of slots required to run a single map task-attempt.
   * This is typically set by schedulers which support high-ram jobs.
   * @param slots the number of slots required to run a single map task-attempt
   */
  void setNumSlotsPerMap(int numSlotsPerMap) {
    this.numSlotsPerMap = numSlotsPerMap;
  }

  /**
   * Get the number of slots required to run a single reduce task-attempt.
   * @return the number of slots required to run a single reduce task-attempt
   */
  int getNumSlotsPerReduce() {
    return numSlotsPerReduce;
  }

  /**
   * Set the number of slots required to run a single reduce task-attempt.
   * This is typically set by schedulers which support high-ram jobs.
   * @param slots the number of slots required to run a single reduce 
   *              task-attempt
   */
  void setNumSlotsPerReduce(int numSlotsPerReduce) {
    this.numSlotsPerReduce = numSlotsPerReduce;
  }

  /**
   * Construct the splits, etc.  This is invoked from an async
   * thread so that split-computation doesn't block anyone. Only the 
   * {@link JobTracker} should invoke this api. Look 
   * at {@link JobTracker#initJob(JobInProgress)} for more details.
   */
  public synchronized void initTasks() 
  throws IOException, KillInterruptedException, UnknownHostException {
    if (tasksInited.get() || isComplete()) {
      return;
    }
    synchronized(jobInitKillStatus){
      if(jobInitKillStatus.killed || jobInitKillStatus.initStarted) {
        return;
      }
      jobInitKillStatus.initStarted = true;
    }

    LOG.info("Initializing " + jobId);

    logSubmissionToJobHistory();
    
    // log the job priority
    setPriority(this.priority);
    
    //
    // generate security keys needed by Tasks
    //
    generateAndStoreTokens();
    
    //
    // read input splits and create a map per a split
    //
    TaskSplitMetaInfo[] taskSplitMetaInfo = createSplits(jobId);
    numMapTasks = taskSplitMetaInfo.length;

    checkTaskLimits();

    // Sanity check the locations so we don't create/initialize unnecessary tasks
    for (TaskSplitMetaInfo split : taskSplitMetaInfo) {
      NetUtils.verifyHostnames(split.getLocations());
    }

    jobtracker.getInstrumentation().addWaitingMaps(getJobID(), numMapTasks);
    jobtracker.getInstrumentation().addWaitingReduces(getJobID(), numReduceTasks);

    createMapTasks(jobFile.toString(), taskSplitMetaInfo);
    
    if (numMapTasks > 0) { 
      nonRunningMapCache = createCache(taskSplitMetaInfo,
          maxLevel);
    }
        
    // set the launch time
    this.launchTime = JobTracker.getClock().getTime();

    createReduceTasks(jobFile.toString());
    
    // Calculate the minimum number of maps to be complete before 
    // we should start scheduling reduces
    completedMapsForReduceSlowstart = 
      (int)Math.ceil(
          (conf.getFloat(MRJobConfig.COMPLETED_MAPS_FOR_REDUCE_SLOWSTART, 
                         DEFAULT_COMPLETED_MAPS_PERCENT_FOR_REDUCE_SLOWSTART) * 
           numMapTasks));
    
    initSetupCleanupTasks(jobFile.toString());
    
    synchronized(jobInitKillStatus){
      jobInitKillStatus.initDone = true;
      if(jobInitKillStatus.killed) {
        //setup not launched so directly terminate
        throw new KillInterruptedException("Job " + jobId + " killed in init");
      }
    }
    
    tasksInited.set(true);
    JobInitedEvent jie = new JobInitedEvent(
        profile.getJobID(),  this.launchTime,
        numMapTasks, numReduceTasks,
        JobStatus.getJobRunState(JobStatus.PREP),
        false);
    
    jobHistory.logEvent(jie, jobId);
   
    // Log the number of map and reduce tasks
    LOG.info("Job " + jobId + " initialized successfully with " + numMapTasks 
             + " map tasks and " + numReduceTasks + " reduce tasks.");
  }

  // Returns true if the job is empty (0 maps, 0 reduces and no setup-cleanup)
  // else return false.
  synchronized boolean isJobEmpty() {
    return maps.length == 0 && reduces.length == 0 && !jobSetupCleanupNeeded;
  }
  
  synchronized boolean isSetupCleanupRequired() {
   return jobSetupCleanupNeeded;
  }

  // Should be called once the init is done. This will complete the job 
  // because the job is empty (0 maps, 0 reduces and no setup-cleanup).
  synchronized void completeEmptyJob() {
    jobComplete();
  }

  synchronized void completeSetup() {
    setupComplete();
  }

  void logSubmissionToJobHistory() throws IOException {
    // log job info
    String username = conf.getUser();
    if (username == null) { username = ""; }
    String jobname = conf.getJobName();
    String jobQueueName = conf.getQueueName();

    setUpLocalizedJobConf(conf, jobId);
    jobHistory.setupEventWriter(jobId, conf);
    JobSubmittedEvent jse =
        new JobSubmittedEvent(jobId, jobname, username, this.startTime,
            jobFile.toString(), status.getJobACLs(), jobQueueName);
    jobHistory.logEvent(jse, jobId);
    
  }

  TaskSplitMetaInfo[] createSplits(org.apache.hadoop.mapreduce.JobID jobId) 
  throws IOException {
    TaskSplitMetaInfo[] allTaskSplitMetaInfo = 
      SplitMetaInfoReader.readSplitMetaInfo(jobId, fs, conf, jobSubmitDir);
    return allTaskSplitMetaInfo;
  }

  /**
   * If the number of taks is greater than the configured value
   * throw an exception that will fail job initialization
   */
  void checkTaskLimits() throws IOException {
    int maxTasks = jobtracker.getMaxTasksPerJob();
    if (maxTasks > 0 && numMapTasks + numReduceTasks > maxTasks) {
      throw new IOException(
                "The number of tasks for this job " + 
                (numMapTasks + numReduceTasks) +
                " exceeds the configured limit " + maxTasks);
    }
  }

  synchronized void createMapTasks(String jobFile, 
		  TaskSplitMetaInfo[] splits) {
    maps = new TaskInProgress[numMapTasks];
    for(int i=0; i < numMapTasks; ++i) {
      inputLength += splits[i].getInputDataLength();
      maps[i] = new TaskInProgress(jobId, jobFile, 
                                   splits[i], 
                                   jobtracker, conf, this, 
                                   i, numSlotsPerMap);
    }
    LOG.info("Input size for job " + jobId + " = " + inputLength
        + ". Number of splits = " + splits.length);

  }

  synchronized void createReduceTasks(String jobFile) {
    this.reduces = new TaskInProgress[numReduceTasks];
    for (int i = 0; i < numReduceTasks; i++) {
      reduces[i] = new TaskInProgress(jobId, jobFile, 
                                      numMapTasks, i, 
                                      jobtracker, conf, 
                                      this, numSlotsPerReduce);
      nonRunningReduces.add(reduces[i]);
    }
  }

  
  synchronized void initSetupCleanupTasks(String jobFile) {
    if (!jobSetupCleanupNeeded) {
      LOG.info("Setup/Cleanup not needed for job " + jobId);
      // nothing to initialize
      return;
    }
    // create cleanup two cleanup tips, one map and one reduce.
    cleanup = new TaskInProgress[2];

    // cleanup map tip. This map doesn't use any splits. Just assign an empty
    // split.
    TaskSplitMetaInfo emptySplit = JobSplit.EMPTY_TASK_SPLIT;
    cleanup[0] = new TaskInProgress(jobId, jobFile, emptySplit, 
            jobtracker, conf, this, numMapTasks, 1);
    cleanup[0].setJobCleanupTask();

    // cleanup reduce tip.
    cleanup[1] = new TaskInProgress(jobId, jobFile, numMapTasks,
                       numReduceTasks, jobtracker, conf, this, 1);
    cleanup[1].setJobCleanupTask();

    // create two setup tips, one map and one reduce.
    setup = new TaskInProgress[2];

    // setup map tip. This map doesn't use any split. Just assign an empty
    // split.
    setup[0] = new TaskInProgress(jobId, jobFile, emptySplit, 
            jobtracker, conf, this, numMapTasks + 1, 1);
    setup[0].setJobSetupTask();

    // setup reduce tip.
    setup[1] = new TaskInProgress(jobId, jobFile, numMapTasks,
                       numReduceTasks + 1, jobtracker, conf, this, 1);
    setup[1].setJobSetupTask();
  }
  
  void setupComplete() {
    status.setSetupProgress(1.0f);
    if (this.status.getRunState() == JobStatus.PREP) {
      changeStateTo(JobStatus.RUNNING);
      JobStatusChangedEvent jse = 
        new JobStatusChangedEvent(profile.getJobID(),
         JobStatus.getJobRunState(JobStatus.RUNNING));
      jobHistory.logEvent(jse, profile.getJobID());
    }
  }

  /////////////////////////////////////////////////////
  // Accessors for the JobInProgress
  /////////////////////////////////////////////////////
  public JobProfile getProfile() {
    return profile;
  }
  public JobStatus getStatus() {
    return status;
  }
  public synchronized long getLaunchTime() {
    return launchTime;
  }
  Map<TaskType, Long> getFirstTaskLaunchTimes() {
    return firstTaskLaunchTimes;
  }
  public long getStartTime() {
    return startTime;
  }
  public long getFinishTime() {
    return finishTime;
  }
  public int desiredMaps() {
    return numMapTasks;
  }
  public synchronized int finishedMaps() {
    return finishedMapTasks;
  }
  public int desiredReduces() {
    return numReduceTasks;
  }
  public synchronized int runningMaps() {
    return runningMapTasks;
  }
  public synchronized int runningReduces() {
    return runningReduceTasks;
  }
  public synchronized int finishedReduces() {
    return finishedReduceTasks;
  }
  public synchronized int pendingMaps() {
    return numMapTasks - runningMapTasks - failedMapTIPs - 
    finishedMapTasks + speculativeMapTasks;
  }
  public synchronized int pendingReduces() {
    return numReduceTasks - runningReduceTasks - failedReduceTIPs - 
    finishedReduceTasks + speculativeReduceTasks;
  }
 
  public int getNumSlotsPerTask(TaskType taskType) {
    if (taskType == TaskType.MAP) {
      return numSlotsPerMap;
    } else if (taskType == TaskType.REDUCE) {
      return numSlotsPerReduce;
    } else {
      return 1;
    }
  }
  public JobPriority getPriority() {
    return this.priority;
  }
  public void setPriority(JobPriority priority) {
    if(priority == null) {
      priority = JobPriority.NORMAL;
    }
    synchronized (this) {
      this.priority = priority;
      status.setJobPriority(priority);
      // log and change to the job's priority
      JobPriorityChangeEvent prEvent = 
        new JobPriorityChangeEvent(jobId, priority);
       
      jobHistory.logEvent(prEvent, jobId);
      
    }
  }

  // Update the job start/launch time (upon restart) and log to history
  synchronized void updateJobInfo(long startTime, long launchTime) {
    // log and change to the job's start/launch time
    this.startTime = startTime;
    this.launchTime = launchTime;
    JobInfoChangeEvent event = 
      new JobInfoChangeEvent(jobId, startTime, launchTime);
     
    jobHistory.logEvent(event, jobId);
    
  }

  /**
   * Get the number of times the job has restarted
   */
  int getNumRestarts() {
    return restartCount;
  }
  
  long getInputLength() {
    return inputLength;
  }
 
  boolean isCleanupLaunched() {
    return launchedCleanup;
  }

  boolean isSetupLaunched() {
    return launchedSetup;
  }

  /** 
   * Get all the tasks of the desired type in this job.
   * @param type {@link TaskType} of the tasks required
   * @return An array of {@link TaskInProgress} matching the given type. 
   *         Returns an empty array if no tasks are found for the given type.  
   */
  TaskInProgress[] getTasks(TaskType type) {
    TaskInProgress[] tasks = null;
    switch (type) {
      case MAP:
      {
        tasks = maps;
      }
      break;
      case REDUCE:
      {
        tasks = reduces;
      }
      break;
      case JOB_SETUP: 
      {
        tasks = setup;
      }
      break;
      case JOB_CLEANUP:
      {
        tasks = cleanup;
      }
      break;
      default:
      {
          tasks = new TaskInProgress[0];
      }
      break;
    }
    return tasks;
  }

  /**
   * Return the nonLocalRunningMaps
   * @return
   */
  Set<TaskInProgress> getNonLocalRunningMaps()
  {
    return nonLocalRunningMaps;
  }
  
  /**
   * Return the runningMapCache
   * @return
   */
  Map<Node, Set<TaskInProgress>> getRunningMapCache()
  {
    return runningMapCache;
  }
  
  /**
   * Return runningReduces
   * @return
   */
  Set<TaskInProgress> getRunningReduces()
  {
    return runningReduces;
  }
  
  /**
   * Get the job configuration
   * @return the job's configuration
   */
  JobConf getJobConf() {
    return conf;
  }
    
  /**
   * Return a vector of completed TaskInProgress objects
   */
  public synchronized Vector<TaskInProgress> reportTasksInProgress(boolean shouldBeMap,
                                                      boolean shouldBeComplete) {
    
    Vector<TaskInProgress> results = new Vector<TaskInProgress>();
    TaskInProgress tips[] = null;
    if (shouldBeMap) {
      tips = maps;
    } else {
      tips = reduces;
    }
    for (int i = 0; i < tips.length; i++) {
      if (tips[i].isComplete() == shouldBeComplete) {
        results.add(tips[i]);
      }
    }
    return results;
  }
  
  /**
   * Return a vector of cleanup TaskInProgress objects
   */
  public synchronized Vector<TaskInProgress> reportCleanupTIPs(
                                               boolean shouldBeComplete) {
    
    Vector<TaskInProgress> results = new Vector<TaskInProgress>();
    for (int i = 0; i < cleanup.length; i++) {
      if (cleanup[i].isComplete() == shouldBeComplete) {
        results.add(cleanup[i]);
      }
    }
    return results;
  }

  /**
   * Return a vector of setup TaskInProgress objects
   */
  public synchronized Vector<TaskInProgress> reportSetupTIPs(
                                               boolean shouldBeComplete) {
    
    Vector<TaskInProgress> results = new Vector<TaskInProgress>();
    for (int i = 0; i < setup.length; i++) {
      if (setup[i].isComplete() == shouldBeComplete) {
        results.add(setup[i]);
      }
    }
    return results;
  }

  ////////////////////////////////////////////////////
  // Status update methods
  ////////////////////////////////////////////////////

  /**
   * Assuming {@link JobTracker} is locked on entry.
   */
  public synchronized void updateTaskStatus(TaskInProgress tip, 
                                            TaskStatus status) {

    double oldProgress = tip.getProgress();   // save old progress
    boolean wasRunning = tip.isRunning();
    boolean wasComplete = tip.isComplete();
    boolean wasPending = tip.isOnlyCommitPending();
    TaskAttemptID taskid = status.getTaskID();
    boolean wasAttemptRunning = tip.isAttemptRunning(taskid);

    
    // If the TIP is already completed and the task reports as SUCCEEDED then 
    // mark the task as KILLED.
    // In case of task with no promotion the task tracker will mark the task 
    // as SUCCEEDED.
    // User has requested to kill the task, but TT reported SUCCEEDED, 
    // mark the task KILLED.
    if ((wasComplete || tip.wasKilled(taskid)) && 
        (status.getRunState() == TaskStatus.State.SUCCEEDED)) {
      status.setRunState(TaskStatus.State.KILLED);
    }
    
    // If the job is complete or task-cleanup is switched off
    // and a task has just reported its state as FAILED_UNCLEAN/KILLED_UNCLEAN, 
    // make the task's state FAILED/KILLED without launching cleanup attempt.
    // Note that if task is already a cleanup attempt, 
    // we don't change the state to make sure the task gets a killTaskAction
    if ((this.isComplete() || jobFailed || jobKilled || !taskCleanupNeeded) && 
        !tip.isCleanupAttempt(taskid)) {
      if (status.getRunState() == TaskStatus.State.FAILED_UNCLEAN) {
        status.setRunState(TaskStatus.State.FAILED);
      } else if (status.getRunState() == TaskStatus.State.KILLED_UNCLEAN) {
        status.setRunState(TaskStatus.State.KILLED);
      }
    }
    
    boolean change = tip.updateStatus(status);
    if (change) {
      TaskStatus.State state = status.getRunState();
      // get the TaskTrackerStatus where the task ran 
      TaskTracker taskTracker = 
        this.jobtracker.getTaskTracker(tip.machineWhereTaskRan(taskid));
      TaskTrackerStatus ttStatus = 
        (taskTracker == null) ? null : taskTracker.getStatus();
      String taskTrackerHttpLocation = null; 

      if (null != ttStatus){
        String host;
        if (NetUtils.getStaticResolution(ttStatus.getHost()) != null) {
          host = NetUtils.getStaticResolution(ttStatus.getHost());
        } else {
          host = ttStatus.getHost();
        }
        taskTrackerHttpLocation = "http://" + host + ":"
            + ttStatus.getHttpPort(); 
      }

      TaskCompletionEvent taskEvent = null;
      if (state == TaskStatus.State.SUCCEEDED) {
        taskEvent = new TaskCompletionEvent(
                                            taskCompletionEventTracker, 
                                            taskid,
                                            tip.idWithinJob(),
                                            status.getIsMap() &&
                                            !tip.isJobCleanupTask() &&
                                            !tip.isJobSetupTask(),
                                            TaskCompletionEvent.Status.SUCCEEDED,
                                            taskTrackerHttpLocation 
                                           );
        taskEvent.setTaskRunTime((int)(status.getFinishTime() 
                                       - status.getStartTime()));
        tip.setSuccessEventNumber(taskCompletionEventTracker); 
      } else if (state == TaskStatus.State.COMMIT_PENDING) {
        // If it is the first attempt reporting COMMIT_PENDING
        // ask the task to commit.
        if (!wasComplete && !wasPending) {
          tip.doCommit(taskid);
        }
        return;
      } else if (state == TaskStatus.State.FAILED_UNCLEAN ||
                 state == TaskStatus.State.KILLED_UNCLEAN) {
        tip.incompleteSubTask(taskid, this.status);
        // add this task, to be rescheduled as cleanup attempt
        if (tip.isMapTask()) {
          mapCleanupTasks.add(taskid);
        } else {
          reduceCleanupTasks.add(taskid);
        }
        // Remove the task entry from jobtracker
        jobtracker.removeTaskEntry(taskid);
      }
      //For a failed task update the JT datastructures. 
      else if (state == TaskStatus.State.FAILED ||
               state == TaskStatus.State.KILLED) {
        // Get the event number for the (possibly) previously successful
        // task. If there exists one, then set that status to OBSOLETE 
        int eventNumber;
        if ((eventNumber = tip.getSuccessEventNumber()) != -1) {
          TaskCompletionEvent t = 
            this.taskCompletionEvents.get(eventNumber);
          if (t.getTaskAttemptId().equals(taskid))
            t.setTaskStatus(TaskCompletionEvent.Status.OBSOLETE);
        }
        
        // Tell the job to fail the relevant task
        failedTask(tip, taskid, status, taskTracker,
                   wasRunning, wasComplete, wasAttemptRunning);

        // Did the task failure lead to tip failure?
        TaskCompletionEvent.Status taskCompletionStatus = 
          (state == TaskStatus.State.FAILED ) ?
              TaskCompletionEvent.Status.FAILED :
              TaskCompletionEvent.Status.KILLED;
        if (tip.isFailed()) {
          taskCompletionStatus = TaskCompletionEvent.Status.TIPFAILED;
        }
        taskEvent = new TaskCompletionEvent(taskCompletionEventTracker, 
                                            taskid,
                                            tip.idWithinJob(),
                                            status.getIsMap() &&
                                            !tip.isJobCleanupTask() &&
                                            !tip.isJobSetupTask(),
                                            taskCompletionStatus, 
                                            taskTrackerHttpLocation
                                           );
      }          

      // Add the 'complete' task i.e. successful/failed
      // It _is_ safe to add the TaskCompletionEvent.Status.SUCCEEDED
      // *before* calling TIP.completedTask since:
      // a. One and only one task of a TIP is declared as a SUCCESS, the
      //    other (speculative tasks) are marked KILLED
      // b. TIP.completedTask *does not* throw _any_ exception at all.
      if (taskEvent != null) {
        this.taskCompletionEvents.add(taskEvent);
        taskCompletionEventTracker++;
        JobTrackerStatistics.TaskTrackerStat ttStat = jobtracker.
           getStatistics().getTaskTrackerStat(tip.machineWhereTaskRan(taskid));
        if(ttStat != null) { // ttStat can be null in case of lost tracker
          ttStat.incrTotalTasks();
        }
        if (state == TaskStatus.State.SUCCEEDED) {
          completedTask(tip, status);
          if(ttStat != null) {
            ttStat.incrSucceededTasks();
          }
        }
      }
    }
        
    //
    // Update JobInProgress status
    //
    if (LOG.isDebugEnabled()) {
      LOG.debug("Taking progress for " + tip.getTIPId() + " from " + 
                 oldProgress + " to " + tip.getProgress());
    }
    
    if (!tip.isJobCleanupTask() && !tip.isJobSetupTask()) {
      double progressDelta = tip.getProgress() - oldProgress;
      if (tip.isMapTask()) {
          this.status.setMapProgress((float) (this.status.mapProgress() +
                                              progressDelta / maps.length));
      } else {
        this.status.setReduceProgress((float) (this.status.reduceProgress() + 
                                           (progressDelta / reduces.length)));
      }
    }
  }

  /**
   * Returns the job-level counters.
   * 
   * @return the job-level counters.
   */
  public synchronized Counters getJobCounters() {
    return jobCounters;
  }
  
  /**
   *  Returns map phase counters by summing over all map tasks in progress.
   */
  public synchronized Counters getMapCounters() {
    return incrementTaskCounters(new Counters(), maps);
  }
    
  /**
   *  Returns map phase counters by summing over all map tasks in progress.
   */
  public synchronized Counters getReduceCounters() {
    return incrementTaskCounters(new Counters(), reduces);
  }
    
  /**
   *  Returns the total job counters, by adding together the job, 
   *  the map and the reduce counters.
   */
  public Counters getCounters() {
    Counters result = new Counters();
    synchronized (this) {
      result.incrAllCounters(getJobCounters());
    }

    // the counters of TIPs are not updated in place.
    // hence read-only access is ok without any locks
    incrementTaskCounters(result, maps);
    return incrementTaskCounters(result, reduces);
  }
    
  /**
   * Increments the counters with the counters from each task.
   * @param counters the counters to increment
   * @param tips the tasks to add in to counters
   * @return counters the same object passed in as counters
   */
  private Counters incrementTaskCounters(Counters counters,
                                         TaskInProgress[] tips) {
    try {
      for (TaskInProgress tip : tips) {
        counters.incrAllCounters(tip.getCounters());
      }
    } catch (LimitExceededException e) {
      // too many user counters/groups, leaving existing counters intact.
    }
    return counters;
  }

  /////////////////////////////////////////////////////
  // Create/manage tasks
  /////////////////////////////////////////////////////
  /**
   * Return a MapTask, if appropriate, to run on the given tasktracker
   */
  public synchronized Task obtainNewMapTask(TaskTrackerStatus tts, 
                                            int clusterSize, 
                                            int numUniqueHosts,
                                            int maxCacheLevel
                                           ) throws IOException {
    if (status.getRunState() != JobStatus.RUNNING) {
      LOG.info("Cannot create task split for " + profile.getJobID());
      return null;
    }
       
    int target = findNewMapTask(tts, clusterSize, numUniqueHosts,
        maxCacheLevel);
    if (target == -1) {
      return null;
    }
    
    Task result = maps[target].getTaskToRun(tts.getTrackerName());
    if (result != null) {
      addRunningTaskToTIP(maps[target], result.getTaskID(), tts, true);
    }

    return result;
  } 
  
  /**
   * Return a MapTask, if appropriate, to run on the given tasktracker
   */
  public synchronized Task obtainNewMapTask(TaskTrackerStatus tts, 
                                            int clusterSize, 
                                            int numUniqueHosts
                                           ) throws IOException {
    return obtainNewMapTask(tts, clusterSize, numUniqueHosts, anyCacheLevel);
  }    

  /*
   * Return task cleanup attempt if any, to run on a given tracker
   */
  public Task obtainTaskCleanupTask(TaskTrackerStatus tts, 
                                                 boolean isMapSlot)
  throws IOException {
    if (!tasksInited.get()) {
      return null;
    }
    synchronized (this) {
      if (this.status.getRunState() != JobStatus.RUNNING || 
          jobFailed || jobKilled) {
        return null;
      }
      String taskTracker = tts.getTrackerName();
      if (!shouldRunOnTaskTracker(taskTracker)) {
        return null;
      }
      TaskAttemptID taskid = null;
      TaskInProgress tip = null;
      if (isMapSlot) {
        if (!mapCleanupTasks.isEmpty()) {
          taskid = mapCleanupTasks.remove(0);
          tip = maps[taskid.getTaskID().getId()];
        }
      } else {
        if (!reduceCleanupTasks.isEmpty()) {
          taskid = reduceCleanupTasks.remove(0);
          tip = reduces[taskid.getTaskID().getId()];
        }
      }
      if (tip != null) {
        return tip.addRunningTask(taskid, taskTracker, true);
      }
      return null;
    }
  }
  
  public synchronized Task obtainNewLocalMapTask(TaskTrackerStatus tts,
                                                     int clusterSize, 
                                                     int numUniqueHosts)
  throws IOException {
    if (!tasksInited.get()) {
      LOG.info("Cannot create task split for " + profile.getJobID());
      return null;
    }
  
    return obtainNewMapTask(tts, clusterSize, numUniqueHosts, maxLevel);
  }
  
  public synchronized Task obtainNewNonLocalMapTask(TaskTrackerStatus tts,
                                                    int clusterSize, 
                                                    int numUniqueHosts)
  throws IOException {
    if (!tasksInited.get()) {
      LOG.info("Cannot create task split for " + profile.getJobID());
      return null;
    }
  
    return obtainNewMapTask(tts, clusterSize, numUniqueHosts,
        NON_LOCAL_CACHE_LEVEL);
  }
  
  /**
   * Return a CleanupTask, if appropriate, to run on the given tasktracker
   * 
   */
  public Task obtainJobCleanupTask(TaskTrackerStatus tts, 
                                             int clusterSize, 
                                             int numUniqueHosts,
                                             boolean isMapSlot
                                            ) throws IOException {
    if(!tasksInited.get() || !jobSetupCleanupNeeded) {
      return null;
    }
    
    synchronized(this) {
      if (!canLaunchJobCleanupTask()) {
        return null;
      }
      
      String taskTracker = tts.getTrackerName();
      // Update the last-known clusterSize
      this.clusterSize = clusterSize;
      if (!shouldRunOnTaskTracker(taskTracker)) {
        return null;
      }
      
      List<TaskInProgress> cleanupTaskList = new ArrayList<TaskInProgress>();
      if (isMapSlot) {
        cleanupTaskList.add(cleanup[0]);
      } else {
        cleanupTaskList.add(cleanup[1]);
      }
      TaskInProgress tip = findTaskFromList(cleanupTaskList,
                             tts, numUniqueHosts, false);
      if (tip == null) {
        return null;
      }
      
      // Now launch the cleanupTask
      Task result = tip.getTaskToRun(tts.getTrackerName());
      if (result != null) {
        addRunningTaskToTIP(tip, result.getTaskID(), tts, true);
        if (jobFailed) {
          result.setJobCleanupTaskState(org.apache.hadoop.mapreduce.JobStatus
                .State.FAILED);
        } else if (jobKilled) {
          result.setJobCleanupTaskState(org.apache.hadoop.mapreduce.JobStatus
                .State.KILLED);
        } else {
          result.setJobCleanupTaskState(org.apache.hadoop.mapreduce
                .JobStatus.State.SUCCEEDED);
        }
      }
      return result;
    }
    
  }
  
  /**
   * Check whether cleanup task can be launched for the job.
   * 
   * Cleanup task can be launched if it is not already launched
   * or job is Killed
   * or all maps and reduces are complete
   * @return true/false
   */
  private synchronized boolean canLaunchJobCleanupTask() {
    // check if the job is running
    if (status.getRunState() != JobStatus.RUNNING &&
        status.getRunState() != JobStatus.PREP) {
      return false;
    }
    // check if cleanup task has been launched already or if setup isn't
    // launched already. The later check is useful when number of maps is
    // zero.
    if (launchedCleanup || !isSetupFinished()) {
      return false;
    }
    // check if job has failed or killed
    if (jobKilled || jobFailed) {
      return true;
    }
    // Check if all maps and reducers have finished.
    boolean launchCleanupTask = 
        ((finishedMapTasks + failedMapTIPs) == (numMapTasks));
    if (launchCleanupTask) {
      launchCleanupTask = 
        ((finishedReduceTasks + failedReduceTIPs) == numReduceTasks);
    }
    return launchCleanupTask;
  }

  /**
   * Return a SetupTask, if appropriate, to run on the given tasktracker
   * 
   */
  public Task obtainJobSetupTask(TaskTrackerStatus tts, 
                                             int clusterSize, 
                                             int numUniqueHosts,
                                             boolean isMapSlot
                                            ) throws IOException {
    if(!tasksInited.get() || !jobSetupCleanupNeeded) {
      return null;
    }
    
    synchronized(this) {
      if (!canLaunchSetupTask()) {
        return null;
      }
      String taskTracker = tts.getTrackerName();
      // Update the last-known clusterSize
      this.clusterSize = clusterSize;
      if (!shouldRunOnTaskTracker(taskTracker)) {
        return null;
      }
      
      List<TaskInProgress> setupTaskList = new ArrayList<TaskInProgress>();
      if (isMapSlot) {
        setupTaskList.add(setup[0]);
      } else {
        setupTaskList.add(setup[1]);
      }
      TaskInProgress tip = findTaskFromList(setupTaskList,
                             tts, numUniqueHosts, false);
      if (tip == null) {
        return null;
      }
      
      // Now launch the setupTask
      Task result = tip.getTaskToRun(tts.getTrackerName());
      if (result != null) {
        addRunningTaskToTIP(tip, result.getTaskID(), tts, true);
      }
      return result;
    }
  }
  
  public synchronized boolean scheduleReduces() {
    return finishedMapTasks >= completedMapsForReduceSlowstart;
  }
  
  /**
   * Check whether setup task can be launched for the job.
   * 
   * Setup task can be launched after the tasks are inited
   * and Job is in PREP state
   * and if it is not already launched
   * or job is not Killed/Failed
   * @return true/false
   */
  private synchronized boolean canLaunchSetupTask() {
    return (tasksInited.get() && status.getRunState() == JobStatus.PREP && 
           !launchedSetup && !jobKilled && !jobFailed);
  }
  

  /**
   * Return a ReduceTask, if appropriate, to run on the given tasktracker.
   * We don't have cache-sensitivity for reduce tasks, as they
   *  work on temporary MapRed files.  
   */
  public synchronized Task obtainNewReduceTask(TaskTrackerStatus tts,
                                               int clusterSize,
                                               int numUniqueHosts
                                              ) throws IOException {
    if (status.getRunState() != JobStatus.RUNNING) {
      LOG.info("Cannot create task split for " + profile.getJobID());
      return null;
    }
    
    // Ensure we have sufficient map outputs ready to shuffle before 
    // scheduling reduces
    if (!scheduleReduces()) {
      return null;
    }

    int  target = findNewReduceTask(tts, clusterSize, numUniqueHosts);
    if (target == -1) {
      return null;
    }
    
    Task result = reduces[target].getTaskToRun(tts.getTrackerName());
    if (result != null) {
      addRunningTaskToTIP(reduces[target], result.getTaskID(), tts, true);
    }

    return result;
  }
  
  // returns the (cache)level at which the nodes matches
  private int getMatchingLevelForNodes(Node n1, Node n2) {
    int count = 0;
    do {
      if (n1.equals(n2)) {
        return count;
      }
      ++count;
      n1 = n1.getParent();
      n2 = n2.getParent();
    } while (n1 != null);
    return this.maxLevel;
  }

  /**
   * Populate the data structures as a task is scheduled.
   * 
   * Assuming {@link JobTracker} is locked on entry.
   * 
   * @param tip The tip for which the task is added
   * @param id The attempt-id for the task
   * @param tts task-tracker status
   * @param isScheduled Whether this task is scheduled from the JT or has 
   *        joined back upon restart
   */
  synchronized void addRunningTaskToTIP(TaskInProgress tip, TaskAttemptID id, 
                                        TaskTrackerStatus tts, 
                                        boolean isScheduled) {
    // Make an entry in the tip if the attempt is not scheduled i.e externally
    // added
    if (!isScheduled) {
      tip.addRunningTask(id, tts.getTrackerName());
    }
    final JobTrackerInstrumentation metrics = jobtracker.getInstrumentation();

    // keeping the earlier ordering intact
    TaskType name;
    String splits = "";
    Enum counter = null;
    if (tip.isJobSetupTask()) {
      launchedSetup = true;
      name = TaskType.JOB_SETUP;
    } else if (tip.isJobCleanupTask()) {
      launchedCleanup = true;
      name = TaskType.JOB_CLEANUP;
    } else if (tip.isMapTask()) {
      ++runningMapTasks;
      name = TaskType.MAP;
      counter = JobCounter.TOTAL_LAUNCHED_MAPS;
      splits = tip.getSplitNodes();
      if (tip.isSpeculating()) {
        speculativeMapTasks++;
        metrics.speculateMap(id);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Chosen speculative task, current speculativeMap task count: "
                    + speculativeMapTasks);
        }
      }
      metrics.launchMap(id);
    } else {
      ++runningReduceTasks;
      name = TaskType.REDUCE;
      counter = JobCounter.TOTAL_LAUNCHED_REDUCES;
      if (tip.isSpeculating()) {
        speculativeReduceTasks++;
        metrics.speculateReduce(id);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Chosen speculative task, current speculativeReduce task count: "
                    + speculativeReduceTasks);
        }
      }
      metrics.launchReduce(id);
    }
    // Note that the logs are for the scheduled tasks only. Tasks that join on 
    // restart has already their logs in place.
    if (tip.isFirstAttempt(id)) {
      TaskStartedEvent tse = new TaskStartedEvent(tip.getTIPId(), 
          tip.getExecStartTime(),
          name, splits);
      
      jobHistory.logEvent(tse, tip.getJob().jobId);
      setFirstTaskLaunchTime(tip);
    }
    if (!tip.isJobSetupTask() && !tip.isJobCleanupTask()) {
      jobCounters.incrCounter(counter, 1);
    }
    
    //TODO The only problem with these counters would be on restart.
    // The jobtracker updates the counter only when the task that is scheduled
    // if from a non-running tip and is local (data, rack ...). But upon restart
    // as the reports come from the task tracker, there is no good way to infer
    // when exactly to increment the locality counters. The only solution is to 
    // increment the counters for all the tasks irrespective of 
    //    - whether the tip is running or not
    //    - whether its a speculative task or not
    //
    // So to simplify, increment the data locality counter whenever there is 
    // data locality.
    if (tip.isMapTask() && !tip.isJobSetupTask() && !tip.isJobCleanupTask()) {
      // increment the data locality counter for maps
      int level = getLocalityLevel(tip, tts);
      switch (level) {
      case 0 :
        LOG.info("Choosing data-local task " + tip.getTIPId());
        jobCounters.incrCounter(JobCounter.DATA_LOCAL_MAPS, 1);
        metrics.launchDataLocalMap(id);
        break;
      case 1:
        LOG.info("Choosing rack-local task " + tip.getTIPId());
        jobCounters.incrCounter(JobCounter.RACK_LOCAL_MAPS, 1);
        metrics.launchRackLocalMap(id);
        break;
      default :
        // check if there is any locality
        if (level != this.maxLevel) {
          LOG.info("Choosing cached task at level " + level + tip.getTIPId());
          jobCounters.incrCounter(JobCounter.OTHER_LOCAL_MAPS, 1);
        }
        break;
      }
    }
  }

  void setFirstTaskLaunchTime(TaskInProgress tip) {
    TaskType key = getTaskType(tip);

    synchronized(firstTaskLaunchTimes) {
      // Could be optimized to do only one lookup with a little more code
      if (!firstTaskLaunchTimes.containsKey(key)) {
        firstTaskLaunchTimes.put(key, tip.getExecStartTime());
      }
    }
  }
    
  public static String convertTrackerNameToHostName(String trackerName) {
    // Ugly!
    // Convert the trackerName to it's host name
    int indexOfColon = trackerName.indexOf(":");
    String trackerHostName = (indexOfColon == -1) ? 
      trackerName : 
      trackerName.substring(0, indexOfColon);
    return trackerHostName.substring("tracker_".length());
  }
    
  /**
   * Note that a task has failed on a given tracker and add the tracker  
   * to the blacklist iff too many trackers in the cluster i.e. 
   * (clusterSize * CLUSTER_BLACKLIST_PERCENT) haven't turned 'flaky' already.
   * 
   * @param taskTracker task-tracker on which a task failed
   */
  synchronized void addTrackerTaskFailure(String trackerName, 
                                          TaskTracker taskTracker) {
    if (flakyTaskTrackers < (clusterSize * CLUSTER_BLACKLIST_PERCENT)) { 
      String trackerHostName = convertTrackerNameToHostName(trackerName);

      Integer trackerFailures = trackerToFailuresMap.get(trackerHostName);
      if (trackerFailures == null) {
        trackerFailures = 0;
      }
      trackerToFailuresMap.put(trackerHostName, ++trackerFailures);

      // Check if this tasktracker has turned 'flaky'
      if (trackerFailures.intValue() == maxTaskFailuresPerTracker) {
        ++flakyTaskTrackers;
        
        // Cancel reservations if appropriate
        if (taskTracker != null) {
          if (trackersReservedForMaps.containsKey(taskTracker)) {
            taskTracker.unreserveSlots(TaskType.MAP, this);
          }
          if (trackersReservedForReduces.containsKey(taskTracker)) {
            taskTracker.unreserveSlots(TaskType.REDUCE, this);
          }
        }
        LOG.info("TaskTracker at '" + trackerHostName + "' turned 'flaky'");
      }
    }
  }
  
  public synchronized void reserveTaskTracker(TaskTracker taskTracker,
                                              TaskType type, int numSlots) {
    Map<TaskTracker, FallowSlotInfo> map =
      (type == TaskType.MAP) ? trackersReservedForMaps : trackersReservedForReduces;
    
    long now = System.currentTimeMillis();
    
    FallowSlotInfo info = map.get(taskTracker);
    int reservedSlots = 0;
    if (info == null) {
      info = new FallowSlotInfo(now, numSlots);
      reservedSlots = numSlots;
    } else {
      // Increment metering info if the reservation is changing
      if (info.getNumSlots() != numSlots) {
        Enum<JobCounter> counter = 
          (type == TaskType.MAP) ? 
              JobCounter.FALLOW_SLOTS_MILLIS_MAPS : 
              JobCounter.FALLOW_SLOTS_MILLIS_REDUCES;
        long fallowSlotMillis = (now - info.getTimestamp()) * info.getNumSlots();
        jobCounters.incrCounter(counter, fallowSlotMillis);
        
        // Update 
        reservedSlots = numSlots - info.getNumSlots();
        info.setTimestamp(now);
        info.setNumSlots(numSlots);
      }
    }
    map.put(taskTracker, info);
    if (type == TaskType.MAP) {
      jobtracker.getInstrumentation().addReservedMapSlots(reservedSlots);
    }
    else {
      jobtracker.getInstrumentation().addReservedReduceSlots(reservedSlots);
    }
    jobtracker.incrementReservations(type, reservedSlots);
  }
  
  public synchronized void unreserveTaskTracker(TaskTracker taskTracker,
                                                TaskType type) {
    Map<TaskTracker, FallowSlotInfo> map =
      (type == TaskType.MAP) ? trackersReservedForMaps : 
                               trackersReservedForReduces;

    FallowSlotInfo info = map.get(taskTracker);
    if (info == null) {
      LOG.warn("Cannot find information about fallow slots for " + 
               taskTracker.getTrackerName());
      return;
    }
    
    long now = System.currentTimeMillis();

    Enum<JobCounter> counter = 
      (type == TaskType.MAP) ? 
          JobCounter.FALLOW_SLOTS_MILLIS_MAPS : 
          JobCounter.FALLOW_SLOTS_MILLIS_REDUCES;
    long fallowSlotMillis = (now - info.getTimestamp()) * info.getNumSlots();
    jobCounters.incrCounter(counter, fallowSlotMillis);

    map.remove(taskTracker);
    if (type == TaskType.MAP) {
      jobtracker.getInstrumentation().decReservedMapSlots(info.getNumSlots());
    }
    else {
      jobtracker.getInstrumentation().decReservedReduceSlots(
        info.getNumSlots());
    }
    jobtracker.decrementReservations(type, info.getNumSlots());
  }
  
  public int getNumReservedTaskTrackersForMaps() {
    return trackersReservedForMaps.size();
  }
  
  public int getNumReservedTaskTrackersForReduces() {
    return trackersReservedForReduces.size();
  }
  
  private int getTrackerTaskFailures(String trackerName) {
    String trackerHostName = convertTrackerNameToHostName(trackerName);
    Integer failedTasks = trackerToFailuresMap.get(trackerHostName);
    return (failedTasks != null) ? failedTasks.intValue() : 0; 
  }
    
  /**
   * Get the black listed trackers for the job
   * 
   * @return List of blacklisted tracker names
 
