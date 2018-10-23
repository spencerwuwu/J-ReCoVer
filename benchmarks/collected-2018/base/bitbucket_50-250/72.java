// https://searchcode.com/api/result/103301179/

package twools;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

/**
 * <p>
 * command-line arguments:
 * </p>
 * 
 * <ul>
 * <li>[input-path] input path</li>
 * <li>[output-path] output path</li>
 * <li>[num-reducers] number of reducers</li>
 * </ul>
 * 
 * 
 * Taken and adapted from edu.umd.cloud9.demo.CloudNineWordCount
 * 
 * @author Andy Luong
 */
public class PreprocessTweetsJSON extends Configured implements Tool {
	private static final Logger sLogger = Logger
			.getLogger(PreprocessTweetsJSON.class);

	private static class PreprocessTweetsJSONMapper extends
			Mapper<LongWritable, Text, Text, Text> {

		Text user = new Text();
		Text data = new Text();

		private static boolean twokenizeFlag;
		private static float diagSize;

		@Override
		public void setup(Context context) {
			twokenizeFlag = context.getConfiguration().getBoolean("twokenize",
					false);
			diagSize = context.getConfiguration().getFloat("diagSize",
					(float) 0.5);
		}

		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {

			try {
				Status status = Status.fromJson(value.toString(), diagSize);
				if (status.hasGeo()) {

					String text = status.getText();

					if (twokenizeFlag) {
						text = Twokenize.tokenize(status.getText()).mkString(
								" ");

					}

					user.set(status.getUserId());
					data.set(status.getCreatedAt() + "\t" + status.getCoord()
							+ "\t" + text + "\t" + status.getUserMentions());

					context.write(user, data);
				}

			} catch (Exception e) {
				System.err
						.println("Bad JSON Input. Exception: " + e.toString());
				System.err.println(value.toString());
			}
		}
	}

	// Reducer will
	private static class PreprocessTweetsJSONReducer extends
			Reducer<Text, Text, Text, Text> {
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {

			for (Text val : values)
				context.write(key, val);
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public PreprocessTweetsJSON() {
	}

	private static int printUsage() {
		System.out
				.println("usage: [input-path] [output-path] [twokenize text (true or false)] [Bounding-box diagonal length] [num-reducers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * Runs this tool.
	 */
	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 5) {
			printUsage();
			return -1;
		}

		String inputPath = args[0];
		String outputPath = args[1];
		String twokenizeFlag = args[2];
		float diagSize = Float.parseFloat(args[3]);
		int reduceTasks = Integer.parseInt(args[4]);

		Configuration conf = new Configuration();
		conf.setBoolean("twokenize", Boolean.valueOf(twokenizeFlag));
		conf.setFloat("diagSize", diagSize);

		Job job = new Job(conf, "PreprocessTweetsJSON");
		job.setJarByClass(PreprocessTweetsJSON.class);

		job.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(PreprocessTweetsJSONMapper.class);
		job.setReducerClass(PreprocessTweetsJSONReducer.class);

		// Delete the output directory if it exists already
		Path outputDir = new Path(outputPath);
		FileSystem.get(conf).delete(outputDir, true);

		sLogger.info("Tool: PreprocessTweetsJSON");
		sLogger.info(" - input path: " + inputPath);
		sLogger.info(" - output path: " + outputPath);
		sLogger.info(" - twokenize: " + twokenizeFlag);
		sLogger.info(" - number of reducers: " + reduceTasks);

		long startTime = System.currentTimeMillis();
		job.waitForCompletion(true);
		sLogger.info("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the
	 * <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(),
				new PreprocessTweetsJSON(), args);
		System.exit(res);
	}

}

