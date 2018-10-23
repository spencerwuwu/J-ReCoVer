// https://searchcode.com/api/result/123311825/

package com.chine.invertedindex.mapreduce;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.chine.invertedindex.analysis.ChineseAnalyzer;

public class BaseMapred {
	public static class InvertedIndexMapper
	    extends Mapper<LongWritable, Text, Text, Text>{
		
		private final static ChineseAnalyzer analyzer = new ChineseAnalyzer();
		private final static Text word = new Text();
		private final static Text filename = new Text();
		
		public void map(LongWritable key, Text val, Context context) 
		    throws IOException, InterruptedException {
			FileSplit fileSplit = (FileSplit)context.getInputSplit();
			String fileName = fileSplit.getPath().getName();
			filename.set(fileName);
			
			String line = val.toString();
			analyzer.process(line);
			for(String term: analyzer) {
				word.set(term);
				context.write(word, filename);
			}
		}
		
	}
	
	public static class InvertedIndexReducer
	    extends Reducer<Text, Text, Text, Text> {
		
		public void reduce(Text key, Iterable<Text> values, Context context) 
		    throws IOException, InterruptedException{
			boolean isFirst = true;
			StringBuilder returnText = new StringBuilder();
			for(Text value: values){
				if(!isFirst) {
					returnText.append(", ");
				}
				else {
					isFirst = false;
				}
				returnText.append(value.toString());
			}
			context.write(key, new Text(returnText.toString()));
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("Usage: InvertedIndex <input path> <output path>");
			System.exit(-1);
		}
		
		Configuration conf = new Configuration();
		Job job = new Job(conf, "InvertedIndex");
		job.setJarByClass(BaseMapred.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.setMapperClass(BaseMapred.InvertedIndexMapper.class);
		job.setReducerClass(BaseMapred.InvertedIndexReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		System.exit(job.waitForCompletion(true) ? 0: 1);
	}
}

