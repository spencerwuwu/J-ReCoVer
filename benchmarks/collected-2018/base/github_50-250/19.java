// https://searchcode.com/api/result/93856032/

package com.hadoop;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.Local_Utils;
import com.Resources;
import com.HDFS_Utils;

public class NumberSort {

	public static class Map extends Mapper<Object,Text,IntWritable,IntWritable>{
		
		public static IntWritable data=new IntWritable();//
		
		public void map(Object key,Text value,Context context) throws IOException, InterruptedException {
			String line=value.toString();
			data.set(Integer.valueOf(line));
			context.write(data, new IntWritable(1));
		}
		
		
	}
	
	public static class Reduce extends Reducer<IntWritable,IntWritable,IntWritable,IntWritable>{
		
		public static IntWritable index=new IntWritable(1);
		
		public void reduce(IntWritable key,Iterable<IntWritable> values,Context context) throws IOException, InterruptedException{
			
			context.write(index, key);
			index=new IntWritable(index.get()+1);
			
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			String file="numberSort";
			String indir="file/"+file;
			String outdir=indir+"/outputfiles";
			Local_Utils.deleteLocalDir(outdir);//
			//mapreduce			
			Configuration conf=new Configuration();
			System.out.println(":"+conf.get("mapred.job.tracker"));
			Job job=new Job(conf,file);
			job.setJarByClass(NumberSort.class);
			//map,combinereduce
			job.setMapperClass(Map.class);
			//job.setCombinerClass(Reduce.class);
			job.setReducerClass(Reduce.class);
			
			//
			job.setOutputKeyClass(IntWritable.class);
			job.setOutputValueClass(IntWritable.class);			
			//
			FileInputFormat.addInputPath(job, new Path(indir));
			FileOutputFormat.setOutputPath(job, new Path(outdir));
			
			System.exit(job.waitForCompletion(true)?0:1);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}

