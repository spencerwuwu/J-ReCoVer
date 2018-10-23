// https://searchcode.com/api/result/95034438/

package xian.zhang.core;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 *  userid1,product1  userid1,product2   userid1,product3
 *  userid1 product1,product2,product3
 * @author zx
 *
 */
public class CombinProductInUser {
	
	public static class CombinProductMapper extends Mapper<LongWritable, Text, IntWritable, Text>{
		@Override
		protected void map(LongWritable key, Text value,Context context)
				throws IOException, InterruptedException {
			String[] items = value.toString().split(","); 
			context.write(new IntWritable(Integer.parseInt(items[0])), new Text(items[1]));
		}
	}
	
	public static class CombinProductReducer extends Reducer<IntWritable, Text, IntWritable, Text>{

		@Override
		protected void reduce(IntWritable key, Iterable<Text> values,Context context)
				throws IOException, InterruptedException {
			StringBuffer sb = new StringBuffer();
			Iterator<Text> it = values.iterator();
			sb.append(it.next().toString());
			while(it.hasNext()){
				sb.append(",").append(it.next().toString());
			}
			context.write(key, new Text(sb.toString()));
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public static boolean run(Path inPath,Path outPath) throws IOException, ClassNotFoundException, InterruptedException{
		
		Configuration conf = new Configuration();
		Job job = new Job(conf,"CombinProductInUser");
		
		job.setJarByClass(CombinProductInUser.class);
		job.setMapperClass(CombinProductMapper.class);
		job.setReducerClass(CombinProductReducer.class);

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, inPath);
		FileOutputFormat.setOutputPath(job, outPath);
		
		return job.waitForCompletion(true);
		
	}
	
}
