// https://searchcode.com/api/result/92487761/

/**
 * Copyright 2008 The Apache Software Foundation
 *
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
package org.apache.hadoop.hbase.mapreduce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.hadoopbackport.JarFinder;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.zookeeper.ZKUtil;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.StringUtils;

/**
 * Utility for {@link TableMapper} and {@link TableReducer}
 */
@SuppressWarnings("unchecked")
public class TableMapReduceUtil {
  static Log LOG = LogFactory.getLog(TableMapReduceUtil.class);
  
  /**
   * Use this before submitting a TableMap job. It will appropriately set up
   * the job.
   *
   * @param table  The table name to read from.
   * @param scan  The scan instance with the columns, time range etc.
   * @param mapper  The mapper class to use.
   * @param outputKeyClass  The class of the output key.
   * @param outputValueClass  The class of the output value.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(String table, Scan scan,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job)
  throws IOException {
    initTableMapperJob(table, scan, mapper, outputKeyClass, outputValueClass,
        job, true);
  }


  /**
   * Use this before submitting a TableMap job. It will appropriately set up
   * the job.
   *
   * @param table Binary representation of the table name to read from.
   * @param scan  The scan instance with the columns, time range etc.
   * @param mapper  The mapper class to use.
   * @param outputKeyClass  The class of the output key.
   * @param outputValueClass  The class of the output value.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @throws IOException When setting up the details fails.
   */
   public static void initTableMapperJob(byte[] table, Scan scan,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job)
  throws IOException {
      initTableMapperJob(Bytes.toString(table), scan, mapper, outputKeyClass, outputValueClass,
              job, true);
  }

