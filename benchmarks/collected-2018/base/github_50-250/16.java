// https://searchcode.com/api/result/93856017/

package com.hadoop;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.Local_Utils;
import com.Resources;
import com.HDFS_Utils;

public class AvgScore {

	public static class Map extends Mapper<LongWritable,Text,Text,IntWritable>{
		
		
		public void map(LongWritable key,Text value,Context context) throws IOException, InterruptedException {
			//
			String line=value.toString();
			System.out.println("");
			System.out.println(value.toString());
			//StringTokenizer
			StringTokenizer tokenizerArticle=new StringTokenizer(line,"\n");
			while(tokenizerArticle.hasMoreElements()){
				StringTokenizer tokenizerLine=new StringTokenizer(tokenizerArticle.nextToken());
				String strName=tokenizerLine.nextToken();
				String strScore=tokenizerLine.nextToken();
				int scoreInt = Integer.parseInt(strScore);
				context.write(new Text(strName), new IntWritable(scoreInt));
			}
			
		}
		
		
	}
	
	public static class Reduce extends Reducer<Text,IntWritable,Text,IntWritable>{
		
		public void reduce(Text key,Iterable<IntWritable> values,Context context) throws IOException, InterruptedException{
			int sum=0;
			int count=0;
			
			for(IntWritable i : values){
				sum+=i.get();
				count++;
			}
			context.write(key, new IntWritable(sum/count));
			
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			String file="avgScore";
			String indir="file/"+file;
			String outdir=indir+"/outputfiles";
			Local_Utils.deleteLocalDir(outdir);//
			//mapreduce			
			Configuration conf=new Configuration();
			System.out.println(":"+conf.get("mapred.job.tracker"));
			Job job=new Job(conf,file);
			job.setJarByClass(AvgScore.class);
			//map,combinereduce
			job.setMapperClass(Map.class);
			//job.setCombinerClass(Reduce.class);
			job.setReducerClass(Reduce.class);
			
			//
			job.setOutputKeyClass(Text.class);
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

