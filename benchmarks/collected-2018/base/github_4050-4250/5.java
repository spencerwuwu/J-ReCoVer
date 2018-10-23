// https://searchcode.com/api/result/8194716/

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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DF;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.LocalDirAllocator;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.http.HttpServer;
import org.apache.hadoop.io.SecureIOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.ipc.ProtocolSignature;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.mapred.TaskController.DebugScriptContext;
import org.apache.hadoop.mapred.TaskController.JobInitializationContext;
import org.apache.hadoop.mapred.CleanupQueue.PathDeletionContext;
import org.apache.hadoop.mapred.TaskController.TaskControllerPathDeletionContext;
import org.apache.hadoop.mapred.TaskController.TaskControllerTaskPathDeletionContext;
import org.apache.hadoop.mapred.TaskController.TaskControllerJobPathDeletionContext;
import org.apache.hadoop.mapred.TaskTrackerStatus.TaskTrackerHealthStatus;
import org.apache.hadoop.mapred.pipes.Submitter;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.mapreduce.MRJobConfig;
import static org.apache.hadoop.mapred.QueueManager.toFullPropertyName;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.filecache.TrackerDistributedCacheManager;
import org.apache.hadoop.mapreduce.security.SecureShuffleUtils;
import org.apache.hadoop.mapreduce.security.TokenCache;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.mapreduce.security.token.JobTokenIdentifier;
import org.apache.hadoop.mapreduce.security.token.JobTokenSecretManager;
import org.apache.hadoop.mapreduce.server.jobtracker.JTConfig;
import org.apache.hadoop.mapreduce.server.tasktracker.TTConfig;
import org.apache.hadoop.mapreduce.server.tasktracker.Localizer;
import org.apache.hadoop.mapreduce.task.reduce.ShuffleHeader;
import org.apache.hadoop.metrics.MetricsContext;
import org.apache.hadoop.metrics.MetricsException;
import org.apache.hadoop.metrics.MetricsRecord;
import org.apache.hadoop.metrics.MetricsUtil;
import org.apache.hadoop.metrics.Updater;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.PolicyProvider;
import org.apache.hadoop.util.DiskChecker;
import org.apache.hadoop.mapreduce.util.ConfigUtil;
import org.apache.hadoop.mapreduce.util.MemoryCalculatorPlugin;
import org.apache.hadoop.mapreduce.util.ResourceCalculatorPlugin;
import org.apache.hadoop.mapreduce.util.ProcfsBasedProcessTree;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.RunJar;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.VersionInfo;
import org.apache.hadoop.util.DiskChecker.DiskErrorException;
import org.apache.hadoop.mapreduce.util.MRAsyncDiskService;

