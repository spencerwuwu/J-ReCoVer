// https://searchcode.com/api/result/75862776/

package elebird;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.serde2.columnar.BytesRefArrayWritable;
import org.apache.hadoop.hive.serde2.columnar.BytesRefWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.twitter.elephantbird.mapreduce.output.RCFileOutputFormat;


public class MakeRC implements Tool {
	
	protected Configuration _conf = new Configuration();
	
	public Configuration getConf() {
		return _conf;
	}

	@Override
	public void setConf(Configuration conf) {
		_conf = conf;

	}
	public MakeRC() {
		this._conf = new Configuration();
	}

	public static class TokenizerMapper extends
			Mapper<Object, Text, Text, IntWritable> {

		private final static IntWritable one = new IntWritable(1);
		
		@Override
		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			context.write(value, one);

		}
	}

	public static class IntSumReducer extends
			Reducer<Text, IntWritable, NullWritable, BytesRefArrayWritable> {
		
		@Override
		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			BytesRefArrayWritable vals = new BytesRefArrayWritable();
			vals.set(0, new BytesRefWritable(key.getBytes()));
			vals.set(1, new BytesRefWritable(Integer.toString(sum).getBytes()));
			context.write(NullWritable.get(), vals);
		}
	}

	public int run(String[] args) throws Exception {
		
		
		RCFileOutputFormat.setColumnNumber(_conf, 2);
		
		Job job = new Job(_conf, "rc lzo");
		job.setJarByClass(MakeRC.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(IntSumReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setOutputValueClass(BytesRefArrayWritable.class);
		job.setOutputFormatClass(RCFileOutputFormat.class);
		
		FileInputFormat.addInputPaths(job, "/user/aalog/shc/test");
		FileOutputFormat.setOutputPath(job, new Path(args[0]));
		System.out.println("???");
		job.submit();
		boolean success = job.waitForCompletion(true);
		return success ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
		
		int returnStatus = 0;
		MakeRC makerc = new MakeRC();
		returnStatus = ToolRunner.run(makerc, args);
		
		if (returnStatus != 0) {
			System.err.println("Sessionlog Job Failed!");
			System.exit(returnStatus);
		}
	}

}

