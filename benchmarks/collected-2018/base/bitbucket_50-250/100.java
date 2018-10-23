// https://searchcode.com/api/result/123696337/

package GenerateNgramPostings;

import java.io.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public class NgramPostings extends Configured implements Tool {
	private static final Logger	sLogger	= Logger.getLogger(NgramPostings.class);
	private static Text outputvalue = new Text();
	private static class MyMapper extends Mapper<LongWritable, Text, Text, Text> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			// Input: postid\tsentenceid\tsentence
			int N = Integer.parseInt(context.getConfiguration().get("WindowSize"));
			String[] line = ((Text) value).toString().split("\\t+\\s*", 2);
			//String sentence = line[2].toLowerCase().replaceAll("[^a-z0-9\\s]+"," ").replaceAll("\\b\\d+\\b","[-numeric-]");
			// numbers still present in final output since value is value, not sentence.
			//unnecessary since already cleaned in generate sentence ID. 
			outputvalue.set(line[0]);
			String[] tokens = line[1].split("\\s+");
			for (Integer i = 0; i < tokens.length - (N - 1); i++) {
				StringBuilder ngram = new StringBuilder();
				for (int j = 0; j < N; j++) {
					ngram.append(tokens[i + j] + " ");
				}
				context.write(new Text(ngram.toString().trim()), outputvalue);
			}
		}
	}

	private static class MyReducer extends Reducer<Text, Text, Text, Text> {
		private static Text	outputval	= new Text();

		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			int counter = 0;
			StringBuilder sb = new StringBuilder(); // for formatting output
			for (Text currentValue : values) {
				counter++;
				sb.append(currentValue.toString());
				sb.append("#");
				if (counter >= 500) { // Split up lines with more than 500 pairs. Avoids
															// memory erros in next phase.
					sb.append("\n" + key.toString() + "\t");
					counter = 0;
				}
			}
			if (counter >= 2) {
				outputval.set(sb.toString());
				context.write(key, outputval);
			}
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public NgramPostings() {}

	private static int printUsage() {
		System.out.println("usage: [input-path] [output-path] [window-size] [num-reducers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * Runs this tool.
	 */
	public int run(String[] args) throws Exception {
		if (args.length != 4) {
			printUsage();
			return -1;
		}

		String inputPath = args[0];
		String outputPath = args[1];
		int reduceTasks = Integer.parseInt(args[3]);
		Integer windowSize = Integer.parseInt(args[2]);

		sLogger.info("Tool: NgramPostings");
		sLogger.info(" - input path: " + inputPath);
		sLogger.info(" - output path: " + outputPath);
		sLogger.info(" - ngram window size: " + windowSize);
		sLogger.info(" - number of reducers: " + reduceTasks);

		Configuration conf = new Configuration();
		conf.set("WindowSize", windowSize.toString());
		Job job = new Job(conf, "NgramPostings");
		job.setJarByClass(NgramPostings.class);

		job.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(MyMapper.class);
		// job.setCombinerClass(MyCombiner.class );
		job.setReducerClass(MyReducer.class);

		// Delete the output directory if it exists already
		Path outputDir = new Path(outputPath);
		FileSystem.get(conf).delete(outputDir, true);

		long startTime = System.currentTimeMillis();
		job.waitForCompletion(true);
		sLogger.info("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the
	 * <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new NgramPostings(), args);

		System.exit(res);
	}
}

