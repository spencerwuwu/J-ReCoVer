// https://searchcode.com/api/result/59415665/

package local.jv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

class WineStats {
  static String[] columnNames = new String[] { "fixed acidity", "volatile acidity", "citric acid", 
    "residual sugar", "chlorides", "free sulfur dioxide", 
    "total sulfur dioxide", "density", "pH", "sulphates", "alcohol", "quality" };
}

class Map extends Mapper<LongWritable, Text, Text, Text> {
  static Log LOG = LogFactory.getLog(Map.class);

  @Override
  protected void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {
    // key - line number (long)
    // value - column values separated by ;

    String[] values = value.toString().split(";");
    String quality = values[values.length-1];
    
    for (int i = 0; i < WineStats.columnNames.length; i++) {
      context.write(new Text(quality + ";" + WineStats.columnNames[i]), new Text(values[i]));
    }
  }
}

class Reduce extends Reducer<Text, Text, Text, Text> {
  static Log LOG = LogFactory.getLog(Reduce.class);

  @Override
  protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException,
      InterruptedException {

    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
    int count = 0;
    for (Text value : values) {
      descriptiveStatistics.addValue(Double.parseDouble(value.toString()));
      count++;
    }
    
    LOG.info(String.format("reduce input key: %s, count = %d", key.toString(), count));
    String value = key.toString();
    value += ";" + descriptiveStatistics.getN();
    value += ";" + descriptiveStatistics.getMin();
    value += ";" + descriptiveStatistics.getMax();
    value += ";" + descriptiveStatistics.getMean();
    value += ";" + descriptiveStatistics.getPercentile(50);
    context.write(null, new Text(value));
  }
}

public class StatisticalMapRedJob {
  static Configuration conf;
  private static FileSystem fs;

  private static void init() throws IOException {
    conf = new Configuration();
    fs = FileSystem.get(conf);
  }
  
  public int run(String pathin, String pathout) throws Exception {
    Job job = new Job(conf);
    job.setJarByClass(StatisticalMapRedJob.class);

    job.setMapperClass(Map.class);
    job.setReducerClass(Reduce.class);

    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    job.setNumReduceTasks(conf.getInt("tasks", 1));

    FileInputFormat.addInputPath(job, new Path(pathin));
    FileOutputFormat.setOutputPath(job, new Path(pathout));

    boolean success = job.waitForCompletion(true);
    return success ? 0 : -1;
  }

  public static void main(String[] args) throws Exception {
    StatisticalMapRedJob mrJob = new StatisticalMapRedJob();
    init();

    if (args.length < 3) {
      System.out.println("Too few arguments. Arguments should be:  " +
          "<hdfs input folder> <hdfs output folder> <num of parallel tasks>");
      System.exit(0);
    }

    String pathin = args[0];
    String pathout = args[1];
    int tasks = Integer.parseInt(args[2]);

    conf.setLong("tasks", tasks);

    // Delete output folder if it already exists
    // fs.delete(new Path(pathout), true);
    
    mrJob.run(pathin, pathout);

    ArrayList<String> results = readResult(pathout);

    // write back as CSV
    try (FSDataOutputStream outputStream = fs.create(new Path(pathout, "result.csv"));
         PrintWriter outputWriter = new PrintWriter(outputStream);) {
      String header = "quality; characteristic; samples count; min; max; mean; median";
      outputWriter.println(header);

      for (String result : results) {
        System.out.println(result);
        outputWriter.println(result);
      }
    }
  }

  /**
   * Method to read lines from files in a folder in HDFS
   */
  public static ArrayList<String> readResult(String pathStr) throws IOException {
    ArrayList<String> factors = new ArrayList<String>();
    Path path = new Path(pathStr);

    if (fs.getFileStatus(path).isDir()) {
      FileStatus[] listFiles = fs.listStatus(path);

      for (int i = 0; i < listFiles.length; i++) {
        try {
          if (listFiles[i].isDir())
            continue;
          // Get the object of DataInputStream
          FSDataInputStream in = fs.open(listFiles[i].getPath());
          BufferedReader br = new BufferedReader(
              new InputStreamReader(in));
          String strLine;
          // Read File Line By Line
          while ((strLine = br.readLine()) != null) {
            factors.add(strLine);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return factors;
  }

}

