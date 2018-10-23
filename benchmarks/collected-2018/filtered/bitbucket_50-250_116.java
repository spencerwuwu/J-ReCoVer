// https://searchcode.com/api/result/54362944/

package drivers.bovw.micc.unifi.it;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import types.bovw.micc.unifi.it.WritableImage;


import formats.bovw.micc.unifi.it.ImageFileInputFormat;

public class FileSizeDriver {

	public static class ComputeSizeMapper extends Mapper<Text, WritableImage, Text, FloatWritable>{
		
		private FloatWritable outValue = new FloatWritable();
		
		public void map(Text key, WritableImage value, Context context) throws IOException, InterruptedException {

			outValue.set(value.getLength());
			context.write(key, outValue);
		}
	}

	public static class ToStringReducer extends Reducer<Text, FloatWritable, Text, Text> {

		private Text outValue = new Text();
		
		public void reduce(Text key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {

			for (FloatWritable val : values) {

				outValue.set(val.toString());
				context.write(key, outValue);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

		if (otherArgs.length != 2) {
			System.err.println("Usage: ClusteringDriver input-path output-path");
			System.exit(2);
		}
		
		Job job = new Job(conf, "kmeans clustering - " + 1);
		job.setJarByClass(FileSizeDriver.class);
		job.setMapperClass(ComputeSizeMapper.class);
		//job.setCombinerClass(ToStringReducer.class);
		job.setReducerClass(ToStringReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(FloatWritable.class);
		job.setInputFormatClass(ImageFileInputFormat.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		job.waitForCompletion(true);
	}
}