  /**
   * Use this before submitting a TableMap job. It will appropriately set up
   * the job.
   *
   * @param table  The table name to read from.
   * @param scan  The scan instance with the columns, time range etc.
   * @param mapper  The mapper class to use.
   * @param outputKeyClass  The class of the output key.
   * @param outputValueClass  The class of the output value.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @param addDependencyJars upload HBase jars and jars for any of the configured
   *           job classes via the distributed cache (tmpjars).
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(String table, Scan scan,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job,
      boolean addDependencyJars, Class<? extends InputFormat> inputFormatClass)
  throws IOException {
    job.setInputFormatClass(inputFormatClass);
    if (outputValueClass != null) job.setMapOutputValueClass(outputValueClass);
    if (outputKeyClass != null) job.setMapOutputKeyClass(outputKeyClass);
    job.setMapperClass(mapper);
    Configuration conf = job.getConfiguration();
    HBaseConfiguration.merge(conf, HBaseConfiguration.create(conf));
    conf.set(TableInputFormat.INPUT_TABLE, table);
    conf.set(TableInputFormat.SCAN, convertScanToString(scan));
    if (addDependencyJars) {
      addDependencyJars(job);
    }
    initCredentials(job);
  }
  
  /**
   * Use this before submitting a TableMap job. It will appropriately set up
   * the job.
   *
   * @param table Binary representation of the table name to read from.
   * @param scan  The scan instance with the columns, time range etc.
   * @param mapper  The mapper class to use.
   * @param outputKeyClass  The class of the output key.
   * @param outputValueClass  The class of the output value.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @param addDependencyJars upload HBase jars and jars for any of the configured
   *           job classes via the distributed cache (tmpjars).
   * @param inputFormatClass The class of the input format
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(byte[] table, Scan scan,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job,
      boolean addDependencyJars, Class<? extends InputFormat> inputFormatClass)
  throws IOException {
      initTableMapperJob(Bytes.toString(table), scan, mapper, outputKeyClass,
              outputValueClass, job, addDependencyJars, inputFormatClass);
  }
  
  /**
   * Use this before submitting a TableMap job. It will appropriately set up
   * the job.
   *
   * @param table Binary representation of the table name to read from.
   * @param scan  The scan instance with the columns, time range etc.
   * @param mapper  The mapper class to use.
   * @param outputKeyClass  The class of the output key.
   * @param outputValueClass  The class of the output value.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @param addDependencyJars upload HBase jars and jars for any of the configured
   *           job classes via the distributed cache (tmpjars).
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(byte[] table, Scan scan,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job,
      boolean addDependencyJars)
  throws IOException {
      initTableMapperJob(Bytes.toString(table), scan, mapper, outputKeyClass,
              outputValueClass, job, addDependencyJars, TableInputFormat.class);
  }
  
  /**
   * Use this before submitting a TableMap job. It will appropriately set up
   * the job.
   *
   * @param table The table name to read from.
   * @param scan  The scan instance with the columns, time range etc.
   * @param mapper  The mapper class to use.
   * @param outputKeyClass  The class of the output key.
   * @param outputValueClass  The class of the output value.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @param addDependencyJars upload HBase jars and jars for any of the configured
   *           job classes via the distributed cache (tmpjars).
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(String table, Scan scan,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job,
      boolean addDependencyJars)
  throws IOException {
      initTableMapperJob(table, scan, mapper, outputKeyClass,
              outputValueClass, job, addDependencyJars, TableInputFormat.class);
  }
  
  /**
   * Use this before submitting a Multi TableMap job. It will appropriately set
   * up the job.
   *
   * @param scans The list of {@link Scan} objects to read from.
   * @param mapper The mapper class to use.
   * @param outputKeyClass The class of the output key.
   * @param outputValueClass The class of the output value.
   * @param job The current job to adjust. Make sure the passed job is carrying
   *          all necessary HBase configuration.
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(List<Scan> scans,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job) throws IOException {
    initTableMapperJob(scans, mapper, outputKeyClass, outputValueClass, job,
        true);
  }

  /**
   * Use this before submitting a Multi TableMap job. It will appropriately set
   * up the job.
   *
   * @param scans The list of {@link Scan} objects to read from.
   * @param mapper The mapper class to use.
   * @param outputKeyClass The class of the output key.
   * @param outputValueClass The class of the output value.
   * @param job The current job to adjust. Make sure the passed job is carrying
   *          all necessary HBase configuration.
   * @param addDependencyJars upload HBase jars and jars for any of the
   *          configured job classes via the distributed cache (tmpjars).
   * @throws IOException When setting up the details fails.
   */
  public static void initTableMapperJob(List<Scan> scans,
      Class<? extends TableMapper> mapper,
      Class<? extends WritableComparable> outputKeyClass,
      Class<? extends Writable> outputValueClass, Job job,
      boolean addDependencyJars) throws IOException {
    job.setInputFormatClass(MultiTableInputFormat.class);
    if (outputValueClass != null) {
      job.setMapOutputValueClass(outputValueClass);
    }
    if (outputKeyClass != null) {
      job.setMapOutputKeyClass(outputKeyClass);
    }
    job.setMapperClass(mapper);
    HBaseConfiguration.addHbaseResources(job.getConfiguration());
    List<String> scanStrings = new ArrayList<String>();

    for (Scan scan : scans) {
      scanStrings.add(convertScanToString(scan));
    }
    job.getConfiguration().setStrings(MultiTableInputFormat.SCANS,
      scanStrings.toArray(new String[scanStrings.size()]));

    if (addDependencyJars) {
      addDependencyJars(job);
    }
  }

