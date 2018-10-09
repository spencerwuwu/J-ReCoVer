// https://searchcode.com/api/result/114133748/

/**
 * hcw29 --> 92
 * wmin = 0.092
 * wmax = 0.992
 * Lmax = 0.000192
 * Lmax test1.txt = 0.15
 */

package project2;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class VertexFilter {
  public static final double wmin = 0.092;
  public static final double wmax = 0.992;

  public static class TokenizerMapper extends
      Mapper<Object, Text, Text, IntWritable> {

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString(), "\\n");
      String currentLine;
      String[] currentLinePieces;
      double w;
      while (itr.hasMoreTokens()) {
        currentLine = itr.nextToken();
        currentLinePieces = currentLine.split("\\s");
        w = new Double(currentLinePieces[2]);
        if ((w <= wmax) && (w >= wmin)) {
          word.set(currentLine);
          context.write(word, one);
        }
      }
    }
  }

  public static class IntSumReducer extends
      Reducer<Text, IntWritable, Text, IntWritable> {
    private IntWritable result = new IntWritable(0);

    public void reduce(Text key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      context.getCounter(Project2.ProjectCounters.VERTICES).increment(1);
      context.write(key, result);
    }
  }

  public static Counters run() throws Exception {
    Configuration conf = new Configuration();
    Job job = new Job(conf, "vertex");
    job.setJarByClass(VertexFilter.class);
    job.setMapperClass(TokenizerMapper.class);
    //job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path("../input"));
    FileOutputFormat.setOutputPath(job, new Path("../output/vertexFilter"));
    job.waitForCompletion(true);
    return job.getCounters();
    //System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}

