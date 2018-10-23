// https://searchcode.com/api/result/67831013/

package util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

public class TextToSequence {

	public void run(String inputUri, String outputUri) throws IOException, InterruptedException, ClassNotFoundException{
		Configuration conf = new Configuration();
		Job job = new Job(conf);
		
//		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setMapperClass(TMapper.class);
		job.setReducerClass(TReducer.class);
		job.setJarByClass(TextToSequence.class);
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(inputUri));
		FileOutputFormat.setOutputPath(job, new Path(outputUri));
		
		System.exit(job.waitForCompletion(true)?0:1);
		
	}
	
	public static class TMapper extends Mapper<LongWritable,Text,LongWritable,Text> {
		
		@Override
		public void map(LongWritable key, Text value, Context context) {
			try {
				context.write(key, value);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static class TReducer extends Reducer<LongWritable, Text, LongWritable, Text> {
		
		@Override
		public void reduce(LongWritable key, Iterable<Text> values, Context context) {
			for (Text t : values) {
				try {
					context.write(key, t);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public static void main (String[] args) {
		String inputUri = args[0];
		String outputUri = args[1];
		try {
			new TextToSequence().run(inputUri, outputUri);
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

