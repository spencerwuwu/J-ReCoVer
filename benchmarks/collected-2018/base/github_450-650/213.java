// https://searchcode.com/api/result/101299760/

/*
 * Copyright 2014 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.mapreduce.v2.app.launcher;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.ShuffleHandler;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptContainerLaunchedEvent;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptDiagnosticsUpdateEvent;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptEvent;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptEventType;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainersRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainersResponse;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainersRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainersResponse;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.client.api.impl.ContainerManagementProtocolProxy;
import org.apache.hadoop.yarn.client.api.impl.ContainerManagementProtocolProxy.ContainerManagementProtocolProxyData;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.util.RackResolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * This class is modified from ContainerLauncherImpl.
 * When launch event is processed, record the location and
 * generate nodes file. Max mem is also reset for mapper in
 * map-collective job.
 *
 * The following is the original document.
 *
 * This class is responsible for launching of containers.
 */
public class MapCollectiveContainerLauncherImpl extends AbstractService
  implements ContainerLauncher {

  static final Log LOG = LogFactory.getLog(ContainerLauncherImpl.class);

  private ConcurrentHashMap<ContainerId, Container> containers = new ConcurrentHashMap<ContainerId, Container>();
  private final AppContext context;
  protected ThreadPoolExecutor launcherPool;
  protected static final int INITIAL_POOL_SIZE = 10;
  private int limitOnPoolSize;
  private Thread eventHandlingThread;
  protected BlockingQueue<ContainerLauncherEvent> eventQueue = new LinkedBlockingQueue<ContainerLauncherEvent>();
  private final AtomicBoolean stopped;
  private ContainerManagementProtocolProxy cmProxy;
  
  // The total number of map tasks should be launched
  private int numMapTasks;
  // Record task locations, and prepare for generating node list
  private Map<Integer, String> taskLocations;
  // Print flag
  private boolean isPrinted;
  
  private Container getContainer(ContainerLauncherEvent event) {
    ContainerId id = event.getContainerID();
    Container c = containers.get(id);
    if (c == null) {
      c = new Container(event.getTaskAttemptID(), event.getContainerID(),
        event.getContainerMgrAddress());
      Container old = containers.putIfAbsent(id, c);
      if (old != null) {
        c = old;
      }
    }
    return c;
  }

  private void removeContainerIfDone(ContainerId id) {
    Container c = containers.get(id);
    if (c != null && c.isCompletelyDone()) {
      containers.remove(id);
    }
  }

  private static enum ContainerState {
    PREP, FAILED, RUNNING, DONE, KILLED_BEFORE_LAUNCH
  }

  private class Container {
    private ContainerState state;
    // store enough information to be able to cleanup the container
    private TaskAttemptId taskAttemptID;
    private ContainerId containerID;
    final private String containerMgrAddress;

    public Container(TaskAttemptId taId, ContainerId containerID,
      String containerMgrAddress) {
      this.state = ContainerState.PREP;
      this.taskAttemptID = taId;
      this.containerMgrAddress = containerMgrAddress;
      this.containerID = containerID;
    }

    public synchronized boolean isCompletelyDone() {
      return state == ContainerState.DONE || state == ContainerState.FAILED;
    }

    @SuppressWarnings("unchecked")
    public synchronized void launch(ContainerRemoteLaunchEvent event) {
      LOG.info("Launching " + taskAttemptID);
      if (this.state == ContainerState.KILLED_BEFORE_LAUNCH) {
        state = ContainerState.DONE;
        sendContainerLaunchFailedMsg(taskAttemptID,
          "Container was killed before it was launched");
        return;
      }

      ContainerManagementProtocolProxyData proxy = null;
      try {

        proxy = getCMProxy(containerMgrAddress, containerID);

        // Construct the actual Container
        ContainerLaunchContext containerLaunchContext = event
          .getContainerLaunchContext();

        // Now launch the actual container
        StartContainerRequest startRequest = StartContainerRequest.newInstance(
          containerLaunchContext, event.getContainerToken());
        List<StartContainerRequest> list = new ArrayList<StartContainerRequest>();
        list.add(startRequest);
        StartContainersRequest requestList = StartContainersRequest
          .newInstance(list);
        StartContainersResponse response = proxy
          .getContainerManagementProtocol().startContainers(requestList);
        if (response.getFailedRequests() != null
          && response.getFailedRequests().containsKey(containerID)) {
          throw response.getFailedRequests().get(containerID).deSerialize();
        }
        ByteBuffer portInfo = response.getAllServicesMetaData().get(
          ShuffleHandler.MAPREDUCE_SHUFFLE_SERVICEID);
        int port = -1;
        if (portInfo != null) {
          port = ShuffleHandler.deserializeMetaData(portInfo);
        }
        LOG.info("Shuffle port returned by ContainerManager for "
          + taskAttemptID + " : " + port);

        if (port < 0) {
          this.state = ContainerState.FAILED;
          throw new IllegalStateException("Invalid shuffle port number " + port
            + " returned for " + taskAttemptID);
        }

        // after launching, send launched event to task attempt to move
        // it from ASSIGNED to RUNNING state
        context.getEventHandler().handle(
          new TaskAttemptContainerLaunchedEvent(taskAttemptID, port));
        this.state = ContainerState.RUNNING;
      } catch (Throwable t) {
        String message = "Container launch failed for " + containerID + " : "
          + StringUtils.stringifyException(t);
        this.state = ContainerState.FAILED;
        sendContainerLaunchFailedMsg(taskAttemptID, message);
      } finally {
        if (proxy != null) {
          cmProxy.mayBeCloseProxy(proxy);
        }
      }
    }

    @SuppressWarnings("unchecked")
    public synchronized void kill() {

      if (this.state == ContainerState.PREP) {
        this.state = ContainerState.KILLED_BEFORE_LAUNCH;
      } else if (!isCompletelyDone()) {
        LOG.info("KILLING " + taskAttemptID);

        ContainerManagementProtocolProxyData proxy = null;
        try {
          proxy = getCMProxy(this.containerMgrAddress, this.containerID);

          // kill the remote container if already launched
          List<ContainerId> ids = new ArrayList<ContainerId>();
          ids.add(this.containerID);
          StopContainersRequest request = StopContainersRequest
            .newInstance(ids);
          StopContainersResponse response = proxy
            .getContainerManagementProtocol().stopContainers(request);
          if (response.getFailedRequests() != null
            && response.getFailedRequests().containsKey(this.containerID)) {
            throw response.getFailedRequests().get(this.containerID)
              .deSerialize();
          }
        } catch (Throwable t) {
          // ignore the cleanup failure
          String message = "cleanup failed for container " + this.containerID
            + " : " + StringUtils.stringifyException(t);
          context.getEventHandler().handle(
            new TaskAttemptDiagnosticsUpdateEvent(this.taskAttemptID, message));
          LOG.warn(message);
        } finally {
          if (proxy != null) {
            cmProxy.mayBeCloseProxy(proxy);
          }
        }
        this.state = ContainerState.DONE;
      }
      // after killing, send killed event to task attempt
      context.getEventHandler().handle(
        new TaskAttemptEvent(this.taskAttemptID,
          TaskAttemptEventType.TA_CONTAINER_CLEANED));
    }
  }

  public MapCollectiveContainerLauncherImpl(AppContext context) {
    super(MapCollectiveContainerLauncherImpl.class.getName());
    this.context = context;
    this.stopped = new AtomicBoolean(false);
    

  }

  @Override
  protected void serviceInit(Configuration conf) throws Exception {
    this.limitOnPoolSize = conf.getInt(
      MRJobConfig.MR_AM_CONTAINERLAUNCHER_THREAD_COUNT_LIMIT,
      MRJobConfig.DEFAULT_MR_AM_CONTAINERLAUNCHER_THREAD_COUNT_LIMIT);
    LOG.info("Upper limit on the thread pool size is " + this.limitOnPoolSize);
    super.serviceInit(conf);
    cmProxy = new ContainerManagementProtocolProxy(conf);

    // Get total number of map tasks
    org.apache.hadoop.mapred.JobConf jobConf = (org.apache.hadoop.mapred.JobConf) conf;
    numMapTasks = jobConf.getNumMapTasks();
    // Initialize task location map
    taskLocations = new HashMap<Integer, String>();
    isPrinted = false;
  }

  protected void serviceStart() throws Exception {

    ThreadFactory tf = new ThreadFactoryBuilder()
      .setNameFormat("ContainerLauncher #%d").setDaemon(true).build();

    // Start with a default core-pool size of 10 and change it dynamically.
    launcherPool = new ThreadPoolExecutor(INITIAL_POOL_SIZE, Integer.MAX_VALUE,
      1, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>(), tf);
    eventHandlingThread = new Thread() {
      @Override
      public void run() {
        ContainerLauncherEvent event = null;
        Set<String> allNodes = new HashSet<String>();

        while (!stopped.get() && !Thread.currentThread().isInterrupted()) {
          try {
            event = eventQueue.take();
          } catch (InterruptedException e) {
            if (!stopped.get()) {
              LOG.error("Returning, interrupted : " + e);
            }
            return;
          }
          allNodes.add(event.getContainerMgrAddress());

          int poolSize = launcherPool.getCorePoolSize();

          // See if we need up the pool size only if haven't reached the
          // maximum limit yet.
          if (poolSize != limitOnPoolSize) {

            // nodes where containers will run at *this* point of time. This is
            // *not* the cluster size and doesn't need to be.
            int numNodes = allNodes.size();
            int idealPoolSize = Math.min(limitOnPoolSize, numNodes);

            if (poolSize < idealPoolSize) {
              // Bump up the pool size to idealPoolSize+INITIAL_POOL_SIZE, the
              // later is just a buffer so we are not always increasing the
              // pool-size
              int newPoolSize = Math.min(limitOnPoolSize, idealPoolSize
                + INITIAL_POOL_SIZE);
              LOG.info("Setting ContainerLauncher pool size to " + newPoolSize
                + " as number-of-nodes to talk to is " + numNodes);
              launcherPool.setCorePoolSize(newPoolSize);
            }
          }

          // the events from the queue are handled in parallel
          // using a thread pool
          launcherPool.execute(createEventProcessor(event));

          // TODO: Group launching of multiple containers to a single
          // NodeManager into a single connection
        }
      }
    };
    eventHandlingThread.setName("ContainerLauncher Event Handler");
    eventHandlingThread.start();
    super.serviceStart();
  }

  private void shutdownAllContainers() {
    for (Container ct : this.containers.values()) {
      if (ct != null) {
        ct.kill();
      }
    }
  }

  protected void serviceStop() throws Exception {
    if (stopped.getAndSet(true)) {
      // return if already stopped
      return;
    }
    // shutdown any containers that might be left running
    shutdownAllContainers();
    if (eventHandlingThread != null) {
      eventHandlingThread.interrupt();
    }
    if (launcherPool != null) {
      launcherPool.shutdownNow();
    }
    super.serviceStop();
  }

  protected EventProcessor createEventProcessor(ContainerLauncherEvent event) {
    return new EventProcessor(event);
  }

  /**
   * Setup and start the container on remote nodemanager.
   */
  class EventProcessor implements Runnable {
    private ContainerLauncherEvent event;

    EventProcessor(ContainerLauncherEvent event) {
      this.event = event;
    }

    @Override
    public void run() {
      LOG.info("Processing the event " + event.toString());

      // Load ContainerManager tokens before creating a connection.
      // TODO: Do it only once per NodeManager.
      ContainerId containerID = event.getContainerID();

      Container c = getContainer(event);
      switch (event.getType()) {

      case CONTAINER_REMOTE_LAUNCH:
        ContainerRemoteLaunchEvent launchEvent = (ContainerRemoteLaunchEvent) event;
        // ZBJ try to log
        try {
          TaskType taskType = launchEvent.getTaskAttemptID().getTaskId()
            .getTaskType();
          String type = "NULL";
          if (taskType == TaskType.MAP) {
            type = "MAP";
          } else if (taskType == TaskType.REDUCE) {
            type = "REDUCE";
          }
          int taskID = launchEvent.getTaskAttemptID().getTaskId().getId();
          String jobID = launchEvent.getTaskAttemptID().getTaskId().getJobId().toString();
          org.apache.hadoop.yarn.api.records.Container container = launchEvent
            .getAllocatedContainer();
          String host = container.getNodeId().getHost();
          // ContainLauncherImpl has config
          RackResolver.init(getConfig());
          String nodeRackName = RackResolver.resolve(
            container.getNodeId().getHost()).getNetworkLocation();
          String memStr = getConfig().get(
            "mapreduce.map.collective.java.memory.mb", "<missing conf2>");
          int maxMem = Integer.parseInt(memStr);
          int conID = containerID.getId();
          int conMem = launchEvent.getAllocatedContainer().getResource()
            .getMemory();
          int conCore = launchEvent.getAllocatedContainer().getResource()
            .getVirtualCores();
          LOG.info("Container launch info: " + "task_id = " + taskID + ", "
            + "task_type = " + type + ", " + "host = " + host + ", "
            + "rack = " + nodeRackName + ", " + "mem = " + maxMem + ", "
            + "container ID = " + conID + ", " + "container mem = " + conMem
            + ", " + "container core = " + conCore);
          if (taskType == TaskType.MAP) {
            // jobID should be the same for all the tasks here
            recordTaskLocations(jobID, taskID, host, nodeRackName);
          }
          List<String> commands = launchEvent.getContainerLaunchContext()
            .getCommands();
          List<String> newCmds = new ArrayList<String>();
          for (String cmd : commands) {
            String newCmd = cmd.replaceFirst("-Xmx[0-9]+M", "-Xmx" + maxMem
              + "M");
            LOG.info("Java cmd: " + newCmd);
            newCmds.add(newCmd);
          }
          launchEvent.getContainerLaunchContext().setCommands(newCmds);
        } catch (Exception e) {
          LOG.error("no rack name found!", e);
        }

        c.launch(launchEvent);
        break;

      case CONTAINER_REMOTE_CLEANUP:
        c.kill();
        break;
      }
      removeContainerIfDone(containerID);
    }
  }
  
  /**
   * All the information are from launchEvent, which gets all the task
   * information from taskAttempt, should match with the task information inside
   * Yarn Child
   * 
   * @param jobID
   * @param taskID
   * @param host
   * @param rack
   */
  private void recordTaskLocations(String jobID, int taskID, String host,
    String rack) {
    taskLocations.put(taskID, host);
    if (taskLocations.size() == this.numMapTasks && !isPrinted) {
      LOG.info("START PRINTING TASK LOCATIONS");
      for (Entry<Integer, String> entry : taskLocations.entrySet()) {
        LOG.info("Task ID: " + entry.getKey() + ". Task Location: "
          + entry.getValue());
      }
      // Write several files to HDFS
      String nodesFile = "/" + jobID + "/nodes";
      String lockFile = "/" + jobID + "/lock";
      try {
        // Write nodes file to HDFS
        Path path = new Path(nodesFile);
        FileSystem fs = FileSystem.get(getConfig());
        FSDataOutputStream out = fs.create(path, true);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        bw.write("#0");
        bw.newLine();
        for (String value : taskLocations.values()) {
          bw.write(value);
          bw.newLine();
        }
        bw.flush();
        out.hflush();
        out.hsync();
        bw.close();
        // Write Lock file
        Path lock = new Path(lockFile);
        FSDataOutputStream lockOut = fs.create(lock, true);
        lockOut.hflush();
        lockOut.hsync();
        lockOut.close();
      } catch (IOException e) {
        LOG.info("Error when writing nodes file to HDFS. ", e);
      }
      isPrinted = true;
    }
  }

  @SuppressWarnings("unchecked")
  void sendContainerLaunchFailedMsg(TaskAttemptId taskAttemptID, String message) {
    LOG.error(message);
    context.getEventHandler().handle(
      new TaskAttemptDiagnosticsUpdateEvent(taskAttemptID, message));
    context.getEventHandler().handle(
      new TaskAttemptEvent(taskAttemptID,
        TaskAttemptEventType.TA_CONTAINER_LAUNCH_FAILED));
  }

  @Override
  public void handle(ContainerLauncherEvent event) {
    try {
      eventQueue.put(event);
    } catch (InterruptedException e) {
      throw new YarnRuntimeException(e);
    }
  }

  public ContainerManagementProtocolProxy.ContainerManagementProtocolProxyData getCMProxy(
    String containerMgrBindAddr, ContainerId containerId) throws IOException {
    return cmProxy.getProxy(containerMgrBindAddr, containerId);
  }
}

