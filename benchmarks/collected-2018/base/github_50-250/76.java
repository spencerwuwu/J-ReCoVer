// https://searchcode.com/api/result/71330636/

package Mapred;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public final class ParserDriverReduce {

  public static class Reduce
      extends Reducer<Text, Text, Text, Text> {

    @Override
    protected void setup(
        Context context)
        throws IOException, InterruptedException {
      context.write(new Text("<All>"), null);
    }

    @Override
    protected void cleanup(
        Context context)
        throws IOException, InterruptedException {
      context.write(new Text("</All>"), null);
    }

    private Text outputKey = new Text();
    public void reduce(Text key, Iterable<Text> values,
                       Context context)
        throws IOException, InterruptedException {
      for (Text value : values) {
        outputKey.set(constructPropertyXml(key, value));
        context.write(outputKey, null);
      }
    }

    public static String constructPropertyXml(Text name, Text value) {
      StringBuilder sb = new StringBuilder();
      if(!name.equals("")&&!value.equals(""))
      sb.append("<Tweet><name>").append(name)
          .append("</name><text>").append(value)
          .append("</text></Tweet>");
      return sb.toString();
    }
  }

  public static void main(String... args) throws Exception {
    runJob(args[0], args[1]);
  }

  public static void runJob(String input,
                            String output)
      throws Exception {
    Configuration conf = new Configuration();

    Job job = new Job(conf);
    job.setJarByClass(ParserDriverReduce.class);
    //job.setReducerClass(MyParserReducer.class);
    job.setInputFormatClass(KeyValueTextInputFormat.class);

    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    FileInputFormat.setInputPaths(job, new Path(input));
    Path outPath = new Path(output);
    FileOutputFormat.setOutputPath(job, outPath);

    outPath.getFileSystem(conf).delete(outPath, true);

    job.waitForCompletion(true);
  }
}
