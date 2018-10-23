// https://searchcode.com/api/result/123696322/

package GenerateDFCount;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class DFcount {

  public static class TokenizerMapper extends Mapper<LongWritable, Text, Text, IntWritable>{
  
    private final static IntWritable one = new IntWritable(1);
    private Text outkey = new Text();
    
    public void map (LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    
			HashSet<String> wordHash = new HashSet<String>();
      String[] sentenceParts = ((Text) value).toString().split("\\t+\\s*", 3);
  		
      String sentence = sentenceParts[2].toLowerCase().replaceAll("[^a-z0-9\\s]+"," ").replaceAll("\\b\\d+\\b","[-numeric-]").replaceAll("\\d+","");
   
      StringTokenizer itr = new StringTokenizer(sentence);
      
      while (itr.hasMoreTokens ()){
        wordHash.add(itr.nextToken());
			}

			for (String word: wordHash){
				outkey.set(word);
				context.write(outkey, one);
			}
    }
  }
  
  public static class IntSumReducer extends Reducer<Text, IntWritable, Text, Text> {

    private Text result = new Text();

    public void reduce(Text key, Iterable<IntWritable> values, Context context)	throws IOException, InterruptedException {
      int sum = 0;
      int totalDocs = 1000;
      for (IntWritable val : values){
        sum += val.get();
      }
      Float idf=new Float(Math.log(totalDocs/sum));
      result.set(idf.toString());
      context.write(key, result);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = new Job(conf, "DF count");
    job.setJarByClass(DFcount.class);
    job.setMapperClass(TokenizerMapper.class);
    //job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}

