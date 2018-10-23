// https://searchcode.com/api/result/134046357/

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * MapReduce job that pipes input to output as MapReduce-created key-val pairs
 * (c) 2012 Jeannie Albrecht
 */


public class Trivial extends Configured implements Tool {
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new Trivial(), args);
		System.exit(res);
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
	 */
	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Error: Wrong number of paramaters");
			System.err.println("trivial ");
			System.exit(1);
		}

		Configuration conf = getConf();
		Job job = new Job(conf, "trivial");

		job.setJarByClass(Trivial.class);
		job.setMapperClass(Trivial.IdentityMapper.class);
		job.setReducerClass(Trivial.IdentityReducer.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		System.out.println(job.getInputFormatClass());

		return job.waitForCompletion(true) ? 0 : 1;
	}

	/**
	 * map: (LongWritable, Text) --> (LongWritable, Text)
	 * 
	 * NOTE: Keys must implement WritableComparable; values must implement
	 * Writable.
	 */
	public static class IdentityMapper extends
			Mapper<LongWritable, Text, LongWritable, Text> {
		/* (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Mapper#map(KEYIN, VALUEIN, org.apache.hadoop.mapreduce.Mapper.Context)
		 */
		@Override
		public void map(LongWritable key, Text val, Context context)
				throws IOException, InterruptedException {
//			System.out.println("key=" + key + ", val=" + val);

			// Write (key, val) out to memory/disk
			context.write(key, val);
		}
	}

	/**
	 * reduce: (LongWritable, Text[]) --> (LongWritable, Text)[]
	 */
	public static class IdentityReducer extends
			Reducer<LongWritable, Text, LongWritable, Text> {
		/* (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN, java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		public void reduce(LongWritable key, Iterable<Text> value,
				Context context) throws IOException, InterruptedException {
			// Write (null, val) for every value. Using null for the key means
			// just the value will be printed.
			for (Text val : value) {
				context.write(null, val);
//				System.out.println("key=" + key + ", val=" + val);
			}
		}
	}
}
