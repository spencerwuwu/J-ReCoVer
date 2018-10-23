// https://searchcode.com/api/result/134046356/

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

public class ClickRate extends Configured implements Tool {
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
		job.setMapOutputKeyClass(Text.class);

		job.setReducerClass(ImpressionReducer.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileOutputFormat.setOutputPath(job, new Path("temp"));

		if (!job.waitForCompletion(true)) {
			return 1;
		}

		Configuration conf2 = getConf();
		Job job2 = new Job(conf2, "aggregateClicks");
		job2.setJarByClass(ClickRate.class);

		job2.setInputFormatClass(KeyValueTextInputFormat.class);
		FileInputFormat.addInputPath(job2, new Path("temp"));
		job2.setMapOutputKeyClass(Text.class);

		job2.setReducerClass(ClickAggregator.class);
		FileOutputFormat.setOutputPath(job2, new Path(args[2]));

		if (!job2.waitForCompletion(true)) {
			return 1;
		}

		System.out.println(job.getCounters());
		System.out.println(job2.getCounters());

		return 0;
	}

	/**
	 * map: (LongWritable, Text) --> (LongWritable, Text) NOTE: Keys must
	 * implement WritableComparable; values must implement Writable.
	 */
	private static class ClickMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		private Text impressionId = new Text();
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
			impressionId.set(obj.get("impressionId").toString());
			clickInfo.set("C " + obj.get("adId").toString());

			// Send (key, val) to the reducer
			context.write(impressionId, clickInfo);
		}
	}

	/**
	 * map: (LongWritable, Text) --> (LongWritable, Text) NOTE: Keys must
	 * implement WritableComparable; values must implement Writable.
	 */
	private static class ImpressionMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		private Text impressionId = new Text();
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
			impressionId.set(obj.get("impressionId").toString());
			clickInfo.set("I " + obj.get("adId").toString() + " "
					+ obj.get("referrer").toString());

			// Send (key, val) to the reducer
			context.write(impressionId, clickInfo);
		}
	}

	/**
	 * reduce: (LongWritable, Text[]) --> (LongWritable, Text)[]
	 */
	private static class ImpressionReducer extends
			Reducer<Text, Text, Text, BooleanWritable> {
		private Text adAndUrl = new Text();
		private BooleanWritable clicked = new BooleanWritable();

		/*
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN,
		 * java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String clickAd = null, impressionAd = null, referrer = null;

			for (Text value : values) {
				StringTokenizer tokenizer = new StringTokenizer(
						value.toString());
				if (!tokenizer.hasMoreTokens()) {
					throw new RuntimeException("No more tokens!");
				}
				String code = tokenizer.nextToken();
//				System.out.println("Code: " + code);
				if (code.equals("C")) {
					if (clickAd != null) {
						throw new RuntimeException(
								"Two clicks for the same impression id??");
					}
					if (!tokenizer.hasMoreTokens()) {
						throw new RuntimeException("No more tokens!");
					}
					clickAd = tokenizer.nextToken();
				} else {
					if (impressionAd != null) {
						throw new RuntimeException(
								"Two impressions for the same impression ID??");
					}
					if (!tokenizer.hasMoreTokens()) {
						throw new RuntimeException("No more tokens!");
					}
					impressionAd = tokenizer.nextToken();
					if (!tokenizer.hasMoreTokens()) {
						throw new RuntimeException("No more tokens!");
					}
					referrer = tokenizer.nextToken();
//					System.out.println("ID/Referrer: " + impressionAd + " "
//							+ referrer);
				}
			}

			if (impressionAd == null) {
				// Skip this click since we have no URL information (shouldn't
				// happen with the full dataset, I think?)
				context.getCounter("laura", "noUrlInfo").increment(1);
				return;
			}

			if (clickAd != null && !clickAd.equals(impressionAd)) {
				throw new RuntimeException(
						"More than two ads for the same impression ID??");
			}

			adAndUrl.set("[" + referrer + ", " + impressionAd + "]");
			clicked.set(clickAd != null);
			context.write(adAndUrl, clicked);
		}
	}

	private static class ClickAggregator extends
			Reducer<Text, Text, Text, DoubleWritable> {
		private DoubleWritable clickRatio = new DoubleWritable();

		/*
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN,
		 * java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
		 */
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			int numClicks = 0, numImpressions = 0;
			for (Text clicked : values) {
				if (clicked.toString().equals("true")) {
					numClicks++;
				}
				numImpressions++;
			}
			clickRatio.set(((double) numClicks) / numImpressions);
			context.write(key, clickRatio);
		}
	}
}
