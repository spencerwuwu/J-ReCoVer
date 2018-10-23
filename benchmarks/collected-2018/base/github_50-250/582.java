// https://searchcode.com/api/result/64792685/

package org.action;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class PatentHistogram extends Configured implements Tool {

  /**
   * The mapper implementation
   */
  public static class MapperInternal extends Mapper<Text, Text, IntWritable, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private IntWritable count = new IntWritable();
   
    public void map(Text key, Text value, Context context)
        throws IOException, InterruptedException {
        count.set(Integer.parseInt(value.toString()));
        context.write(count, one);
    }
  }
 
  /**
   * The reducer implementation
   */
  public static class ReducerInternal extends Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {

    public void reduce(IntWritable key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {

        int count = 0;
        for (IntWritable value : values) {
            count += value.get();
        }
        context.write(key, new IntWritable(count));
    }
  }

  /**
   * The tool runner
   */
  public int run(String[] args) throws Exception {
    Configuration conf = getConf();

    Job job = new Job(conf, "PatentHistogram");
    job.setJarByClass(PatentHistogram.class);

    job.setMapperClass(MapperInternal.class);
    job.setReducerClass(ReducerInternal.class);

    FileInputFormat.setInputPaths(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));

    job.setInputFormatClass(KeyValueTextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(IntWritable.class);

    return job.waitForCompletion(true) ? 1 : 0;
  }

  public static void main(String[] args) throws Exception {
      int res = ToolRunner.run(new Configuration(), new PatentHistogram(), args);
      System.exit(res);
  }
}

