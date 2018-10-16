// https://searchcode.com/api/result/105853923/

package com.dburger.hadoop.anagram;

import java.io.IOException;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class AnagramCount extends Configured implements Tool {

    public static class Map extends Mapper<LongWritable, Text, Text,
            IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private Text newKey = new Text();
        public void map(LongWritable key, Text value,
                Context context)
                throws IOException, InterruptedException {
            char[] chars = value.toString().toCharArray();
            Arrays.sort(chars);
            newKey.set(new String(chars));
            context.write(newKey, one);
        }
    }

    public static class Reduce extends Reducer<Text, IntWritable, Text,
            IntWritable> {
        public void reduce(Text key, Iterator<IntWritable> values,
                Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            while (values.hasNext()) {
                sum += values.next().get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public int run(String[] args) throws Exception {
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);

        Job job = new Job(getConf(), "Anagram Counter");

        job.setJarByClass(AnagramCount.class);

        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setMapperClass(Map.class);
        job.setCombinerClass(Reduce.class);
        job.setReducerClass(Reduce.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        return job.waitForCompletion(true) ? 0 : -1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new AnagramCount(), args);
        System.exit(res);
    }
}

