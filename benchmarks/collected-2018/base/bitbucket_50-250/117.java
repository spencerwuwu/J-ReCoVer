// https://searchcode.com/api/result/54362945/

package drivers.bovw.micc.unifi.it;

import java.io.IOException;
import java.util.Set;

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
import types.bovw.micc.unifi.it.WritableSIFTKeypoint;
import extractors.bovw.micc.unifi.it.DumbSIFTExtractor;
import extractors.bovw.micc.unifi.it.Extractor;
import formats.bovw.micc.unifi.it.ImageFileInputFormat;
import formats.bovw.micc.unifi.it.SIFTKeypointsOutputFormat;

public class FeatureExtractorDriver {

	public static class FeatureMapper extends Mapper<Text, WritableImage, Text, WritableSIFTKeypoint>{
		
		private Extractor extractor = new DumbSIFTExtractor();
		
		public void map(Text key, WritableImage value, Context context) throws IOException, InterruptedException {

			Set<Object> keypoints = extractor.extract(value);
			
			//throw new RuntimeException(":::" + keypoints.size() + ":::");
			
			for(Object kp : keypoints) {
				
				context.write(key, (WritableSIFTKeypoint)kp);
			}
		}
	}

	public static class FeatureReducer extends Reducer<Text, WritableSIFTKeypoint, Text, WritableSIFTKeypoint> {

		public void reduce(Text key, Iterable<WritableSIFTKeypoint> values, Context context) throws IOException, InterruptedException {

			for (WritableSIFTKeypoint val : values) {
				
				context.write(key, val);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

		if (otherArgs.length != 2) {
			System.err.println("Usage: FeatureExtractorDriver input-path output-path");
			System.exit(2);
		}
		
		Job job = new Job(conf, "kmeans clustering - " + 1);
		job.setJarByClass(FeatureExtractorDriver.class);
		job.setMapperClass(FeatureMapper.class);
		//job.setCombinerClass(ToStringReducer.class);
		job.setReducerClass(FeatureReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(WritableSIFTKeypoint.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(WritableSIFTKeypoint.class);
		job.setInputFormatClass(ImageFileInputFormat.class);
		job.setOutputFormatClass(SIFTKeypointsOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		job.waitForCompletion(true);
	}
}

