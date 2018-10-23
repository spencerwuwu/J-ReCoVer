// https://searchcode.com/api/result/65409178/

package org.myorg;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;

public class MapReduce extends Configured implements Tool {

   public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
     private final static IntWritable one = new IntWritable(1);
     Log4netLogParser log4netLogParser = new Log4netLogParser();

     public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
       String line = value.toString();
       String ip = log4netLogParser.GetIP(line);

       context.write(new Text(ip), one);
     }
   }

   public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
     public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {

       int sum = 0;
       for (IntWritable val : values) {
         sum += val.get();
       }
       context.write(key, new IntWritable(sum));
     }
   }

   public int run(String [] args) throws Exception {
     Job job = new Job(getConf());
     job.setJarByClass(MapReduce.class);
     job.setJobName("LogAnalyser");

     job.setOutputKeyClass(Text.class);
     job.setOutputValueClass(IntWritable.class);

     job.setMapperClass(Map.class);
     job.setCombinerClass(Reduce.class);
     job.setReducerClass(Reduce.class);

     job.setInputFormatClass(Log4netInputFormat.class);
     job.setOutputFormatClass(TextOutputFormat.class);

     FileInputFormat.setInputPaths(job, new Path(args[0]));
     FileOutputFormat.setOutputPath(job, new Path(args[1]));

     boolean success = job.waitForCompletion(true);
     return success ? 0 : 1;
   }

   public static void main(String[] args) throws Exception {
     int ret = ToolRunner.run(new MapReduce(), args);
     System.exit(ret);
   }
}
