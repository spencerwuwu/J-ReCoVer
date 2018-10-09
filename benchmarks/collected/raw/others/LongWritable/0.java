// https://searchcode.com/api/result/12375307/

package org.jeffkubina.hadoop.utils;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class OnlyOneTest
{
    public static class Map extends MapReduceBase implements Mapper<LongWritable, LongWritable, LongWritable, LongWritable>
    {
        private LongWritable jobId  = new LongWritable();
        private LongWritable totalJobs = new LongWritable();

        public void map(LongWritable key, LongWritable value, OutputCollector<LongWritable, LongWritable> output, Reporter reporter) throws IOException
        {
            long totalJobsLong = 100;
            totalJobs.set (totalJobsLong);
            for (long i = 0; i < totalJobsLong; i++)
            {
                jobId.set (i);
                totalJobs.set (i);
                output.collect(jobId, totalJobs);
            }
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<LongWritable, LongWritable, LongWritable, LongWritable>
    {
        public void reduce (LongWritable jobId, Iterator<LongWritable> values, OutputCollector<LongWritable, LongWritable> output, Reporter reporter) throws IOException
        {
            while(values.hasNext())
            {
                LongWritable totalJobs = values.next();
                output.collect(jobId, totalJobs);
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        JobConf conf = new JobConf(OnlyOneTest.class);
        conf.setNumReduceTasks(3);
        conf.setJobName ("My job with " + conf.get("mapred.reduce.tasks") + "reducers");
        conf.setNumTasksToExecutePerJvm (2);

        conf.setOutputKeyClass(LongWritable.class);
        conf.setOutputValueClass(LongWritable.class);

        conf.setMapperClass(Map.class);
        //conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(OnlyOneInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileOutputFormat.setOutputPath (conf, new Path ("/user/jmkubin/output"));

        JobClient.runJob(conf);
    }
}


