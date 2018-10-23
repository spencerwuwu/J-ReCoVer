// https://searchcode.com/api/result/101299745/

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

package org.apache.hadoop.mapred;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.ClusterStatus;
import org.apache.hadoop.mapred.EagerTaskInitializationListener;
import org.apache.hadoop.mapred.JobInProgress;
import org.apache.hadoop.mapred.JobQueueJobInProgressListener;
import org.apache.hadoop.mapred.JobStatus;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapred.TaskScheduler;
import org.apache.hadoop.mapred.TaskTrackerStatus;
import org.apache.hadoop.mapreduce.server.jobtracker.TaskTracker;
import org.apache.hadoop.net.DNSToSwitchMapping;
import org.apache.hadoop.net.ScriptBasedMapping;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * This scheduler is modified from original Hadoop FIFO scheduler with task
 * information output.
 */
public class MapCollectiveScheduler extends TaskScheduler {
  
  private static final int MIN_CLUSTER_SIZE_FOR_PADDING = 3;
  public static final Log LOG = LogFactory.getLog(MapCollectiveScheduler.class);
  
  protected JobQueueJobInProgressListener jobQueueJobInProgressListener;
  protected EagerTaskInitializationListener eagerTaskInitializationListener;
  private float padFraction;
  
  private Map<Integer, Map<Integer, String>> jobTaskLocations;
  
  public MapCollectiveScheduler() {
    this.jobQueueJobInProgressListener = new JobQueueJobInProgressListener();
    
    jobTaskLocations = new HashMap<Integer, Map<Integer, String>>();
  }
  
  @Override
  public synchronized void start() throws IOException {
    super.start();
    LOG.info("Map Collective Job Scheduler starts.");
    // Task tracker manager is job tracker
    taskTrackerManager.addJobInProgressListener(jobQueueJobInProgressListener);
    eagerTaskInitializationListener.setTaskTrackerManager(taskTrackerManager);
    eagerTaskInitializationListener.start();
    taskTrackerManager.addJobInProgressListener(
        eagerTaskInitializationListener);
    LOG.info("Job Scheduler starts finishes.");
    
    FileSystem fs = FileSystem.get(getConf());
    fs.delete(new Path("nodes"), false);
    fs.delete(new Path("lock"), false);
  }
  
  @Override
  public synchronized void terminate() throws IOException {
    LOG.info("Job Scheduler ends.");
    if (jobQueueJobInProgressListener != null) {
      taskTrackerManager.removeJobInProgressListener(
          jobQueueJobInProgressListener);
    }
    if (eagerTaskInitializationListener != null) {
      taskTrackerManager.removeJobInProgressListener(
          eagerTaskInitializationListener);
      eagerTaskInitializationListener.terminate();
    }
    super.terminate();
    LOG.info("Job Scheduler ends.");
  }
  
  @Override
  public synchronized void setConf(Configuration conf) {
    super.setConf(conf);
    padFraction = conf.getFloat("mapred.jobtracker.taskalloc.capacitypad", 
                                 0.01f);
    this.eagerTaskInitializationListener =
      new EagerTaskInitializationListener(conf);
  }

