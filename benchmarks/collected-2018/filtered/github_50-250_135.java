// https://searchcode.com/api/result/66636679/

/*
*
*
*Word Count
*
*
*/
package edu.cs.indiana.b649;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

import java.io.IOException;
import java.util.*;
import java.util.StringTokenizer; 

public class WordCount {

        public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();

		//to decide what all to ignore accompanying a word...
		private static String delims = "  . ? ' / - ! ; : _ , a ) ( @ & $ \" %  + *";		

		public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
					String line = value.toString();
					StringTokenizer tokenizer = new StringTokenizer(line,delims);
					while (tokenizer.hasMoreTokens()) { 
							//set everything to lower case alphabets so that different cases are resolved to same word
							word.set((tokenizer.nextToken()).toLowerCase());
							output.collect(word, one); 
					}
		}
        }

	public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
	       public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
			int sum = 0;
			while (values.hasNext())
				sum += values.next().get();
			output.collect(key, new IntWritable(sum));
		}
    	}


 	public static void main(String[] args) throws Exception {
 	     JobConf conf = new JobConf(WordCount.class);
 	     conf.setJobName("adwaraka");
 	
 	     conf.setOutputKeyClass(Text.class);
 	     conf.setOutputValueClass(IntWritable.class);
 	
 	     conf.setMapperClass(Map.class);
 	     conf.setCombinerClass(Reduce.class);
 	     conf.setReducerClass(Reduce.class);
 	
 	     conf.setInputFormat(TextInputFormat.class);
 	     conf.setOutputFormat(TextOutputFormat.class);
 	
 	     FileInputFormat.setInputPaths(conf, new Path(args[0]));
 	     FileOutputFormat.setOutputPath(conf, new Path(args[1]));
 	
 	     JobClient.runJob(conf);
	}
}

