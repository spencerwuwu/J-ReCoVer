// https://searchcode.com/api/result/67830955/

package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lib.VectorWritable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TestVector {

	public static class MyMapper extends Mapper<Object,Text,Text,VectorWritable> {
		@Override
		public void map (Object key, Text value, Context context) {
			List<Double> list = new ArrayList<Double>();
			VectorWritable vector = new VectorWritable();
			list.add(1.1);
			list.add(12.1);
			vector.setData(list);
			try {
				context.write(new Text("a"), vector);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static class MyReducer extends Reducer<Text,VectorWritable,Text,Text> {
		@Override
		public void reduce (Text key, Iterable<VectorWritable> values, Context context) {
			for (VectorWritable vector : values) {
//				System.out.println(vector);
				try {
					context.write(key, new Text(vector.toString()));
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
		Configuration conf = new Configuration();
		try {
			Job job = new Job(conf,"test");
			job.setJarByClass(TestVector.class);
			job.setMapperClass(MyMapper.class);
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(VectorWritable.class);
			job.setReducerClass(MyReducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			
			FileInputFormat.addInputPath(job, new Path("input"));
			FileOutputFormat.setOutputPath(job, new Path("output1"));
			job.waitForCompletion(true);
		} catch (IOException e) {
			
			
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

