// https://searchcode.com/api/result/101299039/

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

package edu.iu.optikmeans;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.arrpar.ArrCombiner;
import edu.iu.harp.arrpar.ArrPartition;
import edu.iu.harp.arrpar.ArrTable;
import edu.iu.harp.arrpar.Double2DArrAvg;
import edu.iu.harp.arrpar.DoubleArrPlus;
import edu.iu.harp.comm.data.Array;
import edu.iu.harp.comm.data.DoubleArray;
import edu.iu.harp.comm.resource.ResourcePool;

public class KMeansAllReduceMapper extends
  CollectiveMapper<String, String, Object, Object> {

  private int jobID;
  private int numMappers;
  private int vectorSize;
  private int numCentroids;
  private int pointsPerFile;
  private int iteration;

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context) throws IOException,
    InterruptedException {
    System.out.println("start setup"
      + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance()
        .getTime()));
    long startTime = System.currentTimeMillis();
    Configuration configuration = context.getConfiguration();
    jobID = configuration.getInt(KMeansConstants.JOB_ID, 0);
    numMappers = configuration.getInt(KMeansConstants.NUM_MAPPERS, 10);
    numCentroids = configuration.getInt(KMeansConstants.NUM_CENTROIDS, 20);
    pointsPerFile = configuration.getInt(KMeansConstants.POINTS_PER_FILE, 20);
    vectorSize = configuration.getInt(KMeansConstants.VECTOR_SIZE, 20);
    iteration = configuration.getInt(KMeansConstants.ITERATION_COUNT, 1);
    long endTime = System.currentTimeMillis();
    System.out.println("config (ms) :" + (endTime - startTime));
  }

  protected void mapCollective(KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    LOG.info("Start collective mapper.");
    List<String> pointFiles = new ArrayList<String>();
    while (reader.nextKeyValue()) {
      String key = reader.getCurrentKey();
      String value = reader.getCurrentValue();
      LOG.info("Key: " + key + ", Value: " + value);
      pointFiles.add(value);
    }
    int numThreads = 30;
    // int numThreads = 1;
    int numCenPartitions = this.numMappers;
    int vectorSize = this.vectorSize;
    Configuration conf = context.getConfiguration();
    runKmeans(pointFiles, numCenPartitions, vectorSize, numThreads, conf,
      context);
    LOG.info("Total iterations in master view: "
      + (System.currentTimeMillis() - startTime));
  }

  private void runKmeans(List<String> fileNames, int numCenPartitions,
    int vectorSize, int numThreads, Configuration conf, Context context)
    throws IOException {
    // -----------------------------------------------------------------------------
    // Load centroids
    // Create centroid array partitions
    long startTime = System.currentTimeMillis();
    int cParSize = this.numCentroids / numCenPartitions;
    int rest = this.numCentroids % numCenPartitions;
    // Note that per col size is vectorSize + 1;
    ArrTable<DoubleArray, DoubleArrPlus> table = new ArrTable<DoubleArray, DoubleArrPlus>(
      0, DoubleArray.class, DoubleArrPlus.class);
    if (this.isMaster()) {
      String cFile = conf.get(KMeansConstants.CFILE);
      Map<Integer, DoubleArray> cenDataMap = createCenDataMap(cParSize, rest,
        numCenPartitions, vectorSize, this.getResourcePool());
      loadCentroids(cenDataMap, vectorSize, cFile, conf);
      addPartitionMapToTable(cenDataMap, table);
    }
    long endTime = System.currentTimeMillis();
    LOG.info("Load centroids (ms): " + (endTime - startTime));
    // Bcast centroids
    startTime = System.currentTimeMillis();
    bcastCentroids(table);
    endTime = System.currentTimeMillis();
    LOG.info("Bcast centroids (ms): " + (endTime - startTime));
    // ---------------------------------------------------------------------------
    // Load data points
    startTime = System.currentTimeMillis();
    List<DoubleArray> pDataList = doTasks(fileNames, "load-data-points",
      new PointLoadTask(this.pointsPerFile, vectorSize, conf), numThreads);
    endTime = System.currentTimeMillis();
    LOG.info("File read (ms): " + (endTime - startTime));
    // -------------------------------------------------------------------------------------
    // For iterations
    ArrTable<DoubleArray, DoubleArrPlus> newTable = null;
    ArrPartition<DoubleArray>[] cPartitions = null;
    for (int i = 0; i < this.iteration; i++) {
      // For each iteration
      LOG.info("Start compute centroids. Iteration: " + i);
      startTime = System.currentTimeMillis();
      Map<Integer, DoubleArray> cenDataMap = createCenDataMap(cParSize, rest,
        numCenPartitions, this.vectorSize, this.getResourcePool());
      endTime = System.currentTimeMillis();
      LOG.info("Create centroids map (ms): " + (endTime - startTime));
      // Compute dot product for each centroid-vector
      startTime = System.currentTimeMillis();
      cPartitions = table.getPartitions();
      doTasksReturnSet(new ObjectArrayList<ArrPartition<DoubleArray>>(
        cPartitions), "centroids-dot-product",
        new CenDotProductTask(vectorSize), numThreads);
      endTime = System.currentTimeMillis();
      LOG.info("Compute centroids dot product (ms): " + (endTime - startTime));
      // Compute distances
      startTime = System.currentTimeMillis();
      Set<Map<Integer, DoubleArray>> localCenDataMaps = doTasksReturnSet(
        pDataList, "centroids-calc", new CenCalcTask(cPartitions, cParSize,
          rest, numCenPartitions, vectorSize, this.getResourcePool(), context),
        numThreads);
      // Release centroids in this iteration
      releasePartitions(cPartitions);
      cPartitions = null;
      endTime = System.currentTimeMillis();
      LOG.info("Calculate centroids (ms): " + (endTime - startTime));
      LOG.info("Local centroid data maps: " + localCenDataMaps.size());
      // Merge localCenDataMaps -> cenDataMap
      startTime = System.currentTimeMillis();
      List<Integer> partitionIDs = new ArrayList<Integer>(cenDataMap.keySet());
      doTasksReturnSet(partitionIDs, "centroids-merge",
        new CenMergeTask(new ArrayList<Map<Integer, DoubleArray>>(
          localCenDataMaps), cenDataMap), numThreads);
      // Release local centroid-vectors map
      releaseLocalCenDataMaps(localCenDataMaps);
      localCenDataMaps = null;
      endTime = System.currentTimeMillis();
      LOG.info("Merge centroids (ms): " + (endTime - startTime));
      // Allreduce
      table = new ArrTable<DoubleArray, DoubleArrPlus>(this.getWorkerID(),
        DoubleArray.class, DoubleArrPlus.class);
      addPartitionMapToTable(cenDataMap, table);
      cenDataMap = null;
      newTable = new ArrTable<DoubleArray, DoubleArrPlus>(1, DoubleArray.class,
        DoubleArrPlus.class);
      /*
       * Old table is altered during allreduce, ignore it. Allreduce:
       * regroup-combine-aggregate(reduce)-allgather table is at the state after
       * regroup-combine, but new table is after allgather. Since in
       * Double2DArrAvg, old table partition is moved to new table, we don't
       * need to release the partitions it uses.
       */
      try {
        startTime = System.currentTimeMillis();
        // For every local centroid-vector, the first element is a count.
        allreduce(table, newTable, new Double2DArrAvg(vectorSize + 1));
        endTime = System.currentTimeMillis();
        LOG.info("Allreduce time (ms): " + (endTime - startTime));
        table = null;
      } catch (Exception e) {
        LOG.error("Fail to do allreduce.", e);
        throw new IOException(e);
      }
      table = newTable;
    }
    // Write out new table
    if (this.isMaster()) {
      LOG.info("Start to write out centroids.");
      startTime = System.currentTimeMillis();
      storeCentroids(conf, this.getResourcePool(), newTable, vectorSize,
        jobID + 1);
      endTime = System.currentTimeMillis();
      LOG.info("Store centroids time (ms): " + (endTime - startTime));
    }
    // Clean all the references
    newTable = null;
  }

  /**
   * Load data points from a file.
   * 
   * @param file
   * @param conf
   * @return
   * @throws IOException
   */
  public static DoubleArray loadPoints(String file, int pDataSize,
    Configuration conf) throws IOException {
    double[] pData = new double[pDataSize];
    long time1 = System.currentTimeMillis();
    Path inputFilePath = new Path(file);
    FileSystem fs = inputFilePath.getFileSystem(conf);
    FSDataInputStream in = fs.open(inputFilePath);
    try {
      for (int i = 0; i < pDataSize; i++) {
        // In distance calculation, dis = p^2 - 2pc + c^2
        // we don't need to calculate p^2, because it is same
        // for a p in all distance calculation. We calculate 2p
        // to reduce the calculation
        pData[i] = in.readDouble() * 2;
      }
    } finally {
      in.close();
    }
    long time2 = System.currentTimeMillis();
    DoubleArray pArray = new DoubleArray();
    pArray.setArray(pData);
    pArray.setSize(pDataSize);
    // Read the input data file directly from HDFS
    LOG.info("Reading the file: " + inputFilePath.toString() + " takes (ms): "
      + (time2 - time1));
    return pArray;
  }

  /**
   * Fill data from centroid file to cenDataMap
   * 
   * @param cenDataMap
   * @param vectorSize
   * @param cFileName
   * @param configuration
   * @throws IOException
   */
  private static void loadCentroids(Map<Integer, DoubleArray> cenDataMap,
    int vectorSize, String cFileName, Configuration configuration)
    throws IOException {
    Path cPath = new Path(cFileName);
    FileSystem fs = FileSystem.get(configuration);
    FSDataInputStream in = fs.open(cPath);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    double[] cData;
    int start;
    int size;
    int collen = vectorSize + 1;
    String[] curLine = null;
    int curPos = 0;
    for (DoubleArray array : cenDataMap.values()) {
      cData = array.getArray();
      start = array.getStart();
      size = array.getSize();
      for (int i = start; i < (start + size); i++) {
        // Don't set the first element in each row
        if (i % collen != 0) {
          // cData[i] = in.readDouble();
          if (curLine == null || curPos == curLine.length - 1) {
            curLine = br.readLine().split(" ");
            curPos = 0;
          } else {
            curPos++;
          }
          cData[i] = Double.parseDouble(curLine[curPos]);
        }
      }
    }
    br.close();
  }

  /**
   * Create array to store the intermediate centroid data in this iteration.
   * Each partition uses once dimensional array to present a two dimensional
   * array. The length of each row is vectorSize + 1, the first element is to
   * count how many data points are in this row during intermediate local sum.
   * 
   * @param cenParSize
   * @param rest
   * @param numPartition
   * @param vectorSize
   * @param resourcePool
   * @return
   */
  static Map<Integer, DoubleArray> createCenDataMap(int cenParSize, int rest,
    int numPartition, int vectorSize, ResourcePool resourcePool) {
    Map<Integer, DoubleArray> cOutMap = new Int2ObjectAVLTreeMap<DoubleArray>();
    for (int i = 0; i < numPartition; i++) {
      DoubleArray doubleArray = new DoubleArray();
      if (rest > 0) {
        // An extra element for every vector as count
        doubleArray.setArray(resourcePool.getDoubleArrayPool().getArray(
          (cenParSize + 1) * (vectorSize + 1)));
        doubleArray.setSize((cenParSize + 1) * (vectorSize + 1));
        Arrays.fill(doubleArray.getArray(), 0);
        rest--;
      } else if (cenParSize > 0) {
        doubleArray.setArray(resourcePool.getDoubleArrayPool().getArray(
          cenParSize * (vectorSize + 1)));
        doubleArray.setSize(cenParSize * (vectorSize + 1));
        Arrays.fill(doubleArray.getArray(), 0);
      } else {
        break;
      }
      cOutMap.put(i, doubleArray);
    }
    return cOutMap;
  }

  /**
   * Broadcast centroids data in partitions
   * 
   * @param table
   * @param numPartitions
   * @throws IOException
   */
  private <T, A extends Array<T>, C extends ArrCombiner<A>> void bcastCentroids(
    ArrTable<A, C> table) throws IOException {
    boolean success = true;
    try {
      success = arrTableBcast(table);
    } catch (Exception e) {
      LOG.error("Fail to bcast.", e);
    }
    if (!success) {
      throw new IOException("Fail to bcast");
    }
  }

  private <A extends Array<?>, C extends ArrCombiner<A>> void addPartitionMapToTable(
    Map<Integer, A> map, ArrTable<A, C> table) throws IOException {
    for (Entry<Integer, A> entry : map.entrySet()) {
      try {
        table
          .addPartition(new ArrPartition<A>(entry.getValue(), entry.getKey()));
      } catch (Exception e) {
        LOG.error("Fail to add partitions", e);
        throw new IOException(e);
      }
    }
  }

  private void releasePartitions(ArrPartition<DoubleArray>[] partitions) {
    for (int i = 0; i < partitions.length; i++) {
      this.getResourcePool().getDoubleArrayPool()
        .releaseArrayInUse(partitions[i].getArray().getArray());
    }
  }

  private void releaseLocalCenDataMaps(
    Set<Map<Integer, DoubleArray>> localCenDataMaps) {
    for (Map<Integer, DoubleArray> localCenDataMap : localCenDataMaps) {
      for (DoubleArray array : localCenDataMap.values()) {
        this.getResourcePool().getDoubleArrayPool()
          .releaseArrayInUse(array.getArray());
      }
    }
  }

  private static void storeCentroids(Configuration configuration,
    ResourcePool resourcePool, ArrTable<DoubleArray, DoubleArrPlus> newTable,
    int vectorSize, int jobID) throws IOException {
    String cFile = configuration.get(KMeansConstants.CFILE);
    Path cPath = new Path(cFile.substring(0, cFile.lastIndexOf("_") + 1)
      + jobID);
    LOG.info("centroids path: " + cPath.toString());
    FileSystem fs = FileSystem.get(configuration);
    fs.delete(cPath, true);
    FSDataOutputStream out = fs.create(cPath);
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
    ArrPartition<DoubleArray> partitions[] = newTable.getPartitions();
    for (ArrPartition<DoubleArray> partition : partitions) {
      for (int i = 0; i < partition.getArray().getSize(); i++) {
        if (i % (vectorSize + 1) == vectorSize) {
          bw.write(partition.getArray().getArray()[i] + "");
          bw.newLine();
        } else if (i % (vectorSize + 1) != 0) {
          // Every row with vectorSize + 1 length, the first one is a count,
          // ignore it in output
          bw.write(partition.getArray().getArray()[i] + " ");
        }
      }
      resourcePool.getDoubleArrayPool().freeArrayInUse(
        partition.getArray().getArray());
    }
    bw.flush();
    bw.close();
    // out.flush();
    // out.sync();
    // out.close();
  }
}