  @Override
  public synchronized List<Task> assignTasks(TaskTracker taskTracker)
      throws IOException {
    // Check for JT safe-mode
    if (taskTrackerManager.isInSafeMode()) {
      LOG.info("JobTracker is in safe-mode, not scheduling any tasks.");
      return null;
    } 

    /*
    LOG.info("TaskTracker name: " + taskTracker.getTrackerName());
    LOG.info("TaskTracker available Job Setup slots: "
      + taskTracker.getAvailableSlots(TaskType.JOB_SETUP));
    LOG.info("TaskTracker available Map slots: "
      + taskTracker.getAvailableSlots(TaskType.MAP));
    LOG.info("TaskTracker available Reduce slots: "
      + taskTracker.getAvailableSlots(TaskType.REDUCE));
    LOG.info("TaskTracker available Task Cleanup slots: "
      + taskTracker.getAvailableSlots(TaskType.TASK_CLEANUP));
    LOG.info("TaskTracker available Job Cleanup slots: "
      + taskTracker.getAvailableSlots(TaskType.JOB_CLEANUP));
    */
    
    TaskTrackerStatus taskTrackerStatus = taskTracker.getStatus(); 
    
    ClusterStatus clusterStatus = taskTrackerManager.getClusterStatus();
    final int numTaskTrackers = clusterStatus.getTaskTrackers();
    final int clusterMapCapacity = clusterStatus.getMaxMapTasks();
    final int clusterReduceCapacity = clusterStatus.getMaxReduceTasks();
  
    Collection<JobInProgress> jobQueue =
      jobQueueJobInProgressListener.getJobQueue();

    //
    // Get map + reduce counts for the current tracker.
    //
    final int trackerMapCapacity = taskTrackerStatus.getMaxMapSlots();
    final int trackerReduceCapacity = taskTrackerStatus.getMaxReduceSlots();
    final int trackerRunningMaps = taskTrackerStatus.countMapTasks();
    final int trackerRunningReduces = taskTrackerStatus.countReduceTasks();

    // Assigned tasks
    List<Task> assignedTasks = new ArrayList<Task>();

    //
    // Compute (running + pending) map and reduce task numbers across pool
    //
    int remainingReduceLoad = 0;
    int remainingMapLoad = 0;
    synchronized (jobQueue) {
      for (JobInProgress job : jobQueue) {
        if (job.getStatus().getRunState() == JobStatus.RUNNING) {
          remainingMapLoad += (job.desiredMaps() - job.finishedMaps());
          if (job.scheduleReduces()) {
            remainingReduceLoad += (job.desiredReduces() - job
              .finishedReduces());
          }
        }
      }
    }

    // Compute the 'load factor' for maps and reduces
    /*
    LOG.info("TaskTracker name: " + taskTracker.getTrackerName());
    LOG.info("remainingMapLoad: " + remainingMapLoad);
    LOG.info("clusterMapCapacity: " + clusterMapCapacity);
    LOG.info("trackerMapCapacity: " + trackerMapCapacity);
    LOG.info("trackerRunningMaps: " + trackerRunningMaps);
    */
    
    double mapLoadFactor = 0.0;
    if (clusterMapCapacity > 0) {
      mapLoadFactor = (double)remainingMapLoad / clusterMapCapacity;
    }
    double reduceLoadFactor = 0.0;
    if (clusterReduceCapacity > 0) {
      reduceLoadFactor = (double)remainingReduceLoad / clusterReduceCapacity;
    }
        
    //
    // In the below steps, we allocate first map tasks (if appropriate),
    // and then reduce tasks if appropriate.  We go through all jobs
    // in order of job arrival; jobs only get serviced if their 
    // predecessors are serviced, too.
    //

    //
    // We assign tasks to the current taskTracker if the given machine 
    // has a workload that's less than the maximum load of that kind of
    // task.
    // However, if the cluster is close to getting loaded i.e. we don't
    // have enough _padding_ for speculative executions etc., we only 
    // schedule the "highest priority" task i.e. the task from the job 
    // with the highest priority.
    //
    
    final int trackerCurrentMapCapacity = 
      Math.min((int)Math.ceil(mapLoadFactor * trackerMapCapacity), 
                              trackerMapCapacity);
    int availableMapSlots = trackerCurrentMapCapacity - trackerRunningMaps;
    boolean exceededMapPadding = false;
    if (availableMapSlots > 0) {
      exceededMapPadding = 
        exceededPadding(true, clusterStatus, trackerMapCapacity);
    }
    
    int numLocalMaps = 0;
    int numNonLocalMaps = 0;
    scheduleMaps:
    for (int i=0; i < availableMapSlots; ++i) {
      synchronized (jobQueue) {
        for (JobInProgress job : jobQueue) {
          if (job.getStatus().getRunState() != JobStatus.RUNNING) {
            continue;
          }
          
          // Not sure if this is going to work
          // and synchronization
          if (job.desiredMaps() > clusterMapCapacity) {
            LOG.info("Job Identifier: " + job.getJobID().getJtIdentifier()
              + ". Job ID: " + job.getJobID().getId() + ". JobStatus: "
              + job.getStatus().getRunState() + ". Desired Map Size: "
              + job.desiredMaps() + ". Cluster Max Map Size: "
              + clusterMapCapacity);
            taskTrackerManager.failJob(job);
            continue;
          }
          if (job.failedMaps.size() > 0) {
            taskTrackerManager.failJob(job);
            continue;
          }

          Task t = null;
          
          // Try to schedule a Map task with locality between node-local 
          // and rack-local
          t = 
            job.obtainNewNodeOrRackLocalMapTask(taskTrackerStatus, 
                numTaskTrackers, taskTrackerManager.getNumberOfUniqueHosts());
          if (t != null) {
            assignedTasks.add(t);
            ++numLocalMaps;
            
            recordTaskLocations(job, t, taskTracker);
            
            // Don't assign map tasks to the hilt!
            // Leave some free slots in the cluster for future task-failures,
            // speculative tasks etc. beyond the highest priority job
            if (exceededMapPadding) {
              break scheduleMaps;
            }
           
            // Try all jobs again for the next Map task 
            break;
          }
          
          // Try to schedule a node-local or rack-local Map task
          t = 
            job.obtainNewNonLocalMapTask(taskTrackerStatus, numTaskTrackers,
                                   taskTrackerManager.getNumberOfUniqueHosts());
          
          if (t != null) {
            assignedTasks.add(t);
            ++numNonLocalMaps;
            
            recordTaskLocations(job, t, taskTracker);
            
            // We assign at most 1 off-switch or speculative task
            // This is to prevent TaskTrackers from stealing local-tasks
            // from other TaskTrackers.
            break scheduleMaps;
          }
        }
      }
    }
    int assignedMaps = assignedTasks.size();
    


    //
    // Same thing, but for reduce tasks
    // However we _never_ assign more than 1 reduce task per heartbeat
    //
    final int trackerCurrentReduceCapacity = 
      Math.min((int)Math.ceil(reduceLoadFactor * trackerReduceCapacity), 
               trackerReduceCapacity);
    final int availableReduceSlots = 
      Math.min((trackerCurrentReduceCapacity - trackerRunningReduces), 1);
    boolean exceededReducePadding = false;
    if (availableReduceSlots > 0) {
      exceededReducePadding = exceededPadding(false, clusterStatus, 
                                              trackerReduceCapacity);
      synchronized (jobQueue) {
        for (JobInProgress job : jobQueue) {
          if (job.getStatus().getRunState() != JobStatus.RUNNING ||
              job.numReduceTasks == 0) {
            continue;
          }

          Task t = 
            job.obtainNewReduceTask(taskTrackerStatus, numTaskTrackers, 
                                    taskTrackerManager.getNumberOfUniqueHosts()
                                    );
          if (t != null) {
            assignedTasks.add(t);
            break;
          }
          
          // Don't assign reduce tasks to the hilt!
          // Leave some free slots in the cluster for future task-failures,
          // speculative tasks etc. beyond the highest priority job
          if (exceededReducePadding) {
            break;
          }
        }
      }
    }
    
    if (LOG.isDebugEnabled()) {
      LOG.debug("Task assignments for " + taskTrackerStatus.getTrackerName() + " --> " +
                "[" + mapLoadFactor + ", " + trackerMapCapacity + ", " + 
                trackerCurrentMapCapacity + ", " + trackerRunningMaps + "] -> [" + 
                (trackerCurrentMapCapacity - trackerRunningMaps) + ", " +
                assignedMaps + " (" + numLocalMaps + ", " + numNonLocalMaps + 
                ")] [" + reduceLoadFactor + ", " + trackerReduceCapacity + ", " + 
                trackerCurrentReduceCapacity + "," + trackerRunningReduces + 
                "] -> [" + (trackerCurrentReduceCapacity - trackerRunningReduces) + 
                ", " + (assignedTasks.size()-assignedMaps) + "]");
    }

    return assignedTasks;
  }
  
