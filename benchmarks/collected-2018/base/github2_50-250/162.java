// https://searchcode.com/api/result/66797561/

package com.jayway.hadoop.gutenberg;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class RowLengthCounter {

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		Job job = new Job();
		job.setJarByClass(RowLengthCounter.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.setMapperClass(RowLengthCountMapper.class);
		job.setReducerClass(RowLengthCountReducer.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	static class RowLengthCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
		
		
		protected void map(LongWritable key, Text row, Mapper<LongWritable,Text,Text,IntWritable>.Context context) throws IOException ,InterruptedException {
			
			// First, you have to find the path to the file that is currently beeing processed
			// Hadoop works with InputSplits. Checkout the hadoop API to know more.
			FileSplit split = null; //TODO: Assign the InputSplit
			int length = 0;         //TODO: get the current row length
			
			context.write(new Text(split.getPath().getName()),new IntWritable(length));
		};
		
	}
	

	static class RowLengthCountReducer extends Reducer<Text, IntWritable, Text,IntWritable> {
		
		/*
		 * This method should calculate the maximum row length for each file 
		 */
		protected void reduce(Text fileName, Iterable<IntWritable> arg1, Reducer<Text,IntWritable,Text,IntWritable>.Context context) throws IOException ,InterruptedException {
			
			
			int maxValue = Integer.MIN_VALUE;
			
			//TODO: calculate the maximum row length per fileName

			context.write(fileName, new IntWritable(maxValue));
		};
	
	}
}

