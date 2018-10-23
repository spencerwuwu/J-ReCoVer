// https://searchcode.com/api/result/93856022/

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

import com.HDFS_Utils;
import com.Local_Utils;

public class Dedup {

	public static class Map extends Mapper<Object,Text,Text,Text>{
		
		public static Text line=new Text();//
		
		public void map(Object key,Text value,Context context) throws IOException, InterruptedException {
			line=value;
			context.write(line, new Text(""));
		}
		
		
	}
	
	public static class Reduce extends Reducer<Text,Text,Text,Text>{
		
		public void reduce(Text key,Iterable<Text> values,Context context) throws IOException, InterruptedException{
			context.write(key, new Text(""));
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			String file="dedup";
			String indir="file/"+file;
			String outdir=indir+"/outputfiles";
			Local_Utils.deleteLocalDir(outdir);//
			//mapreduce			
			Configuration conf=new Configuration();
			System.out.println(":"+conf.get("mapred.job.tracker"));
			Job job=new Job(conf,file);
			job.setJarByClass(Dedup.class);
			//map,combinereduce
			job.setMapperClass(Map.class);
			job.setCombinerClass(Reduce.class);
			job.setReducerClass(Reduce.class);
			
			
			//
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);			
			
			
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

