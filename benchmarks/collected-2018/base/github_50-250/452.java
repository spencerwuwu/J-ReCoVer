// https://searchcode.com/api/result/100948428/

package com.hadoop.mapreduce;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

/**
 * This program is used to subtract the two sets A, B
 * @Usage: hadoop jar jarfile mainclass inputlocation outputlocation
 * Example:-
 * 
 * Input Data:
 * A,1
 * A,2
 * A,3
 * B,3
 * B,4
 * 
 * (A-B): Output
 * 1
 * 2
 *
 * @author Nagamallikarjuna
 * 
 **/
 
public class Subtraction {
	
	public static class MyMapper extends Mapper<LongWritable, Text, Text, Text>
	{
		Text kword = new Text();
		Text vword = new Text();
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
		{
			String line = value.toString();
			String parts[] = line.split("\\,");
			if(parts.length == 2)
			{
				kword.set(parts[1]);
				vword.set(parts[0]);
				context.write(kword, vword);
			}
		}
	}
	
	public static class MyReducer extends Reducer<Text, Text, Text, NullWritable>
	{
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
		{
			sets = new HashSet<String>();
			String setA = "";
			for(Text value : values)
			{
				sets.add(value.toString());
				if(value.toString().equals("A"))
				setA = value.toString();
			}
			if(sets.size() < 2 && setA.equals("A"))
			{
				context.write(key, NullWritable.get());
			}
		}
	}
	
	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException
	{
		Configuration conf = new Configuration();
		
		Job job = new Job(conf, "Subtracting two sets....");
		
		job.setJarByClass(Subtraction.class);
		
		job.setMapperClass(MyMapper.class);
		job.setReducerClass(MyReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		System.exit(job.waitForCompletion(true) ? 0 :1);	
	}
}

