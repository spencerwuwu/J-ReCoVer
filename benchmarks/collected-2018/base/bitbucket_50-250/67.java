// https://searchcode.com/api/result/134046359/

/**
 * MapReduce job that pipes input to output as MapReduce-created key-val pairs
 * (c) 2012 Jeannie Albrecht
 */

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ClickRate2 extends Configured implements Tool {
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new ClickRate(), args);
		System.exit(res);
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
	 */
	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println("Error: Wrong number of paramaters");
			System.exit(1);
		}

		Configuration conf = this.getConf();
		Job job = new Job(conf, "joinClicksAndImpressions");
		job.setJarByClass(ClickRate.class);

		MultipleInputs.addInputPath(job, new Path(args[0]),
				TextInputFormat.class, ClickMapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]),
				TextInputFormat.class, ImpressionMapper.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setReducerClass(ImpressionReducer.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileOutputFormat.setOutputPath(job, new Path(args[2]));

		if (!job.waitForCompletion(true)) {
			return 1;
		}

		System.out.println(job.getCounters());

		return 0;
	}

	/**
	 * map: (LongWritable, Text) --> (Text, Text) NOTE: Keys must
	 * implement WritableComparable; values must implement Writable.
	 */
	private static class ClickMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		private Text adId = new Text();
		private Text clickInfo = new Text();

		/*
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Mapper#map(KEYIN, VALUEIN,
		 * org.apache.hadoop.mapreduce.Mapper.Context)
		 */
		@Override
		public void map(LongWritable key, Text val, Context context)
				throws IOException, InterruptedException {
//			System.out.println(context.getConfiguration().get("mapred.local.dir"));

			// System.out.println("key=" + key + ", val=" + val);
			JSONObject obj = (JSONObject) JSONValue.parse(val.toString());
			adId.set(obj.get("adId").toString());
			clickInfo.set("C " + obj.get("adId").toString());

			// Send (key, val) to the reducer
			context.write(adId, clickInfo);
		}
	}

	/**
	 * map: (LongWritable, Text) --> (LongWritable, Text) NOTE: Keys must
	 * implement WritableComparable; values must implement Writable.
	 */
	private static class ImpressionMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		private Text adId = new Text();
		private Text clickInfo = new Text();

		/*
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Mapper#map(KEYIN, VALUEIN,
		 * org.apache.hadoop.mapreduce.Mapper.Context)
		 */
		@Override
		public void map(LongWritable key, Text val, Context context)
				throws IOException, InterruptedException {
			// System.out.println("key=" + key + ", val=" + val);
			JSONObject obj = (JSONObject) JSONValue.parse(val.toString());
			adId.set(obj.get("adId").toString());
			clickInfo.set("I " + obj.get("impressionId").toString() + " "
					+ obj.get("referrer").toString());

			// Send (key, val) to the reducer
			context.write(adId, clickInfo);
		}
	}

	/**
	 * reduce: (LongWritable, Text[]) --> (LongWritable, Text)[]
	 */
	private static class ImpressionReducer extends
			Reducer<Text, Text, Text, Text> {
		private Text urlAndAdId = new Text();
		private Text numClicksAndImpressions = new Text();

		/*
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN,
		 * java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
		        int clicks = 0;
			int impressions = 0;

			for (Text value : values) {
				StringTokenizer tokenizer = new StringTokenizer(
						value.toString());
				if (!tokenizer.hasMoreTokens()) {
					throw new RuntimeException("No more tokens!");
				}
				String code = tokenizer.nextToken();
//				System.out.println("Code: " + code);
				if (code.equals("C")) {
				        clicks++;
				} else {
				        impressions++;
				}
			}

			urlAndAdId.set("[" + key.toString() + "]");
			numClicksAndImpressions.set("[" + Integer.toString(clicks)
						    + ", " + Integer.toString(impressions) + "]");
			context.write(urlAndAdId, numClicksAndImpressions);
		}
	}
}