  private void recordTaskLocations(JobInProgress job, Task t,
    TaskTracker taskTracker) {
    List <String> tmpList = new ArrayList<String>(1);
    tmpList.add(taskTracker.getStatus().host);
    DNSToSwitchMapping dnsToSwitchMapping = ReflectionUtils.newInstance(conf
      .getClass("topology.node.switch.mapping.impl", ScriptBasedMapping.class,
        DNSToSwitchMapping.class), getConf());
    List <String> rNameList = dnsToSwitchMapping.resolve(tmpList);
    String rName = rNameList.get(0);
    // At current stage, we don't consider rack, just put #0 as Rack ID
    LOG.info("Get dnsToSwitchMapping " + dnsToSwitchMapping.getClass().getName());
    LOG.info("TASK TRACKER HOST: " + taskTracker.getStatus().host);
    LOG.info("Get rack name: " + rName);
    // Record task id and location and print for obtainNewNodeOrRackLocalMapTask
    synchronized (jobTaskLocations) {
      Map<Integer, String> taskLocations = jobTaskLocations.get(job.getJobID()
        .getId());
      if (taskLocations == null) {
        taskLocations = new HashMap<Integer, String>();
        jobTaskLocations.put(job.getJobID().getId(), taskLocations);
      }
      String taskTrackerName = taskTracker.getTrackerName();
      // String ip = taskTrackerName.split("/")[1].split(":")[0];
      // to do : get IP or host name
      String ip = taskTracker.getStatus().host;
      taskLocations.put(t.getTaskID().getTaskID().getId(),
        ip);    
      if (taskLocations.size() == job.desiredMaps()) {
        LOG.info("START PRINTING TASK LOCATIONS OF JOB "
          + job.getJobID().getId());
        for (Entry<Integer, String> entry : taskLocations.entrySet()) {
          LOG.info("Task ID: " + entry.getKey() + ". Task Location: "
            + entry.getValue());
        }
        // Write several files to HDFS
        try {
          // Write nodes file to HDFS
          Path path = new Path("/" + job.getJobID().toString() + "/nodes");
          FileSystem fs = FileSystem.get(getConf());
          FSDataOutputStream out = fs.create(path, true);
          BufferedWriter br = new BufferedWriter(new OutputStreamWriter(out));
          br.write("#0");
          br.newLine();
          for (String value : taskLocations.values()) {
            br.write(value);
            br.newLine();
          }
          br.flush();
          out.sync();
          br.close();
          // Write Lock file
          Path lock = new Path("/" + job.getJobID().toString() + "/lock");
          FSDataOutputStream lockOut = fs.create(lock, true);
          lockOut.sync();
          lockOut.close();
        } catch (IOException e) {
          LOG.info("Error when writing nodes file to HDFS. ", e);
        }
      }
    }
  }

