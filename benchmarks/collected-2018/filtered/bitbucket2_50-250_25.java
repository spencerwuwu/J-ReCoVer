// https://searchcode.com/api/result/46623422/

/**
 *	Copyright 2010 HypoBytes Ltd.
 *
 *	Licensed to HypoBytes Ltd. under one or more contributor
 *	license agreements.  See the NOTICE file distributed with
 *	this work for additional information regarding copyright
 *	ownership.
 *
 *	HypoBytes Ltd. licenses this file to You under the
 *	Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License.
 *
 *	You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *		https://hypobytes.com/licenses/APACHE-2.0
 *
 *	Unless required by applicable law or agreed to in writing,
 *	software distributed under the License is distributed on an
 *	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *	KIND, either express or implied.  See the License for the
 *	specific language governing permissions and limitations
 *	under the License.
 */
package com.hypobytes.ymir.hadoop;

import java.io.IOException;
import java.net.URI;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.testng.Assert;

/**
 * Simple mapred word count program for testing purposes. 
 * @author <a href="mailto:trygve@hypobytes.com">Trygve Sanne Hardersen</a>
 *
 */
public class WordCount {

	public static class WordMapper 
			extends Mapper<Object, Text, Text, IntWritable> {
		
	    private final static IntWritable one = new IntWritable(1);
	    
	    private Text word = new Text();

		/* (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Mapper#map(java.lang.Object, java.lang.Object, org.apache.hadoop.mapreduce.Mapper.Context)
		 */
		@Override
		protected void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			while (itr.hasMoreTokens()) {
				word.set(itr.nextToken());
				context.write(word, one);
			}
		}
	}
	
	public static class SumReducer 
			extends Reducer<Text,IntWritable,Text,IntWritable> {
		
		private IntWritable result = new IntWritable();

		/* (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(java.lang.Object, java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
	}
	
	public static void main(String[] args) throws Exception{
		Configuration config = new HdfsConfiguration();
		DistributedFileSystem dfs = new DistributedFileSystem();
		dfs.initialize(new URI(args[3]), config);
		// create some input
		Assert.assertTrue(dfs.mkdirs(new Path(args[0])));
		FSDataOutputStream out;
		out = dfs.create(new Path(args[0] + Path.SEPARATOR + "file_01"));
		out.write("Hello World Ugle Bjarne".getBytes());
		out.flush(); out.close();
		out = dfs.create(new Path(args[0] + Path.SEPARATOR + "file_02"));
		out.write("Hello World Nils Roger".getBytes());
		out.flush(); out.close();
		
	    Configuration conf = new Configuration();
	    Job job = Job.getInstance(conf, "Word Count");
	    job.setJar(args[2]);
	    job.setMapperClass(WordCount.WordMapper.class);
	    job.setCombinerClass(WordCount.SumReducer.class);
	    job.setReducerClass(WordCount.SumReducer.class);
	    job.setOutputKeyClass(Text.class);
	    job.setOutputValueClass(IntWritable.class);
	    FileInputFormat.addInputPath(job, new Path(args[0]));
	    FileOutputFormat.setOutputPath(job, new Path(args[1]));
	    //System.out.println("Waiting for job to finish");
	    //job.waitForCompletion(true);
	    //System.exit(0);
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

