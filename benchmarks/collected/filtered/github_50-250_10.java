// https://searchcode.com/api/result/74613535/

package edu.cs.iit.cs495;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import java.util.regex.*;
import java.util.*;
import java.io.IOException;

public class WordCountMR {
        public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
           private final static IntWritable one = new IntWritable(1);
           private Text word = new Text();
           public void map (LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
              // Map function goes here
              StringTokenizer st = new StringTokenizer(value.toString());
              Pattern w = Pattern.compile("\\b([\\w\\'\\-]+)\\b");
              while (st.hasMoreTokens()) {
                 Matcher m = w.matcher(st.nextToken());
                 if (m.matches()){
                    word.set(m.group(1).toLowerCase());
                    output.collect(word, one);
                 }
              }
           }
        }

        public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
           public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
              int sum = 0;
              while (values.hasNext()){
                 sum += values.next().get();
              }
              output.collect(key, new IntWritable(sum));
           }
        }
        public static void main(String[] args) throws Exception {
           JobConf conf = new JobConf(WordCountMR.class);
           conf.setJobName("wordcount");
           conf.setOutputKeyClass(Text.class);
           conf.setOutputValueClass(IntWritable.class);

           conf.setMapperClass(Map.class);
           conf.setReducerClass(Reduce.class);
           conf.setCombinerClass(Reduce.class);

           conf.setInputFormat(TextInputFormat.class);
           conf.setOutputFormat(TextOutputFormat.class);

           FileInputFormat.setInputPaths(conf, new Path(args[0]));
           FileOutputFormat.setOutputPath(conf, new Path(args[1]));

           JobClient.runJob(conf);
        }
}

