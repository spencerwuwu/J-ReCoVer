// https://searchcode.com/api/result/64144739/

package fogbow.textgrounder;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * outputs a sorted list of all the words that have come after it in the
 * training corpus, and takes the following command-line arguments:
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
 * @author Andy Luong & Erik Skiles
 */
public class GetTwitterUsers extends Configured implements Tool {
	private static final Logger sLogger = Logger
			.getLogger(GetTwitterUsers.class);

	private static class GetTwitterUsersMapper extends
			Mapper<LongWritable, Text, Text, NullWritable> {

		Text out = new Text();

		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {

			try {
				JSONObject json = new JSONObject(value.toString());
				JSONObject user = json.getJSONObject("user");
				String id = user.getString("id_str");
				out.set(id);
				context.write(out, NullWritable.get());

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static class GetTwitterUsersReducer extends
			Reducer<Text, NullWritable, Text, NullWritable> {
		@Override
		public void reduce(Text key, Iterable<NullWritable> values,
				Context context) throws IOException, InterruptedException {
			context.write(key, NullWritable.get());
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public GetTwitterUsers() {
	}

	private static int printUsage() {
		System.out.println("usage: [input-path] [output-path] [num-reducers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * Runs this tool.
	 */
	public int run(String[] args) throws Exception {
		if (args.length != 3) {
			printUsage();
			return -1;
		}

		String inputPath = args[0];
		String outputPath = args[1];
		int reduceTasks = Integer.parseInt(args[2]);

		sLogger.info("Tool: GetTwitterUsers");
		sLogger.info(" - input path: " + inputPath);
		sLogger.info(" - output path: " + outputPath);
		sLogger.info(" - number of reducers: " + reduceTasks);

		Configuration conf = new Configuration();
		Job job = new Job(conf, "GetTwitterUsers");
		job.setJarByClass(GetTwitterUsers.class);

		job.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);
		job.setMapOutputValueClass(NullWritable.class);

		job.setMapperClass(GetTwitterUsersMapper.class);
		job.setCombinerClass(GetTwitterUsersReducer.class);
		job.setReducerClass(GetTwitterUsersReducer.class);

		// Delete the output directory if it exists already
		Path outputDir = new Path(outputPath);
		FileSystem.get(conf).delete(outputDir, true);

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
		int res = ToolRunner.run(new Configuration(), new GetTwitterUsers(),
				args);
		System.exit(res);
	}

}

