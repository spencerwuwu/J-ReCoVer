// https://searchcode.com/api/result/67830965/

package test;

import java.io.IOException;

import lib.Point;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

public class Test {
	
	public static class MyMapper extends Mapper<Text,Point,Point,Point> {
		
		@Override
		public void map (Text key, Point value, Context context) {
			try {
				context.write(value, value);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static class MyReducer extends Reducer<Point,Point,Point,Point> {
		
		@Override
		public void reduce (Point key, Iterable<Point> values, Context context) {
			for (Point p : values) {
				try {
					context.write(p, p);
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
		String input = args[0];
		String output = args[1];
		
		Configuration conf = new Configuration();
		try {
			Job job = new Job(conf,"my job");
			job.setMapperClass(MyMapper.class);
			job.setMapOutputKeyClass(Point.class);
			job.setMapOutputValueClass(Point.class);
			job.setReducerClass(MyReducer.class);
			job.setOutputKeyClass(Point.class);
			job.setOutputValueClass(Point.class);
			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
			job.setJarByClass(Test.class);
			
			FileInputFormat.addInputPath(job, new Path(input));
			FileOutputFormat.setOutputPath(job, new Path(output));
			
			job.waitForCompletion(true);
			
			
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

