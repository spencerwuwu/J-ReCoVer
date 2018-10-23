// https://searchcode.com/api/result/123311827/

package com.chine.invertedindex.mapreduce.posindex;

import java.io.IOException;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

public class Mapred {
	public static class InvertedIndexMapper
	    extends Mapper<Text, ValuePair, Text, ValuePair> {
		
		@Override
		public void map(Text key, ValuePair value, Context context)
		    throws IOException, InterruptedException {
			context.write(key, value);
		}
	}
	
	public static class InvertedIndexReducer
	    extends Reducer<Text, ValuePair, Text, Text> {
		private Text value = new Text();
		
		@Override
		public void reduce(Text key, Iterable<ValuePair> values, Context context)
		    throws IOException, InterruptedException {
			String valueStr = "";
			for(ValuePair vp: values) {
				valueStr += vp.toString();
			}
			value.set(valueStr);
			context.write(key, value);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("Usage: InvertedIndex <input path> <output path>");
			System.exit(-1);
		}
		
		Configuration conf = new Configuration();
		Job job = new Job(conf, "InvertedIndex");
		job.setJarByClass(Mapred.class);
		
		job.setInputFormatClass(TokenInputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.setMapperClass(InvertedIndexMapper.class);
		job.setReducerClass(InvertedIndexReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(ValuePair.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		System.exit(job.waitForCompletion(true) ? 0: 1);
	}

}

