// https://searchcode.com/api/result/110596653/

package some.hack;

import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import java.io.IOException;
import org.apache.commons.httpclient.HttpMethod;


public class WordCount {
//
//	public static class Map extends MapReduceBase implements
//			CountMapper<LongWritable, Text, Text, IntWritable> {
//		private final static IntWritable one = new IntWritable(1);
//		private Text word = new Text();
//
//		public void map(LongWritable key, Text value,
//				OutputCollector<Text, IntWritable> output, Reporter reporter)
//				throws IOException {
//			String line = value.toString();
//			StringTokenizer tokenizer = new StringTokenizer(line);
//			while (tokenizer.hasMoreTokens()) {
//				word.set(tokenizer.nextToken());
//				output.collect(word, one);
//			}
//		}
//	}
//
//	public static class Reduce extends MapReduceBase implements
//			Reducer<Text, IntWritable, Text, IntWritable> {
//		public void reduce(Text key, Iterator<IntWritable> values,
//				OutputCollector<Text, IntWritable> output, Reporter reporter)
//				throws IOException {
//			int sum = 0;
//			while (values.hasNext()) {
//				sum += values.next().get();
//			}
//			output.collect(key, new IntWritable(sum));
//		}
//	}
//
//	public static void main(String[] args) throws Exception {
//		JobConf conf = new JobConf(WordCount.class);
//		conf.setJobName("wordcount");
//
//		conf.setOutputKeyClass(Text.class);
//		conf.setOutputValueClass(IntWritable.class);
//
//		conf.setMapperClass(Map.class);
//		conf.setCombinerClass(Reduce.class);
//		conf.setReducerClass(Reduce.class);
//
//		conf.setInputFormat(TextInputFormat.class);
//		conf.setOutputFormat(TextOutputFormat.class);
//
//		FileInputFormat.setInputPaths(conf, new Path("/temp/in"));
//		FileOutputFormat.setOutputPath(conf, new Path("/temp/out"));
//
//		JobClient.runJob(conf);
//	}
}

