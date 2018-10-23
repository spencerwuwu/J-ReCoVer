// https://searchcode.com/api/result/75864669/

package bayes;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class BayesTrainNoLine implements Tool{
	
	protected Configuration _conf = new Configuration();
	

	@Override
	public Configuration getConf() {
		return _conf;
	}

	@Override
	public void setConf(Configuration conf) {
		_conf = conf;

	}


	public static class TrainMapper extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			
			String line = value.toString();
			String[] splits = line.split("\\p{Blank}+");
			String gender = splits[0];
			gender = "0".equals(gender) ? "M" : "F";
			for (String seg : splits) {
				String[] kv = seg.split(":");
				if (kv.length != 2)
					continue;
				context.write(new Text(kv[0]), new Text(gender + ":" + kv[1]));

			}
			context.write(new Text("All"), new Text(gender + ":1"));

		}
	}

	public static class TrainReducer extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			long maleSum = 0;
			long femaleSum = 0;
			for (Text val : values) {
				String gender = val.toString().split(":")[0];
				if ("M".equals(gender)) {
					maleSum++;
				} else {
					femaleSum++;
				}
			}
			context.write(new Text(key), new Text("M:" + maleSum + ",F:"
					+ femaleSum));
		}
	}

	public int run(String[] args) throws Exception {

		Job job = new Job(_conf, "Bayes Train No Line Number");

		job.setJarByClass(BayesTrainNoLine.class);
		job.setMapperClass(TrainMapper.class);
		job.setReducerClass(TrainReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(Integer.parseInt(_conf.get("num.reduce", "32")));

		FileInputFormat.addInputPaths(job, _conf.get("train.input"));
		FileOutputFormat.setOutputPath(job, new Path(_conf.get("train.output")));
		System.out.println(_conf.get("train.output"));
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	
	public static void main(String[] args) throws Exception {
		int ret = ToolRunner.run(new BayesTrainNoLine(), args);
		if (ret != 0) {
			System.err.println("Job Failed!");
			System.exit(ret);
		}
	}
}

