// https://searchcode.com/api/result/60247032/

package org.myorg;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;


public class NodeIterator {

  public static class TokenizerMapper 
       extends Mapper<Object, Text, Text, Text>{
    
    private Text node1 = new Text();
    private Text node2 = new Text();
      
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      node1.set(itr.nextToken());
      node2.set(itr.nextToken());
      context.write(node1, node2);
    }
  }
  
  public static class DupRemovingReducer 
       extends Reducer<Text,Text,Text,Text> {

    public void reduce(Text key, Iterable<Text> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      for (Text val : values) {
        if (val.compareTo(key) > 0)
          context.write(key, val);
      }
    }
  }


  public static class Identity 
       extends Mapper<Object, Text, Text, Text>{
    
    private Text node1 = new Text();
    private Text node2 = new Text();
      
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      node1.set(itr.nextToken());
      node2.set(itr.nextToken());
      context.write(node1, node2);
    }
  }
  
  public static class PathGenReducer 
       extends Reducer<Text,Text,Text,Text> {

    private final static Text none = new Text("none");

    public void reduce(Text key, Iterable<Text> values, 
                       Context context
                       ) throws IOException, InterruptedException {

      List<Text> cache = new ArrayList<Text>();
      for (Text val : values) {
        Text pair = new Text( key + "," + val );
        context.write(pair,none);
        Text writable = new Text(val.toString());
        cache.add(writable);
      }

      for (Text val1 : cache) {
        for (Text val2 : cache) {
          if (val1.compareTo(val2) < 0) {
            Text pair = new Text( val1 + "," + val2 );
            context.write(pair, key);
          }
        }
      }
    }
  }

  public static class EdgeClosingReducer 
       extends Reducer<Text,Text,Text,Text> {

    private final static Text one = new Text("1");

    public void reduce(Text key, Iterable<Text> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      StringTokenizer st = new StringTokenizer(key.toString(), ",");
      List<Text> cache = new ArrayList<Text>();
      Text left = new Text(st.nextToken());
      Text right = new Text(st.nextToken());

      for (Text val : values) {
        Text writable = new Text(val.toString());
        cache.add(writable);
      }

      for (Text val : cache) {
        if (val.toString().equals("none")) {
          for (Text valTemp: cache) {
            if (!valTemp.toString().equals("none")) {
              context.write(left, one);
              context.write(right, one);
              context.write(valTemp, one);
            }
            break;
          }
        } 
      }
    }
  }


  public static class NodeCountReducer
       extends Reducer<Text, Text, Text, Text>{
    public void reduce(Text key, Iterator<Text> values, Context context)
           throws IOException, InterruptedException {

      int sum = 0;
      while (values.hasNext()) {
        sum += Integer.parseInt(values.next().toString());
      }
      context.write(key, new Text((new Integer(sum)).toString()));
    }
  }


  public static void main(String[] args) throws Exception {

    Job job1 = new Job(new Configuration(), "round 1");
    Job job2 = new Job(new Configuration(), "round 2");
    Job job3 = new Job(new Configuration(), "round 3");
    Job job4 = new Job(new Configuration(), "round 4");

    job1.setJarByClass(NodeIterator.class);
    job1.setMapperClass(TokenizerMapper.class);
    job1.setCombinerClass(DupRemovingReducer.class);
    job1.setReducerClass(DupRemovingReducer.class);
    job1.setOutputKeyClass(Text.class);
    job1.setOutputValueClass(Text.class);
    // job1.setNumReduceTasks(8);
    // job1.setNumMapTasks(2);
    FileInputFormat.addInputPath(job1, new Path(args[0]));
    FileOutputFormat.setOutputPath(job1, new Path(args[1] + "/temp1"));

    job2.setJarByClass(NodeIterator.class);
    job2.setMapperClass(Identity.class);
    // job2.setCombinerClass(PathGenReducer.class);
    job2.setReducerClass(PathGenReducer.class);
    job2.setOutputKeyClass(Text.class);
    job2.setOutputValueClass(Text.class);
    // job2.setNumReduceTasks(8);
    // job2.setNumMapTasks(2);
    FileInputFormat.addInputPath(job2, new Path(args[1] + "/temp1"));
    FileOutputFormat.setOutputPath(job2, new Path(args[1] + "/temp2"));

    job3.setJarByClass(NodeIterator.class);
    job3.setMapperClass(Identity.class);
    // job3.setCombinerClass(EdgeClosingReducer.class);
    job3.setReducerClass(EdgeClosingReducer.class);
    job3.setOutputKeyClass(Text.class);
    job3.setOutputValueClass(Text.class);
    // job3.setNumReduceTasks(8);
    // job3.setNumMapTasks(2);
    FileInputFormat.addInputPath(job3, new Path(args[1] + "/temp2"));
    FileOutputFormat.setOutputPath(job3, new Path(args[1] + "/temp3"));

    job4.setJarByClass(NodeIterator.class);
    job4.setMapperClass(Identity.class);
    // job4.setCombinerClass(EdgeClosingReducer.class);
    job4.setReducerClass(NodeCountReducer.class);
    job4.setOutputKeyClass(Text.class);
    job4.setOutputValueClass(Text.class);
    // job4.setNumReduceTasks(8);
    // job4.setNumMapTasks(2);
    FileInputFormat.addInputPath(job4, new Path(args[1] + "/temp3"));
    FileOutputFormat.setOutputPath(job4, new Path(args[1] + "/temp4"));

    job1.waitForCompletion(true);
    job2.waitForCompletion(true);
    job3.waitForCompletion(true);
    System.exit(job4.waitForCompletion(true) ? 0 : 1);
  }
}

