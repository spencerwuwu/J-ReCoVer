// https://searchcode.com/api/result/97675007/

//Jonathan Chu
//HW4
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapreduce.Job;
public class CommonFriends {
	public static class FriendsMapper extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text>{
		
		public void map(LongWritable key, Text value, OutputCollector<Text, Text> output,	Reporter report) throws IOException {
			String[] person = value.toString().split(" ");
			for(int i = 1; i < person.length; i++) {
				 
				 Text outkey;
				 if(person[0].compareTo(person[i]) < 0) 
				 	outkey = new Text(person[0] + ", " + person[i]);
				 else 
				 	outkey = new Text(person[i] + ", " + person[0]);
				 String[] otherfriends = new String[person.length - 2];
				 int countof = 0;
				 for(int j = 1; j < person.length; j++) {
				 	if(i != j) {
				 		otherfriends[countof] = person[j];
				 		countof++;
				 	}
				 }
				 output.collect(outkey, new Text(friendsString(otherfriends)));
			}
		}
		public String friendsString(String[] friends) {
			String out = "";
			for(int i = 0; i < friends.length; i++) {
				if(i != friends.length - 1)
					out += friends[i] + ", ";
				else
					out += friends[i];
			}
			return out;
		}
		
	}
	
	public static class FriendsReducer extends MapReduceBase implements Reducer<Text, Text, Text, Text>{
		
		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter report) throws IOException {
			String[] person = values.next().toString().split(", ");
			while(values.hasNext()){
				String[] friend = values.next().toString().split(", ");
				output.collect(key, new Text(intersect(person, friend)));
			}
		}
		public String intersect(String[] one, String[] two) {
			String common = "";
			for(int i = 0; i < one.length; i++) {
				for(int j = 0; j < two.length; j++) {
					if(one[i].equals(two[j]))
						common += two[j] + ", ";
				}
			}
			return common;
		}
		
	}
	public static void main(String[] args) {
		JobConf	conf	=	new	JobConf(CommonFriends.class);	
		conf.setJobName("CommonFriends");	
			
		conf.setMapperClass(FriendsMapper.class);	
//			conf.setCombinerClass(WordCountReducer.class)		-	op5onal	
		conf.setReducerClass(FriendsReducer.class);	
			
		conf.setOutputKeyClass(Text.class);	
		conf.setOutputValueClass(Text.class);	
			
		Job job;
		try {
			job = new Job(conf, "CommonFriends");
			job.setInputFormatClass(org.apache.hadoop.mapreduce.lib.input.TextInputFormat.class);	
			job.setOutputFormatClass(org.apache.hadoop.mapreduce.lib.output.TextOutputFormat.class);	
				
			FileInputFormat.addInputPath(conf,	new	Path(args[0]));	
			FileOutputFormat.setOutputPath(conf,	new	Path(args[1]));	
				
			JobClient.runJob(conf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
