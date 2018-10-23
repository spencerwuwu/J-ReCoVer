// https://searchcode.com/api/result/67830916/

package test;

import java.io.IOException;
import java.io.SequenceInputStream;

import lib.Point;
import lib.ToolJob;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ReadPoint extends ToolJob {

	public static class MyMapper extends Mapper<Text,Point,Text,Point> {
		
		@Override
		public void map (Text key, Point point, Context context) {
			try {
				context.write(key, point);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static class MyReducer extends Reducer<Text,Point,Text,Text> {
		@Override
		public void reduce (Text key, Iterable<Point> points, Context context) {
			for (Point point : points) {
				try {
					context.write(key, new Text(point.toString()));
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
	
	
	
	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("wrong number of args");
			return 0;
		}
		String input = args[0];
		String output = args[1];
//		String nidFile = args[2];
		
		

		Configuration conf = getConf();
		
//		DistributedCache.addCacheFile(new URI(nidFile), conf);
//		nidFile = nidFile.substring(nidFile.lastIndexOf("/")+1,nidFile.length());
//		conf.set("nidFile", nidFile);
		
		Job job = new Job(conf, "createVector");
		job.setJarByClass(ReadPoint.class);
		job.setMapperClass(MyMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Point.class);
		job.setReducerClass(MyReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));
		
		job.waitForCompletion(true);
		
		return 1;
	}

}

