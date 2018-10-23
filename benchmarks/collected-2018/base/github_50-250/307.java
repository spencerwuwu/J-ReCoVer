// https://searchcode.com/api/result/98877468/

/*==========================================================================
 * Copyright (c) 2014 Pivotal Software Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright
 * notices and license terms. Your use of these subcomponents is subject to
 * the terms and conditions of the subcomponent's license, as noted in the
 * LICENSE file.
 *==========================================================================
 */

package com.pivotal.gfxd.demo.mapreduce;

import com.pivotal.gemfirexd.hadoop.mapreduce.Key;
import com.pivotal.gemfirexd.hadoop.mapreduce.Row;
import com.pivotal.gemfirexd.hadoop.mapreduce.RowInputFormat;
import com.pivotal.gemfirexd.hadoop.mapreduce.RowOutputFormat;
import com.pivotal.gemfirexd.internal.engine.GfxdDataSerializable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author William Markito
 */
public class LoadAverage extends Configured implements Tool {

  public static class LoadAverageMapper extends Mapper<Object, Row, Text, LoadKey> {

    public void map(Object key, Row row,
        Context context) throws IOException, InterruptedException {

      try {

        ResultSet rs = row.getRowAsResultSet();

        LoadKey out = new LoadKey();

        out.setHousehold_id(rs.getInt("household_id"));
        out.setHouse_id(rs.getInt("house_id"));
        out.setWeekday(rs.getInt("weekday"));
        out.setPlug_id(rs.getInt("plug_id"));
        out.setTime_slice(rs.getInt("time_slice"));
        out.setValue(rs.getDouble("value"));
        out.setEvent_count(1);

        StringBuilder sb = new StringBuilder();
        sb.append(out.getWeekday());
        sb.append("-");
        sb.append(out.getTime_slice());
        sb.append("-");
        sb.append(out.getPlug_id());

        Text outKey = new Text();
        outKey.set(sb.toString());
        context.write(outKey, out);

      } catch (SQLException sqex) {
        sqex.printStackTrace();
      }

    }
  }

  public static class LoadAverageReducer extends
      Reducer<Text, LoadKey, Key, LoadAverageModel> {

    public void reduce(Text key, Iterable<LoadKey> values,
        Context context) throws IOException, InterruptedException {

      double valueSum = 0;
      int numEvents = 0;
      LoadKey loadKey = null;
      for (LoadKey model : values) {
        valueSum = model.getValue() + valueSum;
        numEvents = model.getEvent_count() + numEvents;

        if (loadKey == null) {
          loadKey = model;
        }
      }

      LoadAverageModel result = new LoadAverageModel(loadKey.getHouse_id(),
          loadKey.getHousehold_id(),
          loadKey.getPlug_id(), loadKey.getWeekday(), loadKey.getTime_slice(),
          valueSum,
          numEvents);

      context.write(new Key(), result);
    }
  }

  /**
   * This method is assuming fs.default.name as args[0]
   *
   * @param args
   * @return
   * @throws Exception
   */
  @Override
  public int run(String[] args) throws Exception {
    System.out.println("Starting MapReduce Job");
    GfxdDataSerializable.initTypes();
    Configuration conf = new Configuration();
    //Configuration conf = getConf();

    Path outputPath = new Path("/output");
    String hdfsHomeDir = "/sensorStore"; //args[1];
    String tableName = "RAW_SENSOR";
    String outTableName = "LOAD_AVERAGES_SHADOW";
    String gfxdURL = conf.get("gemfirexd.url",
        "jdbc:gemfirexd://localhost:1527");

    // conf.set("fs.default.name", args[0]);
    String hdfsUrl = conf.get("fs.defaultFS");

    FileSystem hdfs = FileSystem.get(new URI(hdfsUrl), conf);

    // Retrieve last run timestamp
    long now = System.currentTimeMillis();
    long lastStart = getLastStart(hdfs);

    outputPath.getFileSystem(conf).delete(outputPath, true);

    conf.set(RowInputFormat.HOME_DIR, hdfsHomeDir);
    conf.set(RowInputFormat.INPUT_TABLE, tableName);
    conf.setBoolean(RowInputFormat.CHECKPOINT_MODE, false);
    conf.setLong(RowInputFormat.START_TIME_MILLIS, lastStart);
    conf.setLong(RowInputFormat.END_TIME_MILLIS, now);

    conf.set(RowOutputFormat.OUTPUT_URL, gfxdURL);
    conf.set(RowOutputFormat.OUTPUT_TABLE, outTableName);

    // print config to troubleshoot possible issues
    // Configuration.dumpConfiguration(conf, new PrintWriter(System.out));

    Job job = Job.getInstance(conf, "LoadAverage");

    job.setNumReduceTasks(1);

    job.setInputFormatClass(RowInputFormat.class);

    // configure mapper and reducer
    job.setJarByClass(LoadAverage.class);
    job.setMapperClass(LoadAverageMapper.class);
    job.setReducerClass(LoadAverageReducer.class);

    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(LoadKey.class);

    TextOutputFormat.setOutputPath(job, outputPath);
    job.setOutputFormatClass(RowOutputFormat.class);
    job.setOutputKeyClass(Key.class);
    job.setOutputValueClass(LoadAverageModel.class);

    boolean jobSuccess = job.waitForCompletion(true);
    if (jobSuccess) {
      writeLastStart(hdfs, now);
    }

    return jobSuccess ? 0 : 1;
  }

  private long getLastStart(FileSystem hdfs) throws IOException {
    long lastStart = 0;
    Path file = new Path("/sensorStore/last_mapreduce_timestamp");
    if (hdfs.exists(file)) {
      BufferedReader br = new BufferedReader(
          new InputStreamReader(hdfs.open(file)));
      String line = br.readLine();
      if (line != null && !line.isEmpty()) {
        lastStart = Long.parseLong(line);
      }
    }
    return lastStart;
  }

  private void writeLastStart(FileSystem hdfs,
      long timestamp) throws IOException {
    Path file = new Path("/sensorStore/last_mapreduce_timestamp");
    OutputStream os = hdfs.create(file, true);
    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(os));
    br.write(Long.toString(timestamp));
    br.close();
  }

  public static void main(String[] args) throws Exception {
    // only for testing
    int rc = ToolRunner.run(new LoadAverage(), args);

    System.out.println("Job completed. Return code:" + rc);
    System.exit(rc);
  }

}

