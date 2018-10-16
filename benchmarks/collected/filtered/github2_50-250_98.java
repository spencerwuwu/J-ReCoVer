// https://searchcode.com/api/result/95034441/

package xian.zhang.core;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import xian.zhang.common.Util;

/**
 * 
 * @author zx
 *
 */
public class ProductCo_occurrenceMatrix {

	public static class Co_occurrenceMapper extends Mapper<LongWritable, Text, Text, IntWritable>{

		IntWritable one = new IntWritable(1);
		
		@Override
		protected void map(LongWritable key, Text value, Context context)throws IOException, InterruptedException {
			
			String[] products = Util.DELIMITER.split(value.toString());
			for(int i=1;i<products.length;i++){
				for(int j=1;j<products.length;j++){
					if(i != j){
						context.write(new Text(products[i] + ":" + products[j]), one);
					}
				}
			}
			
		}
		
	}
	
	public static class Co_occurrenceReducer extends Reducer<Text, IntWritable, NullWritable, Text>{

		NullWritable nullKey =NullWritable.get();
		
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values,Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			Iterator<IntWritable> it = values.iterator();
			while(it.hasNext()){
				sum += it.next().get();
			}
			context.write(nullKey, new Text(key.toString().replace(":", ",") + "," + sum));
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public static boolean run(Path inPath,Path outPath) throws IOException, ClassNotFoundException, InterruptedException{
		
		Configuration conf = new Configuration();
		Job job = new Job(conf,"ProductCo_occurrenceMatrix");
		
		job.setJarByClass(ProductCo_occurrenceMatrix.class);
		job.setMapperClass(Co_occurrenceMapper.class);
		job.setReducerClass(Co_occurrenceReducer.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputKeyClass(Text.class);
		
		FileInputFormat.addInputPath(job, inPath);
		FileOutputFormat.setOutputPath(job, outPath);
		
		return job.waitForCompletion(true);
	}
	
}

