// https://searchcode.com/api/result/75864621/

package com.sohu.adrd.data.summary;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.serde2.columnar.BytesRefArrayWritable;
import org.apache.hadoop.hive.serde2.columnar.BytesRefWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.sohu.adrd.data.summary.Summary.HReduce;
import com.sohu.adrd.data.summary.Summary.ZebraMapper;


public class MakeRC extends MRProcessor {
	
	

	public static class TokenizerMapper extends
			Mapper<Object, Text, Text, IntWritable> {

		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text("SUM");

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			context.write(word, one);

		}
	}

	public static class IntSumReducer extends
			Reducer<Text, IntWritable, NullWritable, BytesRefArrayWritable> {

		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			BytesRefArrayWritable vals = new BytesRefArrayWritable();
			vals.set(0, new BytesRefWritable(key.getBytes()));
			vals.set(1, new BytesRefWritable(Integer.toString(sum).getBytes()));
			context.write(NullWritable.get(), vals);
		}
	}

	public int run(String[] args) throws Exception {
		
		Job job = new Job(_conf, "rc lzo");
		job.setJarByClass(MakeRC.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(IntSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPaths(job, "/user/aalog/shc/test");
		FileOutputFormat.setOutputPath(job, new Path("/user/aalog/shc/rc_lzo"));
		return 1;
	}
	
	
	@Override
	protected void configJob(Job job) {
		
		job.setJarByClass(MakeRC.class);
		
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(IntSumReducer.class);
		
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setOutputValueClass(BytesRefArrayWritable.class);
		

		
	}


}

