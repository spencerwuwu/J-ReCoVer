// https://searchcode.com/api/result/97675011/

//Jonathan Chu
//HW4
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;

import org.apache.hadoop.mapred.*;
public class CommonFriends {
	public class FriendsMapper implements Mapper<LongWritable, Text, Text, IntWritable>{
		
		public void map(LongWritable key, Text value, OutputCollector<Text, ArrayWritable> output,	Reporter report) throws IOExcpetion, InterruptedException {
			String[] person = value.split(" ");
			for(int i = 1; i < person.length; i++) {
				 
				 Text outkey;
				 if(person[0].compareTo(person[i]) < 0) 
				 	outkey = person[0] + ", " + person[i];
				 else 
				 	outkey = person[i] + ", " + person[0];
				 String[] otherfriends = new String[person.length - 2];
				 int countof = 0;
				 for(int j = 1; j < person.length; j++) {
				 	if(i != j) {
				 		otherfriends[countof] = person[j];
				 		countof++;
				 	}
				 }
				 output.collect(outkey, new ArrayWritable(otherfriends));
			}
		}
	}
	
	public class FriendsReducer extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable>{
		
		public void reduce(Text key, Iterator<ArrayWritable> values, OutputCollector<Text, ArrayWritable> output, Reporter report) {
			String[] person = values.next().toStrings();
			while(values.hasNext()){
				String[] friend = values.next().toStrings();
				output.collect(key, new ArrayWritable(intersect(person, friend)));
			}
		}
		public String[] intersect(String[] one, String[] two) {
			String[] common = new String[one.length];
			for(int i = 0; i < one.length; i++) {
				for(int j = 0; j < two.length; j++) {
					if(one[i].equals(two[i]))
						common[i] = two[i];
				}
			}
			return common;
		}
	}
}