/*******************************************************
 * TaskTracker is a process that starts and tracks MR Tasks
 * in a networked environment.  It contacts the JobTracker
 * for Task assignments and reporting results.
 *
 *******************************************************/
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class TaskTracker 
    implements MRConstants, TaskUmbilicalProtocol, Runnable, TTConfig {
  /**
   * @deprecated
   */
  @Deprecated
  static final String MAPRED_TASKTRACKER_VMEM_RESERVED_PROPERTY =
    "mapred.tasktracker.vmem.reserved";
  /**
   * @deprecated
   */
  @Deprecated
  static final String MAPRED_TASKTRACKER_PMEM_RESERVED_PROPERTY =
    "mapred.tasktracker.pmem.reserved";


  static final long WAIT_FOR_DONE = 3 * 1000;
  private int httpPort;

  static enum State {NORMAL, STALE, INTERRUPTED, DENIED}

  static{
    ConfigUtil.loadResources();
  }

  public static final Log LOG =
    LogFactory.getLog(TaskTracker.class);

  public static final String MR_CLIENTTRACE_FORMAT =
    "src: %s" +     // src IP
    ", dest: %s" +  // dst IP
    ", maps: %s" + // number of maps
    ", op: %s" +    // operation
    ", reduceID: %s" + // reduce id
    ", duration: %s"; // duration

  public static final Log ClientTraceLog =
    LogFactory.getLog(TaskTracker.class.getName() + ".clienttrace");

  // Job ACLs file is created by TaskTracker under userlogs/$jobid directory for
  // each job at job localization time. This will be used by TaskLogServlet for
  // authorizing viewing of task logs of that job
  static String jobACLsFile = "job-acls.xml";

  volatile boolean running = true;

  private LocalDirAllocator localDirAllocator;
  String taskTrackerName;
  String localHostname;
  InetSocketAddress jobTrackAddr;
    
  InetSocketAddress taskReportAddress;

  Server taskReportServer = null;
  InterTrackerProtocol jobClient;

  private TrackerDistributedCacheManager distributedCacheManager;
    
  // last heartbeat response received
  short heartbeatResponseId = -1;
  
  static final String TASK_CLEANUP_SUFFIX = ".cleanup";

  /*
   * This is the last 'status' report sent by this tracker to the JobTracker.
   * 
   * If the rpc call succeeds, this 'status' is cleared-out by this tracker;
   * indicating that a 'fresh' status report be generated; in the event the
   * rpc calls fails for whatever reason, the previous status report is sent
   * again.
   */
  TaskTrackerStatus status = null;
  
  // The system-directory on HDFS where job files are stored 
  Path systemDirectory = null;
  
  // The filesystem where job files are stored
  FileSystem systemFS = null;
  
  private final HttpServer server;
    
  volatile boolean shuttingDown = false;
    
  Map<TaskAttemptID, TaskInProgress> tasks = new HashMap<TaskAttemptID, TaskInProgress>();
  /**
   * Map from taskId -> TaskInProgress.
   */
  Map<TaskAttemptID, TaskInProgress> runningTasks = null;
  Map<JobID, RunningJob> runningJobs = new TreeMap<JobID, RunningJob>();
  private final JobTokenSecretManager jobTokenSecretManager 
    = new JobTokenSecretManager();

  volatile int mapTotal = 0;
  volatile int reduceTotal = 0;
  boolean justStarted = true;
  boolean justInited = true;
  // Mark reduce tasks that are shuffling to rollback their events index
  Set<TaskAttemptID> shouldReset = new HashSet<TaskAttemptID>();
    
  //dir -> DF
  Map<String, DF> localDirsDf = new HashMap<String, DF>();
  long minSpaceStart = 0;
  //must have this much space free to start new tasks
  boolean acceptNewTasks = true;
  long minSpaceKill = 0;
  //if we run under this limit, kill one task
  //and make sure we never receive any new jobs
  //until all the old tasks have been cleaned up.
  //this is if a machine is so full it's only good
  //for serving map output to the other nodes

  static Random r = new Random();
  public static final String SUBDIR = "taskTracker";
  static final String DISTCACHEDIR = "distcache";
  static final String JOBCACHE = "jobcache";
  static final String OUTPUT = "output";
  private static final String JARSDIR = "jars";
  static final String LOCAL_SPLIT_FILE = "split.dta";
  static final String LOCAL_SPLIT_META_FILE = "split.info";
  static final String JOBFILE = "job.xml";
  static final String JOB_TOKEN_FILE="jobToken"; //localized file

  static final String JOB_LOCAL_DIR = MRJobConfig.JOB_LOCAL_DIR;

  private JobConf fConf;
  private FileSystem localFs;

  private Localizer localizer;

  private int maxMapSlots;
  private int maxReduceSlots;
  private int failures;

  private ACLsManager aclsManager;
  
  // Performance-related config knob to send an out-of-band heartbeat
  // on task completion
  private volatile boolean oobHeartbeatOnTaskCompletion;
  
  // Track number of completed tasks to send an out-of-band heartbeat
  private IntWritable finishedCount = new IntWritable(0);
  
  private MapEventsFetcherThread mapEventsFetcher;
  int workerThreads;
  CleanupQueue directoryCleanupThread;
  private volatile JvmManager jvmManager;
  UserLogCleaner taskLogCleanupThread;
  private TaskMemoryManagerThread taskMemoryManager;
  private boolean taskMemoryManagerEnabled = true;
  private long totalVirtualMemoryOnTT = JobConf.DISABLED_MEMORY_LIMIT;
  private long totalPhysicalMemoryOnTT = JobConf.DISABLED_MEMORY_LIMIT;
  private long mapSlotMemorySizeOnTT = JobConf.DISABLED_MEMORY_LIMIT;
  private long reduceSlotSizeMemoryOnTT = JobConf.DISABLED_MEMORY_LIMIT;
  private long totalMemoryAllottedForTasks = JobConf.DISABLED_MEMORY_LIMIT;
  private long reservedPhysicalMemoryOnTT = JobConf.DISABLED_MEMORY_LIMIT;
  private ResourceCalculatorPlugin resourceCalculatorPlugin = null;

  /**
   * the minimum interval between jobtracker polls
   */
  private volatile int heartbeatInterval =
    JTConfig.JT_HEARTBEAT_INTERVAL_MIN_DEFAULT;
  /**
   * Number of maptask completion events locations to poll for at one time
   */  
  private int probe_sample_size = 500;

  private IndexCache indexCache;

  private MRAsyncDiskService asyncDiskService;
  
  MRAsyncDiskService getAsyncDiskService() {
    return asyncDiskService;
  }

  void setAsyncDiskService(MRAsyncDiskService asyncDiskService) {
    this.asyncDiskService = asyncDiskService;
  }

  /**
  * Handle to the specific instance of the {@link TaskController} class
  */
  private TaskController taskController;
  
  /**
   * Handle to the specific instance of the {@link NodeHealthCheckerService}
   */
  private NodeHealthCheckerService healthChecker;
  
  /*
   * A list of commitTaskActions for whom commit response has been received 
   */
  private List<TaskAttemptID> commitResponses = 
            Collections.synchronizedList(new ArrayList<TaskAttemptID>());

  private ShuffleServerMetrics shuffleServerMetrics;
  /** This class contains the methods that should be used for metrics-reporting
   * the specific metrics for shuffle. The TaskTracker is actually a server for
   * the shuffle and hence the name ShuffleServerMetrics.
   */
  class ShuffleServerMetrics implements Updater {
    private MetricsRecord shuffleMetricsRecord = null;
    private int serverHandlerBusy = 0;
    private long outputBytes = 0;
    private int failedOutputs = 0;
    private int successOutputs = 0;
    private int exceptionsCaught = 0;
    ShuffleServerMetrics(JobConf conf) {
      MetricsContext context = MetricsUtil.getContext("mapred");
      shuffleMetricsRecord = 
                           MetricsUtil.createRecord(context, "shuffleOutput");
      this.shuffleMetricsRecord.setTag("sessionId", conf.getSessionId());
      context.registerUpdater(this);
    }
    synchronized void serverHandlerBusy() {
      ++serverHandlerBusy;
    }
    synchronized void serverHandlerFree() {
      --serverHandlerBusy;
    }
    synchronized void outputBytes(long bytes) {
      outputBytes += bytes;
    }
    synchronized void failedOutput() {
      ++failedOutputs;
    }
    synchronized void successOutput() {
      ++successOutputs;
    }
    synchronized void exceptionsCaught() {
      ++exceptionsCaught;
    }
    public void doUpdates(MetricsContext unused) {
      synchronized (this) {
        if (workerThreads != 0) {
          shuffleMetricsRecord.setMetric("shuffle_handler_busy_percent", 
              100*((float)serverHandlerBusy/workerThreads));
        } else {
          shuffleMetricsRecord.setMetric("shuffle_handler_busy_percent", 0);
        }
        shuffleMetricsRecord.incrMetric("shuffle_output_bytes", 
                                        outputBytes);
        shuffleMetricsRecord.incrMetric("shuffle_failed_outputs", 
                                        failedOutputs);
        shuffleMetricsRecord.incrMetric("shuffle_success_outputs", 
                                        successOutputs);
        shuffleMetricsRecord.incrMetric("shuffle_exceptions_caught",
                                        exceptionsCaught);
        outputBytes = 0;
        failedOutputs = 0;
        successOutputs = 0;
        exceptionsCaught = 0;
      }
      shuffleMetricsRecord.update();
    }
  }
  

  
  
    
  private TaskTrackerInstrumentation myInstrumentation = null;

  public TaskTrackerInstrumentation getTaskTrackerInstrumentation() {
    return myInstrumentation;
  }

  // Currently used only in tests
  void setTaskTrackerInstrumentation(
      TaskTrackerInstrumentation trackerInstrumentation) {
    myInstrumentation = trackerInstrumentation;
  }

  /**
   * A list of tips that should be cleaned up.
   */
  private BlockingQueue<TaskTrackerAction> tasksToCleanup = 
    new LinkedBlockingQueue<TaskTrackerAction>();
    
  @Override
  public ProtocolSignature getProtocolSignature(String protocol,
      long clientVersion, int clientMethodsHash) throws IOException {
    return ProtocolSignature.getProtocolSignature(
        this, protocol, clientVersion, clientMethodsHash);
  }

  /**
   * A daemon-thread that pulls tips off the list of things to cleanup.
   */
  private Thread taskCleanupThread = 
    new Thread(new Runnable() {
        public void run() {
          while (true) {
            try {
              TaskTrackerAction action = tasksToCleanup.take();
              if (action instanceof KillJobAction) {
                purgeJob((KillJobAction) action);
              } else if (action instanceof KillTaskAction) {
                processKillTaskAction((KillTaskAction) action);
              } else {
                LOG.error("Non-delete action given to cleanup thread: "
                          + action);
              }
            } catch (Throwable except) {
              LOG.warn(StringUtils.stringifyException(except));
            }
          }
        }
      }, "taskCleanup");

  void processKillTaskAction(KillTaskAction killAction) throws IOException {
    TaskInProgress tip;
    synchronized (TaskTracker.this) {
      tip = tasks.get(killAction.getTaskID());
    }
    LOG.info("Received KillTaskAction for task: " + 
             killAction.getTaskID());
    purgeTask(tip, false);
  }
  
  public TaskController getTaskController() {
    return taskController;
  }
  
  // Currently this is used only by tests
  void setTaskController(TaskController t) {
    taskController = t;
  }
  
  private RunningJob addTaskToJob(JobID jobId, 
                                  TaskInProgress tip) {
    synchronized (runningJobs) {
      RunningJob rJob = null;
      if (!runningJobs.containsKey(jobId)) {
        rJob = new RunningJob(jobId);
        rJob.localized = false;
        rJob.tasks = new HashSet<TaskInProgress>();
        runningJobs.put(jobId, rJob);
      } else {
        rJob = runningJobs.get(jobId);
      }
      synchronized (rJob) {
        rJob.tasks.add(tip);
      }
      runningJobs.notify(); //notify the fetcher thread
      return rJob;
    }
  }

  private void removeTaskFromJob(JobID jobId, TaskInProgress tip) {
    synchronized (runningJobs) {
      RunningJob rjob = runningJobs.get(jobId);
      if (rjob == null) {
        LOG.warn("Unknown job " + jobId + " being deleted.");
      } else {
        synchronized (rjob) {
          rjob.tasks.remove(tip);
        }
      }
    }
  }

  JobTokenSecretManager getJobTokenSecretManager() {
    return jobTokenSecretManager;
  }
  
  RunningJob getRunningJob(JobID jobId) {
    return runningJobs.get(jobId);
  }

  Localizer getLocalizer() {
    return localizer;
  }

  void setLocalizer(Localizer l) {
    localizer = l;
  }

  public static String getUserDir(String user) {
    return TaskTracker.SUBDIR + Path.SEPARATOR + user;
  }

  public static String getPrivateDistributedCacheDir(String user) {
    return getUserDir(user) + Path.SEPARATOR + TaskTracker.DISTCACHEDIR;
  }
  
  public static String getPublicDistributedCacheDir() {
    return TaskTracker.SUBDIR + Path.SEPARATOR + TaskTracker.DISTCACHEDIR;
  }

  public static String getJobCacheSubdir(String user) {
    return getUserDir(user) + Path.SEPARATOR + TaskTracker.JOBCACHE;
  }

  public static String getLocalJobDir(String user, String jobid) {
    return getJobCacheSubdir(user) + Path.SEPARATOR + jobid;
  }

  static String getLocalJobConfFile(String user, String jobid) {
    return getLocalJobDir(user, jobid) + Path.SEPARATOR + TaskTracker.JOBFILE;
  }
  
  static String getLocalJobTokenFile(String user, String jobid) {
    return getLocalJobDir(user, jobid) + Path.SEPARATOR + TaskTracker.JOB_TOKEN_FILE;
  }


  static String getTaskConfFile(String user, String jobid, String taskid,
      boolean isCleanupAttempt) {
    return getLocalTaskDir(user, jobid, taskid, isCleanupAttempt)
        + Path.SEPARATOR + TaskTracker.JOBFILE;
  }

  static String getJobJarsDir(String user, String jobid) {
    return getLocalJobDir(user, jobid) + Path.SEPARATOR + TaskTracker.JARSDIR;
  }

  static String getJobJarFile(String user, String jobid) {
    return getJobJarsDir(user, jobid) + Path.SEPARATOR + "job.jar";
  }

  static String getJobWorkDir(String user, String jobid) {
    return getLocalJobDir(user, jobid) + Path.SEPARATOR + MRConstants.WORKDIR;
  }

  static String getLocalSplitMetaFile(String user, String jobid, String taskid){
    return TaskTracker.getLocalTaskDir(user, jobid, taskid) + Path.SEPARATOR
        + TaskTracker.LOCAL_SPLIT_META_FILE;
  }

  static String getLocalSplitFile(String user, String jobid, String taskid) {
    return TaskTracker.getLocalTaskDir(user, jobid, taskid) + Path.SEPARATOR
           + TaskTracker.LOCAL_SPLIT_FILE;
  }
  
  static String getIntermediateOutputDir(String user, String jobid,
      String taskid) {
    return getLocalTaskDir(user, jobid, taskid) + Path.SEPARATOR
        + TaskTracker.OUTPUT;
  }

  static String getLocalTaskDir(String user, String jobid, String taskid) {
    return getLocalTaskDir(user, jobid, taskid, false);
  }

  public static String getLocalTaskDir(String user, String jobid, String taskid,
      boolean isCleanupAttempt) {
    String taskDir = getLocalJobDir(user, jobid) + Path.SEPARATOR + taskid;
    if (isCleanupAttempt) {
      taskDir = taskDir + TASK_CLEANUP_SUFFIX;
    }
    return taskDir;
  }

  static String getTaskWorkDir(String user, String jobid, String taskid,
      boolean isCleanupAttempt) {
    String dir = getLocalTaskDir(user, jobid, taskid, isCleanupAttempt);
    return dir + Path.SEPARATOR + MRConstants.WORKDIR;
  }

  String getPid(TaskAttemptID tid) {
    TaskInProgress tip = tasks.get(tid);
    if (tip != null) {
      return jvmManager.getPid(tip.getTaskRunner());  
    }
    return null;
  }
  
  public long getProtocolVersion(String protocol, 
                                 long clientVersion) throws IOException {
    if (protocol.equals(TaskUmbilicalProtocol.class.getName())) {
      return TaskUmbilicalProtocol.versionID;
    } else {
      throw new IOException("Unknown protocol for task tracker: " +
                            protocol);
    }
  }
    
  
  int getHttpPort() {
    return httpPort;
  }

  /**
   * Do the real constructor work here.  It's in a separate method
   * so we can call it again and "recycle" the object after calling
   * close().
   */
  synchronized void initialize() throws IOException, InterruptedException {

    LOG.info("Starting tasktracker with owner as " +
        aclsManager.getMROwner().getShortUserName());

    localFs = FileSystem.getLocal(fConf);
    // use configured nameserver & interface to get local hostname
    if (fConf.get(TT_HOST_NAME) != null) {
      this.localHostname = fConf.get(TT_HOST_NAME);
    }
    if (localHostname == null) {
      this.localHostname =
      DNS.getDefaultHost
      (fConf.get(TT_DNS_INTERFACE,"default"),
       fConf.get(TT_DNS_NAMESERVER,"default"));
    }
 
    // Check local disk, start async disk service, and clean up all 
    // local directories.
    checkLocalDirs(this.fConf.getLocalDirs());
    setAsyncDiskService(new MRAsyncDiskService(fConf));
    getAsyncDiskService().cleanupAllVolumes();

    // Clear out state tables
    this.tasks.clear();
    this.runningTasks = new LinkedHashMap<TaskAttemptID, TaskInProgress>();
    this.runningJobs = new TreeMap<JobID, RunningJob>();
    this.mapTotal = 0;
    this.reduceTotal = 0;
    this.acceptNewTasks = true;
    this.status = null;

    this.minSpaceStart = this.fConf.getLong(TT_LOCAL_DIR_MINSPACE_START, 0L);
    this.minSpaceKill = this.fConf.getLong(TT_LOCAL_DIR_MINSPACE_KILL, 0L);
    //tweak the probe sample size (make it a function of numCopiers)
    probe_sample_size = 
      this.fConf.getInt(TT_MAX_TASK_COMPLETION_EVENTS_TO_POLL, 500);
    
    // Set up TaskTracker instrumentation
    this.myInstrumentation = createInstrumentation(this, fConf);
    
    // bind address
    InetSocketAddress socAddr = NetUtils.createSocketAddr(
        fConf.get(TT_REPORT_ADDRESS, "127.0.0.1:0"));
    String bindAddress = socAddr.getHostName();
    int tmpPort = socAddr.getPort();
    
    this.jvmManager = new JvmManager(this);

    // RPC initialization
    int max = maxMapSlots > maxReduceSlots ?
                       maxMapSlots : maxReduceSlots;
    //set the num handlers to max*2 since canCommit may wait for the duration
    //of a heartbeat RPC
    this.taskReportServer = RPC.getServer(this.getClass(), this, bindAddress,
        tmpPort, 2 * max, false, this.fConf, this.jobTokenSecretManager);

    // Set service-level authorization security policy
    if (this.fConf.getBoolean(
        CommonConfigurationKeys.HADOOP_SECURITY_AUTHORIZATION, false)) {
      PolicyProvider policyProvider = 
        (PolicyProvider)(ReflectionUtils.newInstance(
            this.fConf.getClass(PolicyProvider.POLICY_PROVIDER_CONFIG, 
                MapReducePolicyProvider.class, PolicyProvider.class), 
            this.fConf));
      this.taskReportServer.refreshServiceAcl(fConf, policyProvider);
    }

    this.taskReportServer.start();

    // get the assigned address
    this.taskReportAddress = taskReportServer.getListenerAddress();
    this.fConf.set(TT_REPORT_ADDRESS,
        taskReportAddress.getHostName() + ":" + taskReportAddress.getPort());
    LOG.info("TaskTracker up at: " + this.taskReportAddress);

    this.taskTrackerName = "tracker_" + localHostname + ":" + taskReportAddress;
    LOG.info("Starting tracker " + taskTrackerName);

    Class<? extends TaskController> taskControllerClass = fConf.getClass(
        TT_TASK_CONTROLLER, DefaultTaskController.class, TaskController.class);
    taskController = (TaskController) ReflectionUtils.newInstance(
        taskControllerClass, fConf);


    // setup and create jobcache directory with appropriate permissions
    taskController.setup();

    // Initialize DistributedCache
    this.distributedCacheManager = 
        new TrackerDistributedCacheManager(this.fConf, taskController,
        asyncDiskService);
    this.distributedCacheManager.startCleanupThread();

    this.jobClient = (InterTrackerProtocol) 
    UserGroupInformation.getLoginUser().doAs(
        new PrivilegedExceptionAction<Object>() {
      public Object run() throws IOException {
        return RPC.waitForProxy(InterTrackerProtocol.class,
            InterTrackerProtocol.versionID, 
            jobTrackAddr, fConf);  
      }
    }); 
    this.justInited = true;
    this.running = true;    
    // start the thread that will fetch map task completion events
    this.mapEventsFetcher = new MapEventsFetcherThread();
    mapEventsFetcher.setDaemon(true);
    mapEventsFetcher.setName(
                             "Map-events fetcher for all reduce tasks " + "on " + 
                             taskTrackerName);
    mapEventsFetcher.start();

    Class<? extends ResourceCalculatorPlugin> clazz =
        fConf.getClass(TT_RESOURCE_CALCULATOR_PLUGIN,
            null, ResourceCalculatorPlugin.class);
    resourceCalculatorPlugin = ResourceCalculatorPlugin
            .getResourceCalculatorPlugin(clazz, fConf);
    LOG.info(" Using ResourceCalculatorPlugin : " + resourceCalculatorPlugin);
    initializeMemoryManagement();

    setIndexCache(new IndexCache(this.fConf));

    //clear old user logs
    taskLogCleanupThread.clearOldUserLogs(this.fConf);

    mapLauncher = new TaskLauncher(TaskType.MAP, maxMapSlots);
    reduceLauncher = new TaskLauncher(TaskType.REDUCE, maxReduceSlots);
    mapLauncher.start();
    reduceLauncher.start();

    // create a localizer instance
    setLocalizer(new Localizer(localFs, fConf.getLocalDirs(), taskController));

    //Start up node health checker service.
    if (shouldStartHealthMonitor(this.fConf)) {
      startHealthMonitor(this.fConf);
    }
    
    oobHeartbeatOnTaskCompletion = 
      fConf.getBoolean(TT_OUTOFBAND_HEARBEAT, false);
  }

  /**
   * Are ACLs for authorization checks enabled on the MR cluster ?
   */
  boolean areACLsEnabled() {
    return fConf.getBoolean(MRConfig.MR_ACLS_ENABLED, false);
  }

  public static Class<?>[] getInstrumentationClasses(Configuration conf) {
    return conf.getClasses(TT_INSTRUMENTATION, TaskTrackerMetricsInst.class);
  }

  public static void setInstrumentationClass(
    Configuration conf, Class<? extends TaskTrackerInstrumentation> t) {
    conf.setClass(TT_INSTRUMENTATION,
        t, TaskTrackerInstrumentation.class);
  }
  
  public static TaskTrackerInstrumentation createInstrumentation(
      TaskTracker tt, Configuration conf) {
    try {
      Class<?>[] instrumentationClasses = getInstrumentationClasses(conf);
      if (instrumentationClasses.length == 0) {
        LOG.error("Empty string given for " + TT_INSTRUMENTATION + 
            " property -- will use default instrumentation class instead");
        return new TaskTrackerMetricsInst(tt);
      } else if (instrumentationClasses.length == 1) {
        // Just one instrumentation class given; create it directly
        Class<?> cls = instrumentationClasses[0];
        java.lang.reflect.Constructor<?> c =
          cls.getConstructor(new Class[] {TaskTracker.class} );
        return (TaskTrackerInstrumentation) c.newInstance(tt);
      } else {
        // Multiple instrumentation classes given; use a composite object
        List<TaskTrackerInstrumentation> instrumentations =
          new ArrayList<TaskTrackerInstrumentation>();
        for (Class<?> cls: instrumentationClasses) {
          java.lang.reflect.Constructor<?> c =
            cls.getConstructor(new Class[] {TaskTracker.class} );
          TaskTrackerInstrumentation inst =
            (TaskTrackerInstrumentation) c.newInstance(tt);
          instrumentations.add(inst);
        }
        return new CompositeTaskTrackerInstrumentation(tt, instrumentations);
      }
    } catch(Exception e) {
      // Reflection can throw lots of exceptions -- handle them all by 
      // falling back on the default.
      LOG.error("Failed to initialize TaskTracker metrics", e);
      return new TaskTrackerMetricsInst(tt);
    }
  }

  /**
   * Removes all contents of temporary storage.  Called upon
   * startup, to remove any leftovers from previous run.
   * 
   * Use MRAsyncDiskService.moveAndDeleteAllVolumes instead.
   * @see org.apache.hadoop.mapreduce.util.MRAsyncDiskService#cleanupAllVolumes()
   */
  @Deprecated
  public void cleanupStorage() throws IOException {
    this.fConf.deleteLocalFiles();
  }

  // Object on wait which MapEventsFetcherThread is going to wait.
  private Object waitingOn = new Object();

  private class MapEventsFetcherThread extends Thread {

    private List <FetchStatus> reducesInShuffle() {
      List <FetchStatus> fList = new ArrayList<FetchStatus>();
      for (Map.Entry <JobID, RunningJob> item : runningJobs.entrySet()) {
        RunningJob rjob = item.getValue();
        JobID jobId = item.getKey();
        FetchStatus f;
        synchronized (rjob) {
          f = rjob.getFetchStatus();
          for (TaskInProgress tip : rjob.tasks) {
            Task task = tip.getTask();
            if (!task.isMapTask()) {
              if (((ReduceTask)task).getPhase() == 
                  TaskStatus.Phase.SHUFFLE) {
                if (rjob.getFetchStatus() == null) {
                  //this is a new job; we start fetching its map events
                  f = new FetchStatus(jobId, 
                                      ((ReduceTask)task).getNumMaps());
                  rjob.setFetchStatus(f);
                }
                f = rjob.getFetchStatus();
                fList.add(f);
                break; //no need to check any more tasks belonging to this
              }
            }
          }
        }
      }
      //at this point, we have information about for which of
      //the running jobs do we need to query the jobtracker for map 
      //outputs (actually map events).
      return fList;
    }
      
    @Override
    public void run() {
      LOG.info("Starting thread: " + this.getName());
        
      while (running) {
        try {
          List <FetchStatus> fList = null;
          synchronized (runningJobs) {
            while (((fList = reducesInShuffle()).size()) == 0) {
              try {
                runningJobs.wait();
              } catch (InterruptedException e) {
                LOG.info("Shutting down: " + this.getName());
                return;
              }
            }
          }
          // now fetch all the map task events for all the reduce tasks
          // possibly belonging to different jobs
          boolean fetchAgain = false; //flag signifying whether we want to fetch
                                      //immediately again.
          for (FetchStatus f : fList) {
            long currentTime = System.currentTimeMillis();
            try {
              //the method below will return true when we have not 
              //fetched all available events yet
              if (f.fetchMapCompletionEvents(currentTime)) {
                fetchAgain = true;
              }
            } catch (Exception e) {
              LOG.warn(
                       "Ignoring exception that fetch for map completion" +
                       " events threw for " + f.jobId + " threw: " +
                       StringUtils.stringifyException(e)); 
            }
            if (!running) {
              break;
            }
          }
          synchronized (waitingOn) {
            try {
              if (!fetchAgain) {
                waitingOn.wait(heartbeatInterval);
              }
            } catch (InterruptedException ie) {
              LOG.info("Shutting down: " + this.getName());
              return;
            }
          }
        } catch (Exception e) {
          LOG.info("Ignoring exception "  + e.getMessage());
        }
      }
    } 
  }

  private class FetchStatus {
    /** The next event ID that we will start querying the JobTracker from*/
    private IntWritable fromEventId;
    /** This is the cache of map events for a given job */ 
    private List<TaskCompletionEvent> allMapEvents;
    /** What jobid this fetchstatus object is for*/
    private JobID jobId;
    private long lastFetchTime;
    private boolean fetchAgain;
     
    public FetchStatus(JobID jobId, int numMaps) {
      this.fromEventId = new IntWritable(0);
      this.jobId = jobId;
      this.allMapEvents = new ArrayList<TaskCompletionEvent>(numMaps);
    }
      
    /**
     * Reset the events obtained so far.
     */
    public void reset() {
      // Note that the sync is first on fromEventId and then on allMapEvents
      synchronized (fromEventId) {
        synchronized (allMapEvents) {
          fromEventId.set(0); // set the new index for TCE
          allMapEvents.clear();
        }
      }
    }
    
    public TaskCompletionEvent[] getMapEvents(int fromId, int max) {
        
      TaskCompletionEvent[] mapEvents = 
        TaskCompletionEvent.EMPTY_ARRAY;
      boolean notifyFetcher = false; 
      synchronized (allMapEvents) {
        if (allMapEvents.size() > fromId) {
          int actualMax = Math.min(max, (allMapEvents.size() - fromId));
          List <TaskCompletionEvent> eventSublist = 
            allMapEvents.subList(fromId, actualMax + fromId);
          mapEvents = eventSublist.toArray(mapEvents);
        } else {
          // Notify Fetcher thread. 
          notifyFetcher = true;
        }
      }
      if (notifyFetcher) {
        synchronized (waitingOn) {
          waitingOn.notify();
        }
      }
      return mapEvents;
    }
      
    public boolean fetchMapCompletionEvents(long currTime) throws IOException {
      if (!fetchAgain && (currTime - lastFetchTime) < heartbeatInterval) {
        return false;
      }
      int currFromEventId = 0;
      synchronized (fromEventId) {
        currFromEventId = fromEventId.get();
        List <TaskCompletionEvent> recentMapEvents = 
          queryJobTracker(fromEventId, jobId, jobClient);
        synchronized (allMapEvents) {
          allMapEvents.addAll(recentMapEvents);
        }
        lastFetchTime = currTime;
        if (fromEventId.get() - currFromEventId >= probe_sample_size) {
          //return true when we have fetched the full payload, indicating
          //that we should fetch again immediately (there might be more to
          //fetch
          fetchAgain = true;
          return true;
        }
      }
      fetchAgain = false;
      return false;
    }
  }

  private static LocalDirAllocator lDirAlloc = 
                              new LocalDirAllocator(MRConfig.LOCAL_DIR);

  // intialize the job directory
  RunningJob localizeJob(TaskInProgress tip
                           ) throws IOException, InterruptedException {
    Task t = tip.getTask();
    JobID jobId = t.getJobID();
    RunningJob rjob = addTaskToJob(jobId, tip);

    // Initialize the user directories if needed.
    getLocalizer().initializeUserDirs(t.getUser());

    synchronized (rjob) {
      if (!rjob.localized) {
       
        JobConf localJobConf = localizeJobFiles(t, rjob);
        // initialize job log directory
        initializeJobLogDir(jobId, localJobConf);

        // Now initialize the job via task-controller so as to set
        // ownership/permissions of jars, job-work-dir. Note that initializeJob
        // should be the last call after every other directory/file to be
        // directly under the job directory is created.
        JobInitializationContext context = new JobInitializationContext();
        context.jobid = jobId;
        context.user = t.getUser();
        context.workDir = new File(localJobConf.get(JOB_LOCAL_DIR));
        taskController.initializeJob(context);

        rjob.jobConf = localJobConf;
        rjob.keepJobFiles = ((localJobConf.getKeepTaskFilesPattern() != null) ||
                             localJobConf.getKeepFailedTaskFiles());
        rjob.localized = true;
      }
    }
    return rjob;
  }

  private FileSystem getFS(final Path filePath, JobID jobId, 
      final Configuration conf) throws IOException, InterruptedException {
    RunningJob rJob = runningJobs.get(jobId);
    FileSystem userFs = 
      rJob.ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
        public FileSystem run() throws IOException {
          return filePath.getFileSystem(conf);
      }});
    return userFs;
  }
  
  /**
   * Localize the job on this tasktracker. Specifically
   * <ul>
   * <li>Cleanup and create job directories on all disks</li>
   * <li>Download the job config file job.xml from the FS</li>
   * <li>Create the job work directory and set {@link TaskTracker#JOB_LOCAL_DIR}
   * in the configuration.
   * <li>Download the job jar file job.jar from the FS, unjar it and set jar
   * file in the configuration.</li>
   * </ul>
   * 
   * @param t task whose job has to be localized on this TT
   * @return the modified job configuration to be used for all the tasks of this
   *         job as a starting point.
   * @throws IOException
   */
  JobConf localizeJobFiles(Task t, RunningJob rjob)
      throws IOException, InterruptedException {
    JobID jobId = t.getJobID();
    String userName = t.getUser();

    // Initialize the job directories
    FileSystem localFs = FileSystem.getLocal(fConf);
    getLocalizer().initializeJobDirs(userName, jobId);
    // save local copy of JobToken file
    String localJobTokenFile = localizeJobTokenFile(t.getUser(), jobId);
    rjob.ugi = UserGroupInformation.createRemoteUser(t.getUser());

    Credentials ts = TokenCache.loadTokens(localJobTokenFile, fConf);
    Token<JobTokenIdentifier> jt = TokenCache.getJobToken(ts);
    if (jt != null) { //could be null in the case of some unit tests
      getJobTokenSecretManager().addTokenForJob(jobId.toString(), jt);
    }
    for (Token<? extends TokenIdentifier> token : ts.getAllTokens()) {
      rjob.ugi.addToken(token);
    }
    // Download the job.xml for this job from the system FS
    Path localJobFile =
        localizeJobConfFile(new Path(t.getJobFile()), userName, jobId);

    JobConf localJobConf = new JobConf(localJobFile);
    //WE WILL TRUST THE USERNAME THAT WE GOT FROM THE JOBTRACKER
    //AS PART OF THE TASK OBJECT
    localJobConf.setUser(userName);
    
    // set the location of the token file into jobConf to transfer 
    // the name to TaskRunner
    localJobConf.set(TokenCache.JOB_TOKENS_FILENAME,
        localJobTokenFile);
    

    // create the 'job-work' directory: job-specific shared directory for use as
    // scratch space by all tasks of the same job running on this TaskTracker. 
    Path workDir =
        lDirAlloc.getLocalPathForWrite(getJobWorkDir(userName, jobId
            .toString()), fConf);
    if (!localFs.mkdirs(workDir)) {
      throw new IOException("Mkdirs failed to create " 
                  + workDir.toString());
    }
    System.setProperty(JOB_LOCAL_DIR, workDir.toUri().getPath());
    localJobConf.set(JOB_LOCAL_DIR, workDir.toUri().getPath());
    // Download the job.jar for this job from the system FS
    localizeJobJarFile(userName, jobId, localFs, localJobConf);
    
    return localJobConf;
  }

  // Create job userlog dir.
  // Create job acls file in job log dir, if needed.
  void initializeJobLogDir(JobID jobId, JobConf localJobConf)
      throws IOException {
    // remove it from tasklog cleanup thread first,
    // it might be added there because of tasktracker reinit or restart
    taskLogCleanupThread.unmarkJobFromLogDeletion(jobId);
    localizer.initializeJobLogDir(jobId);

    if (areACLsEnabled()) {
      // Create job-acls.xml file in job userlog dir and write the needed
      // info for authorization of users for viewing task logs of this job.
      writeJobACLs(localJobConf, TaskLog.getJobDir(jobId));
    }
  }

  /**
   *  Creates job-acls.xml under the given directory logDir and writes
   *  job-view-acl, queue-admins-acl, jobOwner name and queue name into this
   *  file.
   *  queue name is the queue to which the job was submitted to.
   *  queue-admins-acl is the queue admins ACL of the queue to which this
   *  job was submitted to.
   * @param conf   job configuration
   * @param logDir job userlog dir
   * @throws IOException
   */
  private static void writeJobACLs(JobConf conf, File logDir)
      throws IOException {
    File aclFile = new File(logDir, jobACLsFile);
    JobConf aclConf = new JobConf(false);

    // set the job view acl in aclConf
    String jobViewACL = conf.get(MRJobConfig.JOB_ACL_VIEW_JOB, " ");
    aclConf.set(MRJobConfig.JOB_ACL_VIEW_JOB, jobViewACL);

    // set the job queue name in aclConf
    String queue = conf.getQueueName();
    aclConf.setQueueName(queue);

    // set the queue admins acl in aclConf
    String qACLName = toFullPropertyName(queue,
        QueueACL.ADMINISTER_JOBS.getAclName());
    String queueAdminsACL = conf.get(qACLName, " ");
    aclConf.set(qACLName, queueAdminsACL);

    // set jobOwner as user.name in aclConf
    String jobOwner = conf.getUser();
    aclConf.set("user.name", jobOwner);

    FileOutputStream out;
    try {
      out = SecureIOUtils.createForWrite(aclFile, 0600);
    } catch (SecureIOUtils.AlreadyExistsException aee) {
      LOG.warn("Job ACL file already exists at " + aclFile, aee);
      return;
    }
    try {
      aclConf.writeXml(out);
    } finally {
      out.close();
    }
  }

  /**
   * Download the job configuration file from the FS.
   * 
   * @param t Task whose job file has to be downloaded
   * @param jobId jobid of the task
   * @return the local file system path of the downloaded file.
   * @throws IOException
   */
  private Path localizeJobConfFile(Path jobFile, String user, JobID jobId)
      throws IOException, InterruptedException {
    final JobConf conf = new JobConf(getJobConf());
    FileSystem userFs = getFS(jobFile, jobId, conf);
    // Get sizes of JobFile
    // sizes are -1 if they are not present.
    FileStatus status = null;
    long jobFileSize = -1;
    try {
      status = userFs.getFileStatus(jobFile);
      jobFileSize = status.getLen();
    } catch(FileNotFoundException fe) {
      jobFileSize = -1;
    }

    Path localJobFile =
        lDirAlloc.getLocalPathForWrite(getLocalJobConfFile(user, jobId.toString()),
            jobFileSize, fConf);

    // Download job.xml
    userFs.copyToLocalFile(jobFile, localJobFile);
    return localJobFile;
  }

  /**
   * Download the job jar file from FS to the local file system and unjar it.
   * Set the local jar file in the passed configuration.
   * 
   * @param jobId
   * @param localFs
   * @param localJobConf
   * @throws IOException
   */
  private void localizeJobJarFile(String user, JobID jobId, FileSystem localFs,
      JobConf localJobConf)
      throws IOException, InterruptedException {
    // copy Jar file to the local FS and unjar it.
    String jarFile = localJobConf.getJar();
    FileStatus status = null;
    long jarFileSize = -1;
    if (jarFile != null) {
      Path jarFilePath = new Path(jarFile);
      FileSystem fs = getFS(jarFilePath, jobId, localJobConf);
      try {
        status = fs.getFileStatus(jarFilePath);
        jarFileSize = status.getLen();
      } catch (FileNotFoundException fe) {
        jarFileSize = -1;
      }
      // Here we check for five times the size of jarFileSize to accommodate for
      // unjarring the jar file in the jars directory
      Path localJarFile =
          lDirAlloc.getLocalPathForWrite(
              getJobJarFile(user, jobId.toString()), 5 * jarFileSize, fConf);

      // Download job.jar
      fs.copyToLocalFile(jarFilePath, localJarFile);

      localJobConf.setJar(localJarFile.toString());

      // Un-jar the parts of the job.jar that need to be added to the classpath
      RunJar.unJar(
        new File(localJarFile.toString()),
        new File(localJarFile.getParent().toString()),
        localJobConf.getJarUnpackPattern());
    }
  }

  protected void launchTaskForJob(TaskInProgress tip, JobConf jobConf,
      UserGroupInformation ugi) throws IOException {
    synchronized (tip) {
      tip.setJobConf(jobConf);
      tip.setUGI(ugi);
      tip.launchTask();
    }
  }
    
  public synchronized void shutdown() throws IOException {
    shuttingDown = true;
    close();
    if (this.server != null) {
      try {
        LOG.info("Shutting down StatusHttpServer");
        this.server.stop();
      } catch (Exception e) {
        LOG.warn("Exception shutting down TaskTracker", e);
      }
    }
  }
  /**
   * Close down the TaskTracker and all its components.  We must also shutdown
   * any running tasks or threads, and cleanup disk space.  A new TaskTracker
   * within the same process space might be restarted, so everything must be
   * clean.
   */
  public synchronized void close() throws IOException {
    //
    // Kill running tasks.  Do this in a 2nd vector, called 'tasksToClose',
    // because calling jobHasFinished() may result in an edit to 'tasks'.
    //
    TreeMap<TaskAttemptID, TaskInProgress> tasksToClose =
      new TreeMap<TaskAttemptID, TaskInProgress>();
    tasksToClose.putAll(tasks);
    for (TaskInProgress tip : tasksToClose.values()) {
      tip.jobHasFinished(false);
    }

    this.running = false;

    if (asyncDiskService != null) {
      // Clear local storage
      asyncDiskService.cleanupAllVolumes();
      
      // Shutdown all async deletion threads with up to 10 seconds of delay
      asyncDiskService.shutdown();
      try {
        if (!asyncDiskService.awaitTermination(10000)) {
          asyncDiskService.shutdownNow();
          asyncDiskService = null;
        }
      } catch (InterruptedException e) {
        asyncDiskService.shutdownNow();
        asyncDiskService = null;
      }
    }
    
    // Shutdown the fetcher thread
    this.mapEventsFetcher.interrupt();
    
    //stop the launchers
    this.mapLauncher.interrupt();
    this.reduceLauncher.interrupt();
    
    this.distributedCacheManager.stopCleanupThread();
    jvmManager.stop();
    
    // shutdown RPC connections
    RPC.stopProxy(jobClient);

    // wait for the fetcher thread to exit
    for (boolean done = false; !done; ) {
      try {
        this.mapEventsFetcher.join();
        done = true;
      } catch (InterruptedException e) {
      }
    }
    
    if (taskReportServer != null) {
      taskReportServer.stop();
      taskReportServer = null;
    }
    if (healthChecker != null) {
      //stop node health checker service
      healthChecker.stop();
      healthChecker = null;
    }
  }

  /**
   * For testing
   */
  TaskTracker() {
    server = null;
  }

  void setConf(JobConf conf) {
    fConf = conf;
  }

  /**
   * Start with the local machine name, and the default JobTracker
   */
  public TaskTracker(JobConf conf) throws IOException, InterruptedException {
    fConf = conf;
    maxMapSlots = conf.getInt(TT_MAP_SLOTS, 2);
    maxReduceSlots = conf.getInt(TT_REDUCE_SLOTS, 2);
    aclsManager = new ACLsManager(fConf, new JobACLsManager(fConf), null);
    this.jobTrackAddr = JobTracker.getAddress(conf);
    InetSocketAddress infoSocAddr = NetUtils.createSocketAddr(
        conf.get(TT_HTTP_ADDRESS, "0.0.0.0:50060"));
    String httpBindAddress = infoSocAddr.getHostName();
    int httpPort = infoSocAddr.getPort();
    this.server = new HttpServer("task", httpBindAddress, httpPort,
        httpPort == 0, conf, aclsManager.getAdminsAcl());
    workerThreads = conf.getInt(TT_HTTP_THREADS, 40);
    this.shuffleServerMetrics = new ShuffleServerMetrics(conf);
    server.setThreads(1, workerThreads);
    // let the jsp pages get to the task tracker, config, and other relevant
    // objects
    FileSystem local = FileSystem.getLocal(conf);
    this.localDirAllocator = new LocalDirAllocator(MRConfig.LOCAL_DIR);
    server.setAttribute("task.tracker", this);
    server.setAttribute("local.file.system", local);
    server.setAttribute("conf", conf);
    server.setAttribute("log", LOG);
    server.setAttribute("localDirAllocator", localDirAllocator);
    server.setAttribute("shuffleServerMetrics", shuffleServerMetrics);
    String exceptionStackRegex = conf.get(JTConfig.SHUFFLE_EXCEPTION_STACK_REGEX);
    String exceptionMsgRegex = conf.get(JTConfig.SHUFFLE_EXCEPTION_MSG_REGEX);
    server.setAttribute("exceptionStackRegex", exceptionStackRegex);
    server.setAttribute("exceptionMsgRegex", exceptionMsgRegex);
    server.addInternalServlet("mapOutput", "/mapOutput", MapOutputServlet.class);
    server.addServlet("taskLog", "/tasklog", TaskLogServlet.class);
    server.start();
    this.httpPort = server.getPort();
    checkJettyPort(httpPort);
    // create task log cleanup thread
    setTaskLogCleanupThread(new UserLogCleaner(fConf));

    UserGroupInformation.setConfiguration(fConf);
    SecurityUtil.login(fConf, TTConfig.TT_KEYTAB_FILE, TTConfig.TT_USER_NAME);

    initialize();
  }

  private void checkJettyPort(int port) throws IOException { 
    //See HADOOP-4744
    if (port < 0) {
      shuttingDown = true;
      throw new IOException("Jetty problem. Jetty didn't bind to a " +
      		"valid port");
    }
  }
  
  private void startCleanupThreads() throws IOException {
    taskCleanupThread.setDaemon(true);
    taskCleanupThread.start();
    directoryCleanupThread = new CleanupQueue();
    // start tasklog cleanup thread
    taskLogCleanupThread.setDaemon(true);
    taskLogCleanupThread.start();
  }

  // only used by tests
  void setCleanupThread(CleanupQueue c) {
    directoryCleanupThread = c;
  }
  
  CleanupQueue getCleanupThread() {
    return directoryCleanupThread;
  }

  UserLogCleaner getTaskLogCleanupThread() {
    return this.taskLogCleanupThread;
  }

  void setTaskLogCleanupThread(UserLogCleaner t) {
    this.taskLogCleanupThread = t;
  }

  void setIndexCache(IndexCache cache) {
    this.indexCache = cache;
  }
  
  /**
   * The connection to the JobTracker, used by the TaskRunner 
   * for locating remote files.
   */
  public InterTrackerProtocol getJobClient() {
    return jobClient;
  }
        
  /** Return the port at which the tasktracker bound to */
  public synchronized InetSocketAddress getTaskTrackerReportAddress() {
    return taskReportAddress;
  }
    
  /** Queries the job tracker for a set of outputs ready to be copied
   * @param fromEventId the first event ID we want to start from, this is
   * modified by the call to this method
   * @param jobClient the job tracker
   * @return a set of locations to copy outputs from
   * @throws IOException
   */  
  private List<TaskCompletionEvent> queryJobTracker(IntWritable fromEventId,
                                                    JobID jobId,
                                                    InterTrackerProtocol jobClient)
    throws IOException {

    TaskCompletionEvent t[] = jobClient.getTaskCompletionEvents(
                                                                jobId,
                                                                fromEventId.get(),
                                                                probe_sample_size);
    //we are interested in map task completion events only. So store
    //only those
    List <TaskCompletionEvent> recentMapEvents = 
      new ArrayList<TaskCompletionEvent>();
    for (int i = 0; i < t.length; i++) {
      if (t[i].isMapTask()) {
        recentMapEvents.add(t[i]);
      }
    }
    fromEventId.set(fromEventId.get() + t.length);
    return recentMapEvents;
  }

  /**
   * Main service loop.  Will stay in this loop forever.
   */
  State offerService() throws Exception {
    long lastHeartbeat = 0;

    while (running && !shuttingDown) {
      try {
        long now = System.currentTimeMillis();

        long waitTime = heartbeatInterval - (now - lastHeartbeat);
        if (waitTime > 0) {
          // sleeps for the wait time or
          // until there are empty slots to schedule tasks
          synchronized (finishedCount) {
            if (finishedCount.get() == 0) {
              finishedCount.wait(waitTime);
            }
            finishedCount.set(0);
          }
        }

        // If the TaskTracker is just starting up:
        // 1. Verify the buildVersion
        // 2. Get the system directory & filesystem
        if(justInited) {
          String jobTrackerBV = jobClient.getBuildVersion();
          if(!VersionInfo.getBuildVersion().equals(jobTrackerBV)) {
            String msg = "Shutting down. Incompatible buildVersion." +
            "\nJobTracker's: " + jobTrackerBV + 
            "\nTaskTracker's: "+ VersionInfo.getBuildVersion();
            LOG.error(msg);
            try {
              jobClient.reportTaskTrackerError(taskTrackerName, null, msg);
            } catch(Exception e ) {
              LOG.info("Problem reporting to jobtracker: " + e);
            }
            return State.DENIED;
          }
          
          String dir = jobClient.getSystemDir();
          if (dir == null) {
            throw new IOException("Failed to get system directory");
          }
          systemDirectory = new Path(dir);
          systemFS = systemDirectory.getFileSystem(fConf);
        }
        
        // Send the heartbeat and process the jobtracker's directives
        HeartbeatResponse heartbeatResponse = transmitHeartBeat(now);

        // Note the time when the heartbeat returned, use this to decide when to send the
        // next heartbeat   
        lastHeartbeat = System.currentTimeMillis();
        
        TaskTrackerAction[] actions = heartbeatResponse.getActions();
        if(LOG.isDebugEnabled()) {
          LOG.debug("Got heartbeatResponse from JobTracker with responseId: " + 
                    heartbeatResponse.getResponseId() + " and " + 
                    ((actions != null) ? actions.length : 0) + " actions");
        }
        if (reinitTaskTracker(actions)) {
          return State.STALE;
        }
            
        // resetting heartbeat interval from the response.
        heartbeatInterval = heartbeatResponse.getHeartbeatInterval();
        justStarted = false;
        justInited = false;
        if (actions != null){ 
          for(TaskTrackerAction action: actions) {
            if (action instanceof LaunchTaskAction) {
              addToTaskQueue((LaunchTaskAction)action);
            } else if (action instanceof CommitTaskAction) {
              CommitTaskAction commitAction = (CommitTaskAction)action;
              if (!commitResponses.contains(commitAction.getTaskID())) {
                LOG.info("Received commit task action for " + 
                          commitAction.getTaskID());
                commitResponses.add(commitAction.getTaskID());
              }
            } else {
              tasksToCleanup.put(action);
            }
          }
        }
        markUnresponsiveTasks();
        killOverflowingTasks();
            
        //we've cleaned up, resume normal operation
        if (!acceptNewTasks && isIdle()) {
          acceptNewTasks=true;
        }
        //The check below may not be required every iteration but we are 
        //erring on the side of caution here. We have seen many cases where
        //the call to jetty's getLocalPort() returns different values at 
        //different times. Being a real paranoid here.
        checkJettyPort(server.getPort());
      } catch (InterruptedException ie) {
        LOG.info("Interrupted. Closing down.");
        return State.INTERRUPTED;
      } catch (DiskErrorException de) {
        String msg = "Exiting task tracker for disk error:\n" +
          StringUtils.stringifyException(de);
        LOG.error(msg);
        synchronized (this) {
          jobClient.reportTaskTrackerError(taskTrackerName, 
                                           "DiskErrorException", msg);
        }
        return State.STALE;
      } catch (RemoteException re) {
        String reClass = re.getClassName();
        if (DisallowedTaskTrackerException.class.getName().equals(reClass)) {
          LOG.info("Tasktracker disallowed by JobTracker.");
          return State.DENIED;
        }
      } catch (Exception except) {
        String msg = "Caught exception: " + 
          StringUtils.stringifyException(except);
        LOG.error(msg);
      }
    }

    return State.NORMAL;
  }

  private long previousUpdate = 0;

  /**
   * Build and transmit the heart beat to the JobTracker
   * @param now current time
   * @return false if the tracker was unknown
   * @throws IOException
   */
  HeartbeatResponse transmitHeartBeat(long now) throws IOException {
    // Send Counters in the status once every COUNTER_UPDATE_INTERVAL
    boolean sendAllCounters;
    if (now > (previousUpdate + COUNTER_UPDATE_INTERVAL)) {
      sendAllCounters = true;
      previousUpdate = now;
    }
    else {
      sendAllCounters = false;
    }

    // 
    // Check if the last heartbeat got through... 
    // if so then build the heartbeat information for the JobTracker;
    // else resend the previous status information.
    //
    if (status == null) {
      synchronized (this) {
        status = new TaskTrackerStatus(taskTrackerName, localHostname, 
                                       httpPort, 
                                       cloneAndResetRunningTaskStatuses(
                                         sendAllCounters),
                                       failures, 
                                       maxMapSlots,
                                       maxReduceSlots); 
      }
    } else {
      LOG.info("Resending 'status' to '" + jobTrackAddr.getHostName() +
               "' with reponseId '" + heartbeatResponseId);
    }
      
    //
    // Check if we should ask for a new Task
    //
    boolean askForNewTask;
    long localMinSpaceStart;
    synchronized (this) {
      askForNewTask = 
        ((status.countOccupiedMapSlots() < maxMapSlots || 
          status.countOccupiedReduceSlots() < maxReduceSlots) && 
         acceptNewTasks); 
      localMinSpaceStart = minSpaceStart;
    }
    if (askForNewTask) {
      checkLocalDirs(fConf.getLocalDirs());
      askForNewTask = enoughFreeSpace(localMinSpaceStart);
      long freeDiskSpace = getFreeSpace();
      long totVmem = getTotalVirtualMemoryOnTT();
      long totPmem = getTotalPhysicalMemoryOnTT();
      long availableVmem = getAvailableVirtualMemoryOnTT();
      long availablePmem = getAvailablePhysicalMemoryOnTT();
      long cumuCpuTime = getCumulativeCpuTimeOnTT();
      long cpuFreq = getCpuFrequencyOnTT();
      int numCpu = getNumProcessorsOnTT();
      float cpuUsage = getCpuUsageOnTT();

      status.getResourceStatus().setAvailableSpace(freeDiskSpace);
      status.getResourceStatus().setTotalVirtualMemory(totVmem);
      status.getResourceStatus().setTotalPhysicalMemory(totPmem);
      status.getResourceStatus().setMapSlotMemorySizeOnTT(
          mapSlotMemorySizeOnTT);
      status.getResourceStatus().setReduceSlotMemorySizeOnTT(
          reduceSlotSizeMemoryOnTT);
      status.getResourceStatus().setAvailableVirtualMemory(availableVmem); 
      status.getResourceStatus().setAvailablePhysicalMemory(availablePmem);
      status.getResourceStatus().setCumulativeCpuTime(cumuCpuTime);
      status.getResourceStatus().setCpuFrequency(cpuFreq);
      status.getResourceStatus().setNumProcessors(numCpu);
      status.getResourceStatus().setCpuUsage(cpuUsage);
    }
    //add node health information
    
    TaskTrackerHealthStatus healthStatus = status.getHealthStatus();
    synchronized (this) {
      if (healthChecker != null) {
        healthChecker.setHealthStatus(healthStatus);
      } else {
        healthStatus.setNodeHealthy(true);
        healthStatus.setLastReported(0L);
        healthStatus.setHealthReport("");
      }
    }
    //
    // Xmit the heartbeat
    //
    HeartbeatResponse heartbeatResponse = jobClient.heartbeat(status, 
                                                              justStarted,
                                                              justInited,
                                                              askForNewTask, 
                                                              heartbeatResponseId);
      
    //
    // The heartbeat got through successfully!
    //
    heartbeatResponseId = heartbeatResponse.getResponseId();
      
    synchronized (this) {
      for (TaskStatus taskStatus : status.getTaskReports()) {
        if (taskStatus.getRunState() != TaskStatus.State.RUNNING &&
            taskStatus.getRunState() != TaskStatus.State.UNASSIGNED &&
            taskStatus.getRunState() != TaskStatus.State.COMMIT_PENDING &&
            !taskStatus.inTaskCleanupPhase()) {
          if (taskStatus.getIsMap()) {
            mapTotal--;
          } else {
            reduceTotal--;
          }
          try {
            myInstrumentation.completeTask(taskStatus.getTaskID());
          } catch (MetricsException me) {
            LOG.warn("Caught: " + StringUtils.stringifyException(me));
          }
          runningTasks.remove(taskStatus.getTaskID());
        }
      }
      
      // Clear transient status information which should only
      // be sent once to the JobTracker
      for (TaskInProgress tip: runningTasks.values()) {
        tip.getStatus().clearStatus();
      }
    }

    // Force a rebuild of 'status' on the next iteration
    status = null;                                

    return heartbeatResponse;
  }

  /**
   * Return the total virtual memory available on this TaskTracker.
   * @return total size of virtual memory.
   */
  long getTotalVirtualMemoryOnTT() {
    return totalVirtualMemoryOnTT;
  }

  /**
   * Return the total physical memory available on this TaskTracker.
   * @return total size of physical memory.
   */
  long getTotalPhysicalMemoryOnTT() {
    return totalPhysicalMemoryOnTT;
  }

  /**
   * Return the free virtual memory available on this TaskTracker.
   * @return total size of free virtual memory.
   */
  long getAvailableVirtualMemoryOnTT() {
    long availableVirtualMemoryOnTT = TaskTrackerStatus.UNAVAILABLE;
    if (resourceCalculatorPlugin != null) {
      availableVirtualMemoryOnTT =
              resourceCalculatorPlugin.getAvailableVirtualMemorySize();
    }
    return availableVirtualMemoryOnTT;
  }

  /**
   * Return the free physical memory available on this TaskTracker.
   * @return total size of free physical memory in bytes
   */
  long getAvailablePhysicalMemoryOnTT() {
    long availablePhysicalMemoryOnTT = TaskTrackerStatus.UNAVAILABLE;
    if (resourceCalculatorPlugin != null) {
      availablePhysicalMemoryOnTT =
              resourceCalculatorPlugin.getAvailablePhysicalMemorySize();
    }
    return availablePhysicalMemoryOnTT;
  }

  /**
   * Return the cumulative CPU used time on this TaskTracker since system is on
   * @return cumulative CPU used time in millisecond
   */
  long getCumulativeCpuTimeOnTT() {
    long cumulativeCpuTime = TaskTrackerStatus.UNAVAILABLE;
    if (resourceCalculatorPlugin != null) {
      cumulativeCpuTime = resourceCalculatorPlugin.getCumulativeCpuTime();
    }
    return cumulativeCpuTime;
  }

  /**
   * Return the number of Processors on this TaskTracker
   * @return number of processors
   */
  int getNumProcessorsOnTT() {
    int numProcessors = TaskTrackerStatus.UNAVAILABLE;
    if (resourceCalculatorPlugin != null) {
      numProcessors = resourceCalculatorPlugin.getNumProcessors();
    }
    return numProcessors;
  }

  /**
   * Return the CPU frequency of this TaskTracker
   * @return CPU frequency in kHz
   */
  long getCpuFrequencyOnTT() {
    long cpuFrequency = TaskTrackerStatus.UNAVAILABLE;
    if (resourceCalculatorPlugin != null) {
      cpuFrequency = resourceCalculatorPlugin.getCpuFrequency();
    }
    return cpuFrequency;
  }

  /**
   * Return the CPU usage in % of this TaskTracker
   * @return CPU usage in %
   */
  float getCpuUsageOnTT() {
    float cpuUsage = TaskTrackerStatus.UNAVAILABLE;
    if (resourceCalculatorPlugin != null) {
      cpuUsage = resourceCalculatorPlugin.getCpuUsage();
    }
    return cpuUsage;
  }
  
  long getTotalMemoryAllottedForTasksOnTT() {
    return totalMemoryAllottedForTasks;
  }

  /**
   * @return The amount of physical memory that will not be used for running
   *         tasks in bytes. Returns JobConf.DISABLED_MEMORY_LIMIT if it is not
   *         
