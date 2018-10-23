// https://searchcode.com/api/result/93856027/

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

public class RelationMap {

	public static int num=0;
	public static class Map extends Mapper<LongWritable,Text,Text,Text>{
		
		
		public void map(LongWritable key,Text value,Context context) throws IOException, InterruptedException {
			String line=value.toString();
			StringTokenizer st=new StringTokenizer(value.toString());
			String[] relations=new String[2];
			int count=0;
			//
			if(line.indexOf("child")!=-1){
				return;
			}
			System.out.println(":"+st.countTokens());
			if(st.countTokens()<1){
				return;
			}
			while(st.hasMoreTokens()){
				if(count>=2){
					System.out.println(st.nextToken());
					break;
				}
				relations[count]=st.nextToken();
				count++;
			}
			//:(John,2+Tom+John),2,1
			context.write(new Text(relations[0]),new Text("1+"+relations[0]+"+"+relations[1]));
			context.write(new Text(relations[1]),new Text("2+"+relations[0]+"+"+relations[1]));
			
		}
		
		
	}
	
	public static class Reduce extends Reducer<Text,Text,Text,Text>{
		
		public void reduce(Text key,Iterable<Text> values,Context context) throws IOException, InterruptedException{
			if(num==0){
				context.write(new Text("grandchild"),new Text("grandparent"));
				num++;
			}
			List<String> childs=new ArrayList<String>(),parents=new ArrayList<String>();
			Iterator<Text> iter=values.iterator();
			while(iter.hasNext()){
				String[] temp=iter.next().toString().split("\\+");
				if("1".equals(temp[0])){
					parents.add(temp[2]);//key,
				}else{
					childs.add(temp[1]);//key,
				}
				
			}
			//,
			//
			//grandchild!=0  grandparent!=0,key,
			//,
			if(childs.size()!=0 && childs.size()!=0){
				for(String child : childs){
					for(String parent : parents){
						context.write(new Text(child), new Text(parent));
					}
				}
			}
			
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			String file="relationMap";
			String indir="file/"+file;
			String outdir=indir+"/outputfiles";
			Local_Utils.deleteLocalDir(outdir);//
			//mapreduce			
			Configuration conf=new Configuration();
			System.out.println(":"+conf.get("mapred.job.tracker"));
			Job job=new Job(conf,file);
			job.setJarByClass(RelationMap.class);
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

