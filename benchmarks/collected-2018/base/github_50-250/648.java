// https://searchcode.com/api/result/67830944/

package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lib.Point;
import lib.ToolJob;
import lib.VectorWritable;

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


public class WritePoint extends ToolJob {

	public static class MyMapper extends Mapper<Object, Text, Text,Text> {
		@Override
		public void map (Object key, Text value, Context context) {
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
	
	public static class MyReducer extends Reducer<Text,Text,Text,Object> {
		@Override
		public void reduce (Text key, Iterable<Text> values, Context context) {
			Point point = new Point();
			point.setId(100);
			VectorWritable vectorWritable = new VectorWritable();
			List<Double> list = new ArrayList<Double>();
			list.add(1.1);
			list.add(2.1);
			vectorWritable.setData(list);
			point.setData(vectorWritable);
			
			try {
				context.write(new Text("a"), point);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		job.setJarByClass(WritePoint.class);
		job.setMapperClass(MyMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(MyReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Point.class);

		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));
		
		
		if (job.waitForCompletion(true))
			return 1;
		else
			return 0;
	}

}