  private boolean exceededPadding(boolean isMapTask, 
                                  ClusterStatus clusterStatus, 
                                  int maxTaskTrackerSlots) { 
    int numTaskTrackers = clusterStatus.getTaskTrackers();
    int totalTasks = 
      (isMapTask) ? clusterStatus.getMapTasks() : 
        clusterStatus.getReduceTasks();
    int totalTaskCapacity = 
      isMapTask ? clusterStatus.getMaxMapTasks() : 
                  clusterStatus.getMaxReduceTasks();

    Collection<JobInProgress> jobQueue =
      jobQueueJobInProgressListener.getJobQueue();

    boolean exceededPadding = false;
    synchronized (jobQueue) {
      int totalNeededTasks = 0;
      for (JobInProgress job : jobQueue) {
        if (job.getStatus().getRunState() != JobStatus.RUNNING ||
            job.numReduceTasks == 0) {
          continue;
        }

        //
        // Beyond the highest-priority task, reserve a little 
        // room for failures and speculative executions; don't 
        // schedule tasks to the hilt.
        //
        totalNeededTasks += 
          isMapTask ? job.desiredMaps() : job.desiredReduces();
        int padding = 0;
        if (numTaskTrackers > MIN_CLUSTER_SIZE_FOR_PADDING) {
          padding = 
            Math.min(maxTaskTrackerSlots,
                     (int) (totalNeededTasks * padFraction));
        }
        if (totalTasks + padding >= totalTaskCapacity) {
          exceededPadding = true;
          break;
        }
      }
    }

    return exceededPadding;
  }

  @Override
  public synchronized Collection<JobInProgress> getJobs(String queueName) {
    return jobQueueJobInProgressListener.getJobQueue();
  }  
}