  public static void initCredentials(Job job) throws IOException {
    if (User.isHBaseSecurityEnabled(job.getConfiguration())) {
      try {
        // init credentials for remote cluster
        String quorumAddress = job.getConfiguration().get(
            TableOutputFormat.QUORUM_ADDRESS);
        if (quorumAddress != null) {
          String[] parts = ZKUtil.transformClusterKey(quorumAddress);
          Configuration peerConf = HBaseConfiguration.create(job
              .getConfiguration());
          peerConf.set(HConstants.ZOOKEEPER_QUORUM, parts[0]);
          peerConf.set("hbase.zookeeper.client.port", parts[1]);
          peerConf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, parts[2]);
          User.getCurrent().obtainAuthTokenForJob(peerConf, job);
        }
        
        User.getCurrent().obtainAuthTokenForJob(job.getConfiguration(), job);
      } catch (InterruptedException ie) {
        LOG.info("Interrupted obtaining user authentication token");
        Thread.interrupted();
      }
    }
  }

  /**
   * Writes the given scan into a Base64 encoded string.
   *
   * @param scan  The scan to write out.
   * @return The scan saved in a Base64 encoded string.
   * @throws IOException When writing the scan fails.
   */
  static String convertScanToString(Scan scan) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(out);
    scan.write(dos);
    return Base64.encodeBytes(out.toByteArray());
  }

  /**
   * Converts the given Base64 string back into a Scan instance.
   *
   * @param base64  The scan details.
   * @return The newly created Scan instance.
   * @throws IOException When reading the scan instance fails.
   */
  static Scan convertStringToScan(String base64) throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(base64));
    DataInputStream dis = new DataInputStream(bis);
    Scan scan = new Scan();
    scan.readFields(dis);
    return scan;
  }

  /**
   * Use this before submitting a TableReduce job. It will
   * appropriately set up the JobConf.
   *
   * @param table  The output table.
   * @param reducer  The reducer class to use.
   * @param job  The current job to adjust.
   * @throws IOException When determining the region count fails.
   */
  public static void initTableReducerJob(String table,
    Class<? extends TableReducer> reducer, Job job)
  throws IOException {
    initTableReducerJob(table, reducer, job, null);
  }

  /**
   * Use this before submitting a TableReduce job. It will
   * appropriately set up the JobConf.
   *
   * @param table  The output table.
   * @param reducer  The reducer class to use.
   * @param job  The current job to adjust.
   * @param partitioner  Partitioner to use. Pass <code>null</code> to use
   * default partitioner.
   * @throws IOException When determining the region count fails.
   */
  public static void initTableReducerJob(String table,
    Class<? extends TableReducer> reducer, Job job,
    Class partitioner) throws IOException {
    initTableReducerJob(table, reducer, job, partitioner, null, null, null);
  }

  /**
   * Use this before submitting a TableReduce job. It will
   * appropriately set up the JobConf.
   *
   * @param table  The output table.
   * @param reducer  The reducer class to use.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @param partitioner  Partitioner to use. Pass <code>null</code> to use
   * default partitioner.
   * @param quorumAddress Distant cluster to write to; default is null for
   * output to the cluster that is designated in <code>hbase-site.xml</code>.
   * Set this String to the zookeeper ensemble of an alternate remote cluster
   * when you would have the reduce write a cluster that is other than the
   * default; e.g. copying tables between clusters, the source would be
   * designated by <code>hbase-site.xml</code> and this param would have the
   * ensemble address of the remote cluster.  The format to pass is particular.
   * Pass <code> &lt;hbase.zookeeper.quorum>:&lt;hbase.zookeeper.client.port>:&lt;zookeeper.znode.parent>
   * </code> such as <code>server,server2,server3:2181:/hbase</code>.
   * @param serverClass redefined hbase.regionserver.class
   * @param serverImpl redefined hbase.regionserver.impl
   * @throws IOException When determining the region count fails.
   */
  public static void initTableReducerJob(String table,
    Class<? extends TableReducer> reducer, Job job,
    Class partitioner, String quorumAddress, String serverClass,
    String serverImpl) throws IOException {
    initTableReducerJob(table, reducer, job, partitioner, quorumAddress,
        serverClass, serverImpl, true);
  }

  /**
   * Use this before submitting a TableReduce job. It will
   * appropriately set up the JobConf.
   *
   * @param table  The output table.
   * @param reducer  The reducer class to use.
   * @param job  The current job to adjust.  Make sure the passed job is
   * carrying all necessary HBase configuration.
   * @param partitioner  Partitioner to use. Pass <code>null</code> to use
   * default partitioner.
   * @param quorumAddress Distant cluster to write to; default is null for
   * output to the cluster that is designated in <code>hbase-site.xml</code>.
   * Set this String to the zookeeper ensemble of an alternate remote cluster
   * when you would have the reduce write a cluster that is other than the
   * default; e.g. copying tables between clusters, the source would be
   * designated by <code>hbase-site.xml</code> and this param would have the
   * ensemble address of the remote cluster.  The format to pass is particular.
   * Pass <code> &lt;hbase.zookeeper.quorum>:&lt;hbase.zookeeper.client.port>:&lt;zookeeper.znode.parent>
   * </code> such as <code>server,server2,server3:2181:/hbase</code>.
   * @param serverClass redefined hbase.regionserver.class
   * @param serverImpl redefined hbase.regionserver.impl
   * @param addDependencyJars upload HBase jars and jars for any of the configured
   *           job classes via the distributed cache (tmpjars).
   * @throws IOException When determining the region count fails.
   */
  public static void initTableReducerJob(String table,
    Class<? extends TableReducer> reducer, Job job,
    Class partitioner, String quorumAddress, String serverClass,
    String serverImpl, boolean addDependencyJars) throws IOException {

    Configuration conf = job.getConfiguration();    
    HBaseConfiguration.merge(conf, HBaseConfiguration.create(conf));
    job.setOutputFormatClass(TableOutputFormat.class);
    if (reducer != null) job.setReducerClass(reducer);
    conf.set(TableOutputFormat.OUTPUT_TABLE, table);
    // If passed a quorum/ensemble address, pass it on to TableOutputFormat.
    if (quorumAddress != null) {
      // Calling this will validate the format
      ZKUtil.transformClusterKey(quorumAddress);
      conf.set(TableOutputFormat.QUORUM_ADDRESS,quorumAddress);
    }
    if (serverClass != null && serverImpl != null) {
      conf.set(TableOutputFormat.REGION_SERVER_CLASS, serverClass);
      conf.set(TableOutputFormat.REGION_SERVER_IMPL, serverImpl);
    }
    job.setOutputKeyClass(ImmutableBytesWritable.class);
    job.setOutputValueClass(Writable.class);
    if (partitioner == HRegionPartitioner.class) {
      job.setPartitionerClass(HRegionPartitioner.class);
      HTable outputTable = new HTable(conf, table);
      int regions = outputTable.getRegionsInfo().size();
      if (job.getNumReduceTasks() > regions) {
        job.setNumReduceTasks(outputTable.getRegionsInfo().size());
      }
    } else if (partitioner != null) {
      job.setPartitionerClass(partitioner);
    }

    if (addDependencyJars) {
      addDependencyJars(job);
    }

    initCredentials(job);
  }

  /**
   * Ensures that the given number of reduce tasks for the given job
   * configuration does not exceed the number of regions for the given table.
   *
   * @param table  The table to get the region count for.
   * @param job  The current job to adjust.
   * @throws IOException When retrieving the table details fails.
   */
  public static void limitNumReduceTasks(String table, Job job)
  throws IOException {
    HTable outputTable = new HTable(job.getConfiguration(), table);
    int regions = outputTable.getRegionsInfo().size();
    if (job.getNumReduceTasks() > regions)
      job.setNumReduceTasks(regions);
  }

  /**
   * Sets the number of reduce tasks for the given job configuration to the
   * number of regions the given table has.
   *
   * @param table  The table to get the region count for.
   * @param job  The current job to adjust.
   * @throws IOException When retrieving the table details fails.
   */
  public static void setNumReduceTasks(String table, Job job)
  throws IOException {
    HTable outputTable = new HTable(job.getConfiguration(), table);
    int regions = outputTable.getRegionsInfo().size();
    job.setNumReduceTasks(regions);
  }

  /**
   * Sets the number of rows to return and cache with each scanner iteration.
   * Higher caching values will enable faster mapreduce jobs at the expense of
   * requiring more heap to contain the cached rows.
   *
   * @param job The current job to adjust.
   * @param batchSize The number of rows to return in batch with each scanner
   * iteration.
   */
  public static void setScannerCaching(Job job, int batchSize) {
    job.getConfiguration().setInt("hbase.client.scanner.caching", batchSize);
  }

  /**
   * Add the HBase dependency jars as well as jars for any of the configured
   * job classes to the job configuration, so that JobClient will ship them
   * to the cluster and add them to the DistributedCache.
   */
  public static void addDependencyJars(Job job) throws IOException {
    try {
      addDependencyJars(job.getConfiguration(),
          org.apache.zookeeper.ZooKeeper.class,
          com.google.protobuf.Message.class,
          com.google.common.collect.ImmutableSet.class,
          org.apache.hadoop.hbase.util.Bytes.class, //one class from hbase.jar
          job.getMapOutputKeyClass(),
          job.getMapOutputValueClass(),
          job.getInputFormatClass(),
          job.getOutputKeyClass(),
          job.getOutputValueClass(),
          job.getOutputFormatClass(),
          job.getPartitionerClass(),
          job.getCombinerClass());
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }    
  }
  
  /**
   * Add the jars containing the given classes to the job's configuration
   * such that JobClient will ship them to the cluster and add them to
   * the DistributedCache.
   */
  public static void addDependencyJars(Configuration conf,
      Class<?>... classes) throws IOException {

    FileSystem localFs = FileSystem.getLocal(conf);
    Set<String> jars = new HashSet<String>();
    // Add jars that are already in the tmpjars variable
    jars.addAll(conf.getStringCollection("tmpjars"));

    // add jars as we find them to a map of contents jar name so that we can avoid
    // creating new jars for classes that have already been packaged.
    Map<String, String> packagedClasses = new HashMap<String, String>();

    // Add jars containing the specified classes
    for (Class<?> clazz : classes) {
      if (clazz == null) continue;

      Path path = findOrCreateJar(clazz, localFs, packagedClasses);
      if (path == null) {
        LOG.warn("Could not find jar for class " + clazz +
                 " in order to ship it to the cluster.");
        continue;
      }
      if (!localFs.exists(path)) {
        LOG.warn("Could not validate jar file " + path + " for class "
                 + clazz);
        continue;
      }
      jars.add(path.toString());
    }
    if (jars.isEmpty()) return;

    conf.set("tmpjars",
             StringUtils.arrayToString(jars.toArray(new String[0])));
  }

  /**
   * If org.apache.hadoop.util.JarFinder is available (0.23+ hadoop), finds
   * the Jar for a class or creates it if it doesn't exist. If the class is in
   * a directory in the classpath, it creates a Jar on the fly with the
   * contents of the directory and returns the path to that Jar. If a Jar is
   * created, it is created in the system temporary directory. Otherwise,
   * returns an existing jar that contains a class of the same name. Maintains
   * a mapping from jar contents to the tmp jar created.
   * @param my_class the class to find.
   * @param fs the FileSystem with which to qualify the returned path.
   * @param packagedClasses a map of class name to path.
   * @return a jar file that contains the class.
   * @throws IOException
   */
  private static Path findOrCreateJar(Class<?> my_class, FileSystem fs,
      Map<String, String> packagedClasses)
  throws IOException {
    // attempt to locate an existing jar for the class.
    String jar = findContainingJar(my_class, packagedClasses);
    if (null == jar || jar.isEmpty()) {
      jar = getJar(my_class);
      updateMap(jar, packagedClasses);
    }

    if (null == jar || jar.isEmpty()) {
      throw new IOException("Cannot locate resource for class " + my_class.getName());
    }

    LOG.debug(String.format("For class %s, using jar %s", my_class.getName(), jar));
    return new Path(jar).makeQualified(fs);
  }

  /**
   * Add entries to <code>packagedClasses</code> corresponding to class files
   * contained in <code>jar</code>.
   * @param jar The jar who's content to list.
   * @param packagedClasses map[class -> jar]
   */
  private static void updateMap(String jar, Map<String, String> packagedClasses) throws IOException {
    ZipFile zip = null;
    try {
      zip = new ZipFile(jar);
      for (Enumeration<? extends ZipEntry> iter = zip.entries(); iter.hasMoreElements();) {
        ZipEntry entry = iter.nextElement();
        if (entry.getName().endsWith("class")) {
          packagedClasses.put(entry.getName(), jar);
        }
      }
    } finally {
      if (null != zip) zip.close();
    }
  }

  /**
   * Find a jar that contains a class of the same name, if any. It will return
   * a jar file, even if that is not the first thing on the class path that
   * has a class with the same name. Looks first on the classpath and then in
   * the <code>packagedClasses</code> map.
   * @param my_class the class to find.
   * @return a jar file that contains the class, or null.
   * @throws IOException
   */
  private static String findContainingJar(Class<?> my_class, Map<String, String> packagedClasses)
      throws IOException {
    ClassLoader loader = my_class.getClassLoader();
    String class_file = my_class.getName().replaceAll("\\.", "/") + ".class";

    // first search the classpath
    for (Enumeration<URL> itr = loader.getResources(class_file); itr.hasMoreElements();) {
      URL url = itr.nextElement();
      if ("jar".equals(url.getProtocol())) {
        String toReturn = url.getPath();
        if (toReturn.startsWith("file:")) {
          toReturn = toReturn.substring("file:".length());
        }
        // URLDecoder is a misnamed class, since it actually decodes
        // x-www-form-urlencoded MIME type rather than actual
        // URL encoding (which the file path has). Therefore it would
        // decode +s to ' 's which is incorrect (spaces are actually
        // either unencoded or encoded as "%20"). Replace +s first, so
        // that they are kept sacred during the decoding process.
        toReturn = toReturn.replaceAll("\\+", "%2B");
        toReturn = URLDecoder.decode(toReturn, "UTF-8");
        return toReturn.replaceAll("!.*$", "");
      }
    }

    // now look in any jars we've packaged using JarFinder. Returns null when
    // no jar is found.
    return packagedClasses.get(class_file);
  }

  /**
   * Invoke 'getJar' on a JarFinder implementation. Useful for some job
   * configuration contexts (HBASE-8140) and also for testing on MRv2. First
   * check if we have HADOOP-9426. Lacking that, fall back to the backport.
   * @param my_class the class to find.
   * @return a jar file that contains the class, or null.
   */
  private static String getJar(Class<?> my_class) {
    String ret = null;
    String hadoopJarFinder = "org.apache.hadoop.util.JarFinder";
    Class<?> jarFinder = null;
    try {
      LOG.debug("Looking for " + hadoopJarFinder + ".");
      jarFinder = Class.forName(hadoopJarFinder);
      LOG.debug(hadoopJarFinder + " found.");
      Method getJar = jarFinder.getMethod("getJar", Class.class);
      ret = (String) getJar.invoke(null, my_class);
    } catch (ClassNotFoundException e) {
      LOG.debug("Using backported JarFinder.");
      ret = JarFinder.getJar(my_class);
    } catch (InvocationTargetException e) {
      // function was properly called, but threw it's own exception. Unwrap it
      // and pass it on.
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      // toss all other exceptions, related to reflection failure
      throw new RuntimeException("getJar invocation failed.", e);
    }

    return ret;
  }
}

