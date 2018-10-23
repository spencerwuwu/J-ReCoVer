// https://searchcode.com/api/result/93856038/

package com.hadoop;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

public class FactoryAddress {

	public static int num=0;
	public static class Map extends Mapper<LongWritable,Text,Text,Text>{
		
		
		public void map(LongWritable key,Text value,Context context) throws IOException, InterruptedException {
			String line=value.toString();
			String[] relations=new String[2];
			//
			if(line.indexOf("address")!=-1){
				return;
			}
			if(line.charAt(0)>'0' && line.charAt(0)<'9'){
				relations[0]=line.substring(0,1).trim();
				relations[1]="2+"+line.substring(1).trim();
			}else{
				relations[0]=line.substring(line.length()-1).trim();
				relations[1]="1+"+line.substring(0, line.length()-1).trim();
			}
			
			
			context.write(new Text(relations[0].trim()),new Text(relations[1].trim()));
			
		}
		
		
	}
	
	public static class Reduce extends Reducer<Text,Text,Text,Text>{
		
		public void reduce(Text key,Iterable<Text> values,Context context) throws IOException, InterruptedException{
			if(num==0){
				context.write(new Text("factory"),new Text("address"));
				num++;
			}
			List<String> facrotys=new ArrayList<String>();
			String address=null;
			Iterator<Text> iter=values.iterator();
			while(iter.hasNext()){
				String[] tmp=iter.next().toString().split("\\+");
				if("1".equals(tmp[0])){
					facrotys.add(tmp[1]);
				}else{
					address=tmp[1];
				}
				
			}
			for(String s : facrotys){
				context.write(new Text(s), new Text(address));
			}
			
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			String file="factoryAddress";
			String indir="file/"+file;
			String outdir=indir+"/outputfiles";
			Local_Utils.deleteLocalDir(outdir);//
			//mapreduce			
			Configuration conf=new Configuration();
			System.out.println(":"+conf.get("mapred.job.tracker"));
			Job job=new Job(conf,file);
			job.setJarByClass(FactoryAddress.class);
			//map,combinereduce
			job.setMapperClass(Map.class);
			//job.setCombinerClass(Reduce.class);
			job.setReducerClass(Reduce.class);
			
			//
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);			
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

