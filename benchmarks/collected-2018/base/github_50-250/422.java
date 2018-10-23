// https://searchcode.com/api/result/97674946/

import java.io.IOException;
import java.util.*;
        
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import edu.umd.cloud9.*;
import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPageInputFormat;

public class InvertedIndex2 {

  public static class InvertedIndexMapper extends Mapper<LongWritable, WikipediaPage, Text, Text> {

    public void map(LongWritable key, WikipediaPage p, Context context)
        throws IOException, InterruptedException {
    	
      String val = p.getContent();

      StringTokenizer tokenizer = new StringTokenizer(val);
      while (tokenizer.hasMoreTokens()){
    	  String word = tokenizer.nextToken();
    	  context.write(new Text(word), new Text(p.getDocid()));
      }
    	
    }
  }

  public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text> {

    public void reduce(Text key, Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
    	Iterator i = values.iterator();
    	String result = "";
    	while (i.hasNext()){
    		result = result + " "+i.next().toString();
    	}
    	context.write(key, new Text(result));
    	
    }
  }

  public static void main(String[] args) throws Exception {
	    Configuration conf = new Configuration();
	        
	        Job job = new Job(conf, "InvertedIndex");
	   
	    job.setOutputKeyClass(Text.class);
	    job.setOutputValueClass(Text.class);
	    
	    job.setMapperClass(InvertedIndexMapper.class);
	    job.setReducerClass(InvertedIndexReducer.class);
	        
	    job.setInputFormatClass(WikipediaPageInputFormat.class);
	    job.setOutputFormatClass(TextOutputFormat.class);
	    
	    FileInputFormat.addInputPath(job, new Path("index_test"));
	    FileOutputFormat.setOutputPath(job, new Path("index2_output"));
	        
	    job.waitForCompletion(true);
	  

  }
}


